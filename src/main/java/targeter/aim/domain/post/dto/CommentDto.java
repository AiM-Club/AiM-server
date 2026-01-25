package targeter.aim.domain.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
}
