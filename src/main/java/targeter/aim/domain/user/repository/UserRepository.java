package targeter.aim.domain.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import targeter.aim.domain.user.entity.SocialLogin;
import targeter.aim.domain.user.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByLoginId(String loginId);

    Optional<User> findByEmail(String email);

    Optional<User> findBySocialLoginAndSocialId(SocialLogin socialLogin, String socialId);

    boolean existsByLoginId(String id);

    boolean existsByNickname(String nickname);

    boolean existsByEmail(String email);
}