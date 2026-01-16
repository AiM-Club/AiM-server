package targeter.aim.domain.label.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import targeter.aim.domain.label.entity.Field;

public class FieldDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "분야 목록 응답")
    public static class FieldResponse {
        @Schema(description = "분야 ID", example = "1")
        private Long id;

        @Schema(description = "분야명", example = "IT")
        private String name;

        public static FieldResponse from(Field field) {
            return FieldResponse.builder()
                    .id(field.getId())
                    .name(field.getName())
                    .build();
        }
    }
}
