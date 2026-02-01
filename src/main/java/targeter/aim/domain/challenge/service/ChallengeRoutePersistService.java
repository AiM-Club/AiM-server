package targeter.aim.domain.challenge.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import targeter.aim.domain.ai.llm.dto.RoutePayload;
import targeter.aim.domain.challenge.dto.ChallengeDto;
import targeter.aim.domain.challenge.entity.*;
import targeter.aim.domain.challenge.repository.ChallengeMemberRepository;
import targeter.aim.domain.challenge.repository.ChallengeRepository;
import targeter.aim.domain.challenge.repository.WeeklyProgressRepository;
import targeter.aim.domain.user.entity.User;
import targeter.aim.domain.user.repository.UserRepository;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChallengeRoutePersistService {

    private final ChallengeRepository challengeRepository;
    private final ChallengeMemberRepository challengeMemberRepository;
    private final WeeklyProgressRepository weeklyProgressRepository;
    private final UserRepository userRepository;

    // 상위 트랜잭션과 독립적으로 실행
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long persistAtomic(Long userId, ChallengeDto.ChallengeCreateRequest req, RoutePayload payload) {
        // 1. Host 유저 조회
        User host = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 2. 멱등성
        Optional<Challenge> existing = challengeRepository.findByHostAndNameAndStartedAtAndModeAndVisibility(
                host, req.getName(), req.getStartedAt(), req.getMode(), req.getVisibility()
        );

        if (existing.isPresent()) {
            log.warn("Challenge with id {} already exists", existing.get().getId());
            return existing.get().getId();
        }

        // 3. Challenge 엔티티 생성 및 저장
        Challenge challenge = Challenge.builder()
                .host(host)
                .name(req.getName())
                .job(req.getJob())
                .startedAt(req.getStartedAt())
                .durationWeek(req.getDurationWeek())
                .status(ChallengeStatus.IN_PROGRESS)
                .mode(req.getMode())
                .visibility(req.getVisibility())
                .build();

        Challenge savedChallenge = challengeRepository.save(challenge);

        // 4. ChallengeMember 저장
        ChallengeMember hostMember = ChallengeMember.builder()
                .id(ChallengeMemberId.of(savedChallenge, host))
                .role(MemberRole.HOST)
                .build();
        challengeMemberRepository.save(hostMember);

        // 5. WeeklyProgress 저장
        for (RoutePayload.Week week : payload.getWeeks()) {
            WeeklyProgress progress = WeeklyProgress.builder()
                    .challenge(savedChallenge)
                    .user(host)
                    .weekNumber(week.getWeekNumber())
                    .title(week.getTitle())
                    .content(week.getContent())
                    .targetTimeSeconds(week.getTargetSeconds())
                    .elapsedTimeSeconds(0)
                    .weeklyStatus(WeeklyStatus.PENDING)
                    .isComplete(false)
                    .build();
            weeklyProgressRepository.save(progress);
        }

        return savedChallenge.getId();
    }
}