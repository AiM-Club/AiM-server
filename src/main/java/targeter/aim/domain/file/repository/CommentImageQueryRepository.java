package targeter.aim.domain.file.repository;

import targeter.aim.domain.file.entity.CommentImage;
import java.util.List;

public interface CommentImageQueryRepository {

    List<CommentImage> findAllByPostId(Long postId);
}