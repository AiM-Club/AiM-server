package targeter.aim.domain.auth.token.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import targeter.aim.domain.auth.token.entity.RefreshToken;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    Optional<RefreshToken> findByUuid(String uuid);
    Optional<RefreshToken> deleteByUuid(String uuid);
    void deleteByUserKey(String userKey);
}