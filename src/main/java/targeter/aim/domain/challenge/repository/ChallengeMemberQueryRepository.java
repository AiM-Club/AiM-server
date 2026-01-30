package targeter.aim.domain.challenge.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import targeter.aim.domain.challenge.entity.ChallengeMode;
import targeter.aim.domain.challenge.entity.ChallengeResult;
import targeter.aim.domain.user.entity.User;

import static targeter.aim.domain.challenge.entity.QChallenge.challenge;
import static targeter.aim.domain.challenge.entity.QChallengeMember.challengeMember;

@Repository
@RequiredArgsConstructor
public class ChallengeMemberQueryRepository {

    private final JPAQueryFactory queryFactory;

    public int countByUserAndResult(User user, ChallengeResult result) {
        Long count = queryFactory
                .select(challengeMember.count())
                .from(challengeMember)
                .where(
                        challengeMember.id.user.eq(user),
                        challengeMember.result.eq(result)
                )
                .fetchOne();
        return count != null ? count.intValue() : 0;
    }

    public int countFinishedChallenges(User user) {
        Long count = queryFactory
                .select(challengeMember.count())
                .from(challengeMember)
                .where(
                        challengeMember.id.user.eq(user),
                        challengeMember.result.isNotNull()
                )
                .fetchOne();

        return count != null ? count.intValue() : 0;
    }

    public int countFinishedChallengesByMode(User user, ChallengeMode mode) {
        Long count = queryFactory
                .select(challengeMember.count())
                .from(challengeMember)
                .join(challengeMember.id.challenge, challenge)
                .where(
                        challengeMember.id.user.eq(user),
                        challenge.mode.eq(mode),
                        challengeMember.result.isNotNull()
                )
                .fetchOne();

        return count != null ? count.intValue() : 0;
    }

    public int countByUserAndResultAndMode(User user, ChallengeResult result, ChallengeMode mode) {
        Long count = queryFactory
                .select(challengeMember.count())
                .from(challengeMember)
                .join(challengeMember.id.challenge, challenge)
                .where(
                        challengeMember.id.user.eq(user),
                        challenge.mode.eq(mode),
                        challengeMember.result.eq(result)
                )
                .fetchOne();

        return count != null ? count.intValue() : 0;
    }
}