package targeter.aim.domain.challenge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import targeter.aim.domain.challenge.entity.ChallengeMode;
import targeter.aim.domain.challenge.entity.ChallengeVisibility;

import java.time.LocalDate;
import java.util.List;

public class ChallengeDto {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ProgressCreateRequest {
        private String name;

        private LocalDate startedAt;

        private Integer duration;

        private List<String> tags;

        private List<String> fields;

        private List<String> jobs;

        private String userRequest;

        private ChallengeMode mode;

        private ChallengeVisibility visibility;
    }
}
