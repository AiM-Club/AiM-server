package targeter.aim.domain.post.repository;

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
import targeter.aim.domain.challenge.entity.ChallengeMode;
import targeter.aim.domain.file.dto.FileDto;
import targeter.aim.domain.file.entity.PostImage;
import targeter.aim.domain.file.entity.ProfileImage;
import targeter.aim.domain.file.entity.QPostAttachedFile;
import targeter.aim.domain.file.entity.QPostAttachedImage;
import targeter.aim.domain.label.entity.QField;
import targeter.aim.domain.label.entity.QTag;
import targeter.aim.domain.post.dto.PostDto;
import targeter.aim.domain.post.entity.Post;
import targeter.aim.domain.post.entity.PostType;
import targeter.aim.domain.post.entity.QPost;
import targeter.aim.domain.user.dto.TierDto;
import targeter.aim.domain.user.dto.UserDto;
import targeter.aim.domain.user.entity.User;
import targeter.aim.system.security.model.UserDetails;

import java.time.LocalDateTime;
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
            PostSortType sortType
    ) {
        JPAQuery<Tuple> query = buildBaseQuery(userDetails, null);
        applySorting(query, sortType);

        List<Tuple> tuples = query
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
            PostSortType sortType,
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
                        JPAExpressions.select(postLiked.count())
                                .from(postLiked)
                                .where(postLiked.post.eq(post))
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
    private void applySorting(JPAQuery<?> query, PostSortType sortType) {
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

        return PostDto.VSRecruitListResponse.builder()
                .postId(p.getId())
                .thumbnail(postImage != null
                        ? FileDto.FileResponse.from(postImage)
                        : null)
                .user(UserDto.UserResponse.builder()
                        .id(user.getId())
                        .nickname(user.getNickname())
                        .tier(TierDto.TierResponse.from(user.getTier()))
                        .profileImage(profileImage != null
                                ? FileDto.FileResponse.from(profileImage)
                                : null)
                        .build())
                .name(p.getTitle())
                .fields(fieldMap.getOrDefault(p.getId(), List.of()))
                .tags(tagMap.getOrDefault(p.getId(), List.of()))
                .job(p.getJob())
                .isLiked(Boolean.TRUE.equals(tuple.get(1, Boolean.class)))
                .likeCount(tuple.get(2, Long.class) == null ? 0 : tuple.get(2, Long.class).intValue())
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
                .limit(limit)                               // 개수 제한 (예: 10개)
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

    public Page<PostDto.PostListResponse> paginateQnaAndReview(
            UserDetails userDetails,
            Pageable pageable,
            PostType type,
            PostSortType sortType,
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
                .title(p.getTitle())
                .job(p.getJob())
                .fields(fieldMap.getOrDefault(p.getId(), List.of()))
                .tags(tagMap.getOrDefault(p.getId(), List.of()))
                .isLiked(Boolean.TRUE.equals(tuple.get(1, Boolean.class)))
                .likeCount(tuple.get(2, Long.class) == null ? 0 : tuple.get(2, Long.class).intValue())
                .user(
                        PostDto.PostUserResponse.builder()
                                .userId(user.getId())
                                .nickname(user.getNickname())
                                .tier(TierDto.TierResponse.from(user.getTier()))
                                .profileImage(profileImage != null
                                        ? FileDto.FileResponse.from(profileImage)
                                        : null)
                                .build()
                )
                .build();
    }
}