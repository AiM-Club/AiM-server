package targeter.aim.domain.challenge.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import targeter.aim.domain.challenge.dto.WeeklyProgressDto;
import targeter.aim.domain.challenge.service.WeeklyProgressService;
import targeter.aim.system.security.model.UserDetails;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/challenges/{challengeId}/weeks")
@Tag(name = "Weekly Progress", description = "챌린지 주차별 내용 관련 API")
public class WeeklyProgressController {

    private final WeeklyProgressService weeklyProgressService;

    @GetMapping
    @Operation(
            summary = "챌린지 주차별 내용 리스트 조회",
            description = "특정 챌린지의 주차별 내용을 리스트 형태로 전체 조회합니다."
    )
    public WeeklyProgressDto.WeekProgressListResponse getWeeklyProgressList(
            @PathVariable Long challengeId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return weeklyProgressService.getVsWeeklyProgressList(challengeId, userDetails);
    }

    @PostMapping(value = "/proof", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "주차별 챌린지 인증샷 업로드",
            description = "주차별로 챌린지 인증샷을 업로드합니다."
    )
    public ResponseEntity<String> uploadProofFile(
            @Valid @ModelAttribute WeeklyProgressDto.ProofUploadRequest request
    ) {
        weeklyProgressService.uploadProofFiles(request);
        return ResponseEntity.ok("챌린지 인증샷 업로드 완료");
    }
}