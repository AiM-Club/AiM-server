package targeter.aim.domain.challenge.repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import targeter.aim.domain.challenge.dto.ChallengeDto;
import targeter.aim.domain.challenge.entity.Challenge;
import targeter.aim.domain.challenge.entity.ChallengeMode;
import targeter.aim.domain.challenge.entity.ChallengeStatus;
import targeter.aim.domain.challenge.entity.QChallenge;
import targeter.aim.domain.challenge.entity.ChallengeVisibility;
import targeter.aim.domain.file.dto.FileDto;
import targeter.aim.domain.file.entity.ChallengeImage;
import targeter.aim.domain.file.entity.ProfileImage;
import targeter.aim.domain.label.entity.QField;
import targeter.aim.domain.label.entity.QTag;
import targeter.aim.domain.user.dto.TierDto;
import targeter.aim.domain.user.entity.User;
import targeter.aim.system.security.model.UserDetails;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static targeter.aim.domain.challenge.entity.QChallenge.challenge;
import static targeter.aim.domain.challenge.entity.QChallengeLiked.challengeLiked;
import static targeter.aim.domain.challenge.entity.QChallengeMember.challengeMember;
import static targeter.aim.domain.label.entity.QField.field;
import static targeter.aim.domain.label.entity.QTag.tag;

@Repository
@RequiredArgsConstructor
public class ChallengeQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 1. VS 챌린지용 Query
     */

    // 기존 전체 조회용
    public Page<ChallengeDto.ChallengeListResponse> paginateVsByType(
            UserDetails userDetails,
            Pageable pageable,
            ChallengeDto.ChallengeFilterType filterType,
            ChallengeDto.ChallengeSortType sortType
    ) {
        return paginateVsByTypeAndKeywordAndField(userDetails, pageable, filterType, sortType, null, null);
    }

    // 검색 전용
    public Page<ChallengeDto.ChallengeListResponse> paginateVsByTypeAndKeyword(
            UserDetails userDetails,
            Pageable pageable,
            ChallengeDto.ChallengeFilterType filterType,
            ChallengeDto.ChallengeSortType sortType,
            String keyword
    ) {
        return paginateVsByTypeAndKeywordAndField(userDetails, pageable, filterType, sortType, keyword, null);
    }

    // 분야 필터링
    public Page<ChallengeDto.ChallengeListResponse> paginateVsByTypeAndKeywordAndField(
            UserDetails userDetails,
            Pageable pageable,
            ChallengeDto.ChallengeFilterType filterType,
            ChallengeDto.ChallengeSortType sortType,
            String keyword,
            String field
    ) {
        // MY + 비로그인 → 빈 결과
        if (filterType == ChallengeDto.ChallengeFilterType.MY && userDetails == null) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        // IN_PROGRESS / COMPLETED → 메모리 정렬
        if (sortType == ChallengeDto.ChallengeSortType.IN_PROGRESS || sortType == ChallengeDto.ChallengeSortType.COMPLETED) {
            List<Tuple> tuples = buildVsBaseQuery(userDetails, filterType, keyword, field).fetch();
            List<ChallengeDto.ChallengeListResponse> all = enrichDetails(tuples);

            List<ChallengeDto.ChallengeListResponse> inProgress = all.stream()
                    .filter(dto -> dto.getStatus() == ChallengeStatus.IN_PROGRESS)
                    .sorted(Comparator.comparing(this::endDate))
                    .toList();

            List<ChallengeDto.ChallengeListResponse> completed = all.stream()
                    .filter(dto -> dto.getStatus() == ChallengeStatus.COMPLETED)
                    .sorted(Comparator.comparing(this::endDate).reversed())
                    .toList();

            List<ChallengeDto.ChallengeListResponse> sorted =
                    sortType == ChallengeDto.ChallengeSortType.IN_PROGRESS
                            ? Stream.concat(inProgress.stream(), completed.stream()).toList()
                            : Stream.concat(completed.stream(), inProgress.stream()).toList();

            return slice(sorted, pageable);
        }

        JPAQuery<Tuple> query = buildVsBaseQuery(userDetails, filterType, keyword, field);
        applyVsSorting(query, sortType);

        List<Tuple> tuples = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = buildCountVsQuery(userDetails, filterType, keyword, field).fetchOne();

        return new PageImpl<>(
                enrichDetails(tuples),
                pageable,
                total == null ? 0 : total
        );
    }

    private JPAQuery<Tuple> buildVsBaseQuery(
            UserDetails userDetails,
            ChallengeDto.ChallengeFilterType filterType,
            String keyword,
            String field
    ) {
        JPAQuery<Tuple> query = queryFactory
                .select(
                        challenge,
                        userDetails != null
                                ? JPAExpressions.selectOne()
                                .from(challengeLiked)
                                .where(
                                        challengeLiked.challenge.eq(challenge)
                                                .and(challengeLiked.user.id.eq(userDetails.getUser().getId()))
                                ).exists()
                                : Expressions.FALSE,
                        JPAExpressions.select(challengeLiked.count())
                                .from(challengeLiked)
                                .where(challengeLiked.challenge.eq(challenge))
                )
                .from(challenge)
                .where(challenge.mode.eq(ChallengeMode.VS))
                .leftJoin(challenge.host).fetchJoin()
                .leftJoin(challenge.host.tier).fetchJoin()
                .leftJoin(challenge.host.profileImage).fetchJoin();

        if (filterType == ChallengeDto.ChallengeFilterType.MY && userDetails != null) {
            query.join(challengeMember)
                    .on(challengeMember.id.challenge.eq(challenge))
                    .where(challengeMember.id.user.id.eq(userDetails.getUser().getId()));
        }

        BooleanExpression keywordPredicate = keywordCondition(keyword);
        BooleanExpression fieldPredicate = fieldFilterCondition(field);

        if (keywordPredicate != null) {
            query.where(keywordPredicate);
        }
        if (fieldPredicate != null) {
            query.where(fieldPredicate);
        }

        return query;
    }

    private JPAQuery<Long> buildCountVsQuery(
            UserDetails userDetails,
            ChallengeDto.ChallengeFilterType filterType,
            String keyword,
            String field
    ) {
        JPAQuery<Long> query = queryFactory
                .select(challenge.count())
                .from(challenge)
                .where(challenge.mode.eq(ChallengeMode.VS));

        if (filterType == ChallengeDto.ChallengeFilterType.MY && userDetails != null) {
            query.join(challengeMember)
                    .on(challengeMember.id.challenge.eq(challenge))
                    .where(challengeMember.id.user.id.eq(userDetails.getUser().getId()));
        }

        BooleanExpression keywordPredicate = keywordCondition(keyword);
        BooleanExpression fieldPredicate = fieldFilterCondition(field);

        if (keywordPredicate != null) {
            query.where(keywordPredicate);
        }
        if (fieldPredicate != null) {
            query.where(fieldPredicate);
        }

        return query;
    }

    private void applyVsSorting(JPAQuery<?> query, ChallengeDto.ChallengeSortType sortType) {
        switch (sortType) {
            case LATEST ->
                    query.orderBy(challenge.createdAt.desc());

            case OLDEST ->
                    query.orderBy(challenge.createdAt.asc());

            case TITLE ->
                    query.orderBy(
                            challenge.name.asc(),
                            challenge.createdAt.desc()
                    );

            default -> {
                // IN_PROGRESS / COMPLETED → 여기서 처리 안 함
            }
        }
    }

    private BooleanExpression fieldFilterCondition(String field) {
        if(field == null || field.isBlank()) return null;

        String upper = field.trim();

        var cField = new QChallenge("cFieldFilter");
        QField f = new QField("fFilter");

        return JPAExpressions
                .selectOne()
                .from(cField)
                .join(cField.fields, f)
                .where(
                        cField.id.eq(challenge.id)
                                .and(f.name.upper().eq(upper))
                )
                .exists();
    }

    /**
     * 2. SOLO 챌린지용 Query
     */
    public Page<ChallengeDto.ChallengeListResponse> paginateSoloByType(
            UserDetails userDetails,
            Pageable pageable,
            ChallengeDto.ChallengeFilterType filterType,
            ChallengeDto.ChallengeSortType sortType
    ) {
        return paginateSoloByTypeAndKeyword(userDetails, pageable, filterType, sortType, null);
    }

    public Page<ChallengeDto.ChallengeListResponse> paginateSoloByTypeAndKeyword(
            UserDetails userDetails,
            Pageable pageable,
            ChallengeDto.ChallengeFilterType filterType,
            ChallengeDto.ChallengeSortType sortType,
            String keyword
    ) {
        // 비로그인 → 빈 결과
        if (userDetails == null) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        JPAQuery<Tuple> query = buildSoloBaseQuery(userDetails, filterType, keyword);
        applySorting(query, sortType);

        List<Tuple> tuples = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = buildCountSoloQuery(filterType, keyword).fetchOne();

        return new PageImpl<>(
                enrichDetails(tuples),
                pageable,
                total == null ? 0 : total
        );
    }

    private JPAQuery<Tuple> buildSoloBaseQuery(
            UserDetails userDetails,
            ChallengeDto.ChallengeFilterType filterType,
            String keyword
    ) {
        JPAQuery<Tuple> query = queryFactory
                .select(
                        challenge,
                        userDetails != null
                                ? JPAExpressions.selectOne()
                                .from(challengeLiked)
                                .where(
                                        challengeLiked.challenge.eq(challenge)
                                                .and(challengeLiked.user.id.eq(userDetails.getUser().getId()))
                                ).exists()
                                : Expressions.FALSE,
                        JPAExpressions.select(challengeLiked.count())
                                .from(challengeLiked)
                                .where(challengeLiked.challenge.eq(challenge))
                )
                .from(challenge)
                .where(
                        challenge.mode.eq(ChallengeMode.SOLO),
                        soloStatusCondition(filterType)
                )
                .leftJoin(challenge.host).fetchJoin()
                .leftJoin(challenge.host.tier).fetchJoin()
                .leftJoin(challenge.host.profileImage).fetchJoin();

        BooleanExpression keywordPredicate = keywordCondition(keyword);

        if (keywordPredicate != null) {
            query.where(keywordPredicate);
        }

        return query;
    }

    private JPAQuery<Long> buildCountSoloQuery(
            ChallengeDto.ChallengeFilterType filterType,
            String keyword
    ) {
        JPAQuery<Long> query = queryFactory
                .select(challenge.count())
                .from(challenge)
                .where(
                        challenge.mode.eq(ChallengeMode.SOLO),
                        soloStatusCondition(filterType)
                );

        BooleanExpression keywordPredicate = keywordCondition(keyword);

        if (keywordPredicate != null) {
            query.where(keywordPredicate);
        }

        return query;
    }

    private BooleanExpression soloStatusCondition(ChallengeDto.ChallengeFilterType filterType) {
        return switch (filterType) {
            case ChallengeDto.ChallengeFilterType.IN_PROGRESS ->
                    challenge.status.eq(ChallengeStatus.IN_PROGRESS);

            case ChallengeDto.ChallengeFilterType.COMPLETED ->
                    challenge.status.eq(ChallengeStatus.COMPLETED);

            default -> null;
        };
    }

    /**
     * 3. 전체(VS+SOLO) 챌린지용 Query
     */
    public Page<ChallengeDto.ChallengeListResponse> paginateAllByType (
            UserDetails userDetails,
            Pageable pageable,
            ChallengeDto.ChallengeSortType sortType
    ) {
        return paginateAllByTypeAndKeyword(userDetails, pageable, sortType, null);
    }

    public Page<ChallengeDto.ChallengeListResponse> paginateAllByTypeAndKeyword (
            UserDetails userDetails,
            Pageable pageable,
            ChallengeDto.ChallengeSortType sortType,
            String keyword
    ) {
        // 비로그인 → 빈 결과
        if (userDetails == null) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        JPAQuery<Tuple> query = buildAllBaseQuery(userDetails, keyword);
        applySorting(query, sortType);

        List<Tuple> tuples = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = buildCountAllQuery(userDetails, keyword).fetchOne();

        return new PageImpl<>(
                enrichDetails(tuples),
                pageable,
                total == null ? 0 : total
        );
    }

    private JPAQuery<Tuple> buildAllBaseQuery(
            UserDetails userDetails,
            String keyword
    ) {
        JPAQuery<Tuple> query = queryFactory
                .select(
                        challenge,
                        userDetails != null
                                ? JPAExpressions.selectOne()
                                .from(challengeLiked)
                                .where(
                                        challengeLiked.challenge.eq(challenge)
                                                .and(challengeLiked.user.id.eq(userDetails.getUser().getId()))
                                ).exists()
                                : Expressions.FALSE,
                        JPAExpressions.select(challengeLiked.count())
                                .from(challengeLiked)
                                .where(challengeLiked.challenge.eq(challenge))
                )
                .from(challenge)
                .join(challengeMember)
                .on(
                        challengeMember.id.challenge.eq(challenge),
                        challengeMember.id.user.id.eq(userDetails.getUser().getId())
                )
                .leftJoin(challenge.host).fetchJoin()
                .leftJoin(challenge.host.tier).fetchJoin()
                .leftJoin(challenge.host.profileImage).fetchJoin();

        BooleanExpression keywordPredicate = keywordCondition(keyword);

        if (keywordPredicate != null) {
            query.where(keywordPredicate);
        }

        return query;
    }

    private JPAQuery<Long> buildCountAllQuery(
            UserDetails userDetails,
            String keyword
    ) {
        JPAQuery<Long> query = queryFactory
                .select(challenge.count())
                .from(challenge)
                .join(challengeMember)
                .on(
                        challengeMember.id.challenge.eq(challenge),
                        challengeMember.id.user.id.eq(userDetails.getUser().getId())
                );

        BooleanExpression keywordPredicate = keywordCondition(keyword);

        if (keywordPredicate != null) {
            query.where(keywordPredicate);
        }

        return query;
    }

    private void applySorting(JPAQuery<?> query, ChallengeDto.ChallengeSortType sortType) {
        switch (sortType) {
            case LATEST ->
                    query.orderBy(challenge.createdAt.desc());

            case OLDEST ->
                    query.orderBy(challenge.createdAt.asc());

            case LIKED ->
                    query.orderBy(challenge.likeCount.desc());

            case TITLE ->
                    query.orderBy(
                            challenge.name.asc(),
                            challenge.createdAt.desc()
                    );
        }
    }

    // 제목 + 분야 + 태그 검색 조건설정
    private BooleanExpression keywordCondition(String keyword) {
        if(keyword == null || keyword.isBlank()) return null;

        String k = keyword.trim();

        BooleanExpression inTitle = challenge.name.containsIgnoreCase(k);

        // 분야 검색
        var cField = new QChallenge("cField");
        QField f = new QField("f");

        BooleanExpression inField = JPAExpressions
                .selectOne()
                .from(cField)
                .join(cField.fields, f)
                .where(cField.id.eq(challenge.id).and(f.name.containsIgnoreCase(k)))
                .exists();

        // 태그 검색
        var cTag = new QChallenge("cTag");
        QTag t = new QTag("t");

        BooleanExpression inTag = JPAExpressions
                .selectOne()
                .from(cTag)
                .join(cTag.tags, t)
                .where(cTag.id.eq(challenge.id).and(t.name.containsIgnoreCase(k)))
                .exists();

        return inTitle.or(inField).or(inTag);
    }

    private List<ChallengeDto.ChallengeListResponse> enrichDetails(List<Tuple> tuples) {
        if (tuples.isEmpty()) return List.of();

        List<Long> ids = tuples.stream()
                .map(t -> t.get(0, Challenge.class).getId())
                .toList();

        Map<Long, List<String>> fieldMap = queryFactory
                .select(challenge.id, field.name)
                .from(challenge)
                .join(challenge.fields, field)
                .where(challenge.id.in(ids))
                .fetch()
                .stream()
                .collect(Collectors.groupingBy(
                        t -> t.get(challenge.id),
                        Collectors.mapping(t -> t.get(field.name), Collectors.toList())
                ));

        Map<Long, List<String>> tagMap = queryFactory
                .select(challenge.id, tag.name)
                .from(challenge)
                .join(challenge.tags, tag)
                .where(challenge.id.in(ids))
                .fetch()
                .stream()
                .collect(Collectors.groupingBy(
                        t -> t.get(challenge.id),
                        Collectors.mapping(t -> t.get(tag.name), Collectors.toList())
                ));

        return tuples.stream()
                .map(t -> mapToDto(t, fieldMap, tagMap))
                .toList();
    }

    private ChallengeDto.ChallengeListResponse mapToDto(
            Tuple tuple,
            Map<Long, List<String>> fieldMap,
            Map<Long, List<String>> tagMap
    ) {
        Challenge c = tuple.get(0, Challenge.class);
        User host = c.getHost();
        ProfileImage profileImage = host.getProfileImage();
        ChallengeImage challengeImage = c.getChallengeImage();

        return ChallengeDto.ChallengeListResponse.builder()
                .challengeId(c.getId())
                .mode(c.getMode())
                .thumbnail(challengeImage != null
                        ? FileDto.FileResponse.from(challengeImage)
                        : null)
                .user(ChallengeDto.UserResponse.builder()
                        .userId(host.getId())
                        .nickname(host.getNickname())
                        .tier(TierDto.TierResponse.from(host.getTier()))
                        .profileImage(profileImage != null
                                ? FileDto.FileResponse.from(profileImage)
                                : null)
                        .build())
                .startedAt(c.getStartedAt())
                .durationWeek(c.getDurationWeek())
                .name(c.getName())
                .fields(fieldMap.getOrDefault(c.getId(), List.of()))
                .tags(tagMap.getOrDefault(c.getId(), List.of()))
                .job(c.getJob())
                .liked(Boolean.TRUE.equals(tuple.get(1, Boolean.class)))
                .likeCount(tuple.get(2, Long.class) == null ? 0 : tuple.get(2, Long.class).intValue())
                .status(c.getStatus())
                .build();
    }

    private PageImpl<ChallengeDto.ChallengeListResponse> slice(
            List<ChallengeDto.ChallengeListResponse> list,
            Pageable pageable
    ) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), list.size());

        return new PageImpl<>(
                start >= list.size() ? List.of() : list.subList(start, end),
                pageable,
                list.size()
        );
    }

    private LocalDate endDate(ChallengeDto.ChallengeListResponse dto) {
        return dto.getStartedAt().plusWeeks(dto.getDurationWeek());
    }

    public List<Challenge> findSimpleMyChallenges(Long userId, ChallengeMode mode) {
        return queryFactory
                .selectFrom(challenge)
                .join(challengeMember)
                .on(challengeMember.id.challenge.eq(challenge))
                .where(
                        challengeMember.id.user.id.eq(userId), // 내가 참여한
                        challenge.mode.eq(mode)                // 모드 일치 (VS/SOLO)
                )
                .orderBy(challenge.startedAt.desc())       // 최신순 정렬
                .fetch();                                  // List로 반환
    }

    /**
     * 4. 전체(공개 + 내가 참여한 비공개) 검색용 Query
     */
    public Page<ChallengeDto.ChallengeListResponse> paginateSearchAll(
            UserDetails userDetails,
            Pageable pageable,
            ChallengeDto.ChallengeSortType sortType
    ) {
        return paginateSearchAllByKeyword(userDetails, pageable, sortType, null);
    }

    public Page<ChallengeDto.ChallengeListResponse> paginateSearchAllByKeyword(
            UserDetails userDetails,
            Pageable pageable,
            ChallengeDto.ChallengeSortType sortType,
            String keyword
    ) {
        JPAQuery<Tuple> query = buildPublicAllBaseQuery(userDetails, keyword);
        applySorting(query, sortType);

        List<Tuple> tuples = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = buildCountPublicAllQuery(userDetails, keyword).fetchOne();

        return new PageImpl<>(
                enrichDetails(tuples),
                pageable,
                total == null ? 0 : total
        );
    }

    private JPAQuery<Tuple> buildPublicAllBaseQuery(
            UserDetails userDetails,
            String keyword
    ) {
        JPAQuery<Tuple> query = queryFactory
                .select(
                        challenge,
                        userDetails != null
                                ? JPAExpressions.selectOne()
                                .from(challengeLiked)
                                .where(
                                        challengeLiked.challenge.eq(challenge)
                                                .and(challengeLiked.user.id.eq(userDetails.getUser().getId()))
                                ).exists()
                                : Expressions.FALSE,
                        JPAExpressions.select(challengeLiked.count())
                                .from(challengeLiked)
                                .where(challengeLiked.challenge.eq(challenge))
                )
                .from(challenge)
                .leftJoin(challenge.host).fetchJoin()
                .leftJoin(challenge.host.tier).fetchJoin()
                .leftJoin(challenge.host.profileImage).fetchJoin()
                .where(visibleToUser(userDetails));

        BooleanExpression keywordPredicate = keywordCondition(keyword);
        if (keywordPredicate != null) {
            query.where(keywordPredicate);
        }

        return query;
    }

    private JPAQuery<Long> buildCountPublicAllQuery(
            UserDetails userDetails,
            String keyword
    ) {
        JPAQuery<Long> query = queryFactory
                .select(challenge.count())
                .from(challenge)
                .where(visibleToUser(userDetails));

        BooleanExpression keywordPredicate = keywordCondition(keyword);
        if (keywordPredicate != null) {
            query.where(keywordPredicate);
        }

        return query;
    }

    private BooleanExpression visibleToUser(UserDetails userDetails) {
        if (userDetails == null) {
            return challenge.visibility.eq(ChallengeVisibility.PUBLIC);
        }

        BooleanExpression isPublic = challenge.visibility.eq(ChallengeVisibility.PUBLIC);

        BooleanExpression isMyPrivate = challenge.visibility.eq(ChallengeVisibility.PRIVATE)
                .and(
                        JPAExpressions.selectOne()
                                .from(challengeMember)
                                .where(
                                        challengeMember.id.challenge.eq(challenge),
                                        challengeMember.id.user.id.eq(userDetails.getUser().getId())
                                )
                                .exists()
                );

        return isPublic.or(isMyPrivate);
    }

    /**
     * 5. 내가 좋아요 누른 챌린지 목록 조회용 Query
     */
    public Page<ChallengeDto.ChallengeListResponse> paginateLiked(
            UserDetails userDetails,
            Pageable pageable,
            ChallengeDto.ChallengeSortType sortType
    ) {
        return paginateLikedByKeyword(userDetails, pageable, sortType, null);
    }

    public Page<ChallengeDto.ChallengeListResponse> paginateLikedByKeyword(
            UserDetails userDetails,
            Pageable pageable,
            ChallengeDto.ChallengeSortType sortType,
            String keyword
    ) {
        // liked는 로그인 필수
        if (userDetails == null) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        JPAQuery<Tuple> query = buildLikedBaseQuery(userDetails, keyword);
        applySorting(query, sortType);

        List<Tuple> tuples = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = buildCountLikedQuery(userDetails, keyword).fetchOne();

        return new PageImpl<>(
                enrichDetails(tuples),
                pageable,
                total == null ? 0 : total
        );
    }

    private JPAQuery<Tuple> buildLikedBaseQuery(
            UserDetails userDetails,
            String keyword
    ) {
        JPAQuery<Tuple> query = queryFactory
                .select(
                        challenge,
                        Expressions.TRUE,
                        JPAExpressions.select(challengeLiked.count())
                                .from(challengeLiked)
                                .where(challengeLiked.challenge.eq(challenge))
                )
                .from(challengeLiked)
                .join(challengeLiked.challenge, challenge)
                .where(challengeLiked.user.id.eq(userDetails.getUser().getId()))
                .leftJoin(challenge.host).fetchJoin()
                .leftJoin(challenge.host.tier).fetchJoin()
                .leftJoin(challenge.host.profileImage).fetchJoin();

        BooleanExpression keywordPredicate = keywordCondition(keyword);
        if (keywordPredicate != null) {
            query.where(keywordPredicate);
        }

        return query;
    }

    private JPAQuery<Long> buildCountLikedQuery(
            UserDetails userDetails,
            String keyword
    ) {
        JPAQuery<Long> query = queryFactory
                .select(challenge.count())
                .from(challengeLiked)
                .join(challengeLiked.challenge, challenge)
                .where(challengeLiked.user.id.eq(userDetails.getUser().getId()));

        BooleanExpression keywordPredicate = keywordCondition(keyword);
        if (keywordPredicate != null) {
            query.where(keywordPredicate);
        }

        return query;
    }
}