package targeter.aim.domain.file.repository;
import targeter.aim.domain.file.entity.ChallengeImage;
import java.util.Optional;

public interface AttachedFileQueryRepository {

    Optional<ChallengeImage> findChallengeImageByChallengeId(Long challengeId);

}