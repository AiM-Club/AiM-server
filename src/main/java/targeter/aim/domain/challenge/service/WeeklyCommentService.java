package targeter.aim.domain.challenge.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import targeter.aim.domain.challenge.dto.WeeklyCommentDto;
import targeter.aim.domain.challenge.entity.Challenge;
import targeter.aim.domain.challenge.entity.ChallengeMode;
import targeter.aim.domain.challenge.entity.WeeklyComment;
import targeter.aim.domain.challenge.entity.WeeklyProgress;
import targeter.aim.domain.challenge.repository.ChallengeRepository;
import targeter.aim.domain.challenge.repository.CommentSortType;
import targeter.aim.domain.challenge.repository.SortOrder;
import targeter.aim.domain.challenge.repository.WeeklyCommentRepository;
import targeter.aim.domain.challenge.repository.WeeklyProgressRepository;
import targeter.aim.domain.file.entity.ChallengeCommentAttachedFile;
import targeter.aim.domain.file.entity.ChallengeCommentImage;
import targeter.aim.domain.file.entity.ChallengeProofAttachedFile;
import targeter.aim.domain.file.entity.ChallengeProofImage;
import targeter.aim.domain.file.handler.FileHandler;
import targeter.aim.domain.user.entity.User;
import targeter.aim.system.exception.model.ErrorCode;
import targeter.aim.system.exception.model.RestException;
import targeter.aim.system.security.model.UserDetails;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WeeklyCommentService {

    private final ChallengeRepository challengeRepository;
    private final WeeklyProgressRepository weeklyProgressRepository;
    private final WeeklyCommentRepository weeklyCommentRepository;

    private final FileHandler fileHandler;

    @Transactional
    public WeeklyCommentDto.WeeklyCommentCreateResponse createWeeklyComment(
            Long challengeId,
            Long weeksId,
            WeeklyCommentDto.WeeklyCommentCreateRequest request,
            UserDetails userDetails
    ) {
        if (userDetails == null) {
            throw new RestException(ErrorCode.AUTH_LOGIN_REQUIRED);
        }
        User loginUser = userDetails.getUser();

        if (request.getContent() == null || request.getContent().length() > 50) {
            throw new RestException(ErrorCode.GLOBAL_BAD_REQUEST);
        }

        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new RestException(ErrorCode.GLOBAL_NOT_FOUND));
        WeeklyProgress weeklyProgress = weeklyProgressRepository.findById(weeksId)
                .orElseThrow(() -> new RestException(ErrorCode.GLOBAL_NOT_FOUND));

        if (!weeklyProgress.getChallenge().getId().equals(challenge.getId())) {
            throw new RestException(ErrorCode.GLOBAL_BAD_REQUEST);
        }

        WeeklyComment toSave = request.toEntity();
        toSave.setUser(loginUser);
        toSave.setWeeklyProgress(weeklyProgress);

        if(request.getParentCommentId() == null) {
            toSave.setDepth(1);
            toSave.setParentComment(null);
        } else {
            Long parentCommentId = request.getParentCommentId();
            WeeklyComment parent = weeklyCommentRepository.findById(parentCommentId)
                    .orElseThrow(() -> new RestException(ErrorCode.GLOBAL_NOT_FOUND, "부모 댓글을 찾을 수 없습니다."));

            if(!parent.getWeeklyProgress().getId().equals(weeksId)) {
                throw new RestException(ErrorCode.GLOBAL_BAD_REQUEST, "부모 댓글이 다른 주차의 댓글입니다.");
            }

            if(parent.getParentComment() != null || parent.getDepth() == null || parent.getDepth() != 1 ) {
                throw new RestException(ErrorCode.GLOBAL_BAD_REQUEST, "대댓글 아래에 대댓글을 작성할 수 없습니다.");
            }

            toSave.setDepth(2);
            toSave.setParentComment(parent);
        }

        WeeklyComment saved = weeklyCommentRepository.save(toSave);

        saveAttachedImages(request.getAttachedImages(), saved);
        saveAttachedFiles(request.getAttachedFiles(), saved);

        return WeeklyCommentDto.WeeklyCommentCreateResponse.builder()
                .challengeId(challengeId)
                .weeksId(weeksId)
                .commentId(saved.getId())
                .depth(saved.getDepth())
                .build();
    }

    private void saveAttachedImages(List<MultipartFile> files, WeeklyComment weeklyComment) {
        if (files == null || files.isEmpty())
            return;

        files.stream()
                .filter(f -> f != null && !f.isEmpty())
                .forEach(file -> {
                    ChallengeCommentImage image = ChallengeCommentImage.from(file, weeklyComment);
                    image.setWeeklyComment(weeklyComment);
                    weeklyComment.addAttachedImage(image);
                    fileHandler.saveFile(file, image);
                });
    }

    private void saveAttachedFiles(List<MultipartFile> files, WeeklyComment weeklyComment) {
        if (files == null || files.isEmpty())
            return;

        files.stream()
                .filter(f -> f != null && !f.isEmpty())
                .forEach(file -> {
                    ChallengeCommentAttachedFile attached = ChallengeCommentAttachedFile.from(file, weeklyComment);
                    attached.setWeeklyComment(weeklyComment);
                    weeklyComment.addAttachedFile(attached);
                    fileHandler.saveFile(file, attached);
                });
    }

    @Transactional(readOnly = true)
    public WeeklyCommentDto.WeeklyCommentListResponse getWeeklyComments(
            Long challengeId,
            Long weeksId,
            CommentSortType sort,
            SortOrder order,
            int page,
            int size,
            UserDetails userDetails
    ) {
        if (userDetails == null) {
            throw new RestException(ErrorCode.AUTH_LOGIN_REQUIRED);
        }

        if (page < 0 || size < 1) {
            throw new RestException(ErrorCode.GLOBAL_INVALID_PARAMETER);
        }

        if (sort == CommentSortType.LATEST && order != SortOrder.DESC) {
            throw new RestException(ErrorCode.GLOBAL_INVALID_PARAMETER);
        }

        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new RestException(ErrorCode.GLOBAL_NOT_FOUND));

        if (challenge.getMode() != ChallengeMode.VS) {
            throw new RestException(ErrorCode.GLOBAL_BAD_REQUEST);
        }

        WeeklyProgress weeklyProgress = weeklyProgressRepository.findById(weeksId)
                .orElseThrow(() -> new RestException(ErrorCode.GLOBAL_NOT_FOUND));

        if (!weeklyProgress.getChallenge().getId().equals(challengeId)) {
            throw new RestException(ErrorCode.GLOBAL_BAD_REQUEST);
        }

        Sort parentSort = Sort.by(Sort.Direction.DESC, "createdAt");
        PageRequest pageRequest = PageRequest.of(page, size, parentSort);

        Page<WeeklyComment> parentPage =
                weeklyCommentRepository.findAllByWeeklyProgress_IdAndParentCommentIsNull(weeksId, pageRequest);

        List<WeeklyCommentDto.WeeklyCommentResponse> content = parentPage.getContent().stream()
                .map(parent -> {
                    WeeklyCommentDto.WeeklyCommentResponse parentDto =
                            WeeklyCommentDto.WeeklyCommentResponse.from(parent);

                    List<WeeklyCommentDto.WeeklyCommentResponse> children =
                            weeklyCommentRepository
                                    .findAllByParentComment_Id(parent.getId(), Sort.by(Sort.Direction.DESC, "createdAt"))
                                    .stream()
                                    .map(WeeklyCommentDto.WeeklyCommentResponse::from)
                                    .toList();

                    parentDto.setChildrenComments(children);
                    return parentDto;
                })
                .toList();

        return WeeklyCommentDto.WeeklyCommentListResponse.of(parentPage, content);
    }
}