package targeter.aim.domain.file.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import targeter.aim.domain.file.entity.CommentAttachedFile;

import java.util.List;

import static targeter.aim.domain.file.entity.QCommentAttachedFile.commentAttachedFile;

@RequiredArgsConstructor
public class CommentAttachedFileQueryRepositoryImpl
        implements CommentAttachedFileQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<CommentAttachedFile> findAllByPostId(Long postId) {
        return queryFactory
                .selectFrom(commentAttachedFile)
                .where(commentAttachedFile.comment.post.id.eq(postId))
                .fetch();
    }
}