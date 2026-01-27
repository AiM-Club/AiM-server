package targeter.aim.domain.file.repository;
import targeter.aim.domain.file.entity.CommentAttachedFile;
import java.util.List;

public interface CommentAttachedFileQueryRepository {

    List<CommentAttachedFile> findAllByPostId(Long postId);
}
