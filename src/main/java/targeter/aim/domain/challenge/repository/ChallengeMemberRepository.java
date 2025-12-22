package targeter.aim.domain.challenge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import targeter.aim.domain.challenge.entity.ChallengeMember;
import targeter.aim.domain.challenge.entity.ChallengeMemberId;

public interface ChallengeMemberRepository extends JpaRepository<ChallengeMember, ChallengeMemberId> {
}
