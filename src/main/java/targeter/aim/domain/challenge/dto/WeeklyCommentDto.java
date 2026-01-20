package targeter.aim.domain.challenge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import targeter.aim.domain.challenge.entity.WeeklyComment;
import targeter.aim.domain.file.dto.FileDto;
import targeter.aim.domain.user.dto.UserDto;

import java.time.LocalDateTime;
import java.util.List;

public class WeeklyCommentDto {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "챌린지 주차별 댓글 조회 응답")
    public static class WeeklyCommentResponse {
        @Schema(description = "댓글 아이디", example = "1")
        private Long commentId;

        @Schema(description = "댓글/대댓글 깊이(1: 댓글, 2:대댓글)", example = "1")
        private Integer depth;

        @Schema(description = "작성자 정보, 여기서 nickname, profileImage, tier 사용")
        private UserDto.UserResponse writerInfo;

        @Schema(description = "댓글 내용", example = "댓글 내용")
        private String content;

        @Schema(description = "첨부된 이미지")
        private List<FileDto.FileResponse> attachedImages;

        @Schema(description = "첨부된 파일")
        private List<FileDto.FileResponse> attachedFiles;

        @Schema(description = "댓글 작성 날짜", example = "ISO DateTime")
        private LocalDateTime createdAt;

        @Schema(description = "최종 수정 날짜", example = "ISO DateTime")
        private LocalDateTime updatedAt;

        @Schema(description = "자식 댓글 목록")
        private List<WeeklyCommentResponse> childrenComments;

        public static WeeklyCommentResponse from(WeeklyComment weeklyComment) {
            return WeeklyCommentResponse.builder()
                    .commentId(weeklyComment.getId())
                    .depth(weeklyComment.getDepth())
                    .writerInfo(UserDto.UserResponse.from(weeklyComment.getUser()))
                    .content(weeklyComment.getContent())
                    .attachedImages(List.of())
                    .attachedFiles(List.of())
                    .createdAt(weeklyComment.getCreatedAt())
                    .updatedAt(weeklyComment.getLastModifiedAt())
                    .childrenComments(List.of())
                    .build();
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "챌린지 주차별 댓글 생성 응답")
    public static class WeeklyCommentCreateResponse {
        @Schema(description = "챌린지 아이디", example = "1")
        private Long challengeId;

        @Schema(description = "주차별 진행상황 아이디", example = "1")
        private Long weeksId;
    }
}