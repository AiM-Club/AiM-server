package targeter.aim.domain.challenge.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import targeter.aim.domain.ai.llm.dto.RoutePayload;
import targeter.aim.domain.challenge.dto.ChallengeDto;
import targeter.aim.domain.challenge.entity.*;
import targeter.aim.domain.challenge.repository.*;
import targeter.aim.domain.file.entity.ChallengeImage;
import targeter.aim.domain.file.handler.FileHandler;
import targeter.aim.domain.label.entity.Field;
import targeter.aim.domain.label.entity.Tag;
import targeter.aim.domain.label.repository.FieldRepository;
import targeter.aim.domain.label.repository.TagRepository;
import targeter.aim.domain.label.service.FieldService;
import targeter.aim.domain.label.service.TagService;
import targeter.aim.domain.user.entity.User;
import targeter.aim.domain.user.repository.UserRepository;
import targeter.aim.domain.user.service.UserService;
import targeter.aim.system.exception.model.ErrorCode;
import targeter.aim.system.exception.model.RestException;
import targeter.aim.system.security.model.UserDetails;

import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChallengeService {

    private final UserRepository userRepository;
    private final ChallengeMemberQueryRepository challengeMemberQueryRepository;

    private final ChallengeRepository challengeRepository;
    private final ChallengeMemberRepository challengeMemberRepository;
    private final WeeklyProgressRepository weeklyProgressRepository;
    private final ChallengeLikedRepository challengeLikedRepository;
    private final TagRepository tagRepository;
    private final FieldRepository fieldRepository;

    private final ChallengeQueryRepository challengeQueryRepository;
    private final WeeklyProgressQueryRepository weeklyProgressQueryRepository;

    private final ChallengeRoutePersistService persistService;
    private final ChallengeRouteGenerationService generationService;
    private final ChallengeCleanupService cleanupService;
    private final TagService tagService;
    private final UserService userService;
    private final FieldService fieldService;
    private final FileHandler fileHandler;

    @Transactional
    public ChallengeDto.ChallengeIdResponse createChallenge(
            UserDetails userDetails,
            ChallengeDto.ChallengeCreateRequest request
    ) {
        User user = userDetails.getUser();

        // 1. 주차별 계획(Payload) 생성
        RoutePayload routePayload = generationService.generateRoute(request);

        // 2. 생성된 데이터 저장
        Long challengeId = persistService.persistAtomic(user.getId(), request, routePayload);

        // 3. 생성된 챌린지 조회
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new RestException(ErrorCode.CHALLENGE_NOT_FOUND));

        try {
            saveThumbnailImage(request.getThumbnail(), challenge);
        } catch (Exception e) {
            cleanupService.deleteChallengeAtomic(challengeId);
            throw new RestException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        // 4. 태그 / 분야 연관관계 매핑
        updateChallengeLabels(challenge, request.getTags(), request.getFields());

        return ChallengeDto.ChallengeIdResponse.from(challenge);
    }

    private void saveThumbnailImage(MultipartFile file, Challenge challenge) {
        if(file != null && !file.isEmpty()) {
            ChallengeImage thumbnailImage = ChallengeImage.from(file, challenge);
            if(thumbnailImage == null) throw new RestException(ErrorCode.FILE_UPLOAD_FAILED);
            challenge.setChallengeImage(thumbnailImage);
            thumbnailImage.setChallenge(challenge);
            fileHandler.saveFile(file, thumbnailImage);
        }
    }

    private void updateChallengeLabels(Challenge challenge, List<String> tagNames, List<String> fieldNames) {
        if (tagNames != null) {
            Set<Tag> tags = tagNames.stream()
                    .map(String::trim)
                    .map(name -> tagRepository.findByName(name)
                            .orElseGet(() -> tagRepository.save(Tag.builder().name(name).build())))
                    .collect(Collectors.toSet());
            challenge.setTags(tags);
        }

        if (fieldNames != null) {
            List<String> trimFieldNames = fieldNames.stream()
                    .map(String::trim)
                    .collect(Collectors.toList());
            List<Field> existFields = fieldRepository.findAllByNameIn(trimFieldNames);

            challenge.setFields(new HashSet<>(existFields));
        }
    }

    @Transactional
    public ChallengeDto.VsResultResponse getVsChallengeResult(Long challengeId) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new RestException(ErrorCode.GLOBAL_NOT_FOUND));

        if (challenge.getMode() != ChallengeMode.VS) {
            throw new RestException(ErrorCode.GLOBAL_BAD_REQUEST);
        }

        List<ChallengeMember> members = challengeMemberRepository.findAllById_Challenge(challenge);

        User hostUser = members.stream()
                .filter(m -> m.getRole() == MemberRole.HOST)
                .map(m -> m.getId().getUser())
                .findFirst()
                .orElseThrow(() -> new RestException(ErrorCode.GLOBAL_NOT_FOUND, "HOST 멤버가 존재하지 않습니다."));

        User memberUser = members.stream()
                .filter(m -> m.getRole() == MemberRole.MEMBER)
                .map(m -> m.getId().getUser())
                .findFirst()
                .orElse(null);

        if(memberUser == null) {
            throw new RestException(ErrorCode.GLOBAL_BAD_REQUEST);
        }

        List<WeeklyProgress> hostWeeks = weeklyProgressRepository.findAllByChallengeAndUser(challenge, hostUser);

        int hostSuccessWeeks = (int) hostWeeks.stream()
                .filter(w -> w.getWeeklyStatus() == WeeklyStatus.SUCCESS)
                .count();

        int hostTotalElapsed = hostWeeks.stream()
                .map(WeeklyProgress::getElapsedTimeSeconds)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        ChallengeMember hostMember = members.stream()
                .filter(m -> m.getRole() == MemberRole.HOST).findFirst().get();
        processSettlement(hostMember, hostSuccessWeeks, challenge.getDurationWeek());

        List<WeeklyProgress> memberWeeks = weeklyProgressRepository.findAllByChallengeAndUser(challenge, memberUser);

        int memberSuccessWeeks = (int) memberWeeks.stream()
                .filter(w -> w.getWeeklyStatus() == WeeklyStatus.SUCCESS)
                .count();

        int memberTotalElapsed = memberWeeks.stream()
                .map(WeeklyProgress::getElapsedTimeSeconds)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        ChallengeMember opponentMember = members.stream()
                .filter(m -> m.getRole() == MemberRole.MEMBER).findFirst().get();
        processSettlement(opponentMember, memberSuccessWeeks, challenge.getDurationWeek());

        // ===== 승자 판정 =====
        Long winnerId = decideWinner(
                hostUser.getId(), memberUser.getId(),
                hostSuccessWeeks, memberSuccessWeeks,
                hostTotalElapsed, memberTotalElapsed
        );

        User winner = winnerId.equals(hostUser.getId()) ? hostUser : memberUser;

        return ChallengeDto.VsResultResponse.from(challenge, winner);
    }

    /**
     * 승자 규칙:
     * 1) 성공 주차 수
     * 2) 총 elapsed
     * 3) host
     */
    private Long decideWinner(
            Long hostId,
            Long memberId,
            int hostSuccessWeeks,
            int memberSuccessWeeks,
            int hostTotalElapsed,
            int memberTotalElapsed
    ) {
        if (hostSuccessWeeks != memberSuccessWeeks) {
            return hostSuccessWeeks > memberSuccessWeeks ? hostId : memberId;
        }
        if (hostTotalElapsed != memberTotalElapsed) {
            return hostTotalElapsed > memberTotalElapsed ? hostId : memberId;
        }
        return hostId;
    }

    private void processSettlement(ChallengeMember member, int successWeeks, int totalDurationWeeks) {
        // 이미 정산된 경우(결과가 있는 경우) 중복 실행 방지
        if (member.getResult() != null) {
            return;
        }

        // 달성률 계산 (0.0 ~ 1.0)
        double rate = (double) successWeeks / totalDurationWeeks;

        // 80% 이상이면 SUCCESS, 아니면 FAIL 저장
        boolean isSuccess = rate >= 0.8;
        member.setResult(isSuccess ? ChallengeResult.SUCCESS : ChallengeResult.FAIL);

        // 변경사항 저장 (JPA Dirty Checking이 동작하지만, 확실한 순서를 위해 flush 권장)
        challengeMemberRepository.saveAndFlush(member);

        // 레벨업 서비스 호출
        userService.checkAndApplyLevelUp(member.getId().getUser());
    }

    @Transactional
    public void settleAllFinishedChallenges() {
        List<Challenge> activeChallenges = challengeRepository.findAllByStatus(ChallengeStatus.IN_PROGRESS);

        LocalDate today = LocalDate.now();
        int count = 0;

        for (Challenge challenge : activeChallenges) {
            LocalDate endDate = challenge.getStartedAt().plusWeeks(challenge.getDurationWeek());

            if (!today.isBefore(endDate)) {
                try {
                    // 챌린지에 속한 모든 멤버(Host, Member)를 가져옴
                    List<ChallengeMember> members = challengeMemberRepository.findAllById_Challenge(challenge);

                    for (ChallengeMember member : members) {
                        // 각 멤버별로 정산 수행 (SOLO는 Host만, VS는 Host/Guest 모두 루프 돔)
                        calculateAndSettle(challenge, member);
                    }

                    // 4. 챌린지 상태 "완료"로 변경
                    challenge.setStatus(ChallengeStatus.COMPLETED);
                    count++;
                } catch (Exception e) {
                    log.error("챌린지 자동 정산 실패 (ID: {}): {}", challenge.getId(), e.getMessage());
                }
            }
        }
        log.info("[Scheduler] 총 {}건의 챌린지 정산 및 종료 처리 완료", count);
    }

    private void calculateAndSettle(Challenge challenge, ChallengeMember member) {
        User user = member.getId().getUser();

        List<WeeklyProgress> weeks = weeklyProgressRepository.findAllByChallengeAndUser(challenge, user);

        int successWeeks = (int) weeks.stream()
                .filter(w -> w.getWeeklyStatus() == WeeklyStatus.SUCCESS)
                .count();

        processSettlement(member, successWeeks, challenge.getDurationWeek());
    }

    @Transactional
    public ChallengeDto.ChallengeIdResponse updateChallenge(
            Long challengeId,
            UserDetails userDetails,
            ChallengeDto.ChallengeUpdateRequest request
    ) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new RestException(ErrorCode.CHALLENGE_NOT_FOUND));

        challenge.canUpdateBy(userDetails);

        // 분야 처리
        Set<Tag> resolvedTags = null;
        if(request.getTags() != null) {
            resolvedTags = tagService.findOrCreateByNames(request.getTags());
        }

        Set<Field> resolvedFields = null;
        if(request.getFields() != null) {
            resolvedFields = fieldService.findFieldByName(request.getFields());
        }

        request.applyTo(challenge, resolvedTags, resolvedFields);

        if (request.getThumbnail() != null && !request.getThumbnail().isEmpty()) {
            if (challenge.getChallengeImage() != null) {
                fileHandler.deleteIfExists(challenge.getChallengeImage());
            }

            ChallengeImage newImage = ChallengeImage.from(request.getThumbnail(), challenge);
            fileHandler.saveFile(request.getThumbnail(), newImage);
            challenge.setChallengeImage(newImage);
        }

        return ChallengeDto.ChallengeIdResponse.from(challenge);
    }

    @Transactional
    public void deleteChallenge(Long challengeId, UserDetails userDetails) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new RestException(ErrorCode.CHALLENGE_NOT_FOUND));

        challenge.canDeleteBy(userDetails);

        if(challenge.getChallengeImage() != null) {
            fileHandler.deleteIfExists(challenge.getChallengeImage());
        }
        weeklyProgressRepository.deleteAllByChallenge(challenge);
        challengeMemberRepository.deleteAllById_Challenge(challenge);

        challenge.getTags().clear();
        challenge.getFields().clear();

        challengeLikedRepository.deleteByChallenge(challenge);
        challengeRepository.delete(challenge);
    }

    @Transactional(readOnly = true)
    public List<ChallengeDto.ChallengeToPostResponse> getChallengeToPost(ChallengeMode challengeMode, UserDetails userDetails) {
        if(userDetails == null) {
            throw new RestException(ErrorCode.AUTH_LOGIN_REQUIRED);
        }
        Long loginUserId = userDetails.getUser().getId();

        List<Challenge> challenges = challengeQueryRepository.findSimpleMyChallenges(loginUserId, challengeMode);

        return challenges.stream()
                .map(ChallengeDto.ChallengeToPostResponse::from) // DTO의 factory method 사용
                .collect(Collectors.toList());
    }


    @Transactional(readOnly = true)
    public ChallengeDto.RecordResponse getMyChallengeRecords(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RestException(ErrorCode.USER_NOT_FOUND));

        int allAttempt = challengeMemberQueryRepository.countFinishedChallenges(user);
        int allSuccess = challengeMemberQueryRepository.countByUserAndResult(user, ChallengeResult.SUCCESS);

        int soloAttempt = challengeMemberQueryRepository.countFinishedChallengesByMode(user, ChallengeMode.SOLO);
        int soloSuccess = challengeMemberQueryRepository.countByUserAndResultAndMode(user, ChallengeResult.SUCCESS, ChallengeMode.SOLO);

        int vsAttempt = challengeMemberQueryRepository.countFinishedChallengesByMode(user, ChallengeMode.VS);
        int vsSuccess = challengeMemberQueryRepository.countByUserAndResultAndMode(user, ChallengeResult.SUCCESS, ChallengeMode.VS);

        ChallengeDto.RecordDetail solo = toDetail(soloAttempt, soloSuccess);
        ChallengeDto.RecordDetail vs = toDetail(vsAttempt, vsSuccess);

        return ChallengeDto.RecordResponse.builder()
                .allSuccessRate(calcRate(allAttempt, allSuccess))
                .soloRecord(solo)
                .vsRecord(vs)
                .build();
    }

    private ChallengeDto.RecordDetail toDetail(int attempt, int success) {
        return ChallengeDto.RecordDetail.builder()
                .successRate(calcRate(attempt, success))
                .attemptCount(attempt)
                .successCount(success)
                .failCount(Math.max(attempt - success, 0))
                .build();
    }

    private int calcRate(int attempt, int success) {
        if (attempt <= 0) return 0;
        return (success * 100) / attempt;
    }
}