package targeter.aim.domain.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import targeter.aim.domain.post.entity.Post;

public class PostLikedDto {
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "VS 모집글 목록 조회 조건")
    public static class PostLikedResponse {
        @Schema(description = "게시글 아이디", example = "1")
        private Long id;

        @Schema(description = "좋아요 여부(true면 좋아요 누른 것입니다.)", example = "true | false")
        private Boolean isLiked;

        public static PostLikedResponse from(Post post, boolean liked) {
            return PostLikedResponse.builder()
                    .id(post.getId())
                    .isLiked(liked)
                    .build();
        }
    }
}
