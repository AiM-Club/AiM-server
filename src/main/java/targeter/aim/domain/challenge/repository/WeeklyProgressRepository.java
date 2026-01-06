package targeter.aim.domain.challenge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import targeter.aim.domain.challenge.entity.Challenge;
import targeter.aim.domain.challenge.entity.WeeklyProgress;

import java.util.Optional;

public interface WeeklyProgressRepository extends JpaRepository<WeeklyProgress, Long> {

    Optional<WeeklyProgress> findByChallengeAndWeekNumber(Challenge challenge, Integer weekNumber);
}
