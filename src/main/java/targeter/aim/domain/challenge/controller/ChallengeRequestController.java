package targeter.aim.domain.challenge.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import targeter.aim.domain.challenge.dto.ChallengeRequestDto;
import targeter.aim.domain.challenge.service.ChallengeRequestService;
import targeter.aim.system.security.model.UserDetails;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/challenges/vs/request")
@Tag(name = "VS Challenge Request", description = "VS 챌린지 초대 관련 API")
public class ChallengeRequestController {

    private final ChallengeRequestService queryService;

    @GetMapping
    @Operation(
            summary = "VS 챌린지 신청/초대 목록 조회",
            description = "로그인 유저(host/applier)에게 들어온 PENDING 요청을 10개씩 페이지네이션 조회합니다. sort/keyword 지원."
    )
    public ChallengeRequestDto.ChallengeRequestPageResponse getVsRequestList(
            @ModelAttribute ChallengeRequestDto.RequestListCondition condition,
            @RequestParam(defaultValue = "0") int page,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails
    ) {
        return queryService.getVsRequestList(userDetails, page, condition);
    }
}