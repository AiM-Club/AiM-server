package targeter.aim.domain.post.controller;

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
import targeter.aim.domain.post.dto.CommentDto;
import targeter.aim.domain.post.service.CommentService;
import targeter.aim.system.security.model.UserDetails;

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
    public CommentDto.CommentPageResponse getComments(
            @PathVariable Long postId,
            @PageableDefault @ParameterObject Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return commentService.getComments(postId, pageable, userDetails);
    }
}