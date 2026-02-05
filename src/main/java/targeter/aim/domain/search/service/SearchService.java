package targeter.aim.domain.search.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import targeter.aim.domain.challenge.dto.ChallengeDto;
import targeter.aim.domain.challenge.service.ChallengeReadService;
import targeter.aim.domain.post.dto.PostDto;
import targeter.aim.domain.post.service.PostReadService;
import targeter.aim.domain.search.dto.SearchDto;
import targeter.aim.domain.search.repository.SearchQueryRepository;
import targeter.aim.system.security.model.UserDetails;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final SearchQueryRepository searchQueryRepository;

    @Transactional(readOnly = true)
    public SearchDto.SearchPageResponse getSearchList(
            SearchDto.ListSearchCondition condition,
            UserDetails userDetails,
            Pageable pageable
    ) {
        Page<SearchDto.SearchListResponse> page = searchQueryRepository.paginateSearchList(userDetails, condition, pageable);

        return SearchDto.SearchPageResponse.from(page);
    }
}