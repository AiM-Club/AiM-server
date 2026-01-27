package targeter.aim.domain.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import targeter.aim.domain.user.dto.UserDto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CommentDto {

    @Getter
    @NoArgsConstructor
    public static class CreateRequest {
        @Schema(description = "댓글 내용", example = "댓글 내용입니다.")
        private String content;
    }

    @Getter
    public static class CreateResponse {
        private final Long postId;

        public CreateResponse(Long postId) {
            this.postId = postId;
        }
    }

    @Getter
    public static class CommentResponse {
        private final Long commentId;
        private final Integer depth;
        private final UserDto.UserResponse writerInfo;
        private final String content;
        private final List<String> attachedImages;
        private final List<String> attachedFiles;
        private final LocalDateTime createdAt;
        private final LocalDateTime updatedAt;
        private final List<CommentResponse> childrenComment;

        public CommentResponse(
                Long commentId,
                Integer depth,
                UserDto.UserResponse writerInfo,
                String content,
                List<String> attachedImages,
                List<String> attachedFiles,
                LocalDateTime createdAt,
                LocalDateTime updatedAt
        ) {
            this.commentId = commentId;
            this.depth = depth;
            this.writerInfo = writerInfo;
            this.content = content;
            this.attachedImages = attachedImages == null ? new ArrayList<>() : attachedImages;
            this.attachedFiles = attachedFiles == null ? new ArrayList<>() : attachedFiles;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            this.childrenComment = new ArrayList<>();
        }

        public void addChild(CommentResponse child) {
            this.childrenComment.add(child);
        }
    }
}
