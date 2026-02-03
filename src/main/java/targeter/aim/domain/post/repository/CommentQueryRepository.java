package targeter.aim.domain.post.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import targeter.aim.domain.file.dto.FileDto;
import targeter.aim.domain.post.dto.CommentDto;
import targeter.aim.domain.post.entity.Comment;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static targeter.aim.domain.file.entity.QCommentAttachedFile.commentAttachedFile;
import static targeter.aim.domain.file.entity.QCommentImage.commentImage;
import static targeter.aim.domain.file.entity.QProfileImage.profileImage;
import static targeter.aim.domain.post.entity.QComment.comment;
import static targeter.aim.domain.user.entity.QTier.tier;
import static targeter.aim.domain.user.entity.QUser.user;

@Repository
@RequiredArgsConstructor
public class CommentQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Page<CommentDto.CommentResponse> paginateByPostId(
            Long postId,
            Pageable pageable) {

        List<Comment> parentComments = queryFactory
                .selectFrom(comment)
                .leftJoin(comment.user, user).fetchJoin()
                .leftJoin(user.tier, tier).fetchJoin()
                .leftJoin(user.profileImage, profileImage).fetchJoin()
                .where(
                        comment.post.id.eq(postId),
                        comment.parent.isNull()
                )
                .orderBy(comment.createdAt.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(comment.count())
                .from(comment)
                .where(
                        comment.post.id.eq(postId),
                        comment.parent.isNull()
                )
                .fetchOne();

        if (parentComments.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, total != null ? total : 0);
        }

        List<Long> parentIds = parentComments.stream()
                .map(Comment::getId)
                .toList();

        List<Comment> childComments = queryFactory
                .selectFrom(comment)
                .leftJoin(comment.user, user).fetchJoin()
                .leftJoin(user.tier, tier).fetchJoin()
                .leftJoin(user.profileImage, profileImage).fetchJoin()
                .where(comment.parent.id.in(parentIds))
                .orderBy(comment.createdAt.asc())
                .fetch();

        List<Long> allCommentIds = Stream.concat(parentComments.stream(), childComments.stream())
                .map(Comment::getId)
                .distinct()
                .toList();

        Map<Long, List<FileDto.FileResponse>> imageMap = fetchImages(allCommentIds);
        Map<Long, List<FileDto.FileResponse>> fileMap = fetchFiles(allCommentIds);

        List<CommentDto.CommentResponse> content = assembleCommentHierarchy(parentComments, childComments, imageMap, fileMap);

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    private List<CommentDto.CommentResponse> assembleCommentHierarchy(
            List<Comment> parents,
            List<Comment> children,
            Map<Long, List<FileDto.FileResponse>> imageMap,
            Map<Long, List<FileDto.FileResponse>> fileMap
    ) {
        Map<Long, List<Comment>> childrenMap = children.stream()
                .collect(Collectors.groupingBy(c -> c.getParent().getId()));

        return parents.stream()
                .map(parent -> {
                    CommentDto.CommentResponse parentDto = mapToDto(parent, imageMap, fileMap);

                    List<Comment> myChildren = childrenMap.getOrDefault(parent.getId(), Collections.emptyList());

                    List<CommentDto.CommentResponse> childDtos = myChildren.stream()
                            .map(child -> mapToDto(child, imageMap, fileMap))
                            .toList();

                    parentDto.setChildrenComments(childDtos);

                    return parentDto;
                })
                .toList();
    }

    private CommentDto.CommentResponse mapToDto(
            Comment c,
            Map<Long, List<FileDto.FileResponse>> imageMap,
            Map<Long, List<FileDto.FileResponse>> fileMap
    ) {
        // 기존 엔티티 내의 컬렉션을 쓰지 않고, 배치 조회한 Map에서 가져옵니다 (N+1 방지)
        List<FileDto.FileResponse> images = imageMap.getOrDefault(c.getId(), Collections.emptyList());
        List<FileDto.FileResponse> files = fileMap.getOrDefault(c.getId(), Collections.emptyList());

        // 빌더 패턴을 사용하여 DTO 생성 (기존 from 메서드 로직 참고하여 재구성)
        return CommentDto.CommentResponse.builder()
                .commentId(c.getId())
                .depth(c.getDepth())
                .writerInfo(CommentDto.UserResponse.from(c.getUser()))
                .content(c.getContents())
                .attachedImages(images)
                .attachedFiles(files)
                .createdAt(c.getCreatedAt())
                .childrenComments(Collections.emptyList()) // 초기화
                .build();
    }

    private Map<Long, List<FileDto.FileResponse>> fetchImages(List<Long> commentIds) {
        return queryFactory
                .selectFrom(commentImage)
                .where(commentImage.comment.id.in(commentIds))
                .fetch()
                .stream()
                .collect(Collectors.groupingBy(
                        img -> img.getComment().getId(),
                        Collectors.mapping(FileDto.FileResponse::from, Collectors.toList())
                ));
    }

    private Map<Long, List<FileDto.FileResponse>> fetchFiles(List<Long> commentIds) {
        return queryFactory
                .selectFrom(commentAttachedFile)
                .where(commentAttachedFile.comment.id.in(commentIds))
                .fetch()
                .stream()
                .collect(Collectors.groupingBy(
                        file -> file.getComment().getId(),
                        Collectors.mapping(FileDto.FileResponse::from, Collectors.toList())
                ));
    }
}
