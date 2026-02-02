package targeter.aim.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import targeter.aim.domain.file.dto.FileDto;
import targeter.aim.domain.user.dto.ProfileDto;
import targeter.aim.domain.user.entity.User;
import targeter.aim.domain.user.repository.ProfileQueryRepository;
import targeter.aim.domain.user.repository.UserRepository;
import targeter.aim.system.exception.model.ErrorCode;
import targeter.aim.system.exception.model.RestException;
import targeter.aim.system.security.model.UserDetails;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final ProfileQueryRepository profileQueryRepository;

    @Transactional(readOnly = true)
    public ProfileDto.ProfileResponse getProfile(Long targetUserId, UserDetails viewer) {

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RestException(ErrorCode.USER_NOT_FOUND));

        // 관심사 / 관심 분야
        List<String> interests = profileQueryRepository.findUserTagNames(targetUserId);
        List<String> fields = profileQueryRepository.findUserFieldNames(targetUserId);

        // 챌린지 기록
        ProfileQueryRepository.Record overall = profileQueryRepository.calcOverallRecord(targetUserId);
        ProfileQueryRepository.Record solo = profileQueryRepository.calcSoloRecord(targetUserId);
        ProfileQueryRepository.Record vs = profileQueryRepository.calcVsRecord(targetUserId);

        boolean isMine = viewer != null && viewer.getUser().getId().equals(targetUserId);

        return ProfileDto.ProfileResponse.builder()
                .userId(target.getId())
                .loginId(target.getLoginId())
                .nickname(target.getNickname())
                .tier(ProfileDto.TierResponse.builder()
                        .name(target.getTier().getName())
                        .build())
                .level(target.getLevel())
                .profileImage(
                        target.getProfileImage() == null
                                ? null
                                : FileDto.FileResponse.from(target.getProfileImage())
                )
                .interests(interests)
                .fields(fields)
                .overall(toRecordDto(overall))
                .solo(toRecordDto(solo))
                .vs(toRecordDto(vs))
                .isMine(isMine)
                .build();
    }

    private ProfileDto.ChallengeRecord toRecordDto(ProfileQueryRepository.Record record) {
        long attempt = record.attempt();
        long success = record.success();
        long fail = attempt - success;
        int successRate = attempt == 0
                ? 0
                : (int) Math.round((success * 100.0) / attempt);

        return ProfileDto.ChallengeRecord.builder()
                .attemptCount(attempt)
                .successCount(success)
                .failCount(fail)
                .successRate(successRate)
                .build();
    }
}