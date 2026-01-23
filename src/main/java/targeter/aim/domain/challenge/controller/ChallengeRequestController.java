package targeter.aim.domain.challenge.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    private final ChallengeRequestService challengeRequestService;

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
        return challengeRequestService.getVsRequestList(userDetails, page, condition);
    }

    @PostMapping("/{requestId}/approve")
    @Operation(
            summary = "VS 챌린지 요청 수락",
            description = "VS 챌린지 요청을 수락합니다. 수락된 해당 챌린지로 이동합니다."
    )
    public ChallengeRequestDto.RequestAccessResponse approveVsRequest(
            @PathVariable Long requestId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails
    ) {
        return challengeRequestService.approveRequest(requestId, userDetails);
    }

    @DeleteMapping("/{requestId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "VS 챌린지 요청 거절",
            description = "VS 챌린지 요청을 거절합니다. 해당 요청은 삭제됩니다."
    )
    @ApiResponse(responseCode = "204", description = "요청 거절 성공")
    public void rejectVsRequest(
            @PathVariable Long requestId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails
    ) {
        challengeRequestService.rejectRequest(requestId, userDetails);
    }
}