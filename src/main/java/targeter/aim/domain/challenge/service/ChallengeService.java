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

    @Transactional
    public ChallengeDto.ChallengeDetailsResponse createChallenge(
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

        challenge.setMode(request.getMode());
        challenge.setVisibility(request.getVisibility());

        // 4. 태그 / 분야 연관관계 매핑
        updateChallengeLabels(challenge, request.getTags(), request.getFields());

        return toChallengeDetailsResponse(challenge);
    }

    @Transactional(readOnly = true)
    public ChallengeDto.ChallengePageResponse getVsChallenges(
            ChallengeDto.ListSearchCondition condition,
            UserDetails userDetails,
            Pageable pageable
    ) {
        ChallengeFilterType filterType = parseFilterType(condition.getFilterType());
        ChallengeSortType sortType = parseSortType(condition.getSort());

        // MY 탭은 로그인 필요
        if (filterType == ChallengeFilterType.MY && userDetails == null) {
            throw new RestException(ErrorCode.AUTH_LOGIN_REQUIRED);
        }

        String keyword = normalizeKeyword(condition.getKeyword());

        var page = challengeQueryRepository.paginateByTypeAndKeyword(
                userDetails,
                pageable,
                filterType,
                sortType,
                keyword
        );

        return ChallengeDto.ChallengePageResponse.from(page);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) return null;
        String k = keyword.trim();
        return k.isEmpty() ? null : k;
    }

    private ChallengeFilterType parseFilterType(String raw) {
        try {
            return ChallengeFilterType.valueOf(raw == null ? "ALL" : raw);
        } catch (IllegalArgumentException e) {
            throw new RestException(ErrorCode.GLOBAL_BAD_REQUEST);
        }
    }

    private ChallengeSortType parseSortType(String raw) {
        try {
            return ChallengeSortType.valueOf(raw == null ? "LATEST" : raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RestException(ErrorCode.GLOBAL_BAD_REQUEST);
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
            Set<Field> fields = fieldNames.stream()
                    .map(String::trim)
                    .map(name -> fieldRepository.findByName(name)
                            .orElseGet(() -> fieldRepository.save(Field.builder().name(name).build())))
                    .collect(Collectors.toSet());
            challenge.setFields(fields);
        }
    }

    private ChallengeDto.ChallengeDetailsResponse toChallengeDetailsResponse(Challenge challenge) {
        User host = challenge.getHost();

        ChallengeDto.ChallengeDetailsResponse.ChallengeInfo info =
                ChallengeDto.ChallengeDetailsResponse.ChallengeInfo.builder()
                        .challengeThumbnail(null)
                        .title(challenge.getName())
                        .tags(challenge.getTags().stream().map(Tag::getName).toList())
                        .fields(challenge.getFields().stream().map(Field::getName).toList())
                        .jobs(List.of(challenge.getJob().split(",")))
                        .startedAt(challenge.getStartedAt())
                        .durationWeek(challenge.getDurationWeek())
                        .status(challenge.getStatus())
                        .build();

        ChallengeDto.ChallengeDetailsResponse.ParticipantDetails me =
                ChallengeDto.ChallengeDetailsResponse.ParticipantDetails.builder()
                        .nickname(host.getNickname())
                        .progressRate("0/0")
                        .successRate(0)
                        .isSuccess(false)
                        .isRealTimeActive(false)
                        .build();

        ChallengeDto.ChallengeDetailsResponse.Participants participants =
                ChallengeDto.ChallengeDetailsResponse.Participants.builder()
                        .me(me)
                        .build();

        return ChallengeDto.ChallengeDetailsResponse.builder()
                .challengeInfo(info)
                .participants(participants)
                .currentWeekDetails(null)
                .build();
    }

    //VS 챌린지 상세 조회

    @Transactional(readOnly = true)
    public ChallengeDto.VsChallengeDetailResponse getVsChallengeDetail(
            Long challengeId,
            UserDetails userDetails
    ) {
        if (userDetails == null) {
            throw new RestException(ErrorCode.AUTH_LOGIN_REQUIRED);
        }
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

        WeeklyProgress myWeek =
                weeklyProgressRepository.findByChallengeAndUserAndWeekNumber(challenge, me, currentWeek)
                        .orElse(null);

        String thumbnail = attachedFileRepository.findChallengeImageByChallengeId(challengeId)
                .map(ChallengeImage::getFilePath)
                .orElse(null);

        ChallengeDto.VsChallengeDetailResponse.ChallengeInfo challengeInfo =
                ChallengeDto.VsChallengeDetailResponse.ChallengeInfo.builder()
                        .thumbnail(thumbnail)
                        .title(challenge.getName())
                        .tags(challenge.getTags().stream().map(Tag::getName).toList())
                        .category(challenge.getFields().stream().map(Field::getName).findFirst().orElse(null))
                        .job(challenge.getJob())
                        .startDate(challenge.getStartedAt().toString())
                        .totalWeeks(totalWeeks)
                        .state(challenge.getStatus().name())
                        .build();

        return ChallengeDto.VsChallengeDetailResponse.builder()
                .challengeInfo(challengeInfo)
                .build();
    }

    // 공통 계산 메서드

    private int calcCurrentWeek(LocalDate startedAt, int totalWeeks) {
        long days = Duration.between(startedAt.atStartOfDay(), LocalDate.now().atStartOfDay()).toDays();
        int week = (int) (days / 7) + 1;
        return Math.min(Math.max(week, 1), totalWeeks);
    }

    private boolean isRealTimeActive(WeeklyProgress wp) {
        return wp != null && wp.getLastModifiedAt() != null &&
                Duration.between(wp.getLastModifiedAt(), LocalDateTime.now()).getSeconds() <= 30;
    }

    private String getProfileImagePath(User user) {
        ProfileImage img = user.getProfileImage();
        return img == null ? null : img.getFilePath();
    }
}