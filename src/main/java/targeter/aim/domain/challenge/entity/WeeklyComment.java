package targeter.aim.domain.challenge.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import targeter.aim.common.auditor.TimeStampedEntity;
import targeter.aim.domain.file.entity.ChallengeCommentAttachedFile;
import targeter.aim.domain.file.entity.ChallengeCommentImage;
import targeter.aim.domain.file.entity.ChallengeProofAttachedFile;
import targeter.aim.domain.file.entity.ChallengeProofImage;
import targeter.aim.domain.user.entity.User;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "weekly_comment")
public class WeeklyComment extends TimeStampedEntity {

    @Id
    @Column(name = "weekly_comment_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private WeeklyComment parentComment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "weekly_progress_id", nullable = false)
    private WeeklyProgress weeklyProgress;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private Integer depth;

    @OneToMany(mappedBy = "weeklyComment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChallengeCommentImage> attachedImages = new ArrayList<>();

    public void addAttachedImage(ChallengeCommentImage image) {
        this.attachedImages.add(image);
        image.setWeeklyComment(this);
    }

    public void removeAttachedImage(ChallengeCommentImage image) {
        this.attachedImages.remove(image);
        image.setWeeklyComment(null);
    }

    @OneToMany(mappedBy = "weeklyComment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChallengeCommentAttachedFile> attachedFiles = new ArrayList<>();

    public void addAttachedFile(ChallengeCommentAttachedFile file) {
        this.attachedFiles.add(file);
        file.setWeeklyComment(this);
    }

    public void removeAttachedFile(ChallengeCommentAttachedFile file) {
        this.attachedFiles.remove(file);
        file.setWeeklyComment(null);
    }
}
