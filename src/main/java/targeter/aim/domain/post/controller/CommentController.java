package targeter.aim.domain.post.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import targeter.aim.domain.challenge.repository.CommentSortType;
import targeter.aim.domain.challenge.repository.SortOrder;
import targeter.aim.domain.post.dto.CommentDto;
import targeter.aim.domain.post.service.CommentService;
import targeter.aim.system.exception.model.ErrorCode;
import targeter.aim.system.exception.model.RestException;
import targeter.aim.system.security.model.UserDetails;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts/{postId}/comments")
@Tag(name = "Post Comment", description = "게시글 댓글 관련 API")
public class CommentController {

    private final CommentService commentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "게시글 댓글 작성",
            description = "게시글 상세페이지에서 댓글/대댓글을 작성합니다."
    )
    public CommentDto.CommentCreateResponse createComment(
            @PathVariable Long postId,
            @Valid @ModelAttribute CommentDto.CommentCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return commentService.createComment(postId, request, userDetails);
    }

    @GetMapping
    @Operation(
            summary = "게시글 댓글 목록 조회",
            description = "게시글 댓글 및 대댓글을 조회합니다."
    )
    public CommentDto.CommentListResponse getComments(
            @PathVariable Long postId,
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
        if (userDetails == null) {
            throw new RestException(ErrorCode.AUTH_LOGIN_REQUIRED);
        }

        return commentService.getComments(postId, sort, order, page, size, userDetails);
    }
}