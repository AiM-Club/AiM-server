package targeter.aim.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import targeter.aim.domain.user.entity.Tier;

public class TierDto {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "티어 정보 응답")
    public static class TierResponse {

        @Schema(description = "티어명", example = "BRONZE | SILVER | GOLD | DIAMOND")
        private String name;

        public static TierResponse from(Tier tier) {
            return TierResponse.builder()
                    .name(tier.getName())
                    .build();
        }
    }
}
