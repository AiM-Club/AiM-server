package targeter.aim.domain.challenge.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import targeter.aim.common.auditor.TimeStampedEntity;
import targeter.aim.domain.file.entity.ChallengeImage;
import targeter.aim.domain.label.entity.Field;
import targeter.aim.domain.label.entity.Tag;
import targeter.aim.domain.user.entity.User;
import targeter.aim.system.exception.model.ErrorCode;
import targeter.aim.system.exception.model.RestException;
import targeter.aim.system.security.model.UserDetails;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name="challenge")
public class Challenge extends TimeStampedEntity {

    @Id
    @Column(name = "challenge_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @Column(nullable = false, length = 15)
    private String name;

    @Column(nullable = false)
    private String job;

    @Column(name = "started_at", nullable = false)
    private LocalDate startedAt;

    @Column(name = "duration_week", nullable = false)
    private Integer durationWeek;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChallengeMode mode;

    @Column(name = "like_count", nullable = false)
    @Builder.Default
    private Integer likeCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChallengeStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChallengeVisibility visibility;

    @OneToOne(mappedBy = "challenge", cascade = CascadeType.ALL, orphanRemoval = true)
    private ChallengeImage challengeImage;

    @ManyToMany
    @JoinTable(
            name = "CHALLENGE_TAG_MAPPING",
            joinColumns = @JoinColumn(name = "challenge_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @Builder.Default
    @ToString.Exclude
    private Set<Tag> tags = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "CHALLENGE_FIELD_MAPPING",
            joinColumns = @JoinColumn(name = "challenge_id"),
            inverseJoinColumns = @JoinColumn(name = "field_id")
    )
    @Builder.Default
    @ToString.Exclude
    private Set<Field> fields = new HashSet<>();

    public void canUpdateBy(UserDetails user) {
        if(this.host.getId().equals(user.getUser().getId())) {
            return;
        }

        throw new RestException(ErrorCode.AUTH_FORBIDDEN);
    }

    public void canDeleteBy(UserDetails user) {
        if(this.host.getId().equals(user.getUser().getId())) {
            return;
        }

        throw new RestException(ErrorCode.AUTH_FORBIDDEN);
    }

    public void startVs() {
        if(this.mode != ChallengeMode.VS) {
            throw new RestException(ErrorCode.GLOBAL_BAD_REQUEST, "VS 챌린지가 아닙니다.");
        }

        if(this.status == ChallengeStatus.COMPLETED) {
            throw new RestException(ErrorCode.GLOBAL_BAD_REQUEST, "이미 완료된 챌린지입니다.");
        }

        this.status = ChallengeStatus.IN_PROGRESS;
    }

    public void addLikedCount() {
        this.likeCount++;
    }

    public void subtractLikedCount() {
        this.likeCount--;
    }
}
