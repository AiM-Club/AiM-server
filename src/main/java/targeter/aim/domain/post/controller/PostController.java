package targeter.aim.domain.post.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
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
import targeter.aim.domain.post.dto.PostDto;
import targeter.aim.domain.post.service.PostService;
import targeter.aim.system.security.annotation.NoJwtAuth;
import targeter.aim.system.security.model.UserDetails;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
@Tag(name = "Post", description = "게시글 관련 API")
public class PostController {

    private final PostService postService;

    @PostMapping(value = "/vs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "VS 챌린지 모집글 생성",
            description = "VS 챌린지에 모집글을 생성합니다."
    )
    public PostDto.PostIdResponse createVsPost(
            @ModelAttribute PostDto.CreateChallengePostRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return postService.createChallengePost(request, userDetails);
    }

    @PostMapping(value = "/qna", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "QnA 게시글 생성",
            description = "QnA 게시글을 생성합니다."
    )
    public PostDto.CreatePostResponse createQnaPost(
            @Valid @ModelAttribute PostDto.CreatePostRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails
    ) {
        return postService.createQnAPost(request, userDetails);
    }

    @PostMapping(value = "/review", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "후기 게시글 생성",
            description = "후기 게시글을 생성합니다."
    )
    public PostDto.CreatePostResponse createReviewPost(
            @Valid @ModelAttribute PostDto.CreatePostRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails
    ) {
        return postService.createReviewPost(request, userDetails);
    }

    @PatchMapping(value = "/{postId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "게시글 수정",
            description = "해당 id의 게시글을 수정합니다."
    )
    public PostDto.PostIdResponse updatePost(
            @PathVariable Long postId,
            @ModelAttribute @Valid @ParameterObject PostDto.UpdatePostRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return postService.updatePost(postId, userDetails, request);
    }

    @DeleteMapping("/{postId}")
    @Operation(
            summary = "게시글 삭제",
            description = "해당 id의 게시글를 삭제합니다."
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiResponse(responseCode = "204", description = "게시글 삭제 성공")
    public void deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        postService.deletePost(postId, userDetails);
    }
}
