package targeter.aim.domain.file.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import targeter.aim.domain.file.entity.CommentAttachedFile;

public interface CommentAttachedFileRepository
        extends JpaRepository<CommentAttachedFile, String>, CommentAttachedFileQueryRepository {
}
