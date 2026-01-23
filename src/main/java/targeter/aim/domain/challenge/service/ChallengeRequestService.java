package targeter.aim.domain.challenge.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import targeter.aim.domain.challenge.dto.ChallengeRequestDto;
import targeter.aim.domain.challenge.entity.*;
import targeter.aim.domain.challenge.repository.ChallengeMemberRepository;
import targeter.aim.domain.challenge.repository.ChallengeRequestQueryRepository;
import targeter.aim.domain.challenge.repository.ChallengeRequestRepository;
import targeter.aim.domain.user.entity.User;
import targeter.aim.system.exception.model.ErrorCode;
import targeter.aim.system.exception.model.RestException;
import targeter.aim.system.security.model.UserDetails;

@Service
@RequiredArgsConstructor
public class ChallengeRequestService {
    private final ChallengeRequestQueryRepository queryRepository;
    private final ChallengeRequestRepository challengeRequestRepository;
    private final ChallengeMemberRepository challengeMemberRepository;

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

    @Transactional
    public ChallengeRequestDto.RequestAccessResponse approveRequest(Long requestId, UserDetails userDetails) {
        ChallengeRequest challengeRequest = queryRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new RestException(ErrorCode.GLOBAL_NOT_FOUND, "해당 챌린지 요청을 찾을 수 없음"));

        challengeRequest.canApplyBy(userDetails);
        challengeRequest.validatePending();

        Challenge challenge = challengeRequest.getChallenge();
        User requester = challengeRequest.getRequester();

        if(challengeMemberRepository.existsById_ChallengeAndId_User(challenge, requester)) {
            throw new RestException(ErrorCode.GLOBAL_CONFLICT, "이미 챌린지에 등록된 유저입니다.");
        }

        ChallengeMemberId challengeMemberId = ChallengeMemberId.of(challenge, requester);

        ChallengeMember challengeMember = ChallengeMember.builder()
                .id(challengeMemberId)
                .role(MemberRole.MEMBER)
                .result(null)
                .build();

        challengeMemberRepository.save(challengeMember);

        challenge.startVs();
        challengeRequest.approve();

        return ChallengeRequestDto.RequestAccessResponse.from(challengeRequest);
    }

    @Transactional
    public void rejectRequest(Long requestId, UserDetails userDetails) {
        ChallengeRequest challengeRequest = queryRepository.findByIdForUpdate(requestId)
            .orElseThrow(() -> new RestException(ErrorCode.GLOBAL_NOT_FOUND, "해당 챌린지 요청을 찾을 수 없음"));

        challengeRequest.canApplyBy(userDetails);
        challengeRequest.validatePending();

        challengeRequestRepository.delete(challengeRequest);
    }
}

