package targeter.aim.domain.challenge.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import targeter.aim.domain.challenge.dto.ChallengeLikedDto;
import targeter.aim.system.security.model.UserDetails;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/challenges/{challengeId}/likes")
@Tag(name = "Challenge", description = "챌린지 관련 API")
public class ChallengeLikedController {

    @PostMapping
    @Operation(
            summary = "챌린지 좋아요 토글",
            description = "챌린지에 좋아요를 누르거나 취소합니다."
    )
    public ChallengeLikedDto.LikedResponse toggleLike(
            @PathVariable Long challengeId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        boolean liked = challengeLikedService.toggleLike(
                userDetails.getUser().getId(),
                challengeId
        );

        return new ChallengeLikedDto.LikedResponse(liked);
    }

}
