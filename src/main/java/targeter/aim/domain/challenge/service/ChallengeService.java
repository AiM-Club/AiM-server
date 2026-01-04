package targeter.aim.domain.challenge.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import targeter.aim.domain.challenge.dto.ChallengeDto;
import targeter.aim.domain.challenge.repository.ChallengeFilterType;
import targeter.aim.domain.challenge.repository.ChallengeQueryRepository;
import targeter.aim.domain.challenge.repository.ChallengeSortType;
import targeter.aim.system.exception.model.ErrorCode;
import targeter.aim.system.exception.model.RestException;
import targeter.aim.system.security.model.UserDetails;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChallengeService {

    private final ChallengeQueryRepository challengeQueryRepository;

    @Transactional(readOnly = true)
    public ChallengeDto.ChallengePageResponse getVsChallenges(
            ChallengeDto.ListSearchCondition condition,
            UserDetails userDetails,
            Pageable pageable
    ) {
        ChallengeFilterType filterType =
                ChallengeFilterType.valueOf(condition.getFilterType());

        ChallengeSortType sortType =
                ChallengeSortType.valueOf(condition.getSort());

        // MY 탭은 로그인 필요
        if (filterType == ChallengeFilterType.MY && userDetails == null) {
            throw new RestException(ErrorCode.AUTH_LOGIN_REQUIRED);
        }

        var page = challengeQueryRepository.paginateByType(
                userDetails,
                pageable,
                filterType,
                sortType
        );

        return ChallengeDto.ChallengePageResponse.from(page);
    }
}