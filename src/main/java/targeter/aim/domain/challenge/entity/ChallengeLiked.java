package targeter.aim.domain.challenge.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import targeter.aim.domain.user.entity.User;

@Entity
@Table(name = "challenge_liked")
@Getter
@NoArgsConstructor
public class ChallengeLiked {

    @EmbeddedId
    private ChallengeLikedId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @MapsId("challengeId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    public ChallengeLiked(User user, Challenge challenge) {
        this.user = user;
        this.challenge = challenge;
        this.id = new ChallengeLikedId(
                user.getId(),
                challenge.getId()
        );
    }
}
