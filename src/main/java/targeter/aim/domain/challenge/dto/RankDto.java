package targeter.aim.domain.challenge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import targeter.aim.domain.file.dto.FileDto;

public class RankDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "TOP20 랭킹 조회 응답")
    public static class Top20RankResponse {

        @Schema(description = "순위")
        private int rank;

        @Schema(description = "유저 ID")
        private Long userId;

        @Schema(description = "유저 정보")
        private UserInfo userInfo;

        @Schema(description = "전체 기록")
        private ChallengeRecord allRecord;

        @Schema(description = "SOLO 기록 (1~3위만 내려줌)")
        private ChallengeRecord soloRecord;

        @Schema(description = "VS 기록 (1~3위만 내려줌)")
        private ChallengeRecord vsRecord;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private String nickname;
        private FileDto.FileResponse profileImage;
        private TierResponse tier;
        private Integer level;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TierResponse {
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "챌린지 기록(시도/성공/실패/성공률)")
    public static class ChallengeRecord {
        private long attemptCount;
        private long successCount;
        private long failCount;

        @Schema(description = "성공률(0~100)")
        private int successRate;
    }
}