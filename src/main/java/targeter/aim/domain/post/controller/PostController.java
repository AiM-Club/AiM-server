package targeter.aim.domain.post.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import targeter.aim.domain.challenge.dto.ChallengeDto;
import targeter.aim.domain.post.dto.PostDto;
import targeter.aim.domain.post.service.PostService;
import targeter.aim.system.security.annotation.NoJwtAuth;
import targeter.aim.system.security.model.UserDetails;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
@Tag(name = "Post", description = "게시글 관련 API")
public class PostController {

    private final PostService postService;

    @PostMapping(value = "/vs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "VS 챌린지 게시글 생성",
            description = "VS 챌린지에 모집글을 생성합니다."
    )
    public ResponseEntity<Map<String, Long>> createVsPost(
            @ModelAttribute PostDto.CreateChallengePostRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        Long postId = postService.createChallengePost(
                request,
                userDetails.getUser().getId()
        );

        return ResponseEntity.ok(Map.of("postId", postId));
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
}
