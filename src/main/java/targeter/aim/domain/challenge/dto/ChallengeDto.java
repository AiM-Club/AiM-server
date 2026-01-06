package targeter.aim.domain.challenge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import targeter.aim.domain.challenge.entity.ChallengeMode;
import targeter.aim.domain.challenge.entity.ChallengeStatus;
import targeter.aim.domain.challenge.entity.ChallengeVisibility;
import targeter.aim.domain.file.dto.FileDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
