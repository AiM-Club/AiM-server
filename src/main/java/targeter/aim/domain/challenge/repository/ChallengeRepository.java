package targeter.aim.domain.challenge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import targeter.aim.domain.challenge.entity.Challenge;
import targeter.aim.domain.user.entity.User;

import java.time.LocalDate;
import java.util.Optional;

public interface ChallengeRepository extends JpaRepository<Challenge, Long> {

    Optional<Challenge> findByHostAndNameAndStartedAt(User host, String name, LocalDate startedAt);
}
