package targeter.aim.domain.post.repository;

import targeter.aim.domain.post.entity.Comment;
import java.util.List;

public interface CommentQueryRepository {
    List<Comment> findAllByPostIdWithUser(Long postId);
}
