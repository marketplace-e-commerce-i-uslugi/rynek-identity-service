package pl.rynek.identity.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import pl.rynek.identity.MutableClock;
import pl.rynek.identity.TestcontainersConfig;

/** Rejestracja, logowanie, sesje i profil na prawdziwym PostgreSQL. */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfig.class, MutableClock.Config.class})
class AuthFlowTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    MutableClock clock;

    @Test
    void registerReturnsTokensAndProfileIsAvailable() throws Exception {
        String email = uniqueEmail();
        String tokens = register("{\"email\":\"" + email.toUpperCase() + "\",\"password\":\"tajnehaslo\",\"displayName\":\"Dom i Kawa\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andReturn().getResponse().getContentAsString();

        mvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer(tokens)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.displayName").value("Dom i Kawa"))
                .andExpect(jsonPath("$.initials").value("DK"))
                .andExpect(jsonPath("$.roles", contains("BUYER")))
                .andExpect(jsonPath("$.onboarded").value(false))
                .andExpect(jsonPath("$.phoneVerified").value(false));
    }

    @Test
    void duplicateEmailIs409IgnoringCase() throws Exception {
        String email = uniqueEmail();
        register(body(email)).andExpect(status().isCreated());
        register(body(email.toUpperCase()))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Konto z tym adresem e-mail już istnieje."));
    }

    @Test
    void invalidRegistrationIs400WithPolishDetail() throws Exception {
        register("{\"email\":\"" + uniqueEmail() + "\",\"password\":\"krotkie\",\"displayName\":\"Anna\"}")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Hasło musi mieć od 8 do 72 znaków."))
                .andExpect(jsonPath("$.errors.password").exists());
        register("{\"password\":\"tajnehaslo\",\"displayName\":\"Anna\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Podaj e-mail albo numer telefonu."));
    }

    @Test
    void loginWithEmailOrPhone() throws Exception {
        String email = uniqueEmail();
        register(body(email)).andExpect(status().isCreated());
        login(email, "tajnehaslo").andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").exists());

        String digits = String.valueOf(600_000_000L + (long) (Math.random() * 99_999_999L));
        String phone = digits.substring(0, 3) + " " + digits.substring(3, 6) + " " + digits.substring(6);
        register("{\"phone\":\"" + phone + "\",\"password\":\"tajnehaslo\",\"displayName\":\"Jan Telefon\"}")
                .andExpect(status().isCreated());
        login("+48" + digits, "tajnehaslo").andExpect(status().isOk());
    }

    @Test
    void wrongPasswordAndUnknownLoginGiveSame401() throws Exception {
        String email = uniqueEmail();
        register(body(email)).andExpect(status().isCreated());
        login(email, "zlehaslo1").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Nieprawidłowy login lub hasło."));
        login(uniqueEmail(), "tajnehaslo").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Nieprawidłowy login lub hasło."));
    }

    @Test
    void refreshRotatesToken() throws Exception {
        String tokens = register(body(uniqueEmail())).andReturn().getResponse().getContentAsString();
        String first = JsonPath.read(tokens, "$.refreshToken");

        String second = JsonPath.read(refresh(first).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.refreshToken");
        assertThat(second).isNotEqualTo(first);
        refresh(second).andExpect(status().isOk());
    }

    @Test
    void reusingRotatedRefreshTokenEndsSession() throws Exception {
        String tokens = register(body(uniqueEmail())).andReturn().getResponse().getContentAsString();
        String stolen = JsonPath.read(tokens, "$.refreshToken");
        String current = JsonPath.read(refresh(stolen).andReturn().getResponse().getContentAsString(), "$.refreshToken");

        clock.advance(Duration.ofMinutes(1));
        refresh(stolen).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Sesja wygasła. Zaloguj się ponownie."));
        refresh(current).andExpect(status().isUnauthorized());
    }

    @Test
    void reuseRightAfterRotationIsTreatedAsSecondTab() throws Exception {
        String tokens = register(body(uniqueEmail())).andReturn().getResponse().getContentAsString();
        String shared = JsonPath.read(tokens, "$.refreshToken");
        String current = JsonPath.read(refresh(shared).andReturn().getResponse().getContentAsString(), "$.refreshToken");

        refresh(shared).andExpect(status().isUnauthorized());
        refresh(current).andExpect(status().isOk());
    }

    @Test
    void concurrentRefreshesWithSameTokenDoNotConflict() throws Exception {
        String tokens = register(body(uniqueEmail())).andReturn().getResponse().getContentAsString();
        String shared = JsonPath.read(tokens, "$.refreshToken");
        Set<Integer> statuses = ConcurrentHashMap.newKeySet();
        Set<String> issued = ConcurrentHashMap.newKeySet();
        CountDownLatch start = new CountDownLatch(1);
        Callable<Void> tab = () -> {
            start.await();
            var response = refresh(shared).andReturn().getResponse();
            statuses.add(response.getStatus());
            if (response.getStatus() == 200) {
                issued.add(JsonPath.read(response.getContentAsString(), "$.refreshToken"));
            }
            return null;
        };
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            var first = pool.submit(tab);
            var second = pool.submit(tab);
            start.countDown();
            first.get();
            second.get();
        } finally {
            pool.shutdown();
        }

        // Jedna karta odświeża, druga dostaje 401 (bez 409/500), a sesja trwa.
        assertThat(statuses).containsExactlyInAnyOrder(200, 401);
        refresh(issued.iterator().next()).andExpect(status().isOk());
    }

    @Test
    void invalidAccessTokenIsIgnoredOnPublicAuthEndpoints() throws Exception {
        // Frontend dołącza wygasły access token także do /auth/refresh; nie może to blokować odnowienia sesji.
        String tokens = register(body(uniqueEmail())).andReturn().getResponse().getContentAsString();
        mvc.perform(post("/api/v1/auth/refresh").header(HttpHeaders.AUTHORIZATION, "Bearer zepsuty.token.xyz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + JsonPath.read(tokens, "$.refreshToken") + "\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer zepsuty.token.xyz"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutEndsOnlyCurrentSession() throws Exception {
        String email = uniqueEmail();
        String first = register(body(email)).andReturn().getResponse().getContentAsString();
        String second = login(email, "tajnehaslo").andReturn().getResponse().getContentAsString();

        mvc.perform(post("/api/v1/auth/logout").header(HttpHeaders.AUTHORIZATION, bearer(first)))
                .andExpect(status().isNoContent());

        refresh(JsonPath.read(first, "$.refreshToken")).andExpect(status().isUnauthorized());
        refresh(JsonPath.read(second, "$.refreshToken")).andExpect(status().isOk());
    }

    @Test
    void protectedEndpointsNeedValidToken() throws Exception {
        mvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401));

        String tokens = register(body(uniqueEmail())).andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(tokens, "$.accessToken");
        String tampered = token.substring(0, token.length() - 4) + "AAAA";
        mvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void onboardingRolesReachTokenAfterRefresh() throws Exception {
        String tokens = register(body(uniqueEmail())).andReturn().getResponse().getContentAsString();

        mvc.perform(put("/api/v1/users/me/roles").header(HttpHeaders.AUTHORIZATION, bearer(tokens))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"roles\":[\"MERCHANT\",\"BUYER\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", contains("BUYER", "MERCHANT")))
                .andExpect(jsonPath("$.onboarded").value(true));

        String refreshed = refresh(JsonPath.read(tokens, "$.refreshToken")).andReturn().getResponse().getContentAsString();
        SignedJWT jwt = SignedJWT.parse(JsonPath.<String>read(refreshed, "$.accessToken"));
        assertThat(jwt.getJWTClaimsSet().getStringListClaim("roles")).containsExactly("BUYER", "MERCHANT");
    }

    @Test
    void adminRoleCannotBeSelfAssigned() throws Exception {
        String tokens = register(body(uniqueEmail())).andReturn().getResponse().getContentAsString();
        mvc.perform(put("/api/v1/users/me/roles").header(HttpHeaders.AUTHORIZATION, bearer(tokens))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"roles\":[\"ADMIN\"]}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/users/me/roles").header(HttpHeaders.AUTHORIZATION, bearer(tokens))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"roles\":[\"KROL\"]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateMeChangesProfile() throws Exception {
        String tokens = register(body(uniqueEmail())).andReturn().getResponse().getContentAsString();
        mvc.perform(patch("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer(tokens))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"displayName\":\"Elektro Market\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Elektro Market"))
                .andExpect(jsonPath("$.initials").value("EM"));
    }

    @Test
    void jwksVerifiesIssuedAccessToken() throws Exception {
        String tokens = register(body(uniqueEmail())).andReturn().getResponse().getContentAsString();
        SignedJWT jwt = SignedJWT.parse(JsonPath.<String>read(tokens, "$.accessToken"));

        String jwks = mvc.perform(get("/internal/auth/jwks")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        RSAKey key = JWKSet.parse(jwks).getKeyByKeyId(jwt.getHeader().getKeyID()).toRSAKey();

        assertThat(key.isPrivate()).isFalse();
        assertThat(jwt.verify(new RSASSAVerifier(key))).isTrue();
        assertThat(jwt.getJWTClaimsSet().getIssuer()).isEqualTo("rynek-identity");
        assertThat(jwt.getJWTClaimsSet().getStringListClaim("roles")).isEqualTo(List.of("BUYER"));
        assertThat(jwt.getJWTClaimsSet().getStringClaim("sid")).isNotBlank();

        mvc.perform(get("/internal/users/" + jwt.getJWTClaimsSet().getSubject()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(jwt.getJWTClaimsSet().getSubject()));
    }

    @Test
    void channelServiceLinksChannelToSellerAccount() throws Exception {
        String tokens = register(body(uniqueEmail())).andReturn().getResponse().getContentAsString();
        String userId = SignedJWT.parse(JsonPath.<String>read(tokens, "$.accessToken")).getJWTClaimsSet().getSubject();
        String channelId = UUID.randomUUID().toString();
        String link = "{\"channelId\":\"" + channelId + "\",\"channelHandle\":\"elektromarket\"}";

        linkChannel(userId, link)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Kanał może założyć usługodawca albo merchant. Wybierz rolę w ustawieniach konta."));

        mvc.perform(put("/api/v1/users/me/roles").header(HttpHeaders.AUTHORIZATION, bearer(tokens))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"roles\":[\"MERCHANT\"]}"))
                .andExpect(status().isOk());
        linkChannel(userId, link).andExpect(status().isOk());
        linkChannel(userId, link).andExpect(status().isOk());
        linkChannel(userId, "{\"channelId\":\"" + UUID.randomUUID() + "\",\"channelHandle\":\"inny\"}")
                .andExpect(status().isConflict());

        mvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer(tokens)))
                .andExpect(jsonPath("$.channelId").value(channelId))
                .andExpect(jsonPath("$.channelHandle").value("elektromarket"));
    }

    @Test
    void notYetImplementedEndpointsStillReturn501ForLoggedInUser() throws Exception {
        String tokens = register(body(uniqueEmail())).andReturn().getResponse().getContentAsString();
        mvc.perform(get("/api/v1/users/me/addresses").header(HttpHeaders.AUTHORIZATION, bearer(tokens)))
                .andExpect(status().isNotImplemented());
    }

    private ResultActions register(String json) throws Exception {
        return mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions login(String login, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"" + login + "\",\"password\":\"" + password + "\"}"));
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"));
    }

    private ResultActions linkChannel(String userId, String json) throws Exception {
        return mvc.perform(put("/internal/users/" + userId + "/channel").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String body(String email) {
        return "{\"email\":\"" + email + "\",\"password\":\"tajnehaslo\",\"displayName\":\"Anna Nowak\"}";
    }

    private static String bearer(String tokensJson) {
        return "Bearer " + JsonPath.read(tokensJson, "$.accessToken");
    }

    private static String uniqueEmail() {
        return "u" + UUID.randomUUID().toString().substring(0, 8) + "@example.pl";
    }
}
