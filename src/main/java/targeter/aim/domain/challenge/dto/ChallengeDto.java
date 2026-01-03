package targeter.aim.domain.challenge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import targeter.aim.domain.challenge.entity.ChallengeMode;
import targeter.aim.domain.challenge.entity.ChallengeStatus;
import targeter.aim.domain.challenge.entity.ChallengeVisibility;
import targeter.aim.domain.file.dto.FileDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ChallengeDto {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ProgressCreateRequest {
        private String name;

        private LocalDate startedAt;

        private Integer duration;

        private List<String> tags;

        private List<String> fields;

        private List<String> jobs;

        private String userRequest;

        private ChallengeMode mode;

        private ChallengeVisibility visibility;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "VS 챌린지 목록 조회 조건")
    public static class ListSearchCondition {

        @Builder.Default
        @Schema(
                description = "탭 필터 (전체 / 내 챌린지 / 초대)",
                example = "ALL",
                allowableValues = {"ALL", "MY"}
        )
        private String filterType = "ALL";

        @Builder.Default
        @Schema(
                description = "정렬 기준",
                example = "LATEST",
                allowableValues = {"LATEST", "OLDEST", "TITLE", "ONGOING", "FINISHED"}
        )
        private String sort = "LATEST";
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "챌린지 작성자 정보")
    public static class UserResponse {
        private Long userId;

        private String nickname;

        private String badge;

        private FileDto.FileResponse profileImage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Builder
    @Schema(description = "챌린지 목록 응답")
    public static class ChallengeListResponse {
        private UserResponse user;

        private LocalDate startDate;

        private String duration;

        private String name;

        private List<String> fields;

        private List<String> tags;

        private String job;

        private Boolean liked;

        private Integer likeCount;

        private LocalDateTime createdAt;

        private LocalDateTime lastModifiedAt;

        private ChallengeStatus status;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "페이지네이션 정보")
    public static class PageInfo {
        private int size;

        private int number;

        private long totalElements;

        private int totalPages;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "챌린지 목록 페이지 응답")
    public static class ChallengePageResponse {
        private List<ChallengeListResponse> content;

        private PageInfo page;

        public static ChallengePageResponse from(org.springframework.data.domain.Page<ChallengeListResponse> page) {
            return new ChallengePageResponse(
                    page.getContent(),
                    new PageInfo(
                            page.getSize(),
                            page.getNumber(),
                            page.getTotalElements(),
                            page.getTotalPages()
                    )
            );
        }
    }
}
