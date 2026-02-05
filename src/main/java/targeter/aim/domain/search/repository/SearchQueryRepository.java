package targeter.aim.domain.search.repository;

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
import org.springframework.util.StringUtils;
import targeter.aim.domain.challenge.entity.Challenge;
import targeter.aim.domain.challenge.entity.ChallengeVisibility;
import targeter.aim.domain.challenge.entity.QChallenge;
import targeter.aim.domain.file.dto.FileDto;
import targeter.aim.domain.label.entity.QField;
import targeter.aim.domain.label.entity.QTag;
import targeter.aim.domain.post.entity.Post;
import targeter.aim.domain.post.entity.PostType;
import targeter.aim.domain.post.entity.QPost;
import targeter.aim.domain.search.dto.SearchDto;
import targeter.aim.domain.user.entity.User;
import targeter.aim.system.security.model.UserDetails;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static targeter.aim.domain.challenge.entity.QChallenge.challenge;
import static targeter.aim.domain.challenge.entity.QChallengeLiked.challengeLiked;
import static targeter.aim.domain.challenge.entity.QChallengeMember.challengeMember;
import static targeter.aim.domain.label.entity.QField.field;
import static targeter.aim.domain.label.entity.QTag.tag;
import static targeter.aim.domain.post.entity.QPost.post;
import static targeter.aim.domain.post.entity.QPostLiked.postLiked;

@Repository
@RequiredArgsConstructor
public class SearchQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Page<SearchDto.SearchListResponse> paginateSearchList(
            UserDetails userDetails,
            SearchDto.ListSearchCondition condition,
            Pageable pageable
    ) {
        String keyword = condition.getKeyword();
        SearchDto.SortType sortType = condition.getSort();

        // 1. 메모리 병합을 위한 조회 개수 계산 (Offset + Limit)
        long neededSize = pageable.getOffset() + pageable.getPageSize();

        // 2. Challenge 조회
        JPAQuery<Tuple> challengeQuery = buildChallengeBaseQuery(userDetails, keyword);
        applyChallengeSorting(challengeQuery, sortType);
        List<Tuple> challengeTuples = challengeQuery.limit(neededSize).fetch();

        // 3. Post 조회
        JPAQuery<Tuple> postQuery = buildPostBaseQuery(userDetails, keyword);
        applyPostSorting(postQuery, sortType);
        List<Tuple> postTuples = postQuery.limit(neededSize).fetch();

        // 4. Enrich (N+1 해결) 및 DTO 변환
        List<SearchDto.SearchListResponse> challenges = enrichChallengeDetails(challengeTuples);
        List<SearchDto.SearchListResponse> posts = enrichPostDetails(postTuples);

        // 5. 리스트 병합 및 최종 정렬 (Java Level)
        List<SearchDto.SearchListResponse> mergedList = new ArrayList<>();
        mergedList.addAll(challenges);
        mergedList.addAll(posts);

        mergedList.sort(getIntegratedComparator(sortType));

        // 6. 페이징 처리 (SubList)
        return paginateMergedList(mergedList, pageable, userDetails, keyword);
    }

    // =================================================================================
    //  Query Builders
    // =================================================================================

    private JPAQuery<Tuple> buildChallengeBaseQuery(UserDetails userDetails, String keyword) {
        return queryFactory
                .select(
                        challenge,
                        userDetails != null
                                ? JPAExpressions.selectOne()
                                .from(challengeLiked)
                                .where(challengeLiked.challenge.eq(challenge)
                                        .and(challengeLiked.user.id.eq(userDetails.getUser().getId())))
                                .exists()
                                : Expressions.FALSE,
                        JPAExpressions.select(challengeLiked.count())
                                .from(challengeLiked)
                                .where(challengeLiked.challenge.eq(challenge))
                )
                .from(challenge)
                .leftJoin(challenge.host).fetchJoin()
                .leftJoin(challenge.host.tier).fetchJoin()
                .leftJoin(challenge.host.profileImage).fetchJoin()
                .where(
                        visibleToUser(userDetails),
                        challengeKeywordCondition(keyword)
                );
    }

    private JPAQuery<Tuple> buildPostBaseQuery(UserDetails userDetails, String keyword) {
        return queryFactory
                .select(
                        post,
                        userDetails != null
                                ? JPAExpressions.selectOne()
                                .from(postLiked)
                                .where(postLiked.post.eq(post)
                                        .and(postLiked.user.id.eq(userDetails.getUser().getId())))
                                .exists()
                                : Expressions.FALSE,
                        JPAExpressions.select(postLiked.count())
                                .from(postLiked)
                                .where(postLiked.post.eq(post))
                )
                .from(post)
                .leftJoin(post.user).fetchJoin()
                .leftJoin(post.user.tier).fetchJoin()
                .leftJoin(post.user.profileImage).fetchJoin()
                .leftJoin(post.challenge).fetchJoin()
                .where(
                        post.type.in(PostType.VS_RECRUIT, PostType.Q_AND_A, PostType.REVIEW),
                        postKeywordCondition(keyword)
                );
    }

    // =================================================================================
    //  Sorting Logic
    // =================================================================================

    private void applyChallengeSorting(JPAQuery<?> query, SearchDto.SortType sortType) {
        switch (sortType) {
            case LATEST -> query.orderBy(challenge.createdAt.desc());
            case OLDEST -> query.orderBy(challenge.createdAt.asc());
            case LIKED -> query.orderBy(challenge.likeCount.desc());
            case TITLE -> query.orderBy(challenge.name.asc(), challenge.createdAt.desc());
        }
    }

    private void applyPostSorting(JPAQuery<?> query, SearchDto.SortType sortType) {
        switch (sortType) {
            case LATEST -> query.orderBy(post.createdAt.desc());
            case OLDEST -> query.orderBy(post.createdAt.asc());
            case LIKED -> query.orderBy(post.likeCount.desc());
            case TITLE -> query.orderBy(post.title.asc(), post.createdAt.desc());
        }
    }

    private Comparator<SearchDto.SearchListResponse> getIntegratedComparator(SearchDto.SortType sortType) {
        return switch (sortType) {
            case LATEST -> Comparator.comparing(SearchDto.SearchListResponse::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
            case OLDEST -> Comparator.comparing(SearchDto.SearchListResponse::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case LIKED -> Comparator.comparing(SearchDto.SearchListResponse::getLikeCount, Comparator.reverseOrder())
                    .thenComparing(SearchDto.SearchListResponse::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
            case TITLE -> Comparator.comparing(SearchDto.SearchListResponse::getName);
        };
    }

    // =================================================================================
    //  Conditions & Filters
    // =================================================================================

    private BooleanExpression visibleToUser(UserDetails userDetails) {
        BooleanExpression isPublic = challenge.visibility.eq(ChallengeVisibility.PUBLIC);
        if (userDetails == null) return isPublic;

        BooleanExpression isMyPrivate = challenge.visibility.eq(ChallengeVisibility.PRIVATE)
                .and(JPAExpressions.selectOne()
                        .from(challengeMember)
                        .where(challengeMember.id.challenge.eq(challenge),
                                challengeMember.id.user.id.eq(userDetails.getUser().getId()))
                        .exists());
        return isPublic.or(isMyPrivate);
    }

    private BooleanExpression challengeKeywordCondition(String keyword) {
        if (!StringUtils.hasText(keyword)) return null;
        String k = keyword.trim();
        var cField = new QChallenge("cFieldSearch");
        QField f = new QField("fSearch");
        var cTag = new QChallenge("cTagSearch");
        QTag t = new QTag("tSearch");

        return challenge.name.containsIgnoreCase(k)
                .or(JPAExpressions.selectOne().from(cField).join(cField.fields, f)
                        .where(cField.id.eq(challenge.id).and(f.name.containsIgnoreCase(k))).exists())
                .or(JPAExpressions.selectOne().from(cTag).join(cTag.tags, t)
                        .where(cTag.id.eq(challenge.id).and(t.name.containsIgnoreCase(k))).exists());
    }

    private BooleanExpression postKeywordCondition(String keyword) {
        if (!StringUtils.hasText(keyword)) return null;
        String k = keyword.trim();
        var pField = new QPost("pFieldSearch");
        QField f = new QField("fPostSearch");
        var pTag = new QPost("pTagSearch");
        QTag t = new QTag("tPostSearch");

        return post.title.containsIgnoreCase(k)
                .or(JPAExpressions.selectOne().from(pField).join(pField.fields, f)
                        .where(pField.id.eq(post.id).and(f.name.containsIgnoreCase(k))).exists())
                .or(JPAExpressions.selectOne().from(pTag).join(pTag.tags, t)
                        .where(pTag.id.eq(post.id).and(t.name.containsIgnoreCase(k))).exists());
    }

    // =================================================================================
    //  Enrichment (N+1 Optimization) & DTO Mapping
    // =================================================================================

    private List<SearchDto.SearchListResponse> enrichChallengeDetails(List<Tuple> tuples) {
        if (tuples.isEmpty()) return new ArrayList<>();
        List<Long> ids = tuples.stream().map(t -> t.get(0, Challenge.class).getId()).toList();
        Map<Long, List<String>> fields = fetchChallengeFields(ids);
        Map<Long, List<String>> tags = fetchChallengeTags(ids);

        return tuples.stream().map(t -> mapToChallengeDto(t, fields, tags)).collect(Collectors.toList());
    }

    private List<SearchDto.SearchListResponse> enrichPostDetails(List<Tuple> tuples) {
        if (tuples.isEmpty()) return new ArrayList<>();
        List<Long> ids = tuples.stream().map(t -> t.get(0, Post.class).getId()).toList();
        Map<Long, List<String>> fields = fetchPostFields(ids);
        Map<Long, List<String>> tags = fetchPostTags(ids);

        return tuples.stream().map(t -> mapToPostDto(t, fields, tags)).collect(Collectors.toList());
    }

    private SearchDto.SearchListResponse mapToChallengeDto(Tuple tuple, Map<Long, List<String>> fieldMap, Map<Long, List<String>> tagMap) {
        Challenge c = tuple.get(0, Challenge.class);
        User host = c.getHost();
        return SearchDto.SearchListResponse.builder()
                .type(SearchDto.Type.CHALLENGE)
                .id(c.getId())
                .postType(null)
                .challengeMode(c.getMode())
                .thumbnail(c.getChallengeImage() != null ? FileDto.FileResponse.from(c.getChallengeImage()) : null)
                .user(SearchDto.UserResponse.from(host))
                .startedAt(c.getStartedAt())
                .durationWeek(c.getDurationWeek())
                .name(c.getName())
                .fields(fieldMap.getOrDefault(c.getId(), List.of()))
                .tags(tagMap.getOrDefault(c.getId(), List.of()))
                .job(c.getJob())
                .isLiked(Boolean.TRUE.equals(tuple.get(1, Boolean.class)))
                .likeCount(tuple.get(2, Long.class) == null ? 0 : tuple.get(2, Long.class).intValue())
                .createdAt(c.getCreatedAt()) // 정렬용
                .build();
    }

    private SearchDto.SearchListResponse mapToPostDto(Tuple tuple, Map<Long, List<String>> fieldMap, Map<Long, List<String>> tagMap) {
        Post p = tuple.get(0, Post.class);
        User user = p.getUser();
        Challenge linkedChallenge = p.getChallenge();
        return SearchDto.SearchListResponse.builder()
                .type(SearchDto.Type.POST)
                .id(p.getId())
                .postType(p.getType())
                .challengeMode(linkedChallenge != null ? linkedChallenge.getMode() : null)
                .thumbnail(p.getPostImage() != null ? FileDto.FileResponse.from(p.getPostImage()) : null)
                .user(SearchDto.UserResponse.from(user))
                .startedAt(linkedChallenge != null ? linkedChallenge.getStartedAt() : null)
                .durationWeek(linkedChallenge != null ? linkedChallenge.getDurationWeek() : null)
                .name(p.getTitle())
                .fields(fieldMap.getOrDefault(p.getId(), List.of()))
                .tags(tagMap.getOrDefault(p.getId(), List.of()))
                .job(p.getJob())
                .isLiked(Boolean.TRUE.equals(tuple.get(1, Boolean.class)))
                .likeCount(tuple.get(2, Long.class) == null ? 0 : tuple.get(2, Long.class).intValue())
                .createdAt(p.getCreatedAt()) // 정렬용
                .build();
    }

    // --- Fetch Helpers ---
    private Map<Long, List<String>> fetchChallengeFields(List<Long> ids) {
        return queryFactory.select(challenge.id, field.name).from(challenge).join(challenge.fields, field)
                .where(challenge.id.in(ids)).fetch().stream()
                .collect(Collectors.groupingBy(t -> t.get(challenge.id), Collectors.mapping(t -> t.get(field.name), Collectors.toList())));
    }
    private Map<Long, List<String>> fetchChallengeTags(List<Long> ids) {
        return queryFactory.select(challenge.id, tag.name).from(challenge).join(challenge.tags, tag)
                .where(challenge.id.in(ids)).fetch().stream()
                .collect(Collectors.groupingBy(t -> t.get(challenge.id), Collectors.mapping(t -> t.get(tag.name), Collectors.toList())));
    }
    private Map<Long, List<String>> fetchPostFields(List<Long> ids) {
        return queryFactory.select(post.id, field.name).from(post).join(post.fields, field)
                .where(post.id.in(ids)).fetch().stream()
                .collect(Collectors.groupingBy(t -> t.get(post.id), Collectors.mapping(t -> t.get(field.name), Collectors.toList())));
    }
    private Map<Long, List<String>> fetchPostTags(List<Long> ids) {
        return queryFactory.select(post.id, tag.name).from(post).join(post.tags, tag)
                .where(post.id.in(ids)).fetch().stream()
                .collect(Collectors.groupingBy(t -> t.get(post.id), Collectors.mapping(t -> t.get(tag.name), Collectors.toList())));
    }

    // =================================================================================
    //  Count & Pagination
    // =================================================================================

    private Page<SearchDto.SearchListResponse> paginateMergedList(
            List<SearchDto.SearchListResponse> mergedList,
            Pageable pageable,
            UserDetails userDetails,
            String keyword
    ) {
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), mergedList.size());

        List<SearchDto.SearchListResponse> content = new ArrayList<>();
        if (start < mergedList.size()) {
            content = mergedList.subList(start, end);
        }

        Long challengeCount = queryFactory.select(challenge.count()).from(challenge)
                .where(visibleToUser(userDetails), challengeKeywordCondition(keyword)).fetchOne();

        Long postCount = queryFactory.select(post.count()).from(post)
                .where(post.type.in(PostType.VS_RECRUIT, PostType.Q_AND_A, PostType.REVIEW), postKeywordCondition(keyword)).fetchOne();

        long total = (challengeCount != null ? challengeCount : 0) + (postCount != null ? postCount : 0);

        return new PageImpl<>(content, pageable, total);
    }
}