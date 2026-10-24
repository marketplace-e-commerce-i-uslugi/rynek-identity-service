package pl.rynek.identity.config;

import java.time.Clock;
import java.util.Arrays;
import java.util.stream.Stream;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import pl.rynek.identity.auth.JwtKeys;
import pl.rynek.identity.auth.JwtProperties;
import pl.rynek.identity.common.ProblemJson;

/**
 * identity wystawia tokeny i sam je sprawdza (Resource Server z własnym kluczem publicznym), więc działa poprawnie
 * także wtedy, gdy gateway ma {@code JWT_ENFORCE=false}. Publiczne są endpointy {@code pub} z README i {@code /internal/**}
 * (gateway ich nie wystawia).
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    /** Endpointy {@code pub} z README (wszystkie to POST). */
    private static final String[] PUBLIC_AUTH_POSTS = {"/api/v1/auth/register", "/api/v1/auth/login",
            "/api/v1/auth/oauth/google", "/api/v1/auth/refresh", "/api/v1/auth/password/forgot", "/api/v1/auth/password/reset"};

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // Bezstanowe API z tokenem Bearer: bez sesji i ciasteczek, więc bez CSRF.
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(HttpMethod.POST, PUBLIC_AUTH_POSTS).permitAll()
                        .requestMatchers("/internal/**").permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(ignoreTokenOnPublicEndpoints())
                        .jwt(jwt -> {
                        })
                        .authenticationEntryPoint(unauthorized()))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(unauthorized())
                        .accessDeniedHandler(forbidden()))
                .build();
    }

    @Bean
    JwtKeys jwtKeys(JwtProperties properties) {
        return JwtKeys.load(properties.privateKey());
    }

    @Bean
    JwtEncoder jwtEncoder(JwtKeys keys) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(keys.signingKey())));
    }

    @Bean
    JwtDecoder jwtDecoder(JwtKeys keys, JwtProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(keys.publicKey())
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));
        return decoder;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    /**
     * Na endpointach publicznych nagłówek {@code Authorization} nie jest sprawdzany. Inaczej wygasły access token,
     * który frontend nadal dołącza, dawałby 401 na {@code POST /auth/refresh}, czyli tam, gdzie się go odnawia.
     */
    private static BearerTokenResolver ignoreTokenOnPublicEndpoints() {
        DefaultBearerTokenResolver bearer = new DefaultBearerTokenResolver();
        RequestMatcher publicEndpoints = new OrRequestMatcher(Stream.concat(
                Arrays.stream(PUBLIC_AUTH_POSTS).map(path -> PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, path)),
                Stream.of(PathPatternRequestMatcher.withDefaults().matcher("/internal/**"))).toList());
        return request -> publicEndpoints.matches(request) ? null : bearer.resolve(request);
    }

    private static AuthenticationEntryPoint unauthorized() {
        return (request, response, ex) -> {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            ProblemJson.write(request, response, HttpStatus.UNAUTHORIZED, "Zaloguj się, aby kontynuować.");
        };
    }

    private static AccessDeniedHandler forbidden() {
        return (request, response, ex) ->
                ProblemJson.write(request, response, HttpStatus.FORBIDDEN, "Nie masz uprawnień do tej operacji.");
    }
}
