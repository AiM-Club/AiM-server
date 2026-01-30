package targeter.aim.domain.challenge.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
            summary = "[Docs] 챌린지 타이머 WebSocket 명세",
            description = """
                **WebSocket(STOMP)을 이용한 챌린지 타이머 제어 명세입니다.**
                
                이 API는 HTTP 요청이 아닌 **WebSocket 메시징**을 통해 동작합니다.
                
                ---
                
                ### 1. 연결 정보 (Connection)
                - **Endpoint:** `wss://{host}/ws-stomp`
                - **Auth:** Header에 `Authorization: Bearer {accesstoken}` 포함 필수
                
                ### 2. 구독 (Subscribe)
                - **Path:** `/sub/challenge/{challengeId}`
                - **설명:** 해당 챌린지 방의 타이머 상태 변경(본인 및 상대방 포함)을 실시간으로 수신합니다.
                
                ### 3. 요청 (Publish)
                - **Path:** `/pub/challenge/{challengeId}/timer`
                - **설명:** 타이머를 시작하거나 종료합니다.
                
                ### 4. 에러 응답 (Error Case)
                로직 실패 시(예: 이미 시작됨, 권한 없음) `/sub` 경로로 아래 JSON이 내려옵니다.
                ```json
                {
                  "status": "ERROR",
                  "code": "CHALLENGE_001",
                  "message": "이미 실행 중인 타이머입니다."
                }
                ```
                """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "성공 시 소켓 응답 (Subscribe로 수신됨)",
                            content = @Content(
                                    schema = @Schema(implementation = TimerDto.TimerUpdateResponse.class),
                                    examples = {
                                            @ExampleObject(
                                                    name = "1. 시작 성공 (START)",
                                                    value = """
                                                        {
                                                          "senderId": 1,
                                                          "status": "ON",
                                                          "startedAt": "2024-02-01T10:00:00",
                                                          "accumulatedTime": null,
                                                          "message": "상대방이 챌린지를 시작했습니다."
                                                        }
                                                        """
                                            ),
                                            @ExampleObject(
                                                    name = "2. 종료 성공 (STOP)",
                                                    value = """
                                                        {
                                                          "senderId": 1,
                                                          "status": "OFF",
                                                          "startedAt": null,
                                                          "accumulatedTime": 3600,
                                                          "message": "상대방이 챌린지를 종료했습니다."
                                                        }
                                                        """
                                            )
                                    }
                            )
                    )
            }
    )
    @PostMapping("/docs/websocket/timer")
    public TimerDto.TimerUpdateResponse timerWebSocketDoc(
            @RequestBody TimerDto.TimerActionRequest request
    ) {
        return null; // Swagger 문서용
    }
}