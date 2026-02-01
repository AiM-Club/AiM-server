package targeter.aim.domain.post.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

    @NoJwtAuth
    @GetMapping("/vs")
    @Operation(
            summary = "VS 모집글 목록 조회",
            description = "VS 모집글 목록을 정렬 조건에 따라 페이지네이션 조회합니다. 홈 화면의 경우 size = 8로 요청해야 합니다."
    )
    public PostDto.VSRecruitPageResponse getVsRecruits(
            @ModelAttribute @ParameterObject PostDto.ListSearchCondition condition,
            @PageableDefault(size = 16) @ParameterObject Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return postService.getVsRecruits(condition, userDetails, pageable);
    }

    @NoJwtAuth
    @GetMapping("/vs/{postId}")
    @Operation(
            summary = "VS 챌린지 모집글 상세 조회",
            description = "특정 VS 챌린지 모집글의 상세 정보를 조회합니다."
    )
    public PostDto.PostVsDetailResponse getVsPostDetail(
            @PathVariable Long postId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails
    ) {
        return postService.getVsPostDetail(postId, userDetails);
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

    @NoJwtAuth
    @GetMapping("/qna/{postId}")
    @Operation(
            summary = "QnA 게시글 상세 조회",
            description = "해당 id의 QnA 게시글을 상세 조회합니다."
    )
    public PostDto.PostDetailResponse getQnaPostDetail(
            @PathVariable Long postId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails
    ) {
        return postService.getPostDetail(postId, userDetails);
    }

    @NoJwtAuth
    @GetMapping("/review/{postId}")
    @Operation(
            summary = "후기 게시글 상세 조회",
            description = "해당 id의 후기 게시글을 상세 조회합니다."
    )
    public PostDto.PostDetailResponse getReviewPostDetail(
            @PathVariable Long postId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails
    ) {
        return postService.getPostDetail(postId, userDetails);
    }

    @NoJwtAuth
    @GetMapping("/review/top")
    @Operation(
            summary = "HOT 후기글 조회",
            description = "최근 3개월 내 좋아요 수가 가장 많은 10개 후기글을 조회합니다."
    )
    public List<PostDto.HotReviewResponse> getHotReview() {
        return postService.getHotReview();
    }

    @NoJwtAuth
    @GetMapping("/vs/top")
    @Operation(
            summary = "HOT VS 모집글 Top10 조회",
            description = " 홈 화면에 노출되는 HOT VS 모집글을 조회합니다."
    )
    public List<PostDto.HotVsPostResponse> getHotVsPosts() {
        return postService.getTop10HotVsPosts();
    }

    @NoJwtAuth
    @GetMapping("/hot/solo")
    @Operation(
            summary = "HOT SOLO 게시글 페이지네이션 조회",
            description = "최근 3개월 내 QnA/후기 게시글 중 좋아요가 많은 SOLO 게시글을 페이지네이션 조회합니다."
    )
    public PostDto.HotPostPageResponse getHotSoloPosts(
            @ModelAttribute @ParameterObject PostDto.ListSearchCondition condition,
            @PageableDefault(size = 8) @ParameterObject Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return postService.getHotSoloPosts(condition, userDetails, pageable);
    }

    @NoJwtAuth
    @GetMapping("/hot/vs")
    @Operation(
            summary = "HOT VS 게시글 페이지네이션 조회",
            description = "최근 3개월 내 QnA/후기 게시글 중 좋아요가 많은 VS 게시글을 페이지네이션 조회합니다."
    )
    public PostDto.HotPostPageResponse getHotVsPosts(
            @ModelAttribute @ParameterObject PostDto.ListSearchCondition condition,
            @PageableDefault(size = 8) @ParameterObject Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return postService.getHotVsPosts(condition, userDetails, pageable);
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

    @NoJwtAuth
    @GetMapping("/qna")
    @Operation(
            summary = "QnA 게시글 목록 조회",
            description = "QnA 게시글을 정렬 조건에 따라 페이지네이션 조회합니다."
    )
    public PostDto.PostPageResponse getQnaPosts(
            @ModelAttribute @ParameterObject PostDto.ListSearchCondition condition,
            @RequestParam(defaultValue = "ALL") String filterType,
            @PageableDefault(size = 16) @ParameterObject Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return postService.getQnaPosts(condition, userDetails, pageable, filterType);
    }

    @NoJwtAuth
    @GetMapping("/review")
    @Operation(
            summary = "후기 게시글 목록 조회",
            description = "후기 게시글을 정렬 조건에 따라 페이지네이션 조회합니다."
    )
    public PostDto.PostPageResponse getReviewPosts(
            @ModelAttribute @ParameterObject PostDto.ListSearchCondition condition,
            @RequestParam(defaultValue = "ALL") String filterType,
            @PageableDefault(size = 16) @ParameterObject Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return postService.getReviewPosts(condition, userDetails, pageable, filterType);
    }

}
