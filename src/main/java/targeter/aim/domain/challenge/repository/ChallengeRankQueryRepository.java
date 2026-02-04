package targeter.aim.domain.challenge.repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import targeter.aim.domain.challenge.entity.ChallengeMode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static targeter.aim.domain.challenge.entity.QChallenge.challenge;
import static targeter.aim.domain.challenge.entity.QWeeklyProgress.weeklyProgress;

@Repository
@RequiredArgsConstructor
public class ChallengeRankQueryRepository {

    private final JPAQueryFactory queryFactory;

    public record Record(long attempt, long success) {}

    /**
     * challenge 1개 단위로 유저 성공률(complete/total) >= 0.7 이면 성공으로 카운트.
     * attempt = 참여한 챌린지 수
     * success = 성공 챌린지 수
     */
    public Map<Long, Record> calcRecordByUsers(List<Long> userIds, ChallengeMode mode) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        List<Tuple> attemptRows = queryFactory
                .select(weeklyProgress.user.id, weeklyProgress.challenge.id.countDistinct())
                .from(weeklyProgress)
                .join(weeklyProgress.challenge, challenge)
                .where(
                        weeklyProgress.user.id.in(userIds),
                        mode == null ? null : challenge.mode.eq(mode)
                )
                .groupBy(weeklyProgress.user.id)
                .fetch();

        Map<Long, Long> attemptMap = new HashMap<>();
        for (Tuple t : attemptRows) {
            Long uid = t.get(weeklyProgress.user.id);
            Long cnt = t.get(weeklyProgress.challenge.id.countDistinct());
            attemptMap.put(uid, cnt == null ? 0L : cnt);
        }

        // completeCnt / totalCnt (challenge 단위)
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

        List<Tuple> successRows = queryFactory
                .select(weeklyProgress.user.id, weeklyProgress.challenge.id.countDistinct())
                .from(weeklyProgress)
                .join(weeklyProgress.challenge, challenge)
                .where(
                        weeklyProgress.user.id.in(userIds),
                        mode == null ? null : challenge.mode.eq(mode),
                        Expressions.list(weeklyProgress.user.id, weeklyProgress.challenge.id).in(
                                JPAExpressions
                                        .select(weeklyProgress.user.id, weeklyProgress.challenge.id)
                                        .from(weeklyProgress)
                                        .join(weeklyProgress.challenge, challenge)
                                        .where(
                                                weeklyProgress.user.id.in(userIds),
                                                mode == null ? null : challenge.mode.eq(mode)
                                        )
                                        .groupBy(weeklyProgress.user.id, weeklyProgress.challenge.id)
                                        .having(ratio.goe(0.7))
                        )
                )
                .groupBy(weeklyProgress.user.id)
                .fetch();

        Map<Long, Long> successMap = new HashMap<>();
        for (Tuple t : successRows) {
            Long uid = t.get(weeklyProgress.user.id);
            Long cnt = t.get(weeklyProgress.challenge.id.countDistinct());
            successMap.put(uid, cnt == null ? 0L : cnt);
        }

        Map<Long, Record> result = new HashMap<>();
        for (Long uid : userIds) {
            long attempt = attemptMap.getOrDefault(uid, 0L);
            long success = successMap.getOrDefault(uid, 0L);
            result.put(uid, new Record(attempt, success));
        }
        return result;
    }
}
