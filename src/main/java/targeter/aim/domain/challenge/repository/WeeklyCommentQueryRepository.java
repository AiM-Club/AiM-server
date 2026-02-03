package targeter.aim.domain.challenge.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import targeter.aim.domain.challenge.dto.WeeklyCommentDto;
import targeter.aim.domain.challenge.entity.WeeklyComment;
import targeter.aim.domain.file.dto.FileDto;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static targeter.aim.domain.challenge.entity.QWeeklyComment.weeklyComment;
import static targeter.aim.domain.challenge.entity.QWeeklyProgress.weeklyProgress;
import static targeter.aim.domain.file.entity.QChallengeCommentAttachedFile.challengeCommentAttachedFile;
import static targeter.aim.domain.file.entity.QChallengeCommentImage.challengeCommentImage;
import static targeter.aim.domain.file.entity.QProfileImage.profileImage;
import static targeter.aim.domain.user.entity.QTier.tier;
import static targeter.aim.domain.user.entity.QUser.user;

@Repository
@RequiredArgsConstructor
public class WeeklyCommentQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Page<WeeklyCommentDto.WeeklyCommentResponse> paginateByChallengeIdAndWeeksId(
            Long challengeId,
            Long weeksId,
            Pageable pageable
    ) {
        List<WeeklyComment> parentComments = queryFactory
                .selectFrom(weeklyComment)
                .join(weeklyComment.weeklyProgress, weeklyProgress)
                .leftJoin(weeklyComment.user, user).fetchJoin()
                .leftJoin(user.tier, tier).fetchJoin()
                .leftJoin(user.profileImage, profileImage).fetchJoin()
                .where(
                        weeklyProgress.challenge.id.eq(challengeId),
                        weeklyProgress.id.eq(weeksId),
                        weeklyComment.parentComment.isNull()
                )
                .orderBy(weeklyComment.createdAt.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(weeklyComment.count())
                .from(weeklyComment)
                .join(weeklyComment.weeklyProgress, weeklyProgress)
                .where(
                        weeklyProgress.challenge.id.eq(challengeId),
                        weeklyProgress.id.eq(weeksId),
                        weeklyComment.parentComment.isNull()
                )
                .fetchOne();

        if (parentComments.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, total != null ? total : 0);
        }

        List<Long> parentIds = parentComments.stream()
                .map(WeeklyComment::getId)
                .toList();

        List<WeeklyComment> childComments = queryFactory
                .selectFrom(weeklyComment)
                .leftJoin(weeklyComment.user, user).fetchJoin()
                .leftJoin(user.tier, tier).fetchJoin()
                .leftJoin(user.profileImage, profileImage).fetchJoin()
                .where(weeklyComment.parentComment.id.in(parentIds))
                .orderBy(weeklyComment.createdAt.asc())
                .fetch();

        List<Long> allCommentIds = Stream.concat(parentComments.stream(), childComments.stream())
                .map(WeeklyComment::getId)
                .distinct()
                .toList();

        Map<Long, List<FileDto.FileResponse>> imageMap = fetchImages(allCommentIds);
        Map<Long, List<FileDto.FileResponse>> fileMap = fetchFiles(allCommentIds);

        List<WeeklyCommentDto.WeeklyCommentResponse> content = assembleHierarchy(
                parentComments,
                childComments,
                imageMap,
                fileMap
        );

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    private List<WeeklyCommentDto.WeeklyCommentResponse> assembleHierarchy(
            List<WeeklyComment> parents,
            List<WeeklyComment> children,
            Map<Long, List<FileDto.FileResponse>> imageMap,
            Map<Long, List<FileDto.FileResponse>> fileMap
    ) {
        Map<Long, List<WeeklyComment>> childrenMap = children.stream()
                .collect(Collectors.groupingBy(c -> c.getParentComment().getId()));

        return parents.stream()
                .map(parent -> {
                    WeeklyCommentDto.WeeklyCommentResponse parentDto = mapToDto(parent, imageMap, fileMap);

                    List<WeeklyComment> myChildren = childrenMap.getOrDefault(parent.getId(), Collections.emptyList());

                    List<WeeklyCommentDto.WeeklyCommentResponse> childDtos = myChildren.stream()
                            .map(child -> mapToDto(child, imageMap, fileMap))
                            .toList();

                    parentDto.setChildrenComments(childDtos);
                    return parentDto;
                })
                .toList();
    }

    private WeeklyCommentDto.WeeklyCommentResponse mapToDto(
            WeeklyComment c,
            Map<Long, List<FileDto.FileResponse>> imageMap,
            Map<Long, List<FileDto.FileResponse>> fileMap
    ) {
        List<FileDto.FileResponse> images = imageMap.getOrDefault(c.getId(), Collections.emptyList());
        List<FileDto.FileResponse> files = fileMap.getOrDefault(c.getId(), Collections.emptyList());

        return WeeklyCommentDto.WeeklyCommentResponse.builder()
                .commentId(c.getId())
                .depth(c.getDepth())
                .writerInfo(WeeklyCommentDto.UserResponse.from(c.getUser()))
                .content(c.getContent())
                .attachedImages(images)
                .attachedFiles(files)
                .createdAt(c.getCreatedAt())
                .childrenComments(Collections.emptyList())
                .build();
    }

    private Map<Long, List<FileDto.FileResponse>> fetchImages(List<Long> commentIds) {
        return queryFactory
                .selectFrom(challengeCommentImage)
                .where(challengeCommentImage.weeklyComment.id.in(commentIds))
                .fetch()
                .stream()
                .collect(Collectors.groupingBy(
                        img -> img.getWeeklyComment().getId(),
                        Collectors.mapping(FileDto.FileResponse::from, Collectors.toList())
                ));
    }

    private Map<Long, List<FileDto.FileResponse>> fetchFiles(List<Long> commentIds) {
        return queryFactory
                .selectFrom(challengeCommentAttachedFile)
                .where(challengeCommentAttachedFile.weeklyComment.id.in(commentIds))
                .fetch()
                .stream()
                .collect(Collectors.groupingBy(
                        file -> file.getWeeklyComment().getId(),
                        Collectors.mapping(FileDto.FileResponse::from, Collectors.toList())
                ));
    }
}
