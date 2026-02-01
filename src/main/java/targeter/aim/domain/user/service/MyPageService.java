package targeter.aim.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import targeter.aim.domain.user.dto.MyPageDto;
import targeter.aim.domain.user.dto.TierDto;
import targeter.aim.domain.user.entity.Tier;
import targeter.aim.domain.user.entity.User;
import targeter.aim.domain.user.repository.TierRepository;
import targeter.aim.domain.user.repository.UserRepository;
import targeter.aim.system.exception.model.ErrorCode;
import targeter.aim.system.exception.model.RestException;
import targeter.aim.system.security.model.UserDetails;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private final UserRepository userRepository;
    private final TierRepository tierRepository;

    public MyPageDto.MyPageResponse getMyPage(UserDetails userDetails) {
        User user = userRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new RestException(ErrorCode.USER_NOT_FOUND));

        int level = user.getLevel();
        Tier currentTier = user.getTier();

        int tierProgressPercent = calculateTierProgressPercent(level, currentTier);
        Tier nextTierEntity = findNextTier(currentTier);

        return new MyPageDto.MyPageResponse(
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
                return 100;
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
}