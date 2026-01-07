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

    @NoJwtAuth
    @GetMapping
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

    @PostMapping
    @Operation(
            summary = "챌린지 생성",
            description = "새로운 챌린지를 생성합니다."
    )
    @ApiResponse(responseCode = "201", description = "챌린지 생성 성공")
    @ResponseStatus(HttpStatus.CREATED)
    public ChallengeDto.ChallengeDetailsResponse createChallenge(
            @RequestBody ChallengeDto.ChallengeCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return challengeService.createChallenge(userDetails, request);
    }

    @GetMapping("/vs/{challengeId}")
    @Operation(
            summary = "VS 챌린지 상세 조회",
            description = "특정 VS 챌린지의 상세 정보와 현재 주차 진행 현황, 상대방 상태를 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "VS 챌린지 상세 조회 성공")
    @ApiResponse(responseCode = "403", description = "PRIVATE 챌린지에 대한 권한 없음")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 challengeId")
    public ChallengeDto.VsChallengeDetailResponse getVsChallengeDetail(
            @PathVariable Long challengeId,
            @RequestParam(value = "filterType", required = false, defaultValue = "ALL") String filterType,
            @RequestParam(value = "sort", required = false, defaultValue = "created_at") String sort,
            @RequestParam(value = "order", required = false, defaultValue = "desc") String order,
            @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "16") Integer size,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return challengeService.getVsChallengeDetail(
                challengeId,
                userDetails,
                filterType,
                sort,
                order,
                page,
                size
        );
    }
}