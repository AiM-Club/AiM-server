package targeter.aim.domain.label.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import targeter.aim.domain.label.entity.Tag;

public class TagDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "태그 목록 응답")
    public static class TagResponse {
        private Long id;

        private String name;

        public static TagResponse from(Tag tag) {
            return TagResponse.builder()
                    .id(tag.getId())
                    .name(tag.getName())
                    .build();
        }
    }
}
