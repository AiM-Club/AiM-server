package targeter.aim.domain.challenge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import targeter.aim.domain.challenge.entity.ApplyStatus;
import targeter.aim.domain.challenge.entity.Challenge;
import targeter.aim.domain.challenge.entity.ChallengeRequest;
import targeter.aim.domain.user.entity.User;

public interface ChallengeRequestRepository extends JpaRepository<ChallengeRequest, Long> {

    boolean existsByChallengeAndRequesterAndApplyStatus(Challenge challenge, User user, ApplyStatus applyStatus);
}
