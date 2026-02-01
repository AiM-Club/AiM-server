package targeter.aim.domain.challenge.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import targeter.aim.domain.challenge.dto.ChallengeDto;
import targeter.aim.domain.challenge.entity.*;
import targeter.aim.domain.challenge.repository.*;
import targeter.aim.domain.user.entity.User;
import targeter.aim.system.exception.model.ErrorCode;
import targeter.aim.system.exception.model.RestException;
import targeter.aim.system.security.model.UserDetails;

import java.time.Duration;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChallengeReadService {

    private final ChallengeRepository challengeRepository;
    private final ChallengeMemberRepository challengeMemberRepository;
    private final ChallengeLikedRepository challengeLikedRepository;

    private final ChallengeQueryRepository challengeQueryRepository;
    private final WeeklyProgressQueryRepository weeklyProgressQueryRepository;

    public ChallengeDto.ChallengePageResponse getVsChallenges(
            ChallengeDto.VsListSearchCondition condition,
            UserDetails userDetails,
            Pageable pageable
    ) {
        ChallengeDto.ChallengeFilterType filterType = condition.getFilterType();
        ChallengeDto.ChallengeSortType sortType = condition.getSort();

        // MY 탭은 로그인 필요
        if (filterType == ChallengeDto.ChallengeFilterType.MY && userDetails == null) {
            throw new RestException(ErrorCode.AUTH_LOGIN_REQUIRED);
        }

        String keyword = normalizeKeyword(condition.getKeyword());
        String field = normalizeField(condition.getField());

        Page<ChallengeDto.ChallengeListResponse> page;

        if(field != null) {
            page = challengeQueryRepository.paginateVsByTypeAndKeywordAndField(
                    userDetails, pageable, filterType, sortType, keyword, field
            );
        } else {
            if (keyword != null) {
                page = challengeQueryRepository.paginateVsByTypeAndKeyword(
                        userDetails, pageable, filterType, sortType, keyword
                );
            } else {
                page = challengeQueryRepository.paginateVsByType(
                        userDetails, pageable, filterType, sortType
                );
            }
        }

        return ChallengeDto.ChallengePageResponse.from(page);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) return null;
        String k = keyword.trim();
        return k.isEmpty() ? null : k;
    }

    private String normalizeField(String field) {
        if (field == null) return null;

        String f = field.trim();
        if(f.isEmpty()) return null;

        String upper = f.toUpperCase(Locale.ROOT);
        if("ALL".equals(upper)) return null;

        return upper;
    }

    // VS 챌린지 Overview 조회
    public ChallengeDto.VsChallengeOverviewResponse getVsChallengeOverview(
            Long challengeId,
            UserDetails userDetails
    ) {
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

        Long loginUserId = (userDetails == null) ? null : userDetails.getUser().getId();

        if (challenge.getVisibility() == ChallengeVisibility.PRIVATE) {
            if (loginUserId == null) {
                throw new RestException(ErrorCode.AUTH_AUTHENTICATION_FAILED);
            }
            boolean isMember = members.stream()
                    .anyMatch(m -> m.getId().getUser().getId().equals(loginUserId));

            if (!isMember) {
                throw new RestException(ErrorCode.AUTH_FORBIDDEN);
            }
        }

        User me = hostUser;
        User opponent = memberUser;

        if(loginUserId != null && memberUser != null && loginUserId.equals(memberUser.getId())) {
            me = memberUser;
            opponent = hostUser;
        }

        int totalWeeks = challenge.getDurationWeek();
        int currentWeek = calcCurrentWeek(challenge.getStartedAt(), totalWeeks);

        // 6) userIds 구성 (상대 없으면 나만)
        List<Long> userIds = (memberUser == null)
                ? List.of(hostUser.getId())
                : List.of(hostUser.getId(), memberUser.getId());

        // 7) (진도율) 완료주차/전체주차 %
        Map<Long, Long> completedTotalMap =
                safeMap(weeklyProgressQueryRepository.completedCountByUsers(challengeId, userIds, totalWeeks));

        int myProgressRate = percent(completedTotalMap.getOrDefault(me.getId(), 0L), totalWeeks);
        int oppoProgressRate = opponent == null ? 0
                : percent(completedTotalMap.getOrDefault(opponent.getId(), 0L), totalWeeks);

        // 8) (성공률) 성공주차/현재주차 %
        //    - 현재주차는 "진행 중인 주차"까지 포함해서 분모로 잡는다 (요구사항/정책에 따라 바꿔도 됨)
        int successEndWeek = Math.max(currentWeek, 1);

        Map<Long, Long> successUpToCurrentMap =
                safeMap(weeklyProgressQueryRepository.successCountByUsers(challengeId, userIds, successEndWeek));

        int mySuccessRate = percent(successUpToCurrentMap.getOrDefault(me.getId(), 0L), successEndWeek);
        int oppoSuccessRate = opponent == null ? 0
                : percent(successUpToCurrentMap.getOrDefault(opponent.getId(), 0L), successEndWeek);

        // 9) (우세현황) 지난주차까지 기준 성공률로 막대폭 산정
        int dominanceEndWeek = Math.max(currentWeek - 1, 0);

        int myDominanceRate = 0;
        int oppoDominanceRate = 0;

        if (dominanceEndWeek > 0) {
            Map<Long, Long> successUpToPrevMap =
                    safeMap(weeklyProgressQueryRepository.successCountByUsers(challengeId, userIds, dominanceEndWeek));

            myDominanceRate = percent(successUpToPrevMap.getOrDefault(me.getId(), 0L), dominanceEndWeek);
            oppoDominanceRate = opponent == null ? 0
                    : percent(successUpToPrevMap.getOrDefault(opponent.getId(), 0L), dominanceEndWeek);
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

        boolean isLiked = false;
        if(loginUserId != null) {
            isLiked = challengeLikedRepository.existsByUserAndChallenge(userDetails.getUser(), challenge);
        }

        // 10) DTO 반환
        return ChallengeDto.VsChallengeOverviewResponse.from(
                challenge,
                isLiked,
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

    public ChallengeDto.ChallengePageResponse getSoloChallenges(
            ChallengeDto.SoloListSearchCondition condition,
            UserDetails userDetails,
            Pageable pageable
    ) {
        ChallengeDto.ChallengeFilterType filterType = condition.getFilterType();
        ChallengeDto.ChallengeSortType sortType = condition.getSort();

        String keyword = normalizeKeyword(condition.getKeyword());

        Page<ChallengeDto.ChallengeListResponse> page;

        if (keyword != null) {
            page = challengeQueryRepository.paginateSoloByTypeAndKeyword(
                    userDetails, pageable, filterType, sortType, keyword
            );
        } else {
            page = challengeQueryRepository.paginateSoloByType(
                    userDetails, pageable, filterType, sortType
            );
        }

        return ChallengeDto.ChallengePageResponse.from(page);
    }

    // SOLO 챌린지 Overview 조회
    public ChallengeDto.SoloChallengeOverviewResponse getSoloChallengeOverview(
            Long challengeId,
            UserDetails userDetails
    ) {
        if (userDetails == null) {
            throw new RestException(ErrorCode.AUTH_LOGIN_REQUIRED);
        }

        Long loginUserId = userDetails.getUser().getId();

        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new RestException(ErrorCode.GLOBAL_NOT_FOUND));

        if (challenge.getMode() != ChallengeMode.SOLO) {
            throw new RestException(ErrorCode.GLOBAL_BAD_REQUEST);
        }

        if (challenge.getVisibility() == ChallengeVisibility.PRIVATE &&
                !challenge.getHost().getId().equals(loginUserId)) {
            throw new RestException(ErrorCode.AUTH_FORBIDDEN);
        }

        User host = challenge.getHost();

        int totalWeeks = challenge.getDurationWeek();
        int currentWeek = calcCurrentWeek(challenge.getStartedAt(), totalWeeks);

        List<Long> userIds = List.of(host.getId());

        Map<Long, Long> completedTotalMap =
                safeMap(weeklyProgressQueryRepository.completedCountByUsers(
                        challengeId,
                        userIds,
                        totalWeeks
                ));

        int progressRate = percent(
                completedTotalMap.getOrDefault(host.getId(), 0L),
                totalWeeks
        );

        int successEndWeek = Math.max(currentWeek, 1);

        Map<Long, Long> completedUpToCurrentMap =
                safeMap(weeklyProgressQueryRepository.completedCountByUsers(
                        challengeId,
                        userIds,
                        successEndWeek
                ));

        int successRate = percent(
                completedUpToCurrentMap.getOrDefault(host.getId(), 0L),
                successEndWeek
        );

        boolean isLiked = false;
        if(loginUserId != null) {
            isLiked = challengeLikedRepository.existsByUserAndChallenge(userDetails.getUser(), challenge);
        }

        return ChallengeDto.SoloChallengeOverviewResponse.from(
                challenge,
                isLiked,
                host,
                progressRate,
                successRate
        );
    }

    public ChallengeDto.ChallengePageResponse getAllChallenges(
            ChallengeDto.AllListSearchCondition condition,
            UserDetails userDetails,
            Pageable pageable
    ) {
        ChallengeDto.ChallengeSortType sortType = condition.getSort();

        String keyword = normalizeKeyword(condition.getKeyword());

        Page<ChallengeDto.ChallengeListResponse> page;

        if (keyword != null) {
            page = challengeQueryRepository.paginateAllByTypeAndKeyword(
                    userDetails, pageable, sortType, keyword
            );
        } else {
            page = challengeQueryRepository.paginateAllByType(
                    userDetails, pageable, sortType
            );
        }

        return ChallengeDto.ChallengePageResponse.from(page);
    }
}
