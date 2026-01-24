package targeter.aim.domain.post.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import targeter.aim.domain.post.dto.PostLikedDto;
import targeter.aim.domain.post.service.PostLikedService;
import targeter.aim.system.security.model.UserDetails;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts/{postId}/likes")
@Tag(name = "Post", description = "게시글 관련 API")
public class PostLikedController {

    private final PostLikedService postLikedService;

    @PostMapping
    @Operation(summary = "게시글 좋아요 토글", description = "로그인한 사용자가 특정 아이디의 게시글을 좋아요 하거나 좋아요 해제합니다.")
    public PostLikedDto.PostLikedResponse togglePostLikes(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return postLikedService.togglePostLikes(postId, userDetails);
    }
}
