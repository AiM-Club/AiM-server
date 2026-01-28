package targeter.aim.domain.challenge.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import targeter.aim.domain.challenge.service.ChallengeService;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChallengeScheduler {

    private final ChallengeService challengeService;

    @Scheduled(cron = "0 0 0 * * *")
    public void autoSettleChallenges() {
        log.info("[Scheduler] 일일 챌린지 정산 시작...");
        long start = System.currentTimeMillis();

        challengeService.settleAllFinishedChallenges();

        long end = System.currentTimeMillis();
        log.info("[Scheduler] 정산 종료. 소요 시간: {}ms", (end - start));
    }
}
