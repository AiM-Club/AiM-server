package targeter.aim.domain.challenge.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import targeter.aim.domain.challenge.entity.WeeklyComment;

import java.util.List;

public interface WeeklyCommentRepository extends JpaRepository<WeeklyComment, Long> {

    @EntityGraph(attributePaths = {"user"})
    List<WeeklyComment> findAllByWeeklyProgress_IdAndParentCommentIsNullOrderByCreatedAtAsc(Long weeklyProgressId);

    @EntityGraph(attributePaths = {"user"})
    List<WeeklyComment> findAllByParentComment_IdOrderByCreatedAtAsc(Long parentCommentId);
}
