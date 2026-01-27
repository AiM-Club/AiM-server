package targeter.aim.domain.challenge.repository;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import targeter.aim.domain.challenge.entity.WeeklyStatus;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static targeter.aim.domain.challenge.entity.QWeeklyProgress.weeklyProgress;

@Repository
@RequiredArgsConstructor
public class WeeklyProgressQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Map<Long, Long> completedCountByUsers(Long challengeId, List<Long> userIds, int endWeek) {
        if (endWeek <= 0 || userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        List<Tuple> rows = queryFactory
                .select(weeklyProgress.user.id, weeklyProgress.count())
                .from(weeklyProgress)
                .where(
                        weeklyProgress.challenge.id.eq(challengeId),
                        weeklyProgress.user.id.in(userIds),
                        weeklyProgress.weekNumber.between(1, endWeek),
                        weeklyProgress.isComplete.isTrue()
                )
                .groupBy(weeklyProgress.user.id)
                .fetch();

        return rows.stream().collect(Collectors.toMap(
                t -> t.get(weeklyProgress.user.id),
                t -> t.get(weeklyProgress.count())
        ));
    }

    public Map<Long, Long> successCountByUsers(Long challengeId, List<Long> userIds, int endWeek) {
        if (endWeek <= 0 || userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        List<Tuple> rows = queryFactory
                .select(weeklyProgress.user.id, weeklyProgress.count())
                .from(weeklyProgress)
                .where(
                        weeklyProgress.challenge.id.eq(challengeId),
                        weeklyProgress.user.id.in(userIds),
                        weeklyProgress.weekNumber.between(1, endWeek),
                        weeklyProgress.weeklyStatus.eq(WeeklyStatus.SUCCESS)
                )
                .groupBy(weeklyProgress.user.id)
                .fetch();

        return rows.stream().collect(Collectors.toMap(
                t -> t.get(weeklyProgress.user.id),
                t -> t.get(weeklyProgress.count())
        ));
    }

    public List<Tuple> weekCompletionRows(Long challengeId, List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return List.of();

        return queryFactory
                .select(weeklyProgress.weekNumber, weeklyProgress.user.id, weeklyProgress.isComplete)
                .from(weeklyProgress)
                .where(
                        weeklyProgress.challenge.id.eq(challengeId),
                        weeklyProgress.user.id.in(userIds)
                )
                .fetch();
    }
}