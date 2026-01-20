package targeter.aim.domain.challenge.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import targeter.aim.domain.challenge.dto.WeeklyCommentDto;
import targeter.aim.domain.challenge.entity.Challenge;
import targeter.aim.domain.challenge.entity.ChallengeMode;
import targeter.aim.domain.challenge.entity.WeeklyComment;
import targeter.aim.domain.challenge.entity.WeeklyProgress;
import targeter.aim.domain.challenge.repository.ChallengeRepository;
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
    public List<WeeklyCommentDto.WeeklyCommentResponse> getWeeklyComments(
            Long challengeId,
            Long weeksId,
            UserDetails userDetails
    ) {
        if (userDetails == null) {
            throw new RestException(ErrorCode.AUTH_LOGIN_REQUIRED);
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

        List<WeeklyComment> parentComments =
                weeklyCommentRepository
                        .findAllByWeeklyProgress_IdAndParentCommentIsNullOrderByCreatedAtAsc(weeksId);

        return parentComments.stream()
                .map(parent -> {
                    WeeklyCommentDto.WeeklyCommentResponse parentDto =
                            WeeklyCommentDto.WeeklyCommentResponse.from(parent);

                    List<WeeklyCommentDto.WeeklyCommentResponse> children =
                            weeklyCommentRepository
                                    .findAllByParentComment_IdOrderByCreatedAtAsc(parent.getId())
                                    .stream()
                                    .map(WeeklyCommentDto.WeeklyCommentResponse::from)
                                    .toList();

                    parentDto.setChildrenComments(children);
                    return parentDto;
                })
                .toList();
    }
}
