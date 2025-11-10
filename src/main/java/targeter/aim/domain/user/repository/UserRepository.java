package targeter.aim.domain.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import targeter.aim.domain.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
}
