package targeter.aim.domain.post.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import targeter.aim.domain.user.entity.User;

@Entity
@Table(name = "post_liked")
@Getter
@NoArgsConstructor
public class PostLiked {

    @EmbeddedId
    private PostLikedId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @MapsId("postId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    public PostLiked(User user, Post post) {
        this.user = user;
        this.post = post;
        this.id = new PostLikedId(
                user.getId(),
                post.getId()
        );
    }
}
