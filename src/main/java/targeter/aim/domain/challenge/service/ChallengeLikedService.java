package targeter.aim.domain.challenge.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import targeter.aim.domain.challenge.entity.Challenge;
import targeter.aim.domain.challenge.entity.ChallengeLiked;
import targeter.aim.domain.challenge.repository.ChallengeLikedRepository;
import targeter.aim.domain.challenge.repository.ChallengeRepository;
import targeter.aim.domain.user.entity.User;
import targeter.aim.domain.user.repository.UserRepository;
import targeter.aim.system.exception.model.ErrorCode;
import targeter.aim.system.exception.model.RestException;

@Service
@RequiredArgsConstructor
public class ChallengeLikedService {

    private final ChallengeLikedRepository challengeLikedRepository;
    private final UserRepository userRepository;
    private final ChallengeRepository challengeRepository;

    @Transactional
    public boolean toggleLike(Long userId, Long challengeId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RestException(ErrorCode.USER_NOT_FOUND));

        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new RestException(ErrorCode.CHALLENGE_NOT_FOUND));

        boolean exists =
                challengeLikedRepository.existsByUserAndChallenge(user, challenge);

        if (exists) {
            challengeLikedRepository.deleteByUserAndChallenge(user, challenge);
            return false;
        }

        challengeLikedRepository.save(new ChallengeLiked(user, challenge));
        return true;
    }
}