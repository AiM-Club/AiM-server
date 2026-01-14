package targeter.aim.domain.challenge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class TimerDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimerActionRequest {
        private String action;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimerUpdateResponse {
        private Long senderId;
        private String status;
        private LocalDateTime startedAt;
        private Integer accumulatedTime;
        private String message;
    }

}
