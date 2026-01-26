package targeter.aim.domain.challenge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import targeter.aim.domain.challenge.entity.Challenge;
import targeter.aim.domain.challenge.entity.WeeklyProgress;
import targeter.aim.domain.user.entity.User;

import java.util.List;
import java.util.Optional;

public interface WeeklyProgressRepository extends JpaRepository<WeeklyProgress, Long> {

    Optional<WeeklyProgress> findByChallengeAndUserAndWeekNumber(Challenge challenge, User user, Integer weekNumber);

    Optional<WeeklyProgress> findByChallengeAndWeekNumber(Challenge challenge, Integer weekNumber);

    long countByChallengeAndUserAndIsCompleteTrue(Challenge challenge, User user);

    List<WeeklyProgress> findAllByChallengeAndUser(Challenge challenge, User user);

    boolean existsByChallengeAndUser(Challenge challenge, User user);

    void deleteAllByChallenge(Challenge challenge);
}