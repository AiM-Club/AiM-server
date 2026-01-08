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
    @Schema(description = "챌린지 생성 요청")
    public static class ChallengeCreateRequest {
        @Schema(description = "챌린지 이름", example = "다이어트 챌린지")
        private String name;

        @Schema(description = "시작일", example = "2026-01-01")
        private LocalDate startedAt;

        @Schema(description = "기간(주)", example = "6")
        private Integer duration;

        @Schema(description = "태그 목록(1~3개)", example = "[\"태그1\", \"태그2\", \"태그3\"]")
        private List<String> tags;

        @Schema(description = "분야 목록(1~3개)", example = "[\"IT\", \"경영\"]")
        private List<String> fields;

        @Schema(description = "직무 목록(1~3개)", example = "[\"직업1\"]")
        private List<String> jobs;

        @Schema(description = "AI 요청사항", example = "4주동안 빠르게 다이어트할 수 있는 방법을 알려줘. 금식은 최대한 자제할거야.")
        private String userRequest;

        @Schema(description = "챌린지 모드", example = "SOLO", allowableValues = {"SOLO", "VS"})
        private ChallengeMode mode;

        @Schema(description = "공개 여부", example = "PUBLIC", allowableValues = {"PUBLIC", "PRIVATE"})
        private ChallengeVisibility visibility;
    }

    // 챌린지 생성 테스트 용도, 추후 챌린지 상세보기 개발 시 수정
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ChallengeDetailsResponse {

        private ChallengeInfo challengeInfo;// 전체 챌린지 정보

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

    //  스펙 기반 VS 챌린지 상세 응답
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "VS 챌린지 상세 조회 응답")
    public static class VsChallengeDetailResponse {

        private ChallengeInfo challengeInfo;

        private Participants participants;

        private CurrentWeekDetail currentWeekDetail;

        private List<CommentNode> comments;

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        public static class ChallengeInfo {
            private String thumbnail;   // url_to_image

            private String title;       // 챌린지 이름

            private List<String> tags;  // 1~3개

            private String category;    // 홈 카테고리(서버에서는 fields 중 1개를 대표로 내려줌)

            private String job;         // 유저 작성 직무

            private String startDate;   // yyyy-MM-dd

            private Integer totalWeeks; // 총 기간

            private String state;       // IN_PROGRESS 등
        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        public static class Participants {
            private Me me;
            private Opponent opponent;
        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        public static class Me {
            private String profileImage; // url_to_profile

            private String nickname;

            private String progressRate; // current/total

            private Integer successRate; // %

            private Boolean isSuccess;   // 70% 이상 여부
        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        public static class Opponent {
            private String profileImage;     // url_to_profile

            private String nickname;

            private String progressRate;     // current/total

            private Integer successRate;     // %

            private Boolean isRealTimeActive; // 실시간 현황
        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        public static class CurrentWeekDetail {
            private Integer weekNumber;

            private String period;     // yyyy-MM-dd ~ yyyy-MM-dd

            private String aiTitle;

            private String aiContent;

            private String recordTime; // HH:mm:ss

            private Boolean isFinished;
        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        public static class CommentNode {
            private Long commentId;

            private String writer;

            private String content;

            private String createdAt; // ISO Datetime string

            private List<CommentNode> children;
        }
    }
}