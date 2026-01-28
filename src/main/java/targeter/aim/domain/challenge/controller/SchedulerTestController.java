package targeter.aim.domain.challenge.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import targeter.aim.domain.challenge.service.ChallengeService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/test/scheduler")
public class SchedulerTestController {

    private final ChallengeService challengeService;

    @PostMapping("/force-run")
    public String forceRunScheduler() {
        challengeService.settleAllFinishedChallenges();
        return "스케줄러 강제 실행 완료! DB와 로그를 확인하세요.";
    }
}