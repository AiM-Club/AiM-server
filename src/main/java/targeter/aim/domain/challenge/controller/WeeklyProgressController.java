package targeter.aim.domain.challenge.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
}
