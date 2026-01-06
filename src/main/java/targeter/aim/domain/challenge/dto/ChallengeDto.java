package targeter.aim.domain.challenge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import targeter.aim.domain.challenge.entity.ChallengeMode;
import targeter.aim.domain.challenge.entity.ChallengeVisibility;

import java.time.LocalDate;
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
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
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
            private String thumbnail;      // url_to_image
            private String title;          // 챌린지 이름
            private List<String> tags;     // 태그
            private List<String> fields;   // 분야(카테고리)
            private String job;            // 직무
            private String startDate;      // yyyy-MM-dd
            private Integer totalWeeks;    // 총 기간
            private String state;          // IN_PROGRESS / COMPLETED
        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        public static class Participants {
            private Participant me;
            private Participant opponent;
        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        public static class Participant {
            private String profileImage;
            private String nickname;
            private String progressRate;       // "현재/전체"
            private Integer successRate;       // 성공률(%)
            private Boolean isSuccess;         // 70% 이상 여부
            private Boolean isRealTimeActive;  // 상대방 실시간 상태
        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        public static class CurrentWeekDetail {
            private Integer weekNumber;
            private String period;         // yyyy-MM-dd ~ yyyy-MM-dd
            private String aiTitle;
            private String aiContent;
            private String recordTime;     // 01:20:30
            private String authFile;       // url_to_auth_file (현재는 null)
            private Boolean isFinished;    // finish 버튼 여부
        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        public static class CommentNode {
            private Long commentId;
            private String writer;
            private String content;
            private String createdAt;
            private List<CommentNode> children;
        }
    }
}
