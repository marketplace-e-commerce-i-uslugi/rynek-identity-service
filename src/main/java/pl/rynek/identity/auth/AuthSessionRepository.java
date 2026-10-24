package pl.rynek.identity.auth;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

interface AuthSessionRepository extends JpaRepository<AuthSession, UUID> {

    /** {@code SELECT ... FOR UPDATE}: wyklucza dwie równoległe rotacje tej samej sesji. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from AuthSession s where s.refreshTokenHash = :hash")
    Optional<AuthSession> findForRefreshByTokenHash(String hash);
}
