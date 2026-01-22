package targeter.aim.domain.challenge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import targeter.aim.domain.challenge.entity.Challenge;
import targeter.aim.domain.challenge.entity.WeeklyProgress;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public class WeeklyProgressDto {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "챌린지 주차별 내용 조회 응답")
    public static class WeekProgressListResponse {
        @Schema(description = "챌린지 아이디(댓글 연동 용)", example = "1")
        private Long challengeId;

        @Schema(description = "총 주차 수(확인용)", example = "4")
        private Integer totalWeeks;

        @Schema(description = "현재 주차", example = "1")
        private Integer currentWeek;

        @Schema(description = "각 주차별 세부 내용 리스트")
        private List<WeeklyProgressDto.WeekProgressListResponse.WeeklyProgressDetails> progressList;

        public static WeeklyProgressDto.WeekProgressListResponse from(Challenge challenge, Integer currentWeek, List<WeeklyProgress> weeklyProgressList) {
            return WeekProgressListResponse.builder()
                    .challengeId(challenge.getId())
                    .totalWeeks(challenge.getDurationWeek())
                    .currentWeek(currentWeek)
                    .progressList(
                            weeklyProgressList == null ? List.of()
                                    :  weeklyProgressList.stream()
                                    .sorted(Comparator.comparing(WeeklyProgress::getWeekNumber))
                                    .map(weeklyProgress -> WeeklyProgressDto.WeekProgressListResponse.WeeklyProgressDetails.from(weeklyProgress, challenge.getStartedAt()))
                                    .toList()
                    )
                    .build();
        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        public static class WeeklyProgressDetails {
            @Schema(description = "주차 번호", example = "1")
            private Integer weekNumber;

            @Schema(description = "주차 시작 일", example = "2026-01-01")
            private LocalDate weekStartDate;

            @Schema(description = "주차 종료 일", example = "2026-01-06")
            private LocalDate weekEndDate;

            @Schema(description = "주차별 챌린지 제목", example = "1주차 : AI가 만든 챌린지 제목")
            private String title;

            @Schema(description = "주차별 챌린지 내용", example = "1주차에 진행할 챌린지 내용입니다. AI가 작성합니다.")
            private String content;

            @Schema(description = "스톱워치 기록(초), 만약 api를 다시 불러와도 최근 저장된 시간을 불러옴", example = "300")
            private Integer stopwatchTimeSeconds;

            @Schema(description = "주차별 챌린지 완료 여부(true/false)", example = "false")
            private Boolean isComplete;

            public static WeeklyProgressDto.WeekProgressListResponse.WeeklyProgressDetails from(WeeklyProgress weeklyProgress, LocalDate challengeStartDate) {
                Integer weekNumber = weeklyProgress.getWeekNumber();
                LocalDate weekStart = challengeStartDate.plusDays((long) (weekNumber - 1) * 7);

                return WeeklyProgressDto.WeekProgressListResponse.WeeklyProgressDetails.builder()
                        .weekNumber(weekNumber)
                        .weekStartDate(weekStart)
                        .weekEndDate(weekStart.plusDays(6))
                        .title(weeklyProgress.getTitle())
                        .content(weeklyProgress.getContent())
                        .stopwatchTimeSeconds(weeklyProgress.getStopwatchTimeSeconds())
                        .isComplete(weeklyProgress.isComplete())
                        .build();
            }
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "챌린지 인증 파일 업로드 요청")
    public static class ProofUploadRequest {
        @Schema(description = "인증하고 싶은 챌린지 주차 아이디", example = "1")
        private Long weeklyProgressId;

        @Schema(description = "인증 이미지 목록")
        private List<MultipartFile> attachedImages;

        @Schema(description = "인증 파일 목록")
        private List<MultipartFile> attachedFiles;
    }
}
