package targeter.aim.domain.challenge.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import targeter.aim.domain.challenge.dto.TimerDto;
import targeter.aim.domain.challenge.entity.Challenge;
import targeter.aim.domain.challenge.repository.ChallengeRepository;
import targeter.aim.domain.challenge.service.ChallengeTimerService;
import targeter.aim.domain.user.entity.User;
import targeter.aim.domain.user.repository.UserRepository;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ChallengeTimerWebSocketController {

    private final ChallengeTimerService challengeTimerService;
    private final ChallengeRepository challengeRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/challenge/{challengeId}/timer")
    public void handleTimerAction(
            @DestinationVariable Long challengeId,
            @Payload TimerDto.TimerActionRequest request,
            Principal principal
    ) {

        try {
            Long userId = Long.valueOf(principal.getName());

            if (userId == null) {
                throw new IllegalStateException("인증되지 않은 사용자");
            }

            Challenge challenge = challengeRepository.findById(challengeId)
                    .orElseThrow(() -> new IllegalStateException("챌린지가 존재하지 않습니다."));

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalStateException("사용자가 존재하지 않습니다."));

            switch (request.getAction()) {
                case "START" -> handleStart(challenge, user);
                case "STOP" -> handleStop(challenge, user);
                default -> throw new IllegalArgumentException("지원하지 않는 타이머 액션입니다.");
            }

        } catch (IllegalStateException e) {

            messagingTemplate.convertAndSend(
                    "/sub/challenge/" + challengeId,
                    Map.of(
                            "status", "ERROR",
                            "message", e.getMessage()
                    )
            );
        }
    }

    private void handleStart(Challenge challenge, User user) {

        LocalDateTime startedAt =
                challengeTimerService.startTimer(challenge, user);

        TimerDto.TimerUpdateResponse response =
                TimerDto.TimerUpdateResponse.builder()
                        .senderId(user.getId())
                        .status("ON")
                        .startedAt(startedAt)
                        .message("상대방이 챌린지를 시작했습니다.")
                        .build();

        messagingTemplate.convertAndSend(
                "/sub/challenge/" + challenge.getId(),
                response
        );
    }

    private void handleStop(Challenge challenge, User user) {

        long accumulatedTime =
                challengeTimerService.stopTimer(challenge, user);

        messagingTemplate.convertAndSend(
                "/sub/challenge/" + challenge.getId(),
                TimerDto.TimerUpdateResponse.builder()
                        .senderId(user.getId())
                        .status("OFF")
                        .accumulatedTime((int) accumulatedTime)
                        .message("상대방이 챌린지를 종료했습니다.")
                        .build()
        );
    }
}