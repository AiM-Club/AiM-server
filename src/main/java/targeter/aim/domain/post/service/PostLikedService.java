package targeter.aim.domain.post.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import targeter.aim.domain.post.dto.PostLikedDto;
import targeter.aim.domain.post.entity.Post;
import targeter.aim.domain.post.entity.PostLiked;
import targeter.aim.domain.post.repository.PostLikedRepository;
import targeter.aim.domain.post.repository.PostRepository;
import targeter.aim.domain.user.entity.User;
import targeter.aim.domain.user.repository.UserRepository;
import targeter.aim.system.exception.model.ErrorCode;
import targeter.aim.system.exception.model.RestException;
import targeter.aim.system.security.model.UserDetails;

@Service
@RequiredArgsConstructor
public class PostLikedService {

    private final PostLikedRepository postLikedRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;

    @Transactional
    public PostLikedDto.PostLikedResponse togglePostLikes(Long postId, UserDetails userDetails) {

        User user = userRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new RestException(ErrorCode.USER_NOT_FOUND));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RestException(ErrorCode.POST_NOT_FOUND));

        boolean exists = postLikedRepository.existsByPostAndUser(post, user);

        if(exists) {
            postLikedRepository.deleteByPostAndUser(post, user);
            return PostLikedDto.PostLikedResponse.builder()
                    .id(post.getId())
                    .isLiked(false)
                    .build();
        }

        postLikedRepository.save(new PostLiked(user, post));

        return PostLikedDto.PostLikedResponse.builder()
                .id(post.getId())
                .isLiked(true)
                .build();
    }
}
