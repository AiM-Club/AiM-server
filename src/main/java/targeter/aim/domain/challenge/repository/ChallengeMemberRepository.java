package targeter.aim.domain.challenge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import targeter.aim.domain.challenge.entity.Challenge;
import targeter.aim.domain.challenge.entity.ChallengeMember;
import targeter.aim.domain.challenge.entity.ChallengeMemberId;
import targeter.aim.domain.user.entity.User;

import java.util.List;

public interface ChallengeMemberRepository extends JpaRepository<ChallengeMember, ChallengeMemberId> {

    List<ChallengeMember> findAllById_Challenge(Challenge challenge);

    boolean existsById_ChallengeAndId_User(Challenge challenge, User user);
}
