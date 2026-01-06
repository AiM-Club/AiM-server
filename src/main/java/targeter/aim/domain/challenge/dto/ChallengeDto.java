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
    public static class ChallengeCreateRequest {
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

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ChallengeDetailsResponse {
        private ChallengeInfo challengeInfo;        // 전체 챌린지 정보

        private Participants participants;          // 참여자 정보 (vs에는 me,opponent / solo에는 me)

        private CurrentWeekDetails currentWeekDetails;  // 이번주 챌린지 내용 상세 정보

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        public static class ChallengeInfo {
            private FileDto.FileResponse challengeThumbnail;

            private String title;

            private List<String> tags;

            private List<String> fields;

            private List<String> jobs;

            private LocalDate startedAt;

            private Integer durationWeek;

            private ChallengeStatus status;
        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        public static class Participants {
            private ParticipantDetails me;

            private ParticipantDetails opponent;
        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        public static class ParticipantDetails {
            private FileDto.FileResponse profileImage;

            private String nickname;

            private String progressRate;

            private Integer successRate;

            private Boolean isSuccess;

            private Boolean isRealTimeActive;
        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        public static class CurrentWeekDetails {
            private Integer weekNumber;

            private String period;

            private String weekTitle;

            private String weekContent;

            private String recordTime;

            private FileDto.FileResponse certifiedFile;

            private Boolean isFinished;

            private List<CommentDetails> comments;
        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        public static class CommentDetails {
            private Long commentId;

            private String writer;

            private String content;

            private LocalDateTime createdAt;

            private List<CommentDetails> replyComments;
        }
    }
//
//    @Data
//    @AllArgsConstructor
//    @NoArgsConstructor
//    @Builder
//    public static class ProgressCreateRequest {
//        private String name;
//
//        private LocalDate startedAt;
//
//        private Integer duration;
//
//        private List<String> tags;
//
//        private List<String> fields;
//
//        private List<String> jobs;
//
//        private String userRequest;
//    }
}
