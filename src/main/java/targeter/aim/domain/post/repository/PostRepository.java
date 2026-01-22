package targeter.aim.domain.post.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import targeter.aim.domain.post.entity.Post;

public interface PostRepository extends JpaRepository<Post, Long> {
}
