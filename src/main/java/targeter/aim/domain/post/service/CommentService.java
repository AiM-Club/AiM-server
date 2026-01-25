package targeter.aim.domain.post.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import targeter.aim.domain.file.service.FileService;
import targeter.aim.domain.post.dto.CommentDto;
import targeter.aim.domain.post.entity.Comment;
import targeter.aim.domain.post.entity.Post;
import targeter.aim.domain.post.repository.CommentRepository;
import targeter.aim.domain.post.repository.PostRepository;
import targeter.aim.domain.user.entity.User;
import targeter.aim.domain.user.repository.UserRepository;
import targeter.aim.system.exception.model.ErrorCode;
import targeter.aim.system.exception.model.RestException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final FileService fileService;

    @Transactional
    public CommentDto.CreateResponse createComment(
            Long postId,
            Long userId,
            String content,
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

        Comment comment = Comment.createRoot(user, post, content);
        Comment saved = commentRepository.save(comment);

        fileService.saveCommentImages(saved, attachedImages);
        fileService.saveCommentFiles(saved, attachedFiles);

        return new CommentDto.CreateResponse(postId);
    }
}