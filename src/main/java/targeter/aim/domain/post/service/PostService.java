package targeter.aim.domain.post.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import targeter.aim.domain.ai.llm.dto.RoutePayload;
import targeter.aim.domain.challenge.dto.ChallengeDto;
import targeter.aim.domain.challenge.entity.Challenge;
import targeter.aim.domain.challenge.entity.ChallengeMode;
import targeter.aim.domain.challenge.repository.ChallengeRepository;
import targeter.aim.domain.file.entity.PostAttachedFile;
import targeter.aim.domain.file.entity.PostAttachedImage;
import targeter.aim.domain.file.entity.PostImage;
import targeter.aim.domain.file.handler.FileHandler;
import targeter.aim.domain.label.entity.Field;
import targeter.aim.domain.label.entity.Tag;
import targeter.aim.domain.label.repository.FieldRepository;
import targeter.aim.domain.label.repository.TagRepository;
import targeter.aim.domain.post.dto.PostDto;
import targeter.aim.domain.post.repository.PostQueryRepository;
import targeter.aim.domain.post.repository.PostSortType;
import targeter.aim.domain.post.entity.Post;
import targeter.aim.domain.post.repository.PostRepository;
import targeter.aim.domain.post.entity.PostType;
import targeter.aim.domain.user.entity.User;
import targeter.aim.domain.user.repository.UserRepository;
import targeter.aim.system.exception.model.ErrorCode;
import targeter.aim.system.exception.model.RestException;
import targeter.aim.system.security.model.UserDetails;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostQueryRepository postQueryRepository;
    private final ChallengeRepository challengeRepository;
    private final TagRepository tagRepository;
    private final FieldRepository fieldRepository;

    private final FileHandler fileHandler;

    @Transactional(readOnly = true)
    public PostDto.VSRecruitPageResponse getVsRecruits(
            PostDto.ListSearchCondition condition,
            UserDetails userDetails,
            Pageable pageable
    ) {
        PostSortType sortType = parseSortType(condition.getSort());

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

    private String normalizeKeyword(String keyword) {
        if (keyword == null) return null;
        String k = keyword.trim();
        return k.isEmpty() ? null : k;
    }

    private PostSortType parseSortType(String raw) {
        try {
            return PostSortType.valueOf(raw == null ? "LATEST" : raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RestException(ErrorCode.GLOBAL_BAD_REQUEST);
        }
    }

    @Transactional
    public Long createChallengePost(
            PostDto.CreateChallengePostRequest request,
            Long userId
    ) {

        Challenge challenge = challengeRepository.findById(request.getChallengeId())
                .orElseThrow(() -> new RestException(ErrorCode.CHALLENGE_NOT_FOUND));

        if (!challenge.getHost().getId().equals(userId)) {
            throw new RestException(ErrorCode.AUTH_FORBIDDEN);
        }

        if (challenge.getMode() != ChallengeMode.VS) {
            throw new RestException(ErrorCode.CHALLENGE_MODE_NOT_VS);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RestException(ErrorCode.USER_NOT_FOUND));

        Post post = Post.builder()
                .user(user)
                .title(request.getTitle())
                .job(request.getJob())
                .startedAt(challenge.getStartedAt())
                .durationWeek(challenge.getDurationWeek())
                .content(request.getContents())
                .mode(challenge.getMode())
                .type(PostType.VS_RECRUIT)
                .build();

        Post saved = postRepository.save(post);

        updatePostLabels(saved, request.getTags(), request.getFields());

        saveThumbnail(request.getThumbnail(), saved);
        saveAttachedImages(request.getImages(), saved);
        saveAttachedFiles(request.getFiles(), saved);

        return saved.getId();
    }

    @Transactional
    public PostDto.CreatePostResponse createQnAPost (
            PostDto.CreatePostRequest request,
            UserDetails userDetails
    ) {
        if (userDetails == null) {
            throw new RestException(ErrorCode.AUTH_LOGIN_REQUIRED);
        }
        User user = userDetails.getUser();

        Challenge challenge = challengeRepository.findById(request.getChallengeId())
                .orElseThrow(() -> new RestException(ErrorCode.CHALLENGE_NOT_FOUND));

        if(!challenge.getStartedAt().isEqual(request.getStartedAt()) || !challenge.getDurationWeek().equals(request.getDurationWeek())) {
            throw new RestException(ErrorCode.GLOBAL_CONFLICT, "입력한 챌린지와 게시글의 정보가 다릅니다.");
        }

        Post saved = request.toEntity();
        saved.setUser(user);
        saved.setMode(challenge.getMode());
        saved.setType(PostType.Q_AND_A);

        saveThumbnail(request.getThumbnail(), saved);
        saveAttachedImages(request.getImages(), saved);
        saveAttachedFiles(request.getFiles(), saved);

        // 4. 태그 / 분야 연관관계 매핑
        updatePostLabels(saved, request.getTags(), request.getFields());
        postRepository.save(saved);

        return PostDto.CreatePostResponse.from(saved);
    }

    private void saveThumbnail(MultipartFile thumbnail, Post post) {
        if (thumbnail == null || thumbnail.isEmpty()) return;

        PostImage postImage = PostImage.from(thumbnail, post);
        post.setThumbnail(postImage);
        fileHandler.saveFile(thumbnail, postImage);
    }

    private void saveAttachedImages(List<MultipartFile> images, Post post) {
        if (images == null || images.isEmpty()) return;

        images.forEach(image -> {
            if (image == null || image.isEmpty()) return;

            PostAttachedImage imageFile = PostAttachedImage.from(image, post);
            post.addAttachedImage(imageFile);
            fileHandler.saveFile(image, imageFile);
        });
    }

    private void saveAttachedFiles(List<MultipartFile> files, Post post) {
        if (files == null || files.isEmpty()) return;

        files.forEach(file -> {
            if (file == null || file.isEmpty()) return;

            PostAttachedFile attachedFile = PostAttachedFile.from(file, post);
            post.addAttachedFile(attachedFile);
            fileHandler.saveFile(file, attachedFile);
        });
    }

    private void updatePostLabels(Post post, List<String> tagNames, List<String> fieldNames) {
        if (tagNames != null) {
            Set<Tag> tags = tagNames.stream()
                    .map(String::trim)
                    .map(name -> tagRepository.findByName(name)
                            .orElseGet(() -> tagRepository.save(Tag.builder().name(name).build())))
                    .collect(Collectors.toSet());
            post.setTags(tags);
        }

        if (fieldNames != null) {
            List<String> trimFieldNames = fieldNames.stream()
                    .map(String::trim)
                    .collect(Collectors.toList());
            List<Field> existFields = fieldRepository.findAllByNameIn(trimFieldNames);

            post.setFields(new HashSet<>(existFields));
        }
    }

    @Transactional(readOnly = true)
    public PostDto.PostVsDetailResponse getVsPostDetail(
            Long postId,
            UserDetails userDetails
    ) {

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RestException(ErrorCode.POST_NOT_FOUND));

        if (post.getType() != PostType.VS_RECRUIT) {
            throw new RestException(ErrorCode.POST_NOT_FOUND);
        }

        Long userId = userDetails != null
                ? userDetails.getUser().getId()
                : null;

        PostDto.PostVsDetailResponse response =
                postQueryRepository.findVsPostDetail(postId, userId);

        if (response == null) {
            throw new RestException(ErrorCode.POST_NOT_FOUND);
        }

        return response;
    }

}
