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
import targeter.aim.system.security.model.UserDetails;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts/{postId}/comments")
@Tag(name = "Post Comment", description = "게시글 댓글 관련 API")
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "게시글 댓글 작성", description = "게시글 상세페이지에서 댓글을 작성합니다.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommentDto.CreateResponse createComment(
            @PathVariable Long postId,
            @RequestPart("content") String content,
            @RequestPart(value = "attachedImages", required = false) List<MultipartFile> attachedImages,
            @RequestPart(value = "attachedFiles", required = false) List<MultipartFile> attachedFiles,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getId();
        return commentService.createComment(postId, userId, content, attachedImages, attachedFiles);
    }
}