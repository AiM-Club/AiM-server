package targeter.aim.domain.challenge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import targeter.aim.domain.challenge.entity.Challenge;
import targeter.aim.domain.challenge.entity.ChallengeLiked;
import targeter.aim.domain.challenge.entity.ChallengeLikedId;
import targeter.aim.domain.user.entity.User;

public interface ChallengeLikedRepository extends JpaRepository<ChallengeLiked, ChallengeLikedId> {

    boolean existsByUserAndChallenge(User user, Challenge challenge);

    void deleteByUserAndChallenge(User user, Challenge challenge);
}
