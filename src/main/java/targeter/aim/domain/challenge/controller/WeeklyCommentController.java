package targeter.aim.domain.challenge.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import targeter.aim.domain.challenge.dto.WeeklyCommentDto;
import targeter.aim.domain.challenge.repository.CommentSortType;
import targeter.aim.domain.challenge.repository.SortOrder;
import targeter.aim.domain.challenge.service.WeeklyCommentService;
import targeter.aim.system.security.model.UserDetails;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/challenges/{challengeId}/weeks/{weeksId}/comments")
@Tag(name = "Weekly Comment", description = "VS 챌린지 주차별 댓글 관련 API")
public class WeeklyCommentController {

    private final WeeklyCommentService weeklyCommentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "VS 챌린지 주차별 댓글 작성",
            description = "VS 대결 챌린지 상세 페이지의 주차별 진행상황(weeks)에 댓글을 작성합니다."
    )
    public WeeklyCommentDto.WeeklyCommentCreateResponse createWeeklyComment(
            @PathVariable Long challengeId,
            @PathVariable Long weeksId,
            @RequestPart("content") String content,
            @RequestPart(value = "attachedImages", required = false) List<MultipartFile> attachedImages,
            @RequestPart(value = "attachedFiles", required = false) List<MultipartFile> attachedFiles,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return weeklyCommentService.createWeeklyComment(
                challengeId, weeksId, content, attachedImages, attachedFiles, userDetails
        );
    }

    @GetMapping
    @Operation(
            summary = "VS 챌린지 주차별 댓글 목록 조회",
            description = "VS 대결 챌린지의 특정 주차(weeks)에 작성된 댓글 목록(부모/대댓글)을 최신순으로 조회합니다."
    )
    public WeeklyCommentDto.WeeklyCommentListResponse getWeeklyComments(
            @PathVariable Long challengeId,
            @PathVariable Long weeksId,
            @Parameter(description = "정렬 기준", example = "LATEST")
            @RequestParam(defaultValue = "LATEST") CommentSortType sort,
            @Parameter(description = "정렬 방향", example = "DESC")
            @RequestParam(defaultValue = "DESC") SortOrder order,
            @Parameter(description = "페이지 번호(0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return weeklyCommentService.getWeeklyComments(
                challengeId, weeksId, sort, order, page, size, userDetails
        );
    }
}