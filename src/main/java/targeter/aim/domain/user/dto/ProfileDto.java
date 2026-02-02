package targeter.aim.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;
import targeter.aim.domain.file.dto.FileDto;

import java.util.List;

public class ProfileDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "프로필 조회 응답")
    public static class ProfileResponse {

        @Schema(description = "유저 ID")
        private Long userId;

        @Schema(description = "아이디(loginId)")
        private String loginId;

        @Schema(description = "닉네임")
        private String nickname;

        @Schema(description = "뱃지/티어 정보")
        private TierResponse tier;

        @Schema(description = "레벨")
        private Integer level;

        @Schema(description = "프로필 이미지")
        private FileDto.FileResponse profileImage;

        @Schema(description = "관심사(태그)")
        private List<String> interests;

        @Schema(description = "관심 분야")
        private List<String> fields;

        @Schema(description = "전체 챌린지 기록")
        private ChallengeRecord overall;

        @Schema(description = "SOLO 기록")
        private ChallengeRecord solo;

        @Schema(description = "VS 기록")
        private ChallengeRecord vs;

        @Schema(description = "본인 프로필 여부")
        private Boolean isMine;
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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "프로필 수정 요청")
    public static class ProfileUpdateRequest {

        @Schema(description = "닉네임(10자 이하)")
        private String nickname;

        @Schema(description = "관심사(태그) - 문자열 목록")
        private List<String> interests;

        @Schema(description = "관심 분야 - 문자열 목록")
        private List<String> fields;

        @Schema(description = "프로필 이미지(<=10MB, jpg/png/pdf)")
        private MultipartFile profileImage;
    }
}