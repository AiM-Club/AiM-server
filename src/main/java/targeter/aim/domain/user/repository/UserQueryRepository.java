package targeter.aim.domain.user.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import targeter.aim.domain.challenge.entity.ChallengeMode;

import java.util.List;

import static targeter.aim.domain.challenge.entity.QChallenge.challenge;
import static targeter.aim.domain.challenge.entity.QWeeklyProgress.weeklyProgress;
import static targeter.aim.domain.label.entity.QField.field;
import static targeter.aim.domain.label.entity.QTag.tag;
import static targeter.aim.domain.user.entity.QUser.user;

@Repository
@RequiredArgsConstructor
public class UserQueryRepository {

    private final JPAQueryFactory queryFactory;

    public record Record(long attempt, long success) {}

    public List<String> findUserTagNames(Long userId) {
        return queryFactory
                .select(tag.name)
                .from(user)
                .join(user.tags, tag)
                .where(user.id.eq(userId))
                .orderBy(tag.name.asc())
                .fetch();
    }

    public List<String> findUserFieldNames(Long userId) {
        return queryFactory
                .select(field.name)
                .from(user)
                .join(user.fields, field)
                .where(user.id.eq(userId))
                .orderBy(field.name.asc())
                .fetch();
    }

    public Record calcOverallRecord(Long userId) {
        return calcRecordByMode(userId, null);
    }

    public Record calcSoloRecord(Long userId) {
        return calcRecordByMode(userId, ChallengeMode.SOLO);
    }

    public Record calcVsRecord(Long userId) {
        return calcRecordByMode(userId, ChallengeMode.VS);
    }

    private Record calcRecordByMode(Long userId, ChallengeMode mode) {

        BooleanBuilder where = new BooleanBuilder();
        where.and(weeklyProgress.user.id.eq(userId));

        if (mode != null) {
            where.and(challenge.mode.eq(mode));
        }
        Long attempt = queryFactory
                .select(weeklyProgress.challenge.id.countDistinct())
                .from(weeklyProgress)
                .join(weeklyProgress.challenge, challenge)
                .where(where)
                .fetchOne();

        long attemptCount = attempt == null ? 0L : attempt;

        NumberExpression<Integer> completeCnt = new CaseBuilder()
                .when(weeklyProgress.isComplete.isTrue()).then(1)
                .otherwise(0)
                .sum();

        NumberExpression<Long> totalCnt = weeklyProgress.id.count();

        NumberExpression<Double> ratio = Expressions.numberTemplate(
                Double.class,
                "({0} * 1.0) / {1}",
                completeCnt,
                totalCnt
        );
        List<Long> successChallengeIds = queryFactory
                .select(weeklyProgress.challenge.id)
                .from(weeklyProgress)
                .join(weeklyProgress.challenge, challenge)
                .where(where)
                .groupBy(weeklyProgress.challenge.id)
                .having(ratio.goe(0.7))
                .fetch();

        long successCount = (successChallengeIds == null) ? 0L : successChallengeIds.size();

        return new Record(attemptCount, successCount);
    }
}
