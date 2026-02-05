package targeter.aim.domain.search.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import targeter.aim.domain.challenge.dto.ChallengeDto;
import targeter.aim.domain.challenge.service.ChallengeReadService;
import targeter.aim.domain.post.dto.PostDto;
import targeter.aim.domain.post.service.PostReadService;
import targeter.aim.domain.search.dto.SearchDto;
import targeter.aim.system.security.model.UserDetails;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final PostReadService postService;
    private final ChallengeReadService challengeService;

    @Transactional(readOnly = true)
    public SearchDto.SearchResponse search(
            String keyword,
            PostDto.PostSortType postSort,
            ChallengeDto.ChallengeSortType challengeSort,
            Pageable pageable,
            UserDetails userDetails
    ) {
        PostDto.PostPageResponse posts = postService.searchPosts(keyword, postSort, pageable, userDetails);
        ChallengeDto.ChallengePageResponse challenges = challengeService.searchChallenges(keyword, challengeSort, pageable, userDetails);

        return SearchDto.SearchResponse.builder()
                .posts(posts)
                .challenges(challenges)
                .build();
    }
}