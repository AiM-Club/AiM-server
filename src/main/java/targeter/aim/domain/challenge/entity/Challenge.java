package targeter.aim.domain.challenge.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import targeter.aim.common.auditor.TimeStampedEntity;
import targeter.aim.domain.label.entity.Tag;
import targeter.aim.domain.user.entity.User;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChallengeStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChallengeVisibility visibility;

    @ManyToMany
    @JoinTable(
            name = "CHALLENGE_TAG_MAPPING",
            joinColumns = @JoinColumn(name = "challenge_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @Builder.Default
    @ToString.Exclude
    private Set<Tag> tags = new HashSet<>();
}
