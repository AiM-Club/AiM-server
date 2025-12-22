package targeter.aim.domain.challenge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import targeter.aim.domain.challenge.entity.WeeklyComment;

public interface WeeklyCommentRepository extends JpaRepository<WeeklyComment, Long> {
}
