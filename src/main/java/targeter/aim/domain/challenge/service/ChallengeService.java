package targeter.aim.domain.challenge.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import targeter.aim.domain.ai.llm.dto.RoutePayload;
import targeter.aim.domain.challenge.dto.ChallengeDto;
import targeter.aim.domain.challenge.entity.*;
import targeter.aim.domain.challenge.repository.*;
import targeter.aim.domain.file.entity.ChallengeImage;
import targeter.aim.domain.file.entity.ProfileImage;
import targeter.aim.domain.file.repository.AttachedFileRepository;
import targeter.aim.domain.label.entity.Field;
import targeter.aim.domain.label.entity.Tag;
import targeter.aim.domain.label.repository.FieldRepository;
import targeter.aim.domain.label.repository.TagRepository;
import targeter.aim.domain.user.entity.User;
import targeter.aim.domain.user.repository.UserRepository;
import targeter.aim.system.exception.model.ErrorCode;
import targeter.aim.system.exception.model.RestException;
import targeter.aim.system.security.model.UserDetails;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChallengeService {

    private final ChallengeQueryRepository challengeQueryRepository;
    private final ChallengeRepository challengeRepository;
    private final WeeklyProgressRepository weeklyProgressRepository;
    private final ChallengeMemberRepository challengeMemberRepository;
    private final WeeklyCommentRepository weeklyCommentRepository;
    private final AttachedFileRepository attachedFileRepository;

    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final FieldRepository fieldRepository;

    private final ChallengeRoutePersistService persistService;
    private final ChallengeRouteGenerationService generationService;


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

    @Transactional
    public ChallengeDto.ChallengeDetailsResponse createChallenge(UserDetails userDetails, ChallengeDto.ChallengeCreateRequest request) {

        User user = userDetails.getUser();
        // 1. 주차별 계획(Payload) 생성
        RoutePayload routePayload = generationService.generateRoute(request);

        // 2. 생성된 데이터 저장
        Long challengeId = persistService.persistAtomic(user.getId(), request, routePayload);

        // 4. 생성된 챌린지 조회 및 추가 정보
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new RestException(ErrorCode.CHALLENGE_NOT_FOUND));

        challenge.setMode(request.getMode());
        challenge.setVisibility(request.getVisibility());

        // 5. 태그 / 분야 연관관계 매핑
        updateChallengeLabels(challenge, request.getTags(), request.getFields());

        return toChallengeDetailsResponse(challenge, user.getId());
    }

    private void updateChallengeLabels(Challenge challenge, List<String> tagNames, List<String> fieldNames) {
        // Tag 처리
        if(tagNames != null) {
            Set<Tag> tags = new HashSet<>();
            for (String name: tagNames) {
                String normalizedName = name.trim();
                Tag tag = tagRepository.findByName(normalizedName)
                        .orElseGet(() -> tagRepository.save(Tag.builder().name(normalizedName).build()));
                tags.add(tag);
            }
            challenge.setTags(tags);
        }

        // field 처리
        if(fieldNames != null) {
            Set<Field> fields = new HashSet<>();
            for (String name: fieldNames) {
                String normalizedName = name.trim();
                Field field = fieldRepository.findByName(normalizedName)
                        .orElseGet(() -> fieldRepository.save(Field.builder().name(normalizedName).build()));
                fields.add(field);
            }
            challenge.setFields(fields);
        }
    }

    private ChallengeDto.ChallengeDetailsResponse toChallengeDetailsResponse(Challenge challenge, Long currentUserId) {
        User host = challenge.getHost();

        // 1. ChallengeInfo
        ChallengeDto.ChallengeDetailsResponse.ChallengeInfo info = ChallengeDto.ChallengeDetailsResponse.ChallengeInfo.builder()
                .challengeThumbnail(null)
                .title(challenge.getName())
                .tags(challenge.getTags().stream().map(Tag::getName).collect(Collectors.toList()))
                .fields(challenge.getFields().stream().map(Field::getName).collect(Collectors.toList()))
                .jobs(List.of(challenge.getJob().split(",")))
                .startedAt(challenge.getStartedAt())
                .durationWeek(challenge.getDurationWeek())
                .status(challenge.getStatus())
                .build();

        // 2. Participants
        ChallengeDto.ChallengeDetailsResponse.ParticipantDetails me = ChallengeDto.ChallengeDetailsResponse.ParticipantDetails.builder()
                .profileImage(null)
                .nickname(host.getNickname())
                .progressRate("0%")
                .successRate(0)
                .isSuccess(false)
                .isRealTimeActive(false)
                .build();

        ChallengeDto.ChallengeDetailsResponse.Participants participants = ChallengeDto.ChallengeDetailsResponse.Participants.builder()
                .me(me)
                .opponent(null)
                .build();

        // 3. CurrentWeekDetails
        WeeklyProgress week1Progress = weeklyProgressRepository.findByChallengeAndWeekNumber(challenge, 1)
                .orElse(WeeklyProgress.builder() // 예외 방지용 더미
                        .weekNumber(1)
                        .title("생성 중...")
                        .content("데이터를 불러오는 중입니다.")
                        .isComplete(false)
                        .stopwatchTimeSeconds(0)
                        .build());

        ChallengeDto.ChallengeDetailsResponse.CurrentWeekDetails currentWeek = ChallengeDto.ChallengeDetailsResponse.CurrentWeekDetails.builder()
                .weekNumber(week1Progress.getWeekNumber())
                .period(null) // TODO: 날짜 계산 로직 필요 (start date 기준 1주차 기간)
                .weekTitle(week1Progress.getTitle())
                .weekContent(week1Progress.getContent())
                .recordTime(String.valueOf(week1Progress.getStopwatchTimeSeconds())) // 포맷팅 필요 시 수정
                .isFinished(week1Progress.getIsComplete())
                .comments(Collections.emptyList()) // 생성 시 댓글 없음
                .build();

        return ChallengeDto.ChallengeDetailsResponse.builder()
                .challengeInfo(info)
                .participants(participants)
                .currentWeekDetails(currentWeek)
                .build();
    }

    // VS 챌린지 상세 조회 (ChallengeController에서 호출)
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

        String category = challenge.getFields().stream()
                .map(Field::getName)
                .findFirst()
                .orElse(null);

        String job = Arrays.stream(Optional.ofNullable(challenge.getJob()).orElse("").split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .findFirst()
                .orElse(null);

        ChallengeDto.VsChallengeDetailResponse.ChallengeInfo challengeInfo =
                ChallengeDto.VsChallengeDetailResponse.ChallengeInfo.builder()
                        .thumbnail(thumbnail)
                        .title(challenge.getName())
                        .tags(challenge.getTags().stream().map(Tag::getName).toList())
                        .category(category)
                        .job(job)
                        .startDate(challenge.getStartedAt() == null ? null : challenge.getStartedAt().toString())
                        .totalWeeks(totalWeeks)
                        .state(challenge.getStatus() == null ? null : challenge.getStatus().name())
                        .build();

        ChallengeDto.VsChallengeDetailResponse.Me meDto =
                ChallengeDto.VsChallengeDetailResponse.Me.builder()
                        .profileImage(getProfileImagePath(me))
                        .nickname(me.getNickname())
                        .progressRate(currentWeek + "/" + totalWeeks)
                        .successRate(mySuccessRate)
                        .isSuccess(mySuccessRate >= 70)
                        .build();

        ChallengeDto.VsChallengeDetailResponse.Opponent opponentDto =
                ChallengeDto.VsChallengeDetailResponse.Opponent.builder()
                        .profileImage(opponent == null ? null : getProfileImagePath(opponent))
                        .nickname(opponent == null ? null : opponent.getNickname())
                        .progressRate(currentWeek + "/" + totalWeeks)
                        .successRate(opponentSuccessRate)
                        .isRealTimeActive(opponentRealTime)
                        .build();

        ChallengeDto.VsChallengeDetailResponse.Participants participants =
                ChallengeDto.VsChallengeDetailResponse.Participants.builder()
                        .me(meDto)
                        .opponent(opponent == null ? null : opponentDto)
                        .build();

        ChallengeDto.VsChallengeDetailResponse.CurrentWeekDetail currentWeekDetail =
                ChallengeDto.VsChallengeDetailResponse.CurrentWeekDetail.builder()
                        .weekNumber(currentWeek)
                        .period(calcPeriod(challenge.getStartedAt(), currentWeek))
                        .aiTitle(myWeek == null ? null : myWeek.getTitle())
                        .aiContent(myWeek == null ? null : myWeek.getContent())
                        .recordTime(formatSeconds(myWeek == null ? null : myWeek.getStopwatchTimeSeconds()))
                        .isFinished(myWeek != null && Boolean.TRUE.equals(myWeek.getIsComplete()))
                        .build();

        List<ChallengeDto.VsChallengeDetailResponse.CommentNode> comments = buildCommentTreeForSpec(myWeek);

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

    private List<ChallengeDto.VsChallengeDetailResponse.CommentNode> buildCommentTreeForSpec(WeeklyProgress myWeek) {
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