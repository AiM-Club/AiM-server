package targeter.aim.domain.challenge.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import targeter.aim.domain.challenge.dto.ChallengeDto;
import targeter.aim.domain.challenge.entity.ChallengeMode;
import targeter.aim.domain.challenge.repository.AllChallengeSortType;
import targeter.aim.domain.challenge.repository.SortOrder;
import targeter.aim.domain.challenge.service.ChallengeService;
import targeter.aim.system.security.annotation.NoJwtAuth;
import targeter.aim.system.security.model.UserDetails;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/challenges")
@Tag(name = "Challenge", description = "챌린지 관련 API")
public class ChallengeController {

    private final ChallengeService challengeService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "챌린지 생성",
            description = "새로운 챌린지를 생성합니다."
    )
    @ApiResponse(responseCode = "201", description = "챌린지 생성 성공")
    @ResponseStatus(HttpStatus.CREATED)
    public ChallengeDto.ChallengeIdResponse createChallenge(
            @ModelAttribute @Valid ChallengeDto.ChallengeCreateRequest request,
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

    @NoJwtAuth("VS 결과는 비로그인 유저도 열람 가능")
    @GetMapping("/vs/{challengeId}/result")
    @Operation(
            summary = "VS 챌린지 최종 결과 조회",
            description = "VS 챌린지 최종 승리자 정보를 반환합니다."
    )
    public ChallengeDto.VsResultResponse getVsResult(
            @PathVariable Long challengeId
    ) {
        return challengeService.getVsChallengeResult(challengeId);
    }

    @GetMapping("/solo")
    @Operation(
            summary = "SOLO 챌린지 목록 조회",
            description = "로그인한 사용자가 SOLO 챌린지 목록을 탭(진행 중/진행 완료)과 정렬 조건에 따라 페이지네이션 조회합니다."
    )
    public ChallengeDto.ChallengePageResponse getSoloChallenges(
            @ModelAttribute @ParameterObject ChallengeDto.SoloChallengeListRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return challengeService.getSoloChallenges(request, userDetails);
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

    @PatchMapping(value = "/{challengeId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "챌린지 수정",
            description = "해당 id의 챌린지를 수정합니다."
    )
    public ChallengeDto.ChallengeIdResponse updateChallenge(
            @PathVariable Long challengeId,
            @ModelAttribute @Valid @ParameterObject ChallengeDto.ChallengeUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return challengeService.updateChallenge(challengeId, userDetails, request);
    }

    @DeleteMapping("/{challengeId}")
    @Operation(
            summary = "챌린지 삭제",
            description = "해당 id의 챌린지를 삭제합니다."
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiResponse(responseCode = "204", description = "챌린지 삭제 성공")
    public void deleteChallenge(
            @PathVariable Long challengeId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        challengeService.deleteChallenge(challengeId, userDetails);
    }

    @GetMapping("/my-list")
    @Operation(
            summary = "내가 참여한 챌린지 리스트 조회(게시글 작성용)",
            description = "내가 참여한 챌린지를 Mode에 맞춰 출력합니다. 게시글 작성 시 이용됩니다."
    )
    public List<ChallengeDto.ChallengeToPostResponse> getChallengeForPost(
            @RequestParam ChallengeMode mode,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return challengeService.getChallengeToPost(mode, userDetails);
    }

    @GetMapping("/all")
    @Operation(
            summary = "ALL 챌린지 목록 조회",
            description = "로그인한 사용자가 참여한 모든 SOLO/VS 챌린지를 정렬 조건과 함께 조회합니다."
    )
    public ChallengeDto.ChallengePageResponse getAllChallenges(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "CREATED_AT") AllChallengeSortType sort,
            @RequestParam(defaultValue = "DESC") SortOrder order,
            @PageableDefault(size = 8) @ParameterObject Pageable pageable
    ) {
        var page = challengeService.getAllChallenges(
                userDetails,
                pageable,
                sort,
                order
        );

        return ChallengeDto.ChallengePageResponse.from(page);
    }

}