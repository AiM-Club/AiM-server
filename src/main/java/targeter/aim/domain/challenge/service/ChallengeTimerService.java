package targeter.aim.domain.challenge.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import targeter.aim.domain.challenge.entity.Challenge;
import targeter.aim.domain.challenge.entity.WeeklyProgress;
import targeter.aim.domain.challenge.repository.ChallengeMemberRepository;
import targeter.aim.domain.challenge.repository.WeeklyProgressRepository;
import targeter.aim.domain.challenge.timer.ChallengeRunningSessionManager;
import targeter.aim.domain.user.entity.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChallengeTimerService {

    private final ChallengeRunningSessionManager sessionManager;
    private final WeeklyProgressRepository weeklyProgressRepository;
    private final ChallengeMemberRepository memberRepository;

    // 현재 주차 계산
    public int calculateCurrentWeek(Challenge challenge) {
        LocalDate startDate = challenge.getStartedAt();
        LocalDate today = LocalDate.now();

        long days = ChronoUnit.DAYS.between(startDate, today);
        int week = (int) (days / 7) + 1;

        if (week < 1) {
            return 1;
        }
        if (week > challenge.getDurationWeek()) {
            return challenge.getDurationWeek();
        }

        return week;
    }

    @Transactional
    public LocalDateTime startTimer(Challenge challenge, User user) {

        log.error("START user={} challenge={} week={}",
                user.getId(),
                challenge.getId(),
                calculateCurrentWeek(challenge)
        );

        int weekNumber = calculateCurrentWeek(challenge);

        if (!memberRepository.existsById_ChallengeAndId_User(challenge, user)) {
            throw new IllegalStateException("챌린지 참가자가 아닙니다.");
        }

        WeeklyProgress progress =
                weeklyProgressRepository
                        .findByChallengeAndUserAndWeekNumber(challenge, user, weekNumber)
                        .orElseThrow(() -> new IllegalStateException("주차 기록이 없습니다."));

        if (progress.isComplete()) {
            throw new IllegalStateException("이미 완료된 주차 챌린지입니다.");
        }

        if (sessionManager.isRunning(challenge.getId(), user.getId())) {
            throw new IllegalStateException("이미 실행 중입니다.");
        }

        return sessionManager.start(challenge.getId(), user.getId());
    }

    @Transactional
    public long stopTimer(Challenge challenge, User user) {

        int weekNumber = calculateCurrentWeek(challenge);

        if (!memberRepository.existsById_ChallengeAndId_User(challenge, user)) {
            throw new IllegalStateException("챌린지 참가자가 아닙니다.");
        }

        WeeklyProgress progress =
                weeklyProgressRepository
                        .findByChallengeAndUserAndWeekNumber(challenge, user, weekNumber)
                        .orElseThrow(() -> new IllegalStateException("주차 기록이 없습니다."));

        if (progress.isComplete()) {
            throw new IllegalStateException("이미 완료된 주차 챌린지입니다.");
        }

        long elapsedSeconds = sessionManager.stop(challenge.getId(), user.getId());

        progress.addElapsedTime((int) elapsedSeconds);  // 진행시간 저장 후 더하기
        progress.decideWeeklyStatusOnComplete();        // 성공여부 체크
        progress.complete();

        return progress.getElapsedTimeSeconds();
    }

}

