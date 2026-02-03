package targeter.aim.domain.post.repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import targeter.aim.domain.challenge.entity.ChallengeMode;
import targeter.aim.domain.file.dto.FileDto;
import targeter.aim.domain.file.entity.PostImage;
import targeter.aim.domain.file.entity.ProfileImage;
import targeter.aim.domain.label.entity.QField;
import targeter.aim.domain.label.entity.QTag;
import targeter.aim.domain.post.dto.PostDto;
import targeter.aim.domain.post.entity.Post;
import targeter.aim.domain.post.entity.PostType;
import targeter.aim.domain.post.entity.QPost;
import targeter.aim.domain.user.dto.TierDto;
import targeter.aim.domain.user.entity.User;
import targeter.aim.system.security.model.UserDetails;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static targeter.aim.domain.label.entity.QField.field;
import static targeter.aim.domain.label.entity.QTag.tag;
import static targeter.aim.domain.post.entity.QPost.post;
import static targeter.aim.domain.post.entity.QPostLiked.postLiked;

@Repository
@RequiredArgsConstructor
public class PostQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Page<PostDto.VSRecruitListResponse> paginateByType(
            UserDetails userDetails,
            Pageable pageable,
            PostDto.PostSortType sortType
    ) {
        JPAQuery<Tuple> query = buildBaseQuery(userDetails, null);
        applySorting(query, sortType);

        List<Tuple> tuples = query
                .leftJoin(post.challenge).fetchJoin()
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = buildCountQuery(null).fetchOne();

        return new PageImpl<>(
                enrichDetails(tuples),
                pageable,
                total == null ? 0 : total
        );
    }

    // 분야 필터링
    public Page<PostDto.VSRecruitListResponse> paginateByTypeAndKeyword(
            UserDetails userDetails,
            Pageable pageable,
            PostDto.PostSortType sortType,
            String keyword
    ) {
        JPAQuery<Tuple> query = buildBaseQuery(userDetails, keyword);
        applySorting(query, sortType);

        List<Tuple> tuples = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = buildCountQuery(keyword).fetchOne();

        return new PageImpl<>(
                enrichDetails(tuples),
                pageable,
                total == null ? 0 : total
        );
    }

    private JPAQuery<Tuple> buildBaseQuery(
            UserDetails userDetails,
            String keyword
    ) {
        JPAQuery<Tuple> query = queryFactory
                .select(
                        post,
                        userDetails != null
                                ? JPAExpressions.selectOne()
                                .from(postLiked)
                                .where(
                                        postLiked.post.eq(post)
                                                .and(postLiked.user.id.eq(userDetails.getUser().getId()))
                                ).exists()
                                : Expressions.FALSE,
                        post.likeCount
                )
                .from(post)
                .where(post.type.eq(PostType.VS_RECRUIT))
                .leftJoin(post.user).fetchJoin()
                .leftJoin(post.user.tier).fetchJoin()
                .leftJoin(post.user.profileImage).fetchJoin();

        BooleanExpression keywordPredicate = keywordCondition(keyword);

        if (keywordPredicate != null) {
            query.where(keywordPredicate);
        }

        return query;
    }

    private JPAQuery<Long> buildCountQuery(
            String keyword
    ) {
        JPAQuery<Long> query = queryFactory
                .select(post.count())
                .from(post)
                .where(post.type.eq(PostType.VS_RECRUIT));

        BooleanExpression keywordPredicate = keywordCondition(keyword);
        if (keywordPredicate != null) {
            query.where(keywordPredicate);
        }

        return query;
    }

    // 제목 + 분야 + 태그 검색 조건설정
    private BooleanExpression keywordCondition(String keyword) {
        if (keyword == null || keyword.isBlank()) return null;

        String k = keyword.trim();

        BooleanExpression inTitle = post.title.containsIgnoreCase(k);

        // 분야 검색
        var pField = new QPost("pField");
        QField f = new QField("f");

        BooleanExpression inField = JPAExpressions
                .selectOne()
                .from(pField)
                .join(pField.fields, f)
                .where(pField.id.eq(post.id).and(f.name.containsIgnoreCase(k)))
                .exists();

        // 태그 검색
        var pTag = new QPost("pTag");
        QTag t = new QTag("t");

        BooleanExpression inTag = JPAExpressions
                .selectOne()
                .from(pTag)
                .join(pTag.tags, t)
                .where(pTag.id.eq(post.id).and(t.name.containsIgnoreCase(k)))
                .exists();

        return inTitle.or(inField).or(inTag);
    }

    // 정렬
    private void applySorting(JPAQuery<?> query, PostDto.PostSortType sortType) {
        switch (sortType) {
            case LATEST -> query.orderBy(post.createdAt.desc());

            case OLDEST -> query.orderBy(post.createdAt.asc());

            case LIKED -> query.orderBy(post.likeCount.desc());

            case TITLE -> query.orderBy(
                    post.title.asc(),
                    post.createdAt.desc()
            );
        }
    }

    //DTO 매핑
    private List<PostDto.VSRecruitListResponse> enrichDetails(List<Tuple> tuples) {
        if (tuples.isEmpty()) return List.of();

        List<Long> ids = tuples.stream()
                .map(t -> t.get(0, Post.class).getId())
                .toList();

        Map<Long, List<String>> fieldMap = fetchFields(ids);
        Map<Long, List<String>> tagMap = fetchTags(ids);

        return tuples.stream()
                .map(t -> mapToDto(t, fieldMap, tagMap))
                .toList();
    }

    private Map<Long, List<String>> fetchFields(List<Long> ids) {
        return queryFactory
                .select(post.id, field.name)
                .from(post)
                .join(post.fields, field)
                .where(post.id.in(ids))
                .fetch()
                .stream()
                .collect(Collectors.groupingBy(
                        t -> t.get(post.id),
                        Collectors.mapping(t -> t.get(field.name), Collectors.toList())
                ));
    }

    private Map<Long, List<String>> fetchTags(List<Long> ids) {
        return queryFactory
                .select(post.id, tag.name)
                .from(post)
                .join(post.tags, tag)
                .where(post.id.in(ids))
                .fetch()
                .stream()
                .collect(Collectors.groupingBy(
                        t -> t.get(post.id),
                        Collectors.mapping(t -> t.get(tag.name), Collectors.toList())
                ));
    }

    private PostDto.VSRecruitListResponse mapToDto(
            Tuple tuple,
            Map<Long, List<String>> fieldMap,
            Map<Long, List<String>> tagMap
    ) {
        Post p = tuple.get(0, Post.class);
        User user = p.getUser();
        ProfileImage profileImage = user.getProfileImage();
        PostImage postImage = p.getPostImage();
        Integer likeCount = tuple.get(2, Integer.class);

        return PostDto.VSRecruitListResponse.builder()
                .postId(p.getId())
                .thumbnail(postImage != null
                        ? FileDto.FileResponse.from(postImage)
                        : null)
                .user(PostDto.UserResponse.builder()
                        .userId(user.getId())
                        .nickname(user.getNickname())
                        .tier(TierDto.TierResponse.from(user.getTier()))
                        .profileImage(profileImage != null
                                ? FileDto.FileResponse.from(profileImage)
                                : null)
                        .build())
                .startedAt(p.getChallenge().getStartedAt())
                .durationWeek(p.getChallenge().getDurationWeek())
                .name(p.getTitle())
                .fields(fieldMap.getOrDefault(p.getId(), List.of()))
                .tags(tagMap.getOrDefault(p.getId(), List.of()))
                .job(p.getJob())
                .isLiked(Boolean.TRUE.equals(tuple.get(1, Boolean.class)))
                .likeCount(likeCount == null ? 0 : likeCount)
                .build();
    }

    public List<Post> findTopLikedReviewInLast3Months(int limit) {
        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);

        return queryFactory
                .selectFrom(post)
                .where(
                        post.type.eq(PostType.REVIEW),      // 후기글만
                        post.createdAt.goe(threeMonthsAgo)  // 최근 3개월 내
                )
                .orderBy(
                        post.likeCount.desc(),              // 좋아요 많은 순
                        post.createdAt.desc()               // (동점일 경우 최신순)
                )
                .limit(limit)
                .fetch();
    }

    public List<PostDto.HotVsPostResponse> findTop10HotVsPosts() {
        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);

        List<Tuple> tuples = queryFactory
                .select(
                        post,
                        postLiked.count()
                )
                .from(post)
                .leftJoin(postLiked)
                .on(postLiked.post.eq(post))
                .where(
                        post.type.eq(PostType.VS_RECRUIT),
                        post.createdAt.goe(threeMonthsAgo)
                )
                .groupBy(post.id)
                .orderBy(
                        postLiked.count().desc(),
                        post.createdAt.desc()
                )
                .limit(10)
                .fetch();

        if (tuples.isEmpty()) {
            return List.of();
        }

        List<Long> postIds = tuples.stream()
                .map(t -> t.get(post).getId())
                .toList();

        Map<Long, List<String>> fieldMap = fetchFields(postIds);

        return tuples.stream()
                .map(tuple -> {
                    Post p = tuple.get(post);

                    List<String> fields = fieldMap
                            .getOrDefault(p.getId(), List.of())
                            .stream()
                            .limit(3)
                            .toList();

                    return new PostDto.HotVsPostResponse(
                            p.getId(),
                            p.getTitle(),
                            fields
                    );
                })
                .toList();
    }

    // HOT 게시글(SOLO/VS) 페이지네이션 조회
    public Page<PostDto.HotPostListResponse> paginateHotPosts(
            UserDetails userDetails,
            Pageable pageable,
            PostDto.PostSortType sortType,
            ChallengeMode mode
    ) {
        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);

        NumberExpression<Long> likeCountExpr = postLiked.count();

        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();
        orderSpecifiers.add(likeCountExpr.desc());
        orderSpecifiers.addAll(List.of(secondaryHotOrder(sortType)));

        JPAQuery<Tuple> query = queryFactory
                .select(
                        post,
                        userDetails != null
                                ? JPAExpressions.selectOne()
                                .from(postLiked)
                                .where(
                                        postLiked.post.eq(post)
                                                .and(postLiked.user.id.eq(userDetails.getUser().getId()))
                                ).exists()
                                : Expressions.FALSE,
                        likeCountExpr
                )
                .from(post)
                .leftJoin(postLiked)
                .on(postLiked.post.eq(post))
                .leftJoin(post.user).fetchJoin()
                .leftJoin(post.user.tier).fetchJoin()
                .leftJoin(post.user.profileImage).fetchJoin()
                .leftJoin(post.challenge).fetchJoin()
                .where(
                        post.type.in(PostType.Q_AND_A, PostType.REVIEW),
                        post.createdAt.goe(threeMonthsAgo),
                        post.challenge.mode.eq(mode)
                )
                .groupBy(post.id)
                .orderBy(orderSpecifiers.toArray(new OrderSpecifier[0]));

        List<Tuple> tuples = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(post.countDistinct())
                .from(post)
                .where(
                        post.type.in(PostType.Q_AND_A, PostType.REVIEW),
                        post.createdAt.goe(threeMonthsAgo),
                        post.challenge.mode.eq(mode)
                )
                .fetchOne();

        return new PageImpl<>(
                enrichHotDetails(tuples),
                pageable,
                total == null ? 0 : total
        );
    }

    private OrderSpecifier<?>[] secondaryHotOrder(PostDto.PostSortType sortType) {
        return switch (sortType) {
            case OLDEST -> new OrderSpecifier[]{
                    post.createdAt.asc()
            };
            case TITLE -> new OrderSpecifier[]{
                    post.title.asc(),
                    post.createdAt.desc()
            };
            case LIKED, LATEST -> new OrderSpecifier[]{
                    post.createdAt.desc()
            };
        };
    }

    private List<PostDto.HotPostListResponse> enrichHotDetails(List<Tuple> tuples) {
        if (tuples.isEmpty()) return List.of();

        List<Long> ids = tuples.stream()
                .map(t -> t.get(0, Post.class).getId())
                .toList();

        Map<Long, List<String>> fieldMap = fetchFields(ids);
        Map<Long, List<String>> tagMap = fetchTags(ids);

        return tuples.stream()
                .map(t -> mapToHotDto(t, fieldMap, tagMap))
                .toList();
    }

    private PostDto.HotPostListResponse mapToHotDto(
            Tuple tuple,
            Map<Long, List<String>> fieldMap,
            Map<Long, List<String>> tagMap
    ) {
        Post p = tuple.get(0, Post.class);
        User user = p.getUser();
        ProfileImage profileImage = user.getProfileImage();

        Long likeCnt = tuple.get(2, Long.class);

        return PostDto.HotPostListResponse.builder()
                .postId(p.getId())
                .user(PostDto.UserResponse.builder()
                        .userId(user.getId())
                        .nickname(user.getNickname())
                        .tier(TierDto.TierResponse.from(user.getTier()))
                        .profileImage(profileImage != null
                                ? FileDto.FileResponse.from(profileImage)
                                : null)
                        .build())
                .startedAt(p.getChallenge().getStartedAt())
                .durationWeek(p.getChallenge().getDurationWeek())
                .title(p.getTitle())
                .fields(fieldMap.getOrDefault(p.getId(), List.of()))
                .tags(tagMap.getOrDefault(p.getId(), List.of()))
                .job(p.getJob())
                .liked(Boolean.TRUE.equals(tuple.get(1, Boolean.class)))
                .likeCount(likeCnt == null ? 0 : likeCnt.intValue())
                .mode(p.getChallenge().getMode())
                .build();
    }

    // Qna, Review 게시글 목록 조회
    public Page<PostDto.PostListResponse> paginateQnaAndReview(
            UserDetails userDetails,
            Pageable pageable,
            PostType type,
            PostDto.PostSortType sortType,
            String keyword,
            ChallengeMode mode
    ) {
        JPAQuery<Tuple> query = buildBaseQueryForQnaAndReview(
                userDetails,
                type,
                keyword,
                mode
        );

        applySorting(query, sortType);

        List<Tuple> tuples = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = buildCountQueryForQnaAndReview(
                type,
                keyword,
                mode
        ).fetchOne();

        return new PageImpl<>(
                enrichPostListDetails(tuples),
                pageable,
                total == null ? 0 : total
        );
    }

    private JPAQuery<Tuple> buildBaseQueryForQnaAndReview(
            UserDetails userDetails,
            PostType type,
            String keyword,
            ChallengeMode mode
    ) {
        JPAQuery<Tuple> query = queryFactory
                .select(
                        post,
                        userDetails != null
                                ? JPAExpressions.selectOne()
                                .from(postLiked)
                                .where(
                                        postLiked.post.eq(post)
                                                .and(postLiked.user.id.eq(userDetails.getUser().getId()))
                                ).exists()
                                : Expressions.FALSE,
                        JPAExpressions.select(postLiked.count())
                                .from(postLiked)
                                .where(postLiked.post.eq(post))
                )
                .from(post)
                .leftJoin(post.challenge)
                .where(
                        post.type.eq(type),
                        mode != null ? post.challenge.mode.eq(mode) : null
                )
                .leftJoin(post.user).fetchJoin()
                .leftJoin(post.user.tier).fetchJoin()
                .leftJoin(post.user.profileImage).fetchJoin();

        BooleanExpression keywordPredicate = keywordCondition(keyword);
        if (keywordPredicate != null) {
            query.where(keywordPredicate);
        }

        return query;
    }

    private JPAQuery<Long> buildCountQueryForQnaAndReview(
            PostType type,
            String keyword,
            ChallengeMode mode
    ) {
        JPAQuery<Long> query = queryFactory
                .select(post.count())
                .from(post)
                .leftJoin(post.challenge)
                .where(
                        post.type.eq(type),
                        mode != null ? post.challenge.mode.eq(mode) : null
                );

        BooleanExpression keywordPredicate = keywordCondition(keyword);
        if (keywordPredicate != null) {
            query.where(keywordPredicate);
        }

        return query;
    }


    private List<PostDto.PostListResponse> enrichPostListDetails(List<Tuple> tuples) {
        if (tuples.isEmpty()) return List.of();

        List<Long> ids = tuples.stream()
                .map(t -> t.get(0, Post.class).getId())
                .toList();

        Map<Long, List<String>> fieldMap = fetchFields(ids);
        Map<Long, List<String>> tagMap = fetchTags(ids);

        return tuples.stream()
                .map(t -> mapToPostListDto(t, fieldMap, tagMap))
                .toList();
    }

    private PostDto.PostListResponse mapToPostListDto(
            Tuple tuple,
            Map<Long, List<String>> fieldMap,
            Map<Long, List<String>> tagMap
    ) {
        Post p = tuple.get(0, Post.class);
        User user = p.getUser();
        ProfileImage profileImage = user.getProfileImage();
        PostImage postImage = p.getPostImage();

        return PostDto.PostListResponse.builder()
                .postId(p.getId())
                .thumbnail(postImage != null
                        ? FileDto.FileResponse.from(postImage)
                        : null)
                .user(
                        PostDto.UserResponse.builder()
                                .userId(user.getId())
                                .nickname(user.getNickname())
                                .tier(TierDto.TierResponse.from(user.getTier()))
                                .profileImage(profileImage != null
                                        ? FileDto.FileResponse.from(profileImage)
                                        : null)
                                .build()
                )
                .startedAt(p.getChallenge().getStartedAt())
                .durationWeek(p.getChallenge().getDurationWeek())
                .name(p.getTitle())
                .job(p.getJob())
                .fields(fieldMap.getOrDefault(p.getId(), List.of()))
                .tags(tagMap.getOrDefault(p.getId(), List.of()))
                .isLiked(Boolean.TRUE.equals(tuple.get(1, Boolean.class)))
                .likeCount(tuple.get(2, Long.class) == null ? 0 : tuple.get(2, Long.class).intValue())

                .build();
    }

    // 내가 쓴 게시글 목록 조회
    public Page<PostDto.PostListResponse> paginateMyPosts(
            UserDetails userDetails,
            Pageable pageable,
            PostDto.PostSortType sortType,
            String keyword,
            List<PostType> types
    ) {
        JPAQuery<Tuple> query = buildBaseQueryForMyPosts(
                userDetails,
                keyword,
                types
        );

        applySorting(query, sortType);

        List<Tuple> tuples = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = buildCountQueryForMyPosts(
                userDetails,
                keyword,
                types
        ).fetchOne();

        return new PageImpl<>(
                enrichPostListDetails(tuples),
                pageable,
                total == null ? 0 : total
        );
    }

    private JPAQuery<Tuple> buildBaseQueryForMyPosts(
            UserDetails userDetails,
            String keyword,
            List<PostType> types
    ) {
        JPAQuery<Tuple> query = queryFactory
                .select(
                        post,
                        JPAExpressions.selectOne()
                                .from(postLiked)
                                .where(
                                        postLiked.post.eq(post)
                                                .and(postLiked.user.id.eq(userDetails.getUser().getId()))
                                ).exists(),
                        JPAExpressions.select(postLiked.count())
                                .from(postLiked)
                                .where(postLiked.post.eq(post))
                )
                .from(post)
                .where(
                        post.user.id.eq(userDetails.getUser().getId()),
                        types != null ? post.type.in(types) : null
                )
                .leftJoin(post.user).fetchJoin()
                .leftJoin(post.user.tier).fetchJoin()
                .leftJoin(post.user.profileImage).fetchJoin();

        BooleanExpression keywordPredicate = keywordCondition(keyword);
        if (keywordPredicate != null) {
            query.where(keywordPredicate);
        }

        return query;
    }

    private JPAQuery<Long> buildCountQueryForMyPosts(
            UserDetails userDetails,
            String keyword,
            List<PostType> types
    ) {
        JPAQuery<Long> query = queryFactory
                .select(post.count())
                .from(post)
                .where(
                        post.user.id.eq(userDetails.getUser().getId()),
                        types != null ? post.type.in(types) : null
                );

        BooleanExpression keywordPredicate = keywordCondition(keyword);
        if (keywordPredicate != null) {
            query.where(keywordPredicate);
        }

        return query;
    }

    // 내가 좋아요 누른 게시글 목록 조회
    public Page<PostDto.PostListResponse> paginateLikedPosts(
            UserDetails userDetails,
            Pageable pageable,
            PostDto.PostSortType sortType,
            String keyword,
            List<PostType> types
    ) {
        JPAQuery<Tuple> query = buildBaseQueryForLikedPosts(
                userDetails,
                keyword,
                types
        );

        applySorting(query, sortType);

        List<Tuple> tuples = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = buildCountQueryForLikedPosts(
                userDetails,
                keyword,
                types
        ).fetchOne();

        return new PageImpl<>(
                enrichPostListDetails(tuples),
                pageable,
                total == null ? 0 : total
        );
    }

    private JPAQuery<Tuple> buildBaseQueryForLikedPosts(
            UserDetails userDetails,
            String keyword,
            List<PostType> types
    ) {
        JPAQuery<Tuple> query = queryFactory
                .select(
                        post,
                        Expressions.TRUE,
                        JPAExpressions.select(postLiked.count())
                                .from(postLiked)
                                .where(postLiked.post.eq(post))
                )
                .from(postLiked)
                .join(postLiked.post, post)
                .where(
                        postLiked.user.id.eq(userDetails.getUser().getId()),
                        types != null ? post.type.in(types) : null
                )
                .leftJoin(post.user).fetchJoin()
                .leftJoin(post.user.tier).fetchJoin()
                .leftJoin(post.user.profileImage).fetchJoin();

        BooleanExpression keywordPredicate = keywordCondition(keyword);
        if (keywordPredicate != null) {
            query.where(keywordPredicate);
        }

        return query;
    }

    private JPAQuery<Long> buildCountQueryForLikedPosts(
            UserDetails userDetails,
            String keyword,
            List<PostType> types
    ) {
        JPAQuery<Long> query = queryFactory
                .select(post.count())
                .from(postLiked)
                .join(postLiked.post, post)
                .where(
                        postLiked.user.id.eq(userDetails.getUser().getId()),
                        types != null ? post.type.in(types) : null
                );

        BooleanExpression keywordPredicate = keywordCondition(keyword);
        if (keywordPredicate != null) {
            query.where(keywordPredicate);
        }

        return query;
    }

}