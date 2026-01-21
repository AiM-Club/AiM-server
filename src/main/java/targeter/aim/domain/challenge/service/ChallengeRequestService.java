package targeter.aim.domain.challenge.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import targeter.aim.domain.challenge.dto.ChallengeRequestDto;
import targeter.aim.domain.challenge.repository.ChallengeRequestQueryRepository;
import targeter.aim.system.exception.model.ErrorCode;
import targeter.aim.system.exception.model.RestException;
import targeter.aim.system.security.model.UserDetails;

@Service
@RequiredArgsConstructor
public class ChallengeRequestService {
    private final ChallengeRequestQueryRepository queryRepository;

    /**
     * VS 챌린지 신청/초대 목록 조회
     * - page size는 요구사항대로 10 고정
     */
    @Transactional(readOnly = true)
    public ChallengeRequestDto.ChallengeRequestPageResponse getVsRequestList(
            UserDetails userDetails,
            int page,
            ChallengeRequestDto.RequestListCondition condition
    ) {
        if (userDetails == null || userDetails.getUser() == null) {
            throw new RestException(ErrorCode.AUTH_AUTHENTICATION_FAILED);
        }
        if (page < 0) {
            throw new RestException(ErrorCode.GLOBAL_BAD_REQUEST);
        }

        PageRequest pageable = PageRequest.of(page, 10);

        Page<ChallengeRequestDto.RequestListResponse> result =
                queryRepository.paginateVsRequests(userDetails, pageable, condition);

        return ChallengeRequestDto.ChallengeRequestPageResponse.from(result);
    }
}

