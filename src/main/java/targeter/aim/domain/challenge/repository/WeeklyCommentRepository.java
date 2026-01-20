package targeter.aim.domain.challenge.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import targeter.aim.domain.challenge.entity.WeeklyComment;

import java.util.List;

public interface WeeklyCommentRepository extends JpaRepository<WeeklyComment, Long> {

    @EntityGraph(attributePaths = {"user"})
    Page<WeeklyComment> findAllByWeeklyProgress_IdAndParentCommentIsNull(
            Long weeklyProgressId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"user"})
    List<WeeklyComment> findAllByParentComment_Id(
            Long parentCommentId,
            Sort sort
    );
}
