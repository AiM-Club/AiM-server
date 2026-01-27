package targeter.aim.domain.file.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import targeter.aim.domain.file.entity.CommentImage;

import java.util.List;

import static targeter.aim.domain.file.entity.QCommentImage.commentImage;

@RequiredArgsConstructor
public class CommentImageQueryRepositoryImpl
        implements CommentImageQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<CommentImage> findAllByPostId(Long postId) {
        return queryFactory
                .selectFrom(commentImage)
                .where(commentImage.comment.post.id.eq(postId))
                .fetch();
    }
}