package targeter.aim.domain.challenge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;
import targeter.aim.domain.challenge.entity.WeeklyComment;
import targeter.aim.domain.file.dto.FileDto;
import targeter.aim.domain.user.dto.TierDto;
import targeter.aim.domain.user.entity.User;

import java.time.LocalDateTime;
import java.util.List;

public class WeeklyCommentDto {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "페이지 정보")
    public static class PageInfo {
        private int page;

        private int size;

        private long totalElements;

        private int totalPages;

        public static PageInfo from(Page<?> page) {
            return PageInfo.builder()
                    .page(page.getNumber())
                    .size(page.getSize())
                    .totalElements(page.getTotalElements())
                    .totalPages(page.getTotalPages())
                    .build();
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "챌린지 주차별 댓글 목록 조회 응답")
    public static class WeeklyCommentPageResponse {
        @Schema(description = "댓글 목록(부모 댓글 + childrenComments 포함)")
        private List<WeeklyCommentResponse> comments;

        @Schema(description = "페이지 메타 정보")
        private PageInfo pageInfo;

        public static WeeklyCommentPageResponse from(Page<WeeklyCommentResponse> page) {
            return new WeeklyCommentPageResponse(
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
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "댓글 작성자 정보")
    public static class UserResponse {

        @Schema(description = "유저 아이디", example = "1")
        private Long userId;

        @Schema(description = "유저 닉네임", example = "닉네임")
        private String nickname;

        @Schema(description = "티어명", example = "BRONZE")
        private TierDto.TierResponse tier;

        @Schema(description = "프로필 이미지")
        private FileDto.FileResponse profileImage;

        public static UserResponse from(User user) {
            return UserResponse.builder()
                    .userId(user.getId())
                    .nickname(user.getNickname())
                    .tier(TierDto.TierResponse.from(user.getTier()))
                    .profileImage(FileDto.FileResponse.from(user.getProfileImage()))
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

        @Schema(description = "작성자 정보")
        private UserResponse writerInfo;

        @Schema(description = "댓글 내용", example = "댓글 내용")
        private String content;

        @Schema(description = "첨부된 이미지")
        private List<FileDto.FileResponse> attachedImages;

        @Schema(description = "첨부된 파일")
        private List<FileDto.FileResponse> attachedFiles;

        @Schema(description = "댓글 작성 날짜", example = "ISO DateTime")
        private LocalDateTime createdAt;

        @Schema(description = "자식 댓글 목록")
        private List<WeeklyCommentResponse> childrenComments;

        public static WeeklyCommentResponse from(WeeklyComment weeklyComment) {
            return WeeklyCommentResponse.builder()
                    .commentId(weeklyComment.getId())
                    .depth(weeklyComment.getDepth())
                    .writerInfo(UserResponse.from(weeklyComment.getUser()))
                    .content(weeklyComment.getContent())
                    .attachedImages(weeklyComment.getAttachedImages().stream()
                            .map(FileDto.FileResponse::from)
                            .toList())
                    .attachedFiles(weeklyComment.getAttachedFiles().stream()
                            .map(FileDto.FileResponse::from)
                            .toList())
                    .createdAt(weeklyComment.getCreatedAt())
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