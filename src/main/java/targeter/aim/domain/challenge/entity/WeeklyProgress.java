package targeter.aim.domain.challenge.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import targeter.aim.common.auditor.TimeStampedEntity;
import targeter.aim.domain.file.entity.ChallengeProofAttachedFile;
import targeter.aim.domain.file.entity.ChallengeProofImage;
import targeter.aim.domain.user.entity.User;
import targeter.aim.system.exception.model.ErrorCode;
import targeter.aim.system.exception.model.RestException;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name="weekly_progress")
public class WeeklyProgress extends TimeStampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    @Column(name = "weekly_progress_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "week_number", nullable = false)
    private Integer weekNumber;

    @Column
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "target_time_seconds")
    private Integer targetTimeSeconds;

    @Column(name = "elapsed_time_seconds", nullable = false)
    @Builder.Default
    private Integer elapsedTimeSeconds = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "weekly_status", nullable = false)
    private WeeklyStatus weeklyStatus = WeeklyStatus.PENDING;

    @Column(name = "is_complete", nullable = false)
    private Boolean isComplete;

    @OneToMany(mappedBy = "weeklyProgress", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChallengeProofImage> attachedImages = new ArrayList<>();

    public void addAttachedImage(ChallengeProofImage image) {
        this.attachedImages.add(image);
        image.setWeeklyProgress(this);
    }

    public void removeAttachedImage(ChallengeProofImage image) {
        this.attachedImages.remove(image);
        image.setWeeklyProgress(null);
    }

    @OneToMany(mappedBy = "weeklyProgress", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChallengeProofAttachedFile> attachedFiles = new ArrayList<>();

    public void addAttachedFile(ChallengeProofAttachedFile file) {
        this.attachedFiles.add(file);
        file.setWeeklyProgress(this);
    }

    public void removeAttachedFile(ChallengeProofAttachedFile file) {
        this.attachedFiles.remove(file);
        file.setWeeklyProgress(null);
    }

    public void complete() {
        this.isComplete = true;
    }

    public boolean isComplete() {
        return Boolean.TRUE.equals(this.isComplete);
    }

    public void addElapsedTime(Integer seconds) {
        if(seconds < 0) throw new RestException(ErrorCode.GLOBAL_BAD_REQUEST, "초는 0초 이상 넘겨줘야 함.");
        this.elapsedTimeSeconds = (this.elapsedTimeSeconds == null ? 0 : this.elapsedTimeSeconds ) + seconds;
    }

    public boolean meetsEightyPercents() {
        if(this.targetTimeSeconds == null || this.targetTimeSeconds <= 0) return false;
        int elapsed = this.elapsedTimeSeconds == null ? 0 : this.elapsedTimeSeconds;
        return elapsed * 10 >= this.targetTimeSeconds * 8;
    }

    public void decideWeeklyStatusOnComplete() {
        if(this.targetTimeSeconds == null || this.targetTimeSeconds <= 0) {
            this.weeklyStatus = WeeklyStatus.FAIL;
            return;
        }
        this.weeklyStatus = meetsEightyPercents() ? WeeklyStatus.SUCCESS : WeeklyStatus.FAIL;
    }
}