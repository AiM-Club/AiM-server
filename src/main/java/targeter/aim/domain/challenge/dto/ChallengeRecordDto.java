package targeter.aim.domain.challenge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

public class ChallengeRecordDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecordResponse {

        @Schema(description = "전체 챌린지 성공률(퍼센트)", example = "70")
        private Integer allSuccessRate;

        @Schema(description = "SOLO 기록")
        private RecordDetail soloRecord;

        @Schema(description = "VS 기록")
        private RecordDetail vsRecord;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecordDetail {

        @Schema(description = "성공률(퍼센트)", example = "70")
        private Integer successRate;

        @Schema(description = "시도 횟수", example = "30")
        private Integer attemptCount;

        @Schema(description = "성공 횟수", example = "21")
        private Integer successCount;

        @Schema(description = "실패 횟수", example = "9")
        private Integer failCount;
    }
}
