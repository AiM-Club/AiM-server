package targeter.aim.domain.challenge.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import targeter.aim.common.auditor.TimeStampedEntity;
import targeter.aim.domain.user.entity.User;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name="challenge_request")
public class ChallengeRequest extends TimeStampedEntity {

    @Id
    @Column(name = "challenge_request_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;      // 요청자(Member)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applier_id", nullable = false)
    private User applier;        // 승인자(Host)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplyStatus applyStatus;

    @Column(name = "pending_marker")
    private Integer pendingMarker;
}
