package targeter.aim.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class MyPageDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MyPageResponse {

        @Schema(description = "현재 레벨", example = "37")
        private int level;

        @Schema(description = "현재 티어", example = "SILVER")
        private TierDto.TierResponse tier;

        @Schema(description = "티어 진행률 (%)", example = "45")
        private int tierProgressPercent;

        @Schema(
                description = "다음 티어 (다이아몬드인 경우 null)",
                example = "GOLD",
                nullable = true
        )
        private TierDto.TierResponse nextTier;
    }
}