package targeter.aim.domain.challenge.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import targeter.aim.common.auditor.TimeStampedEntity;
import targeter.aim.domain.user.entity.User;
import targeter.aim.system.exception.model.ErrorCode;
import targeter.aim.system.exception.model.RestException;
import targeter.aim.system.security.model.UserDetails;

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

    public void canApplyBy(UserDetails userDetails) {
        if(userDetails == null || userDetails.getUser() == null) {
            throw new RestException(ErrorCode.AUTH_FORBIDDEN, "인증 토큰이 없어 승인할 수 없습니다.");
        }
        long loginUser = userDetails.getUser().getId();

        if(this.applier != null && this.applier.getId().equals(loginUser)) {
            return;
        }

        throw new RestException(ErrorCode.AUTH_FORBIDDEN, "승인자가 아니어서 승인할 수 없습니다.");
    }

    public void validatePending() {
        if(this.applyStatus == ApplyStatus.PENDING) return;
        throw new RestException(ErrorCode.GLOBAL_BAD_REQUEST, "이미 승인/거절된 요청입니다.");
    }

    public void approve() {
        validatePending();
        this.applyStatus = ApplyStatus.APPROVED;
    }
}
