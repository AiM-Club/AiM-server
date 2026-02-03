package targeter.aim.domain.challenge.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import targeter.aim.domain.challenge.dto.WeeklyCommentDto;
import targeter.aim.domain.challenge.service.WeeklyCommentService;
import targeter.aim.system.security.model.UserDetails;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/challenges/{challengeId}/weeks/{weeksId}/comments")
@Tag(name = "Weekly Comment", description = "챌린지 주차별 댓글 관련 API")
public class WeeklyCommentController {

    private final WeeklyCommentService weeklyCommentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "챌린지 주차별 댓글 작성",
            description = "챌린지 상세 페이지의 주차별 진행상황(weeks)에 댓글을 작성합니다."
    )
    public WeeklyCommentDto.WeeklyCommentCreateResponse createWeeklyComment(
            @PathVariable Long challengeId,
            @PathVariable Long weeksId,
            @Valid @ModelAttribute WeeklyCommentDto.WeeklyCommentCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return weeklyCommentService.createWeeklyComment(
                challengeId, weeksId, request, userDetails
        );
    }

    @GetMapping
    @Operation(
            summary = "챌린지 주차별 댓글 목록 조회",
            description = "챌린지 주차별 댓글 및 대댓글을 조회합니다."
    )
    public WeeklyCommentDto.WeeklyCommentPageResponse getWeeklyComments(
            @PathVariable Long challengeId,
            @PathVariable Long weeksId,
            @PageableDefault @ParameterObject Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return weeklyCommentService.getWeeklyComments(challengeId, weeksId, pageable, userDetails);
    }

}