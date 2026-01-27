package targeter.aim.domain.post.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import targeter.aim.common.auditor.TimeStampedEntity;
import targeter.aim.domain.file.entity.ChallengeCommentAttachedFile;
import targeter.aim.domain.file.entity.ChallengeCommentImage;
import targeter.aim.domain.file.entity.CommentAttachedFile;
import targeter.aim.domain.file.entity.CommentImage;
import targeter.aim.domain.user.entity.User;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "comment")
public class Comment extends TimeStampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    @Setter(AccessLevel.NONE)
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

    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CommentImage> attachedImages = new ArrayList<>();

    public void addAttachedImage(CommentImage image) {
        this.attachedImages.add(image);
        image.setComment(this);
    }

    public void removeAttachedImage(CommentImage image) {
        this.attachedImages.remove(image);
        image.setComment(null);
    }

    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CommentAttachedFile> attachedFiles = new ArrayList<>();

    public void addAttachedFile(CommentAttachedFile file) {
        this.attachedFiles.add(file);
        file.setComment(this);
    }

    public void removeAttachedFile(CommentAttachedFile file) {
        this.attachedFiles.remove(file);
        file.setComment(null);
    }
}
