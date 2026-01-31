package targeter.aim.domain.challenge.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import targeter.aim.domain.challenge.dto.ChallengeRecordDto;
import targeter.aim.domain.challenge.entity.ChallengeMode;
import targeter.aim.domain.challenge.entity.ChallengeResult;
import targeter.aim.domain.challenge.repository.ChallengeMemberQueryRepository;
import targeter.aim.domain.user.entity.User;
import targeter.aim.domain.user.repository.UserRepository;
import targeter.aim.system.exception.model.ErrorCode;
import targeter.aim.system.exception.model.RestException;

@Service
@RequiredArgsConstructor
public class ChallengeRecordService {

    private final UserRepository userRepository;
    private final ChallengeMemberQueryRepository challengeMemberQueryRepository;

    @Transactional(readOnly = true)
    public ChallengeRecordDto.RecordResponse getMyChallengeRecords(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RestException(ErrorCode.USER_NOT_FOUND));

        int allAttempt = challengeMemberQueryRepository.countFinishedChallenges(user);
        int allSuccess = challengeMemberQueryRepository.countByUserAndResult(user, ChallengeResult.SUCCESS);

        int soloAttempt = challengeMemberQueryRepository.countFinishedChallengesByMode(user, ChallengeMode.SOLO);
        int soloSuccess = challengeMemberQueryRepository.countByUserAndResultAndMode(user, ChallengeResult.SUCCESS, ChallengeMode.SOLO);

        int vsAttempt = challengeMemberQueryRepository.countFinishedChallengesByMode(user, ChallengeMode.VS);
        int vsSuccess = challengeMemberQueryRepository.countByUserAndResultAndMode(user, ChallengeResult.SUCCESS, ChallengeMode.VS);

        ChallengeRecordDto.RecordDetail solo = toDetail(soloAttempt, soloSuccess);
        ChallengeRecordDto.RecordDetail vs = toDetail(vsAttempt, vsSuccess);

        return ChallengeRecordDto.RecordResponse.builder()
                .allSuccessRate(calcRate(allAttempt, allSuccess))
                .soloRecord(solo)
                .vsRecord(vs)
                .build();
    }

    private ChallengeRecordDto.RecordDetail toDetail(int attempt, int success) {
        return ChallengeRecordDto.RecordDetail.builder()
                .successRate(calcRate(attempt, success))
                .attemptCount(attempt)
                .successCount(success)
                .failCount(Math.max(attempt - success, 0))
                .build();
    }

    private int calcRate(int attempt, int success) {
        if (attempt <= 0) return 0;
        return (success * 100) / attempt;
    }
}
