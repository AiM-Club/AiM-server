package targeter.aim.domain.file.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import targeter.aim.domain.file.entity.CommentImage;

public interface CommentImageRepository
        extends JpaRepository<CommentImage, String>, CommentImageQueryRepository {
}
