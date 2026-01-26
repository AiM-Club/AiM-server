package targeter.aim.domain.challenge.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import targeter.aim.domain.challenge.dto.ChallengeRequestDto;
import targeter.aim.domain.challenge.entity.*;
import targeter.aim.domain.challenge.repository.ChallengeMemberRepository;
import targeter.aim.domain.challenge.repository.ChallengeRepository;
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
    private final ChallengeRepository challengeRepository;

    @Transactional
    public ChallengeRequestDto.SendRequestResponse sendRequest(Long challengeId, UserDetails userDetails) {
        if(userDetails == null) {
            throw new RestException(ErrorCode.AUTH_AUTHENTICATION_FAILED);
        }

        User requester = userDetails.getUser();
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new RestException(ErrorCode.CHALLENGE_NOT_FOUND));
        if(challenge.getMode() != ChallengeMode.VS) {
            throw new RestException(ErrorCode.GLOBAL_BAD_REQUEST, "VS 챌린지만 가능한 기능입니다.");
        }

        ChallengeMember hostMember = challengeMemberRepository
                .findFirstById_ChallengeAndRole(challenge, MemberRole.HOST)
                .orElseThrow(() -> new RestException(ErrorCode.GLOBAL_NOT_FOUND, "해당 챌린지의 HOST를 찾을 수 없습니다."));
        User hostUser = hostMember.getId().getUser();

        if(hostUser.getId().equals(requester.getId())) {
            throw new RestException(ErrorCode.GLOBAL_BAD_REQUEST, "자신의 챌린지에 자신이 요청할 수 없습니다.");
        }
        if(challengeMemberRepository.existsById_ChallengeAndId_User(challenge, requester)) {
            throw new RestException(ErrorCode.GLOBAL_BAD_REQUEST, "이미 해당 챌린지의 멤버입니다.");
        }

        long memberCount = challengeMemberRepository.countById_ChallengeAndRole(challenge, MemberRole.MEMBER);
        if(memberCount >= 1) {
            throw new RestException(ErrorCode.GLOBAL_BAD_REQUEST, "멤버는 1명만 받을 수 있습니다.");
        }

        boolean existPending = challengeRequestRepository
                .existsByChallengeAndRequesterAndApplyStatus(challenge, requester, ApplyStatus.PENDING);
        if(existPending) {
            throw new RestException(ErrorCode.GLOBAL_CONFLICT, "이미 대기중인 요청이 있습니다.");
        }

        ChallengeRequest saved = challengeRequestRepository.save(
                ChallengeRequest.builder()
                        .requester(requester)
                        .applier(hostUser)
                        .challenge(challenge)
                        .applyStatus(ApplyStatus.PENDING)
                        .pendingMarker(1)
                        .build()
        );

        return ChallengeRequestDto.SendRequestResponse.from(saved);
    }

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

