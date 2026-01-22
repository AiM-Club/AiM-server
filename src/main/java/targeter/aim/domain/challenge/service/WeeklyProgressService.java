package targeter.aim.domain.challenge.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import targeter.aim.domain.challenge.dto.WeeklyProgressDto;
import targeter.aim.domain.challenge.entity.Challenge;
import targeter.aim.domain.challenge.entity.ChallengeMode;
import targeter.aim.domain.challenge.entity.WeeklyProgress;
import targeter.aim.domain.challenge.repository.ChallengeRepository;
import targeter.aim.domain.challenge.repository.WeeklyProgressRepository;
import targeter.aim.domain.file.entity.ChallengeProofAttachedFile;
import targeter.aim.domain.file.entity.ChallengeProofImage;
import targeter.aim.domain.file.handler.FileHandler;
import targeter.aim.domain.user.entity.User;
import targeter.aim.system.exception.model.ErrorCode;
import targeter.aim.system.exception.model.RestException;
import targeter.aim.system.security.model.UserDetails;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WeeklyProgressService {

    private final ChallengeRepository challengeRepository;
    private final WeeklyProgressRepository weeklyProgressRepository;
    private final FileHandler fileHandler;

    @Transactional(readOnly = true)
    public WeeklyProgressDto.WeekProgressListResponse getVsWeeklyProgressList(
            Long challengeId,
            UserDetails userDetails
    ) {
        if (userDetails == null) {
            throw new RestException(ErrorCode.AUTH_LOGIN_REQUIRED);
        }
        User loginUser = userDetails.getUser();

        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new RestException(ErrorCode.GLOBAL_NOT_FOUND));

        if (challenge.getMode() != ChallengeMode.VS) {
            throw new RestException(ErrorCode.GLOBAL_BAD_REQUEST);
        }

        List<WeeklyProgress> weeklyProgressList =
                weeklyProgressRepository.findAllByChallengeAndUser(challenge, loginUser);

        int currentWeek = calcCurrentWeek(challenge.getStartedAt(), challenge.getDurationWeek());

        return WeeklyProgressDto.WeekProgressListResponse.from(
                challenge, currentWeek, weeklyProgressList
        );
    }

    private int calcCurrentWeek(LocalDate startedAt, int totalWeeks) {
        long days = Duration.between(
                startedAt.atStartOfDay(),
                LocalDate.now().atStartOfDay()
        ).toDays();

        int week = (int) (days / 7) + 1;
        return Math.min(Math.max(week, 1), totalWeeks);
    }

    @Transactional
    public void uploadProofFiles(WeeklyProgressDto.ProofUploadRequest request) {
        WeeklyProgress weeklyProgress = weeklyProgressRepository.findById(request.getWeeklyProgressId())
                .orElseThrow(() -> new RestException(ErrorCode.GLOBAL_NOT_FOUND, "해당하는 주차별 챌린지 내용을 찾을 수 없습니다."));

        saveAttachedImages(request.getAttachedImages(), weeklyProgress);
        saveAttachedFiles(request.getAttachedFiles(), weeklyProgress);
    }

    private void saveAttachedImages(List<MultipartFile> files, WeeklyProgress weeklyProgress) {
        if (files == null || files.isEmpty())
            return;

        files.stream()
                .filter(f -> f != null && !f.isEmpty())
                .forEach(file -> {
                    ChallengeProofImage image = ChallengeProofImage.from(file, weeklyProgress);
                    image.setWeeklyProgress(weeklyProgress);
                    weeklyProgress.addAttachedImage(image);
                    fileHandler.saveFile(file, image);
        });
    }

    private void saveAttachedFiles(List<MultipartFile> files, WeeklyProgress weeklyProgress) {
        if (files == null || files.isEmpty())
            return;

        files.stream()
                .filter(f -> f != null && !f.isEmpty())
                .forEach(file -> {
                    ChallengeProofAttachedFile attached = ChallengeProofAttachedFile.from(file, weeklyProgress);
                    attached.setWeeklyProgress(weeklyProgress);
                    weeklyProgress.addAttachedFile(attached);
                    fileHandler.saveFile(file, attached);
        });
    }
}