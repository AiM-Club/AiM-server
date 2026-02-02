package targeter.aim.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import targeter.aim.domain.challenge.entity.ChallengeResult;
import targeter.aim.domain.challenge.repository.ChallengeMemberQueryRepository;
import targeter.aim.domain.file.dto.FileDto;
import targeter.aim.domain.file.entity.ProfileImage;
import targeter.aim.domain.file.handler.FileHandler;
import targeter.aim.domain.user.dto.TierDto;
import targeter.aim.domain.user.dto.UserDto;
import targeter.aim.domain.user.entity.Tier;
import targeter.aim.domain.user.entity.User;
import targeter.aim.domain.user.repository.TierRepository;
import targeter.aim.domain.user.repository.UserQueryRepository;
import targeter.aim.domain.user.repository.UserRepository;
import targeter.aim.system.exception.model.ErrorCode;
import targeter.aim.system.exception.model.RestException;
import targeter.aim.system.security.model.UserDetails;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserQueryRepository userQueryRepository;
    private final TierRepository tierRepository;
    private final ChallengeMemberQueryRepository challengeMemberQueryRepository;

    private final PasswordEncoder passwordEncoder;
    private final FileHandler fileHandler;

    @Transactional(readOnly = true)
    public UserDto.UserResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RestException(ErrorCode.USER_NOT_FOUND));

        return UserDto.UserResponse.from(user);
    }

    @Transactional
    public void checkAndApplyLevelUp(User user) {
        int successCount = challengeMemberQueryRepository.countByUserAndResult(user, ChallengeResult.SUCCESS);
        int totalCount = challengeMemberQueryRepository.countFinishedChallenges(user);

        if (totalCount == 0) return;

        int currentLevel = user.getLevel() == null ? 1 : user.getLevel();

        double score = calculateScore(successCount, totalCount, currentLevel);

        if (score >= 1.0) {
            int newLevel = currentLevel + 1;
            user.setLevel(newLevel);

            Tier newTier = determineTierByLevel(newLevel);
            user.setTier(newTier);
            log.info("User {} Level Up! {} -> {}, Tier -> {}", user.getId(), currentLevel, newLevel, newTier.getName());
        }
    }

    private double calculateScore(double success, double total, double level) {
        double term1 = (success / total) * (0.5 + 0.002 * level);
        double term2 = Math.min(success / (30.0 + 1.2 * level), 1.0) * (0.5 - 0.002 * level);
        return term1 + term2;
    }

    private Tier determineTierByLevel(int level) {
        if (level <= 30) return tierRepository.findByName("BRONZE").orElseThrow();
        if (level <= 60) return tierRepository.findByName("SILVER").orElseThrow();
        if (level <= 80) return tierRepository.findByName("GOLD").orElseThrow();
        return tierRepository.findByName("DIAMOND").orElseThrow();
    }

    @Transactional(readOnly = true)
    public List<UserDto.RankTop10Response> getTop10UserRank() {
        List<User> users = userRepository
                .findAllByOrderByLevelDescIdAsc(PageRequest.of(0, 10))
                .getContent();

        List<UserDto.RankTop10Response> result = new ArrayList<>();
        for (int i = 0; i < users.size(); i++) {
            result.add(UserDto.RankTop10Response.of(i + 1, users.get(i)));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public UserDto.MyPageResponse getMyPage(UserDetails userDetails) {
        User user = userRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new RestException(ErrorCode.USER_NOT_FOUND));

        int level = user.getLevel();
        Tier currentTier = user.getTier();

        int tierProgressPercent = calculateTierProgressPercent(level, currentTier);
        Tier nextTierEntity = findNextTier(currentTier);

        return new UserDto.MyPageResponse(
                level,
                TierDto.TierResponse.from(currentTier),
                tierProgressPercent,
                nextTierEntity == null
                        ? null
                        : TierDto.TierResponse.from(nextTierEntity)
        );
    }

    // 티어 진행률 계산
    private int calculateTierProgressPercent(int level, Tier tier) {
        int start;
        int end;

        switch (tier.getName()) {
            case "BRONZE" -> {
                start = 1;
                end = 30;
            }
            case "SILVER" -> {
                start = 31;
                end = 60;
            }
            case "GOLD" -> {
                start = 61;
                end = 80;
            }
            case "DIAMOND" -> {
                start = 81;
                end = 100;
            }
            default -> throw new RestException(ErrorCode.TIER_NOT_FOUND);
        }

        double progress = (double) (level - start) / (end - start);
        return Math.min(100, (int) Math.round(progress * 100));
    }

    // 다음 티어 계산
    private Tier findNextTier(Tier currentTier) {
        return switch (currentTier.getName()) {
            case "BRONZE" -> tierRepository.findByName("SILVER").orElse(null);
            case "SILVER" -> tierRepository.findByName("GOLD").orElse(null);
            case "GOLD" -> tierRepository.findByName("DIAMOND").orElse(null);
            case "DIAMOND" -> null;
            default -> null;
        };
    }

    @Transactional(readOnly = true)
    public UserDto.ProfileResponse getProfile(Long targetUserId, UserDetails viewer) {

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RestException(ErrorCode.USER_NOT_FOUND));

        // 관심사 / 관심 분야
        List<String> tags = userQueryRepository.findUserTagNames(targetUserId);
        List<String> fields = userQueryRepository.findUserFieldNames(targetUserId);

        // 챌린지 기록
        UserQueryRepository.Record overall = userQueryRepository.calcOverallRecord(targetUserId);
        UserQueryRepository.Record solo = userQueryRepository.calcSoloRecord(targetUserId);
        UserQueryRepository.Record vs = userQueryRepository.calcVsRecord(targetUserId);

        boolean isMine = viewer != null && viewer.getUser().getId().equals(targetUserId);

        return UserDto.ProfileResponse.builder()
                .userId(target.getId())
                .loginId(target.getLoginId())
                .nickname(target.getNickname())
                .tier(TierDto.TierResponse.builder()
                        .name(target.getTier().getName())
                        .build())
                .level(target.getLevel())
                .profileImage(
                        target.getProfileImage() == null
                                ? null
                                : FileDto.FileResponse.from(target.getProfileImage())
                )
                .tags(tags)
                .fields(fields)
                .allChallengeRecord(toRecordDto(overall))
                .soloChallengeRecord(toRecordDto(solo))
                .vsChallengeRecord(toRecordDto(vs))
                .isMine(isMine)
                .build();
    }

    private UserDto.ChallengeRecord toRecordDto(UserQueryRepository.Record record) {
        long attempt = record.attempt();
        long success = record.success();
        long fail = attempt - success;
        double successRate = attempt == 0
                ? 0
                : Math.round((success * 100.0) / attempt);

        return UserDto.ChallengeRecord.builder()
                .attemptCount(attempt)
                .successCount(success)
                .failCount(fail)
                .successRate(successRate)
                .build();
    }

    @Transactional
    public UserDto.ProfileResponse updateMyProfile(UserDto.UpdateProfileRequest request, UserDetails userDetails) {
        if (userDetails == null) {
            throw new RestException(ErrorCode.AUTH_LOGIN_REQUIRED);
        }

        User me = userRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new RestException(ErrorCode.USER_NOT_FOUND));

        cannotCreateWithDuplicatedLoginIdOrNickname(request, me);

        request.applyTo(me, passwordEncoder);
        if(!request.getProfileImage().isEmpty()) {
            saveProfileImage(request.getProfileImage(), me);
        }

        return getProfile(me.getId(), userDetails);
    }

    private void cannotCreateWithDuplicatedLoginIdOrNickname(UserDto.UpdateProfileRequest request, User found) {
        if (request.getLoginId() != null &&
                !found.getLoginId().equals(request.getLoginId()) &&
                userRepository.existsByLoginId(request.getLoginId())) {
            throw new RestException(ErrorCode.USER_ALREADY_LOGIN_ID_EXISTS);
        }
        if (request.getNickname() != null &&
                !found.getNickname().equals(request.getNickname()) &&
                userRepository.existsByNickname(request.getNickname())) {
            throw new RestException(ErrorCode.USER_ALREADY_NICKNAME_EXISTS);
        }
    }

    private void saveProfileImage(MultipartFile image, User user) {
        if (image == null || image.isEmpty()) return;

        ProfileImage profileImage = ProfileImage.from(image, user);
        user.setProfileImage(profileImage);
        fileHandler.saveFile(image, profileImage);
    }
}
