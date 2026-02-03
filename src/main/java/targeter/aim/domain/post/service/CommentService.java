package targeter.aim.domain.post.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import targeter.aim.domain.challenge.repository.*;
import targeter.aim.domain.file.entity.CommentAttachedFile;
import targeter.aim.domain.file.entity.CommentImage;
import targeter.aim.domain.file.handler.FileHandler;
import targeter.aim.domain.post.dto.CommentDto;
import targeter.aim.domain.post.entity.Comment;
import targeter.aim.domain.post.entity.Post;
import targeter.aim.domain.post.repository.CommentQueryRepository;
import targeter.aim.domain.post.repository.CommentRepository;
import targeter.aim.domain.post.repository.PostRepository;
import targeter.aim.domain.user.entity.User;
import targeter.aim.system.exception.model.ErrorCode;
import targeter.aim.system.exception.model.RestException;
import targeter.aim.system.security.model.UserDetails;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final CommentQueryRepository commentQueryRepository;

    private final FileHandler fileHandler;

    @Transactional
    public CommentDto.CommentCreateResponse createComment(
            Long postId,
            CommentDto.CommentCreateRequest request,
            UserDetails userDetails
    ) {
        if (userDetails == null) {
            throw new RestException(ErrorCode.AUTH_LOGIN_REQUIRED);
        }
        User loginUser = userDetails.getUser();

        if (request.getContent() == null || request.getContent().length() > 50) {
            throw new RestException(ErrorCode.GLOBAL_BAD_REQUEST);
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RestException(ErrorCode.GLOBAL_NOT_FOUND));

        Comment toSave = request.toEntity();
        toSave.setUser(loginUser);
        toSave.setPost(post);

        if(request.getParentCommentId() == null) {
            toSave.setDepth(1);
            toSave.setParent(null);
        } else {
            Long parentCommentId = request.getParentCommentId();
            Comment parent = commentRepository.findById(parentCommentId)
                    .orElseThrow(() -> new RestException(ErrorCode.GLOBAL_NOT_FOUND, "부모 댓글을 찾을 수 없습니다."));

            if (parent.getPost() == null || !parent.getPost().getId().equals(postId)) {
                throw new RestException(ErrorCode.GLOBAL_BAD_REQUEST, "부모 댓글이 해당 게시글에 속하지 않습니다.");
            }

            if(parent.getParent() != null || parent.getDepth() == null || parent.getDepth() != 1 ) {
                throw new RestException(ErrorCode.GLOBAL_BAD_REQUEST, "대댓글 아래에 대댓글을 작성할 수 없습니다.");
            }

            toSave.setDepth(2);
            toSave.setParent(parent);
        }

        Comment saved = commentRepository.save(toSave);

        saveAttachedImages(request.getAttachedImages(), saved);
        saveAttachedFiles(request.getAttachedFiles(), saved);

        return CommentDto.CommentCreateResponse.builder()
                .postId(postId)
                .commentId(saved.getId())
                .depth(saved.getDepth())
                .build();
    }

    private void saveAttachedImages(List<MultipartFile> files, Comment comment) {
        if (files == null || files.isEmpty())
            return;

        files.stream()
                .filter(f -> f != null && !f.isEmpty())
                .forEach(file -> {
                    CommentImage image = CommentImage.from(file, comment);
                    image.setComment(comment);
                    comment.addAttachedImage(image);
                    fileHandler.saveFile(file, image);
                });
    }

    private void saveAttachedFiles(List<MultipartFile> files, Comment comment) {
        if (files == null || files.isEmpty())
            return;

        files.stream()
                .filter(f -> f != null && !f.isEmpty())
                .forEach(file -> {
                    CommentAttachedFile attached = CommentAttachedFile.from(file, comment);
                    attached.setComment(comment);
                    comment.addAttachedFile(attached);
                    fileHandler.saveFile(file, attached);
                });
    }

    @Transactional(readOnly = true)
    public CommentDto.CommentPageResponse getComments(
            Long postId,
            Pageable pageable,
            UserDetails userDetails
    ) {
        if (userDetails == null) {
            throw new RestException(ErrorCode.AUTH_LOGIN_REQUIRED);
        }

        Page<CommentDto.CommentResponse> pageResult = commentQueryRepository.paginateByPostId(postId, pageable);

        return CommentDto.CommentPageResponse.from(pageResult);
    }
}