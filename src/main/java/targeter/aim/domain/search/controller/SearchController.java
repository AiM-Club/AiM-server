package targeter.aim.domain.search.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import targeter.aim.domain.challenge.dto.ChallengeDto;
import targeter.aim.domain.post.dto.PostDto;
import targeter.aim.domain.search.dto.SearchDto;
import targeter.aim.domain.search.service.SearchService;
import targeter.aim.system.security.annotation.NoJwtAuth;
import targeter.aim.system.security.model.UserDetails;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search")
@Tag(name = "Search", description = "게시글 + 챌린지 통합 검색 API")
public class SearchController {

    private final SearchService searchService;

    @NoJwtAuth
    @GetMapping
    @Operation(
            summary = "통합 검색",
            description = "키워드 기반으로 게시글(/api/posts/search) + 챌린지(/api/challenges/search)를 통합 검색합니다."
    )
    public SearchDto.SearchPageResponse search(
            @ModelAttribute @ParameterObject SearchDto.ListSearchCondition condition,
            @PageableDefault(size = 16) @ParameterObject Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return searchService.getSearchList(condition, userDetails, pageable);
    }
}