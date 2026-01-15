package targeter.aim.domain.challenge.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import targeter.aim.domain.challenge.dto.TimerDto;
import targeter.aim.domain.challenge.entity.Challenge;
import targeter.aim.domain.challenge.repository.ChallengeRepository;
import targeter.aim.domain.challenge.service.ChallengeTimerService;
import targeter.aim.domain.user.entity.User;
import targeter.aim.domain.user.repository.UserRepository;
import targeter.aim.system.exception.model.ErrorCode;
import targeter.aim.system.exception.model.RestException;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Tag(name = "Timer WebSocket Docs")
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
                throw new RestException(ErrorCode.AUTH_LOGIN_REQUIRED);
            }

            Challenge challenge = challengeRepository.findById(challengeId)
                    .orElseThrow(() -> new RestException(ErrorCode.CHALLENGE_NOT_FOUND));

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RestException(ErrorCode.USER_NOT_FOUND));

            switch (request.getAction()) {
                case "START" -> handleStart(challenge, user);
                case "STOP" -> handleStop(challenge, user);
                default -> throw new RestException(ErrorCode.CHALLENGE_INVALID_TIMER_ACTION);
            }

        } catch (RestException e) {

            messagingTemplate.convertAndSend(
                    "/sub/challenge/" + challengeId,
                    Map.of(
                            "status", "ERROR",
                            "code", e.getErrorCode().name(),
                            "message", e.getErrorCode().getMessage()
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

    @Operation(
            summary = "챌린지 타이머 WebSocket 명세 (Swagger 문서용)",
            description = """
        WebSocket(STOMP) 기반 챌린지 타이머 제어 명세

        [WebSocket Endpoint]
        ws://{host}/ws-stomp

        [Publish]
        /pub/challenge/{challengeId}/timer

        [Subscribe]
        /sub/challenge/{challengeId}

        ※ 본 API는 Swagger 문서화를 위한 dummy 엔드포인트입니다.
        """
    )
    @PostMapping("/docs/websocket/timer")
    public TimerDto.TimerUpdateResponse timerWebSocketDoc(
            @RequestBody TimerDto.TimerActionRequest request
    ) {
        return null; // Swagger 문서용
    }
}