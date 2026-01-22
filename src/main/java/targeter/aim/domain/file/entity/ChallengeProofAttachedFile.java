package targeter.aim.domain.file.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.web.multipart.MultipartFile;
import targeter.aim.domain.challenge.entity.WeeklyProgress;
import targeter.aim.system.exception.model.ErrorCode;
import targeter.aim.system.exception.model.RestException;

import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@DiscriminatorValue("CHALLENGE_PROOF_ATTACHED_FILE")
public class ChallengeProofAttachedFile extends AttachedFile {
    @ManyToOne(fetch = FetchType.LAZY)
    private WeeklyProgress weeklyProgress;

    public static ChallengeProofAttachedFile from(MultipartFile file, WeeklyProgress weeklyProgress) {
        String uuId = UUID.randomUUID().toString();
        String filePath = uuId + "." + extractExt(file.getOriginalFilename()).toLowerCase();

        return ChallengeProofAttachedFile.builder()
                .uuid(uuId)
                .fileName(safeFilename(file.getOriginalFilename()))
                .handlingType(HandlingType.DOWNLOADABLE)
                .weeklyProgress(weeklyProgress)
                .size(file.getSize())
                .filePath(filePath)
                .build();
    }

    private static String extractExt(String fileName) {
        if(fileName == null) return null;
        int idx = fileName.lastIndexOf('.');
        if(idx < 0 || idx == fileName.length() - 1) return null;
        return fileName.substring(idx + 1).toUpperCase();
    }

    private static String safeFilename(String fileName) {
        if(fileName == null || fileName.isBlank()) return "file";
        return fileName.replace("\r", "_")
                .replace("\n", "_")
                .replace(" ", "_");
    }
}
