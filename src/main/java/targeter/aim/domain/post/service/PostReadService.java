package targeter.aim.domain.post.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import targeter.aim.domain.challenge.entity.ChallengeMode;
import targeter.aim.domain.post.dto.PostDto;
import targeter.aim.domain.post.entity.Post;
import targeter.aim.domain.post.entity.PostType;
import targeter.aim.domain.post.repository.PostLikedRepository;
import targeter.aim.domain.post.repository.PostQueryRepository;
import targeter.aim.domain.post.repository.PostRepository;
import targeter.aim.system.exception.model.ErrorCode;
import targeter.aim.system.exception.model.RestException;
import targeter.aim.system.security.model.UserDetails;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostReadService {

    private final PostRepository postRepository;
    private final PostQueryRepository postQueryRepository;
    private final PostLikedRepository postLikedRepository;

    public PostDto.VSRecruitPageResponse getVsRecruits(
            PostDto.ListSearchCondition condition,
            UserDetails userDetails,
            Pageable pageable
    ) {
        PostDto.PostSortType sortType = condition.getSort();
        String keyword = normalizeKeyword(condition.getKeyword());

        Page<PostDto.VSRecruitListResponse> page;

        if (keyword != null) {
            page = postQueryRepository.paginateByTypeAndKeyword(
                    userDetails, pageable, sortType, keyword
            );
        } else {
            page = postQueryRepository.paginateByType(
                    userDetails, pageable, sortType
            );
        }

        return PostDto.VSRecruitPageResponse.from(page);
    }

    public PostDto.HotPostPageResponse getHotSoloPosts(
            PostDto.ListSearchCondition condition,
            UserDetails userDetails,
            Pageable pageable
    ) {
        PostDto.PostSortType sortType = condition.getSort();

        Page<PostDto.HotPostListResponse> page = postQueryRepository.paginateHotPosts(
                userDetails,
                pageable,
                sortType,
                ChallengeMode.SOLO
        );

        return PostDto.HotPostPageResponse.from(page);
    }

    public PostDto.HotPostPageResponse getHotVsPosts(
            PostDto.ListSearchCondition condition,
            UserDetails userDetails,
            Pageable pageable
    ) {
        PostDto.PostSortType sortType = condition.getSort();

        Page<PostDto.HotPostListResponse> page = postQueryRepository.paginateHotPosts(
                userDetails,
                pageable,
                sortType,
                ChallengeMode.VS
        );

        return PostDto.HotPostPageResponse.from(page);
    }

    public PostDto.PostVsDetailResponse getVsPostDetail(
            Long postId,
            UserDetails userDetails
    ) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RestException(ErrorCode.POST_NOT_FOUND));

        if (post.getType() != PostType.VS_RECRUIT) {
            throw new RestException(ErrorCode.POST_NOT_FOUND);
        }

        boolean isLiked = false;
        if (userDetails != null) {
            isLiked = postLikedRepository.existsByPostAndUser(
                    post, userDetails.getUser()
            );
        }

        return PostDto.PostVsDetailResponse.from(post, isLiked);
    }

    public PostDto.PostDetailResponse getPostDetail(
            Long postId,
            UserDetails userDetails
    ) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RestException(ErrorCode.POST_NOT_FOUND));

        if (post.getType() == PostType.VS_RECRUIT) {
            throw new RestException(ErrorCode.POST_NOT_FOUND);
        }

        boolean isLiked = false;
        if (userDetails != null) {
            isLiked = postLikedRepository.existsByPostAndUser(
                    post, userDetails.getUser()
            );
        }

        return PostDto.PostDetailResponse.from(post, isLiked);
    }

    public List<PostDto.HotReviewResponse> getHotReview() {
        List<Post> hotReviews = postQueryRepository.findTopLikedReviewInLast3Months(10);

        return hotReviews.stream()
                .map(PostDto.HotReviewResponse::from)
                .collect(Collectors.toList());
    }

    public List<PostDto.HotVsPostResponse> getTop10HotVsPosts() {
        return postQueryRepository.findTop10HotVsPosts();
    }

    public PostDto.PostPageResponse getQnaPosts(
            PostDto.ListSearchCondition condition,
            UserDetails userDetails,
            Pageable pageable
    ) {
        return getQnaAndReviewPosts(
                condition,
                userDetails,
                pageable,
                PostType.Q_AND_A
        );
    }

    public PostDto.PostPageResponse getReviewPosts(
            PostDto.ListSearchCondition condition,
            UserDetails userDetails,
            Pageable pageable
    ) {
        return getQnaAndReviewPosts(
                condition,
                userDetails,
                pageable,
                PostType.REVIEW
        );
    }

    private PostDto.PostPageResponse getQnaAndReviewPosts(
            PostDto.ListSearchCondition condition,
            UserDetails userDetails,
            Pageable pageable,
            PostType postType
    ) {
        PostDto.PostSortType sortType = condition.getSort();
        PostDto.PostFilterType filterType = condition.getFilter();
        String keyword = normalizeKeyword(condition.getKeyword());
        ChallengeMode mode = parseFilterType(filterType);

        Page<PostDto.PostListResponse> page =
                postQueryRepository.paginateQnaAndReview(
                        userDetails,
                        pageable,
                        postType,
                        sortType,
                        keyword,
                        mode
                );

        return PostDto.PostPageResponse.from(page);
    }

    private ChallengeMode parseFilterType(PostDto.PostFilterType filterType) {
        if (filterType == null || filterType.equals(PostDto.PostFilterType.ALL)) {
            return null;
        }

        return switch (filterType) {
            case VS -> ChallengeMode.VS;
            case SOLO -> ChallengeMode.SOLO;
            default -> throw new RestException(ErrorCode.GLOBAL_BAD_REQUEST);
        };
    }

    public PostDto.PostPageResponse getMyPosts(
            PostDto.ListSearchCondition condition,
            UserDetails userDetails,
            Pageable pageable
    ) {
        if (userDetails == null) {
            throw new RestException(ErrorCode.AUTH_LOGIN_REQUIRED);
        }
        PostDto.PostSortType sortType = condition.getSort();
        PostDto.PostFilterType filterType = condition.getFilter();
        String keyword = normalizeKeyword(condition.getKeyword());

        List<PostType> types;
        if (filterType == null || filterType.equals(PostDto.PostFilterType.ALL)) {
            types = null;
        } else {
            types = switch (filterType) {
                case VS_RECRUIT -> List.of(PostType.VS_RECRUIT);
                case COMMUNITY -> List.of(PostType.Q_AND_A, PostType.REVIEW);
                default -> throw new RestException(ErrorCode.GLOBAL_BAD_REQUEST);
            };
        }

        Page<PostDto.PostListResponse> page =
                postQueryRepository.paginateMyPosts(
                        userDetails,
                        pageable,
                        sortType,
                        keyword,
                        types
                );

        return PostDto.PostPageResponse.from(page);
    }

    public PostDto.PostPageResponse getMyLikedPosts(
            PostDto.ListSearchCondition condition,
            UserDetails userDetails,
            Pageable pageable
    ) {
        if (userDetails == null) {
            throw new RestException(ErrorCode.AUTH_LOGIN_REQUIRED);
        }

        PostDto.PostSortType sortType = condition.getSort();
        PostDto.PostFilterType filterType = condition.getFilter();
        String keyword = normalizeKeyword(condition.getKeyword());

        List<PostType> types;
        if (filterType == null || filterType.equals(PostDto.PostFilterType.ALL)) {
            types = null;
        } else {
            types = switch (filterType) {
                case VS_RECRUIT -> List.of(PostType.VS_RECRUIT);
                case COMMUNITY -> List.of(PostType.Q_AND_A, PostType.REVIEW);
                default -> throw new RestException(ErrorCode.GLOBAL_BAD_REQUEST);
            };
        }

        Page<PostDto.PostListResponse> page =
                postQueryRepository.paginateLikedPosts(
                        userDetails,
                        pageable,
                        sortType,
                        keyword,
                        types
                );

        return PostDto.PostPageResponse.from(page);
    }

    public PostDto.PostPageResponse searchPosts(
            String keyword,
            PostDto.PostSortType sort,
            Pageable pageable,
            UserDetails userDetails
    ) {
        String k = normalizeKeyword(keyword);

        Page<PostDto.PostListResponse> page =
                postQueryRepository.paginateSearchAllByKeyword(userDetails, pageable, sort, k);

        return PostDto.PostPageResponse.from(page);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) return null;
        String k = keyword.trim();
        return k.isEmpty() ? null : k;
    }
}
