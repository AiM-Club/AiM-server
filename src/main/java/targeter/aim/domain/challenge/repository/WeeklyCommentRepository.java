package targeter.aim.domain.challenge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import targeter.aim.domain.challenge.entity.WeeklyComment;

import java.util.List;

public interface WeeklyCommentRepository extends JpaRepository<WeeklyComment, Long> {

    List<WeeklyComment> findAllByWeeklyProgress_IdOrderByCreatedAtAsc(Long weeklyProgressId);
}
