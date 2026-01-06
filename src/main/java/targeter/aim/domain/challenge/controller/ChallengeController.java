package targeter.aim.domain.challenge.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import targeter.aim.domain.challenge.dto.ChallengeDto;
import targeter.aim.domain.challenge.service.ChallengeService;
import targeter.aim.system.security.model.UserDetails;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/challenge")
public class ChallengeController {
    private final ChallengeService challengeService;

    @PostMapping
    public ChallengeDto.ChallengeDetailsResponse createChallenge(
            @RequestBody ChallengeDto.ChallengeCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return challengeService.createChallenge(userDetails, request);
    }
}
