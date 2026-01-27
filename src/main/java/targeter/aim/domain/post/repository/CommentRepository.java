package targeter.aim.domain.post.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import targeter.aim.domain.post.entity.Comment;


public interface CommentRepository extends JpaRepository<Comment, Long>, CommentQueryRepository {
}