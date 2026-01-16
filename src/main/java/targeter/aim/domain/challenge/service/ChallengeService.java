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

    private final ChallengeRepository challengeRepository;
    private final ChallengeMemberRepository challengeMemberRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final FieldRepository fieldRepository;

    private final ChallengeQueryRepository challengeQueryRepository;
    private final WeeklyProgressQueryRepository weeklyProgressQueryRepository;

    private final ChallengeRoutePersistService persistService;
    private final ChallengeRouteGenerationService generationService;

    @Transactional
    public ChallengeDto.ChallengeCreateResponse createChallenge(
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

        return ChallengeDto.ChallengeCreateResponse.from(challenge.getId());
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

    //VS 챌린지 Overview 조회
    @Transactional(readOnly = true)
    public ChallengeDto.VsChallengeOverviewResponse getVsChallengeOverview(
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

        // 6) userIds 구성 (상대 없으면 나만)
        List<Long> userIds = (opponent == null)
                ? List.of(me.getId())
                : List.of(me.getId(), opponent.getId());

        // 7) (진도율) 완료주차/전체주차 %
        Map<Long, Long> completedTotalMap =
                safeMap(weeklyProgressQueryRepository.completedCountByUsers(challengeId, userIds, totalWeeks));

        int myProgressRate = percent(completedTotalMap.getOrDefault(me.getId(), 0L), totalWeeks);
        int oppoProgressRate = opponent == null ? 0
                : percent(completedTotalMap.getOrDefault(opponent.getId(), 0L), totalWeeks);

        // 8) (성공률) 성공주차/현재주차 %
        //    - 현재주차는 "진행 중인 주차"까지 포함해서 분모로 잡는다 (요구사항/정책에 따라 바꿔도 됨)
        int successEndWeek = Math.max(currentWeek, 1);

        Map<Long, Long> completedUpToCurrentMap =
                safeMap(weeklyProgressQueryRepository.completedCountByUsers(challengeId, userIds, successEndWeek));

        int mySuccessRate = percent(completedUpToCurrentMap.getOrDefault(me.getId(), 0L), successEndWeek);
        int oppoSuccessRate = opponent == null ? 0
                : percent(completedUpToCurrentMap.getOrDefault(opponent.getId(), 0L), successEndWeek);

        // 9) (우세현황) 지난주차까지 기준 성공률로 막대폭 산정
        int dominanceEndWeek = Math.max(currentWeek - 1, 0);

        int myDominanceRate = 0;
        int oppoDominanceRate = 0;

        if (dominanceEndWeek > 0) {
            Map<Long, Long> completedUpToPrevMap =
                    safeMap(weeklyProgressQueryRepository.completedCountByUsers(challengeId, userIds, dominanceEndWeek));

            myDominanceRate = percent(completedUpToPrevMap.getOrDefault(me.getId(), 0L), dominanceEndWeek);
            oppoDominanceRate = opponent == null ? 0
                    : percent(completedUpToPrevMap.getOrDefault(opponent.getId(), 0L), dominanceEndWeek);
        }

        int myPercent;
        int opponentPercent;

        if (opponent == null) {
            // 상대가 없으면 내 100 / 상대 0 (정책)
            myPercent = 100;
            opponentPercent = 0;
        } else {
            int sum = myDominanceRate + oppoDominanceRate;
            if (sum == 0) {
                myPercent = 50;
                opponentPercent = 50;
            } else {
                myPercent = (int) Math.round((myDominanceRate * 100.0) / sum);
                opponentPercent = 100 - myPercent;
            }
        }

        ChallengeDto.VsChallengeOverviewResponse.Dominance dominance =
                ChallengeDto.VsChallengeOverviewResponse.Dominance.of(
                        oppoDominanceRate,
                        myDominanceRate,
                        opponentPercent,
                        myPercent
                );

        // 10) DTO 반환
        return ChallengeDto.VsChallengeOverviewResponse.from(
                challenge,
                dominance,
                me, myProgressRate, mySuccessRate,
                opponent, oppoProgressRate, oppoSuccessRate
        );
    }

    private int calcCurrentWeek(LocalDate startedAt, int totalWeeks) {
        long days = Duration.between(startedAt.atStartOfDay(), LocalDate.now().atStartOfDay()).toDays();
        int week = (int) (days / 7) + 1;
        return Math.min(Math.max(week, 1), totalWeeks);
    }

    private int percent(long numerator, int denominator) {
        if (denominator <= 0) return 0;
        return (int) Math.round((numerator * 100.0) / denominator);
    }

    private Map<Long, Long> safeMap(Map<Long, Long> map) {
        return map == null ? Map.of() : map;
    }
}