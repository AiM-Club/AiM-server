package targeter.aim.domain.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import targeter.aim.domain.challenge.dto.ChallengeDto;
import targeter.aim.domain.post.dto.PostDto;

public class SearchDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "통합 검색 응답")
    public static class SearchResponse {

        @Schema(description = "게시글 검색 결과")
        private PostDto.PostPageResponse posts;

        @Schema(description = "챌린지 검색 결과")
        private ChallengeDto.ChallengePageResponse challenges;
    }
}
