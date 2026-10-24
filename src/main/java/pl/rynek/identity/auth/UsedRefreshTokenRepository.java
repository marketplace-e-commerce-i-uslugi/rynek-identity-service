package pl.rynek.identity.auth;

import org.springframework.data.jpa.repository.JpaRepository;

interface UsedRefreshTokenRepository extends JpaRepository<UsedRefreshToken, String> {
}
