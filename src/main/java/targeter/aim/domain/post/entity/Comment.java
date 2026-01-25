package targeter.aim.domain.post.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import targeter.aim.common.auditor.TimeStampedEntity;
import targeter.aim.domain.user.entity.User;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Comment extends TimeStampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private Comment parent;

    @OneToMany(mappedBy = "parent")
    private List<Comment> children = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contents;

    @Column(nullable = false)
    private Integer depth;

    // 댓글
    public static Comment createRoot(User user, Post post, String contents) {
        return new Comment(
                null,
                null,
                new ArrayList<>(),
                user,
                post,
                contents,
                1
        );
    }

    //  대댓글
    public static Comment createChild(User user, Post post, Comment parent, String contents) {
        return new Comment(
                null,
                parent,
                new ArrayList<>(),
                user,
                post,
                contents,
                parent.depth + 1
        );
    }
}
