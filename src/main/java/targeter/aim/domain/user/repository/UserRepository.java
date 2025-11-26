package targeter.aim.domain.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import targeter.aim.domain.user.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLoginId(String loginId);
    boolean existsByLoginId(String id);
}
