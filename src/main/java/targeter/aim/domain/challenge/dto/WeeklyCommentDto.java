package targeter.aim.domain.challenge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;
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
    @Schema(description = "페이지 정보")
    public static class PageInfo {
        @Schema(description = "현재 페이지(0부터 시작)", example = "0")
        private int page;

        @Schema(description = "페이지 크기", example = "10")
        private int size;

        @Schema(description = "전체 요소 수", example = "123")
        private long totalElements;

        @Schema(description = "전체 페이지 수", example = "13")
        private int totalPages;

        @Schema(description = "다음 페이지 존재 여부", example = "true")
        private boolean hasNext;

        public static PageInfo from(Page<?> page) {
            return PageInfo.builder()
                    .page(page.getNumber())
                    .size(page.getSize())
                    .totalElements(page.getTotalElements())
                    .totalPages(page.getTotalPages())
                    .hasNext(page.hasNext())
                    .build();
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "챌린지 주차별 댓글 목록 조회 응답")
    public static class WeeklyCommentListResponse {
        @Schema(description = "댓글 목록(부모 댓글 + childrenComments 포함)")
        private List<WeeklyCommentResponse> comments;

        @Schema(description = "페이지 메타 정보")
        private PageInfo pageInfo;

        public static WeeklyCommentListResponse of(
                Page<?> page,
                List<WeeklyCommentResponse> comments
        ) {
            return WeeklyCommentListResponse.builder()
                    .pageInfo(PageInfo.from(page))
                    .comments(comments)
                    .build();
        }
    }

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
    @Schema(description = "챌린지 주차별 댓글 생성 요청")
    public static class WeeklyCommentCreateRequest {
        @Schema(description = "부모 댓글 아이디(대댓글의 경우메만 사용", example = "10")
        private Long parentCommentId;

        @Schema(description = "댓글 내용", example = "댓글 내용입니다.")
        private String content;

        @Schema(description = "첨부 이미지 목록")
        private List<MultipartFile> attachedImages;

        @Schema(description = "첨부 파일 목록")
        private List<MultipartFile> attachedFiles;

        public WeeklyComment toEntity() {
            return WeeklyComment.builder()
                    .content(content)
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

        @Schema(description = "댓글 아이디", example = "1")
        private Long commentId;

        @Schema(description = "댓글 깊이(댓글이면 1, 대댓글이면 2", example = "1 | 2")
        private Integer depth;
    }
}