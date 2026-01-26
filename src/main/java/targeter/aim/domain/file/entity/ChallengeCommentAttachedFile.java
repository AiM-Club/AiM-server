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
import targeter.aim.domain.challenge.entity.WeeklyComment;

import java.util.UUID;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@DiscriminatorValue("CHALLENGE_COMMENT_ATTACHED_FILE")
public class ChallengeCommentAttachedFile extends AttachedFile {
    @ManyToOne(fetch = FetchType.LAZY)
    private WeeklyComment weeklyComment;

    public static ChallengeCommentAttachedFile from(MultipartFile file, WeeklyComment weeklyComment) {
        String uuId = UUID.randomUUID().toString();
        String filePath = uuId + "." + extractExt(file.getOriginalFilename()).toLowerCase();

        return ChallengeCommentAttachedFile.builder()
                .uuid(uuId)
                .fileName(safeFilename(file.getOriginalFilename()))
                .handlingType(HandlingType.DOWNLOADABLE)
                .weeklyComment(weeklyComment)
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
