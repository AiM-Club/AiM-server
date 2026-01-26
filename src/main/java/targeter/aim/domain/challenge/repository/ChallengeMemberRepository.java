package targeter.aim.domain.challenge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import targeter.aim.domain.challenge.entity.Challenge;
import targeter.aim.domain.challenge.entity.ChallengeMember;
import targeter.aim.domain.challenge.entity.ChallengeMemberId;
import targeter.aim.domain.challenge.entity.MemberRole;
import targeter.aim.domain.user.entity.User;

import java.util.List;
import java.util.Optional;

public interface ChallengeMemberRepository extends JpaRepository<ChallengeMember, ChallengeMemberId> {

    List<ChallengeMember> findAllById_Challenge(Challenge challenge);

    Optional<ChallengeMember> findFirstById_ChallengeAndRole(Challenge challenge, MemberRole role);

    long countById_ChallengeAndRole(Challenge challenge, MemberRole role);

    boolean existsById_ChallengeAndId_User(Challenge challenge, User user);

    void deleteAllById_Challenge(Challenge challenge);
}
