package targeter.aim.domain.challenge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import targeter.aim.domain.challenge.entity.ChallengeRequest;

public interface ChallengeRequestRepository extends JpaRepository<ChallengeRequest, Long> {
}
