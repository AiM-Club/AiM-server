package targeter.aim.domain.post.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import targeter.aim.domain.post.entity.Post;
import targeter.aim.domain.post.entity.PostLiked;
import targeter.aim.domain.post.entity.PostLikedId;
import targeter.aim.domain.user.entity.User;

public interface PostLikedRepository extends JpaRepository<PostLiked, PostLikedId> {

    boolean existsByPostAndUser(Post post, User user);

    void deleteByPostAndUser(Post post, User user);
}
