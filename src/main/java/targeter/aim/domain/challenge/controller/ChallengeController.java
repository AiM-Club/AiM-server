package targeter.aim.domain.challenge.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import targeter.aim.domain.challenge.dto.ChallengeDto;
import targeter.aim.domain.challenge.entity.ChallengeMode;
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

    @GetMapping("/records")
    @Operation(
            summary = "유저 챌린지 기록 조회",
            description = "로그인한 사용자의 전체/SOLO/VS 챌린지 성공률 및 횟수를 조회합니다."
    )
    public ChallengeDto.RecordResponse getMyChallengeRecords(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return challengeService.getMyChallengeRecords(userDetails.getUser().getId());
    }
}