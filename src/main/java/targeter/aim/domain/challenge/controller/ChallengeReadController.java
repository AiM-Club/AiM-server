package targeter.aim.domain.challenge.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import targeter.aim.domain.challenge.dto.ChallengeDto;
import targeter.aim.domain.challenge.service.ChallengeReadService;
import targeter.aim.system.security.annotation.NoJwtAuth;
import targeter.aim.system.security.model.UserDetails;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/challenges")
@Tag(name = "Challenge (Read)", description = "챌린지 조회 및 검색 관련 API")
public class ChallengeReadController {

    private final ChallengeReadService challengeService;

    @NoJwtAuth
    @GetMapping("/vs")
    @Operation(
            summary = "VS 챌린지 목록 조회",
            description = "VS 챌린지 목록을 탭(ALL/MY)과 정렬 조건에 따라 페이지네이션 조회합니다."
    )
    public ChallengeDto.ChallengePageResponse getVsChallenges(
            @ModelAttribute @ParameterObject ChallengeDto.VsListSearchCondition condition,
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

    @GetMapping("/solo")
    @Operation(
            summary = "SOLO 챌린지 목록 조회",
            description = "로그인한 사용자가 SOLO 챌린지 목록을 탭(진행 중/진행 완료)과 정렬 조건에 따라 페이지네이션 조회합니다."
    )
    public ChallengeDto.ChallengePageResponse getSoloChallenges(
            @ModelAttribute @ParameterObject ChallengeDto.SoloListSearchCondition request,
            @PageableDefault(size = 16) @ParameterObject Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return challengeService.getSoloChallenges(request, userDetails, pageable);
    }

    @GetMapping("/solo/{challengeId}/overview")
    @Operation(
            summary = "SOLO 챌린지 상세 Overview 조회",
            description = "로그인한 사용자가 특정 SOLO 챌린지의 상세 정보와 주최자 진행 현황을 조회합니다."
    )
    public ChallengeDto.SoloChallengeOverviewResponse getSoloChallengeOverview(
            @PathVariable Long challengeId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return challengeService.getSoloChallengeOverview(challengeId, userDetails);
    }

    @GetMapping("/all")
    @Operation(
            summary = "ALL 챌린지 목록 조회",
            description = "로그인한 사용자가 참여한 모든 SOLO/VS 챌린지를 정렬 조건과 함께 조회합니다."
    )
    public ChallengeDto.ChallengePageResponse getAllChallenges(
            @ModelAttribute @ParameterObject ChallengeDto.AllListSearchCondition request,
            @PageableDefault(size = 16) @ParameterObject Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return challengeService.getAllChallenges(request, userDetails, pageable);
    }
    @NoJwtAuth
    @GetMapping("/search")
    @Operation(
            summary = "챌린지 검색",
            description = "키워드 기반으로 공개(PUBLIC) + (로그인 시) 내가 참여한 PRIVATE 챌린지까지 검색합니다."
    )
    public ChallengeDto.ChallengePageResponse searchChallenges(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "LATEST") ChallengeDto.ChallengeSortType sort,
            @PageableDefault(size = 16) @ParameterObject Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return challengeService.searchChallenges(keyword, sort, pageable, userDetails);
    }
}
