package targeter.aim.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import targeter.aim.domain.challenge.entity.ChallengeResult;
import targeter.aim.domain.challenge.repository.ChallengeMemberQueryRepository;
import targeter.aim.domain.challenge.repository.ChallengeMemberRepository;
import targeter.aim.domain.user.dto.UserDto;
import targeter.aim.domain.user.entity.User;
import targeter.aim.domain.user.repository.UserRepository;
import targeter.aim.system.exception.model.ErrorCode;
import targeter.aim.system.exception.model.RestException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ChallengeMemberRepository challengeMemberRepository;
    private final ChallengeMemberQueryRepository challengeMemberQueryRepository;

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
            user.setLevel(currentLevel + 1);
            log.info("User {} Level Up! {} -> {}", user.getId(), currentLevel, currentLevel + 1);
        }
    }

    private double calculateScore(double success, double total, double level) {
        double term1 = (success / total) * (0.5 + 0.002 * level);
        double term2 = Math.min(success / (30.0 + 1.2 * level), 1.0) * (0.5 - 0.002 * level);
        return term1 + term2;
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
}
