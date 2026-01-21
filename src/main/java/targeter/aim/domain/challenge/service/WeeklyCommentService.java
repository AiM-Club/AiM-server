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

    @Transactional
    public WeeklyCommentDto.WeeklyCommentCreateResponse createWeeklyComment(
            Long challengeId,
            Long weeksId,
            String content,
            List<MultipartFile> attachedImages,
            List<MultipartFile> attachedFiles,
            UserDetails userDetails
    ) {
        if (userDetails == null) {
            throw new RestException(ErrorCode.AUTH_LOGIN_REQUIRED);
        }
        User loginUser = userDetails.getUser();

        if (content == null || content.length() > 50) {
            throw new RestException(ErrorCode.GLOBAL_BAD_REQUEST);
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

        WeeklyComment weeklyComment = WeeklyComment.builder()
                .weeklyProgress(weeklyProgress)
                .user(loginUser)
                .content(content)
                .depth(1)
                .build();

        weeklyCommentRepository.save(weeklyComment);

        return WeeklyCommentDto.WeeklyCommentCreateResponse.builder()
                .challengeId(challengeId)
                .weeksId(weeksId)
                .build();
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