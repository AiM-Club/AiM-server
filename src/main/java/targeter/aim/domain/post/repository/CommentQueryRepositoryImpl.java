package targeter.aim.domain.post.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import targeter.aim.domain.post.entity.Comment;

import java.util.List;

import static targeter.aim.domain.post.entity.QComment.comment;
import static targeter.aim.domain.user.entity.QUser.user;

@RequiredArgsConstructor
public class CommentQueryRepositoryImpl implements CommentQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Comment> findAllByPostIdWithUser(Long postId) {
        return queryFactory
                .selectFrom(comment)
                .join(comment.user, user).fetchJoin()
                .leftJoin(comment.parent).fetchJoin()
                .where(comment.post.id.eq(postId))
                .fetch();
    }
}