package targeter.aim.domain.challenge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class TimerDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "타이머 액션 요청 DTO")
    public static class TimerActionRequest {

        @Schema(description = "타이머 액션", example = "START", allowableValues = { "START", "STOP" })
        private String action;

    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "타이머 상태 업데이트 응답 DTO")
    public static class TimerUpdateResponse {

        @Schema(description = "이벤트 발생 사용자 ID", example = "1")
        private Long senderId;

        @Schema(description = "타이머 상태", example = "ON | OFF")
        private String status;

        @Schema(description = "타이머 시작 시간", example =
                "2026-01-15T04:22:37.184284")
        private LocalDateTime startedAt;

        @Schema(description = "누적 시간 (초 단위)", example = "120")
        private Integer accumulatedTime;

        @Schema(description = "상태 메시지", example = "상대방이 챌린지를 시작했습니다.")
        private String message;
    }

}
