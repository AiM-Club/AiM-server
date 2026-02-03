package targeter.aim.domain.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;
import targeter.aim.domain.file.dto.FileDto;
import targeter.aim.domain.post.entity.Comment;
import targeter.aim.domain.user.dto.TierDto;
import targeter.aim.domain.user.dto.UserDto;
import targeter.aim.domain.user.entity.User;

import java.time.LocalDateTime;
import java.util.List;

public class CommentDto {

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
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "게시글 댓글 목록 페이지 응답")
    public static class CommentPageResponse {
        private List<CommentResponse> content;
        private PageInfo page;

        public static CommentPageResponse from(Page<CommentResponse> page) {
            return new CommentPageResponse(
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
    @Schema(description = "게시글 댓글 조회 응답")
    public static class CommentResponse {
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
        private List<CommentDto.CommentResponse> childrenComments;

        public static CommentDto.CommentResponse from(Comment comment) {
            return CommentDto.CommentResponse.builder()
                    .commentId(comment.getId())
                    .depth(comment.getDepth())
                    .writerInfo(UserResponse.from(comment.getUser()))
                    .content(comment.getContents())
                    .attachedImages(comment.getAttachedImages().stream()
                            .map(FileDto.FileResponse::from)
                            .toList())
                    .attachedFiles(comment.getAttachedFiles().stream()
                            .map(FileDto.FileResponse::from)
                            .toList())
                    .createdAt(comment.getCreatedAt())
                    .childrenComments(List.of())
                    .build();
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "게시글 댓글 생성 요청")
    public static class CommentCreateRequest {
        @Schema(description = "부모 댓글 아이디(대댓글의 경우메만 사용", example = "10")
        private Long parentCommentId;

        @Schema(description = "댓글 내용", example = "댓글 내용입니다.")
        private String content;

        @Schema(description = "첨부 이미지 목록")
        private List<MultipartFile> attachedImages;

        @Schema(description = "첨부 파일 목록")
        private List<MultipartFile> attachedFiles;

        public Comment toEntity() {
            return Comment.builder()
                    .contents(content)
                    .build();
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "게시글 댓글 생성 응답")
    public static class CommentCreateResponse {
        @Schema(description = "게시글 아이디", example = "1")
        private Long postId;

        @Schema(description = "댓글 아이디", example = "1")
        private Long commentId;

        @Schema(description = "댓글 깊이(댓글이면 1, 대댓글이면 2", example = "1 | 2")
        private Integer depth;
    }
}
