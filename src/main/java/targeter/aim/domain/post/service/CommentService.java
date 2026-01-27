package targeter.aim.domain.post.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import targeter.aim.domain.file.repository.CommentAttachedFileRepository;
import targeter.aim.domain.file.repository.CommentImageRepository;
import targeter.aim.domain.file.service.FileService;
import targeter.aim.domain.post.dto.CommentDto;
import targeter.aim.domain.post.entity.Comment;
import targeter.aim.domain.post.entity.Post;
import targeter.aim.domain.post.repository.CommentRepository;
import targeter.aim.domain.post.repository.PostRepository;
import targeter.aim.domain.user.dto.UserDto;
import targeter.aim.domain.user.entity.User;
import targeter.aim.domain.user.repository.UserRepository;
import targeter.aim.system.exception.model.ErrorCode;
import targeter.aim.system.exception.model.RestException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final FileService fileService;

    private final CommentImageRepository commentImageRepository;
    private final CommentAttachedFileRepository commentAttachedFileRepository;

    @Transactional
    public CommentDto.CreateResponse createComment(
            Long postId,
            Long userId,
            String content,
            Long parentCommentId,
            List<MultipartFile> attachedImages,
            List<MultipartFile> attachedFiles
    ) {
        if (content == null || content.isBlank()) {
            throw new RestException(ErrorCode.COMMENT_CONTENT_EMPTY);
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RestException(ErrorCode.POST_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RestException(ErrorCode.USER_NOT_FOUND));

        Comment comment;

        if (parentCommentId == null) {
            comment = Comment.createRoot(user, post, content);
        } else {
            Comment parent = commentRepository.findById(parentCommentId)
                    .orElseThrow(() -> new RestException(ErrorCode.COMMENT_NOT_FOUND));

            if (parent.getPost() == null || parent.getPost().getId() == null || !parent.getPost().getId().equals(postId)) {
                throw new RestException(ErrorCode.GLOBAL_INVALID_PARAMETER);
            }

            comment = Comment.createChild(user, post, parent, content);
        }

        Comment saved = commentRepository.save(comment);

        fileService.saveCommentImages(saved, attachedImages);
        fileService.saveCommentFiles(saved, attachedFiles);

        return new CommentDto.CreateResponse(postId);
    }

    @Transactional(readOnly = true)
    public List<CommentDto.CommentResponse> getComments(Long postId, String sort, String order, String filterType) {

        CommentSortType sortType = CommentSortType.from(sort);
        CommentOrderType orderType = CommentOrderType.from(order);
        CommentFilterType filter = CommentFilterType.from(filterType);

        postRepository.findById(postId)
                .orElseThrow(() -> new RestException(ErrorCode.POST_NOT_FOUND));

        List<Comment> comments = commentRepository.findAllByPostIdWithUser(postId);
        comments.sort(
                Comparator.comparing(Comment::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
        );

        if (orderType == CommentOrderType.DESC) {
            Collections.reverse(comments);
        }

        Map<Long, List<String>> imageUuidsByCommentId =
                commentImageRepository.findAllByPostId(postId).stream()
                        .collect(Collectors.groupingBy(
                                ci -> ci.getComment().getId(),
                                Collectors.mapping(ci -> ci.getUuid(), Collectors.toList())
                        ));

        Map<Long, List<String>> fileUuidsByCommentId =
                commentAttachedFileRepository.findAllByPostId(postId).stream()
                        .collect(Collectors.groupingBy(
                                cf -> cf.getComment().getId(),
                                Collectors.mapping(cf -> cf.getUuid(), Collectors.toList())
                        ));

        Map<Long, CommentDto.CommentResponse> responseMap = new LinkedHashMap<>();

        for (Comment c : comments) {
            Long cid = c.getId();

            CommentDto.CommentResponse resp = new CommentDto.CommentResponse(
                    cid,
                    c.getDepth(),
                    UserDto.UserResponse.from(c.getUser()),
                    c.getContents(),
                    imageUuidsByCommentId.getOrDefault(cid, List.of()),
                    fileUuidsByCommentId.getOrDefault(cid, List.of()),
                    c.getCreatedAt(),
                    c.getLastModifiedAt()
            );

            responseMap.put(cid, resp);
        }

        List<CommentDto.CommentResponse> roots = new ArrayList<>();

        for (Comment c : comments) {
            CommentDto.CommentResponse current = responseMap.get(c.getId());

            if (c.getParent() == null) {
                roots.add(current);
                continue;
            }

            CommentDto.CommentResponse parent = responseMap.get(c.getParent().getId());
            if (parent != null) {
                parent.addChild(current);
            } else {
                roots.add(current);
            }
        }

        if (filter == CommentFilterType.ROOT_ONLY) {
            return roots;
        }

        return roots;
    }

    public enum CommentSortType {
        LATEST;

        public static CommentSortType from(String value) {
            try {
                return CommentSortType.valueOf(value == null ? "LATEST" : value.trim().toUpperCase());
            } catch (Exception e) {
                throw new RestException(ErrorCode.GLOBAL_INVALID_PARAMETER);
            }
        }
    }

    public enum CommentOrderType {
        ASC, DESC;

        public static CommentOrderType from(String value) {
            try {
                return CommentOrderType.valueOf(value == null ? "DESC" : value.trim().toUpperCase());
            } catch (Exception e) {
                throw new RestException(ErrorCode.GLOBAL_INVALID_PARAMETER);
            }
        }
    }

    public enum CommentFilterType {
        ALL,
        ROOT_ONLY;

        public static CommentFilterType from(String value) {
            try {
                return CommentFilterType.valueOf(value == null ? "ALL" : value.trim().toUpperCase());
            } catch (Exception e) {
                throw new RestException(ErrorCode.GLOBAL_INVALID_PARAMETER);
            }
        }
    }
}