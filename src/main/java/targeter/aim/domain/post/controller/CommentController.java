package targeter.aim.domain.post.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
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

    @Operation(
            summary = "게시글 댓글 작성", description = "게시글 상세페이지에서 댓글/대댓글을 작성합니다."
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommentDto.CreateResponse createComment(
            @PathVariable Long postId,
            @RequestParam("content") String content,
            @RequestParam(value = "parentCommentId", required = false) Long parentCommentId,
            @RequestPart(value = "attachedImages", required = false) List<MultipartFile> attachedImages,
            @RequestPart(value = "attachedFiles", required = false) List<MultipartFile> attachedFiles,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (userDetails == null) {
            throw new RestException(ErrorCode.AUTH_LOGIN_REQUIRED);
        }

        Long userId = userDetails.getUser().getId();

        return commentService.createComment(
                postId,
                userId,
                content,
                parentCommentId,
                attachedImages,
                attachedFiles
        );
    }

    @Operation(
            summary = "게시글 댓글 목록 조회", description = "게시글 댓글 및 대댓글을 조회합니다."
    )
    @GetMapping
    public List<CommentDto.CommentResponse> getComments(
            @PathVariable Long postId,
            @RequestParam(value = "sort", required = false, defaultValue = "LATEST") String sort,
            @RequestParam(value = "order", required = false, defaultValue = "ASC") String order,
            @RequestParam(value = "filterType", required = false, defaultValue = "ALL") String filterType,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (userDetails == null) {
            throw new RestException(ErrorCode.AUTH_LOGIN_REQUIRED);
        }

        return commentService.getComments(postId, sort, order, filterType);
    }
}