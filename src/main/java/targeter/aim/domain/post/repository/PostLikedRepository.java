package targeter.aim.domain.post.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import targeter.aim.domain.post.entity.PostLiked;
import targeter.aim.domain.post.entity.PostLikedId;

public interface PostLikedRepository extends JpaRepository<PostLiked, PostLikedId> {
}
