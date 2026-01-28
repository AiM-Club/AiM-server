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
import targeter.aim.domain.post.dto.PostDto;
import targeter.aim.domain.post.service.PostService;
import targeter.aim.system.security.annotation.NoJwtAuth;
import targeter.aim.system.security.model.UserDetails;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
@Tag(name = "Post", description = "게시글 관련 API")
public class PostController {

    private final PostService postService;

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

    @PostMapping("/vs")
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

    @PostMapping(value = "/qna", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "QnA 게시글 생성",
            description = "QnA 게시글을 생성합니다."
    )
    public PostDto.CreatePostResponse createPost(
            @Valid @ModelAttribute PostDto.CreatePostRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails
    ) {
        return postService.createQnAPost(request, userDetails);
    }
}
