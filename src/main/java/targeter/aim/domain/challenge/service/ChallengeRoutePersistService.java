package targeter.aim.domain.challenge.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import targeter.aim.domain.ai.llm.dto.RoutePayload;
import targeter.aim.domain.challenge.dto.ChallengeDto;
import targeter.aim.domain.challenge.entity.*;
import targeter.aim.domain.challenge.repository.ChallengeMemberRepository;
import targeter.aim.domain.challenge.repository.ChallengeRepository;
import targeter.aim.domain.challenge.repository.WeeklyCommentRepository;
import targeter.aim.domain.challenge.repository.WeeklyProgressRepository;
import targeter.aim.domain.file.entity.AttachedFile;
import targeter.aim.domain.file.entity.ChallengeImage;
import targeter.aim.domain.file.entity.ProfileImage;
import targeter.aim.domain.file.repository.AttachedFileRepository;
import targeter.aim.domain.label.entity.Field;
import targeter.aim.domain.label.entity.Tag;
import targeter.aim.domain.user.entity.User;
import targeter.aim.domain.user.repository.UserRepository;
import targeter.aim.system.exception.model.ErrorCode;
import targeter.aim.system.exception.model.RestException;
import targeter.aim.system.security.model.UserDetails;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChallengeRoutePersistService {

    private final ChallengeRepository challengeRepository;
    private final ChallengeMemberRepository challengeMemberRepository;
    private final WeeklyProgressRepository weeklyProgressRepository;
    private final UserRepository userRepository;

    private final WeeklyCommentRepository weeklyCommentRepository;
    private final AttachedFileRepository attachedFileRepository;

    // 상위 트랜잭션과 독립적으로 실행
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long persistAtomic(Long userId, ChallengeDto.ProgressCreateRequest req, RoutePayload payload) {
        // 1. Host 유저 조회
        User host = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 2. 멱등성
        Optional<Challenge> existing = challengeRepository.findByHostAndNameAndStartedAt(host, req.getName(), req.getStartedAt());

        if (existing.isPresent()) {
            log.warn("Challenge with id {} already exists", existing.get().getId());
            return existing.get().getId();
        }

        // 3. Challenge 엔티티 생성 및 저장
        Challenge challenge = Challenge.builder()
                .host(host)
                .name(req.getName())
                .job(String.join(",", req.getJobs()))
                .startedAt(req.getStartedAt())
                .durationWeek(req.getDuration())
                .mode(req.getMode())
                .visibility(req.getVisibility())
                .status(ChallengeStatus.IN_PROGRESS)
                .build();

        Challenge savedChallenge = challengeRepository.save(challenge);

        // 4. ChallengeMember 저장
        ChallengeMember hostMember = ChallengeMember.builder()
                .id(ChallengeMemberId.of(savedChallenge, host))
                .role(MemberRole.HOST)
                .build();
        challengeMemberRepository.save(hostMember);

        // 5. WeeklyProgress 저장
        for (RoutePayload.Week week : payload.getWeeks()) {
            WeeklyProgress progress = WeeklyProgress.builder()
                    .challenge(savedChallenge)
                    .user(host)
                    .weekNumber(week.getWeekNumber())
                    .title(week.getTitle())
                    .content(week.getContent())
                    .stopwatchTimeSeconds(week.getTargetSeconds())
                    .isComplete(false)
                    .build();
            weeklyProgressRepository.save(progress);
        }

        return savedChallenge.getId();
    }

    @Transactional(readOnly = true)
    public ChallengeDto.VsChallengeDetailResponse getVsChallengeDetail(
            Long challengeId,
            UserDetails userDetails,
            String filterType,
            String sort,
            String order,
            Integer page,
            Integer size
    ) {
        return getVsChallengeDetail(challengeId, userDetails);
    }

    @Transactional(readOnly = true)
    public ChallengeDto.VsChallengeDetailResponse getVsChallengeDetail(Long challengeId, UserDetails userDetails) {

        Long loginUserId = userDetails.getUser().getId();

        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new RestException(ErrorCode.GLOBAL_NOT_FOUND));

        if (challenge.getMode() != ChallengeMode.VS) {
            throw new RestException(ErrorCode.GLOBAL_BAD_REQUEST);
        }

        List<ChallengeMember> members = challengeMemberRepository.findAllById_Challenge(challenge);

        boolean isMember = members.stream()
                .anyMatch(m -> m.getId().getUser().getId().equals(loginUserId));

        if (challenge.getVisibility() == ChallengeVisibility.PRIVATE && !isMember) {
            throw new RestException(ErrorCode.AUTH_FORBIDDEN);
        }

        User me = userRepository.findById(loginUserId)
                .orElseThrow(() -> new RestException(ErrorCode.USER_NOT_FOUND));

        User opponent = members.stream()
                .map(m -> m.getId().getUser())
                .filter(u -> !u.getId().equals(loginUserId))
                .findFirst()
                .orElse(null);

        int totalWeeks = challenge.getDurationWeek();
        int currentWeek = calcCurrentWeek(challenge.getStartedAt(), totalWeeks);

        WeeklyProgress myWeek = weeklyProgressRepository
                .findByChallengeAndUserAndWeekNumber(challenge, me, currentWeek)
                .orElse(null);

        WeeklyProgress opponentWeek = (opponent == null) ? null :
                weeklyProgressRepository.findByChallengeAndUserAndWeekNumber(challenge, opponent, currentWeek)
                        .orElse(null);

        long myCompleted = weeklyProgressRepository.countByChallengeAndUserAndIsCompleteTrue(challenge, me);
        long opponentCompleted = (opponent == null) ? 0 :
                weeklyProgressRepository.countByChallengeAndUserAndIsCompleteTrue(challenge, opponent);

        int mySuccessRate = calcSuccessRate(myCompleted, currentWeek);
        int opponentSuccessRate = calcSuccessRate(opponentCompleted, currentWeek);

        boolean opponentRealTime = isRealTimeActive(opponentWeek);

        String thumbnail = attachedFileRepository.findChallengeImageByChallengeId(challengeId)
                .map(ChallengeImage::getFilePath)
                .orElse(null);

        ChallengeDto.VsChallengeDetailResponse.ChallengeInfo challengeInfo =
                ChallengeDto.VsChallengeDetailResponse.ChallengeInfo.builder()
                        .thumbnail(thumbnail)
                        .title(challenge.getName())
                        .tags(challenge.getTags().stream().map(Tag::getName).toList())
                        .fields(challenge.getFields().stream().map(Field::getName).toList())
                        .job(challenge.getJob())
                        .startDate(challenge.getStartedAt().toString())
                        .totalWeeks(totalWeeks)
                        .state(challenge.getStatus().name())
                        .build();

        ChallengeDto.VsChallengeDetailResponse.Participant meDto =
                ChallengeDto.VsChallengeDetailResponse.Participant.builder()
                        .profileImage(getProfileImagePath(me))
                        .nickname("ME")
                        .progressRate(currentWeek + "/" + totalWeeks)
                        .successRate(mySuccessRate)
                        .isSuccess(mySuccessRate >= 70)
                        .isRealTimeActive(false)
                        .build();

        ChallengeDto.VsChallengeDetailResponse.Participant opponentDto =
                ChallengeDto.VsChallengeDetailResponse.Participant.builder()
                        .profileImage(opponent == null ? null : getProfileImagePath(opponent))
                        .nickname(opponent == null ? null : opponent.getNickname())
                        .progressRate(currentWeek + "/" + totalWeeks)
                        .successRate(opponentSuccessRate)
                        .isSuccess(opponentSuccessRate >= 70)
                        .isRealTimeActive(opponentRealTime)
                        .build();

        ChallengeDto.VsChallengeDetailResponse.Participants participants =
                ChallengeDto.VsChallengeDetailResponse.Participants.builder()
                        .me(meDto)
                        .opponent(opponentDto)
                        .build();

        String authFile = (myWeek == null) ? null
                : attachedFileRepository.findWeeklyAuthFileByWeeklyProgressId(myWeek.getId())
                .map(AttachedFile::getFilePath)
                .orElse(null);

        ChallengeDto.VsChallengeDetailResponse.CurrentWeekDetail currentWeekDetail =
                ChallengeDto.VsChallengeDetailResponse.CurrentWeekDetail.builder()
                        .weekNumber(currentWeek)
                        .period(calcPeriod(challenge.getStartedAt(), currentWeek))
                        .aiTitle(myWeek == null ? null : myWeek.getTitle())
                        .aiContent(myWeek == null ? null : myWeek.getContent())
                        .recordTime(formatSeconds(myWeek == null ? null : myWeek.getStopwatchTimeSeconds()))
                        .authFile(authFile)
                        .isFinished(myWeek != null && Boolean.TRUE.equals(myWeek.getIsComplete()))
                        .build();

        List<ChallengeDto.VsChallengeDetailResponse.CommentNode> comments = buildCommentTree(myWeek);

        return ChallengeDto.VsChallengeDetailResponse.builder()
                .challengeInfo(challengeInfo)
                .participants(participants)
                .currentWeekDetail(currentWeekDetail)
                .comments(comments)
                .build();
    }

    private int calcCurrentWeek(LocalDate startedAt, int totalWeeks) {
        LocalDate today = LocalDate.now();
        long days = Duration.between(startedAt.atStartOfDay(), today.atStartOfDay()).toDays();
        int week = (int) (days / 7) + 1;
        if (week < 1) week = 1;
        if (week > totalWeeks) week = totalWeeks;
        return week;
    }

    private String calcPeriod(LocalDate startedAt, int weekNumber) {
        LocalDate weekStart = startedAt.plusDays((long) (weekNumber - 1) * 7);
        LocalDate weekEnd = weekStart.plusDays(6);
        return weekStart + " ~ " + weekEnd;
    }

    private int calcSuccessRate(long completedWeeks, int currentWeek) {
        if (currentWeek <= 0) return 0;
        double rate = (double) completedWeeks / (double) currentWeek * 100.0;
        return (int) Math.round(rate);
    }

    private boolean isRealTimeActive(WeeklyProgress wp) {
        if (wp == null || wp.getLastModifiedAt() == null) return false;
        return Duration.between(wp.getLastModifiedAt(), LocalDateTime.now()).getSeconds() <= 30;
    }

    private String getProfileImagePath(User user) {
        ProfileImage img = user.getProfileImage();
        return img == null ? null : img.getFilePath();
    }

    private String formatSeconds(Integer seconds) {
        if (seconds == null || seconds < 0) return "00:00:00";
        int h = seconds / 3600;
        int m = (seconds % 3600) / 60;
        int s = seconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    private List<ChallengeDto.VsChallengeDetailResponse.CommentNode> buildCommentTree(WeeklyProgress myWeek) {
        if (myWeek == null) return List.of();

        List<WeeklyComment> comments = weeklyCommentRepository
                .findAllByWeeklyProgress_IdOrderByCreatedAtAsc(myWeek.getId());

        Map<Long, ChallengeDto.VsChallengeDetailResponse.CommentNode> nodeMap = new LinkedHashMap<>();
        Map<Long, List<Long>> childrenMap = new LinkedHashMap<>();

        for (WeeklyComment c : comments) {
            nodeMap.put(
                    c.getId(),
                    ChallengeDto.VsChallengeDetailResponse.CommentNode.builder()
                            .commentId(c.getId())
                            .writer(c.getUser().getNickname())
                            .content(c.getContent())
                            .createdAt(c.getCreatedAt() == null ? null : c.getCreatedAt().toString())
                            .children(new ArrayList<>())
                            .build()
            );

            if (c.getParentComment() != null) {
                childrenMap.computeIfAbsent(c.getParentComment().getId(), k -> new ArrayList<>()).add(c.getId());
            }
        }

        for (Map.Entry<Long, List<Long>> e : childrenMap.entrySet()) {
            Long parentId = e.getKey();
            ChallengeDto.VsChallengeDetailResponse.CommentNode parent = nodeMap.get(parentId);
            if (parent == null) continue;

            for (Long childId : e.getValue()) {
                ChallengeDto.VsChallengeDetailResponse.CommentNode child = nodeMap.get(childId);
                if (child != null) parent.getChildren().add(child);
            }
        }

        return comments.stream()
                .filter(c -> c.getParentComment() == null)
                .map(c -> nodeMap.get(c.getId()))
                .filter(Objects::nonNull)
                .toList();
    }
}