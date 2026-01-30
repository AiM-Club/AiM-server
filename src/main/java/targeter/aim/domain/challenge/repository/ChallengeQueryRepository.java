package targeter.aim.domain.challenge.repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
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
import targeter.aim.domain.file.dto.FileDto;
import targeter.aim.domain.file.entity.ChallengeImage;
import targeter.aim.domain.file.entity.ProfileImage;
import targeter.aim.domain.label.entity.QField;
import targeter.aim.domain.label.entity.QTag;
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
import static targeter.aim.domain.challenge.repository.MyChallengeSortType.CREATED_AT;
import static targeter.aim.domain.challenge.repository.MyChallengeSortType.TITLE;
import static targeter.aim.domain.label.entity.QField.field;
import static targeter.aim.domain.label.entity.QTag.tag;

@Repository
@RequiredArgsConstructor
public class ChallengeQueryRepository {

    private final JPAQueryFactory queryFactory;

    // 기존 전체 조회용
    public Page<ChallengeDto.ChallengeListResponse> paginateByType(
            UserDetails userDetails,
            Pageable pageable,
            ChallengeFilterType filterType,
            ChallengeSortType sortType
    ) {
        JPAQuery<Tuple> query = buildBaseQuery(userDetails, filterType, null, null);
        applySorting(query, sortType);

        List<Tuple> tuples = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = buildCountQuery(userDetails, filterType, null, null).fetchOne();

        return new PageImpl<>(
                enrichDetails(tuples),
                pageable,
                total == null ? 0 : total
        );
    }

    // 분야 필터링
    public Page<ChallengeDto.ChallengeListResponse> paginateByTypeAndKeywordAndField(
            UserDetails userDetails,
            Pageable pageable,
            ChallengeFilterType filterType,
            ChallengeSortType sortType,
            String keyword,
            String field
    ) {
        // MY + 비로그인 → 빈 결과
        if (filterType == ChallengeFilterType.MY && userDetails == null) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        // ONGOING / FINISHED → 메모리 정렬
        if (sortType == ChallengeSortType.ONGOING || sortType == ChallengeSortType.FINISHED) {
            List<Tuple> tuples = buildBaseQuery(userDetails, filterType, keyword, field).fetch();
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
                    sortType == ChallengeSortType.ONGOING
                            ? Stream.concat(inProgress.stream(), completed.stream()).toList()
                            : Stream.concat(completed.stream(), inProgress.stream()).toList();

            return slice(sorted, pageable);
        }

        // 나머지 → QueryDSL
        JPAQuery<Tuple> query = buildBaseQuery(userDetails, filterType, keyword, field);
        applySorting(query, sortType);

        List<Tuple> tuples = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = buildCountQuery(userDetails, filterType, keyword, field).fetchOne();

        return new PageImpl<>(
                enrichDetails(tuples),
                pageable,
                total == null ? 0 : total
        );
    }

    // 검색 전용(위 메서드에서 field만 null)
    public Page<ChallengeDto.ChallengeListResponse> paginateByTypeAndKeyword(
            UserDetails userDetails,
            Pageable pageable,
            ChallengeFilterType filterType,
            ChallengeSortType sortType,
            String keyword
    ) {
        return paginateByTypeAndKeywordAndField(userDetails, pageable, filterType, sortType, keyword, null);
    }

    // 공통 Query 구성
    private JPAQuery<Tuple> buildBaseQuery(
            UserDetails userDetails,
            ChallengeFilterType filterType,
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

        if (filterType == ChallengeFilterType.MY && userDetails != null) {
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

    private JPAQuery<Long> buildCountQuery(
            UserDetails userDetails,
            ChallengeFilterType filterType,
            String keyword,
            String field
    ) {
        JPAQuery<Long> query = queryFactory
                .select(challenge.count())
                .from(challenge)
                .where(challenge.mode.eq(ChallengeMode.VS));

        if (filterType == ChallengeFilterType.MY && userDetails != null) {
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

    // 정렬
    private void applySorting(JPAQuery<?> query, ChallengeSortType sortType) {
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
                // ONGOING / FINISHED → 여기서 처리 안 함
            }
        }
    }

    //DTO 매핑
    private List<ChallengeDto.ChallengeListResponse> enrichDetails(List<Tuple> tuples) {
        if (tuples.isEmpty()) return List.of();

        List<Long> ids = tuples.stream()
                .map(t -> t.get(0, Challenge.class).getId())
                .toList();

        Map<Long, List<String>> fieldMap = fetchFields(ids);
        Map<Long, List<String>> tagMap = fetchTags(ids);

        return tuples.stream()
                .map(t -> mapToDto(t, fieldMap, tagMap))
                .toList();
    }

    private Map<Long, List<String>> fetchFields(List<Long> ids) {
        return queryFactory
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
    }

    private Map<Long, List<String>> fetchTags(List<Long> ids) {
        return queryFactory
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
                .thumbnail(challengeImage != null
                        ? FileDto.FileResponse.from(challengeImage)
                        : null)
                .user(ChallengeDto.UserResponse.builder()
                        .userId(host.getId())
                        .nickname(host.getNickname())
                        .badge(host.getTier().getName())
                        .profileImage(profileImage != null
                                ? FileDto.FileResponse.from(profileImage)
                                : null)
                        .build())
                .startDate(c.getStartedAt())
                .duration(c.getDurationWeek() + "주")
                .name(c.getName())
                .fields(fieldMap.getOrDefault(c.getId(), List.of()))
                .tags(tagMap.getOrDefault(c.getId(), List.of()))
                .job(c.getJob())
                .liked(Boolean.TRUE.equals(tuple.get(1, Boolean.class)))
                .likeCount(tuple.get(2, Long.class) == null ? 0 : tuple.get(2, Long.class).intValue())
                .createdAt(c.getCreatedAt())
                .lastModifiedAt(c.getLastModifiedAt())
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
        return dto.getStartDate()
                .plusWeeks(Integer.parseInt(dto.getDuration().replace("주", "")));
    }

    private BooleanExpression soloStatusCondition(String filterType) {

        return switch (filterType) {
            case "IN_PROGRESS" ->
                    challenge.status.eq(ChallengeStatus.IN_PROGRESS);

            case "COMPLETE" ->
                    challenge.status.eq(ChallengeStatus.COMPLETED);

            default -> null;
        };
    }

    private void applySoloSorting(JPAQuery<?> query, String sort) {
        switch (sort) {
            case "LATEST" ->
                    query.orderBy(challenge.createdAt.desc());

            case "OLDEST" ->
                    query.orderBy(challenge.createdAt.asc());

            case "TITLE" ->
                    query.orderBy(
                            challenge.name.asc(),
                            challenge.createdAt.desc()
                    );

            default ->
                    query.orderBy(challenge.createdAt.desc());
        }
    }

    public Page<ChallengeDto.ChallengeListResponse> paginateSoloChallenges(
            ChallengeDto.SoloChallengeListRequest request,
            String keyword,
            Pageable pageable
    ) {
        JPAQuery<Tuple> query = queryFactory
                .select(
                        challenge,
                        Expressions.FALSE,
                        JPAExpressions.select(challengeLiked.count())
                                .from(challengeLiked)
                                .where(challengeLiked.challenge.eq(challenge))
                )
                .from(challenge)
                .where(
                        challenge.mode.eq(ChallengeMode.SOLO),
                        soloStatusCondition(request.getFilterType())
                )
                .leftJoin(challenge.host)
                .leftJoin(challenge.host.tier)
                .leftJoin(challenge.host.profileImage);

        if (keyword != null) {
            query.where(keywordCondition(keyword));
        }

        applySoloSorting(query, request.getSort());

        List<Tuple> tuples = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(challenge.count())
                .from(challenge)
                .where(
                        challenge.mode.eq(ChallengeMode.SOLO),
                        soloStatusCondition(request.getFilterType())
                );

        if (keyword != null) {
            countQuery.where(keywordCondition(keyword));
        }

        Long total = countQuery.fetchOne();

        return new PageImpl<>(
                enrichDetails(tuples),
                pageable,
                total == null ? 0 : total
        );
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


    public Page<ChallengeDto.ChallengeListResponse> paginateMyAllChallenges(
            Long userId,
            Pageable pageable,
            MyChallengeSortType sortType,
            SortOrder sortOrder
    ) {
        JPAQuery<Tuple> query = queryFactory
                .select(
                        challenge,
                        JPAExpressions.selectOne()
                                .from(challengeLiked)
                                .where(
                                        challengeLiked.challenge.eq(challenge)
                                                .and(challengeLiked.user.id.eq(userId))
                                ).exists(),
                        JPAExpressions.select(challengeLiked.count())
                                .from(challengeLiked)
                                .where(challengeLiked.challenge.eq(challenge))
                )
                .from(challenge)
                .join(challengeMember)
                .on(
                        challengeMember.id.challenge.eq(challenge),
                        challengeMember.id.user.id.eq(userId)
                )
                .leftJoin(challenge.host).fetchJoin()
                .leftJoin(challenge.host.tier).fetchJoin()
                .leftJoin(challenge.host.profileImage).fetchJoin();

        applyMyAllSorting(query, sortType, sortOrder);

        List<Tuple> tuples = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(challenge.count())
                .from(challenge)
                .join(challengeMember)
                .on(
                        challengeMember.id.challenge.eq(challenge),
                        challengeMember.id.user.id.eq(userId)
                )
                .fetchOne();

        return new PageImpl<>(
                enrichDetails(tuples),
                pageable,
                total == null ? 0 : total
        );
    }

    private void applyMyAllSorting(
            JPAQuery<?> query,
            MyChallengeSortType sortType,
            SortOrder order
    ) {
        boolean isAsc = order == SortOrder.ASC;

        switch (sortType) {

            case END_DATE ->
                    query.orderBy(
                        isAsc ? challenge.startedAt.asc()
                                : challenge.startedAt.desc()
                );

            case TITLE ->
                    query.orderBy(
                            isAsc ? challenge.name.asc()
                                    : challenge.name.desc(),
                            challenge.createdAt.desc()
                    );

            case CREATED_AT ->
                    query.orderBy(
                            isAsc ? challenge.createdAt.asc()
                                    : challenge.createdAt.desc()
                    );

        }
    }

}