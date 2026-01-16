package targeter.aim.domain.challenge.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import targeter.aim.domain.challenge.dto.ChallengeDto;
import targeter.aim.domain.challenge.service.ChallengeService;
import targeter.aim.system.security.annotation.NoJwtAuth;
import targeter.aim.system.security.model.UserDetails;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/challenges")
@Tag(name = "Challenge", description = "챌린지 관련 API")
public class ChallengeController {

    private final ChallengeService challengeService;

    @PostMapping
    @Operation(
            summary = "챌린지 생성",
            description = "새로운 챌린지를 생성합니다."
    )
    @ApiResponse(responseCode = "201", description = "챌린지 생성 성공")
    @ResponseStatus(HttpStatus.CREATED)
    public ChallengeDto.ChallengeCreateResponse createChallenge(
            @RequestBody ChallengeDto.ChallengeCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return challengeService.createChallenge(userDetails, request);
    }

    @NoJwtAuth
    @GetMapping("/vs")
    @Operation(
            summary = "VS 챌린지 목록 조회",
            description = "VS 챌린지 목록을 탭(ALL/MY)과 정렬 조건에 따라 페이지네이션 조회합니다."
    )
    public ChallengeDto.ChallengePageResponse getVsChallenges(
            @ModelAttribute @ParameterObject ChallengeDto.ListSearchCondition condition,
            @PageableDefault(size = 16) @ParameterObject Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return challengeService.getVsChallenges(condition, userDetails, pageable);
    }

    @NoJwtAuth("VS 챌린지 상세 조회는 인증을 필요로 하지 않음")
    @GetMapping("/vs/{challengeId}/overview")
    @Operation(
            summary = "VS 챌린지 상세 Overview 조회",
            description = "특정 VS 챌린지의 상세 정보와 우세현황 및 챌린지 멤버 정보를 조회합니다."
    )
    public ChallengeDto.VsChallengeOverviewResponse getVsChallengeOverview(
            @PathVariable Long challengeId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return challengeService.getVsChallengeOverview(challengeId, userDetails);
    }

    @GetMapping("/vs/{challengeId}/weeks")
    @Operation(
            summary = "VS 챌린지 주차별 내용 리스트 조회",
            description = "특정 VS 챌린지의 주차별 내용을 리스트 형태로 전체 조회합니다."
    )
    public ChallengeDto.WeekProgressListResponse getWeeklyProgressList(
            @PathVariable Long challengeId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return challengeService.getVsWeeklyProgressList(challengeId, userDetails);
    }
}