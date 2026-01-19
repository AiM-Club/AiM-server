package targeter.aim.domain.challenge.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import targeter.aim.domain.challenge.entity.Challenge;
import targeter.aim.domain.challenge.repository.ChallengeMemberRepository;
import targeter.aim.domain.challenge.repository.ChallengeRepository;
import targeter.aim.domain.challenge.repository.WeeklyProgressRepository;
import targeter.aim.system.exception.model.ErrorCode;
import targeter.aim.system.exception.model.RestException;

@Service
@RequiredArgsConstructor
public class ChallengeCleanupService {

    private final WeeklyProgressRepository weeklyProgressRepository;
    private final ChallengeMemberRepository challengeMemberRepository;
    private final ChallengeRepository challengeRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteChallengeAtomic(Long challengeId) {
        if (challengeId == null) return;

        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new RestException(ErrorCode.CHALLENGE_NOT_FOUND));

        weeklyProgressRepository.deleteAllByChallenge(challenge);
        challengeMemberRepository.deleteAllById_Challenge(challenge);
        challengeRepository.deleteById(challengeId);
    }
}
