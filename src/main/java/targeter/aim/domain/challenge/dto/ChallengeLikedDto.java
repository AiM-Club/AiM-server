package targeter.aim.domain.challenge.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class ChallengeLikedDto {

    @Getter
    @AllArgsConstructor
    public static class LikeResponse {
        private boolean likes;
    }
}
