package targeter.aim.domain.challenge.entity;

import jakarta.persistence.*;
import lombok.*;
import targeter.aim.domain.user.entity.User;

import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Builder
public class ChallengeMemberId implements Serializable {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public static ChallengeMemberId of(Challenge challenge, User user) {
        return ChallengeMemberId.builder()
                .challenge(challenge)
                .user(user)
                .build();
    }
}