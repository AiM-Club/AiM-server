package targeter.aim.domain.file.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.web.multipart.MultipartFile;
import targeter.aim.domain.user.entity.Tier;
import targeter.aim.system.exception.model.ErrorCode;
import targeter.aim.system.exception.model.RestException;

import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@DiscriminatorValue("BADGE_IMAGE")
public class BadgeImage extends AttachedFile {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tier_id", nullable = false)
    private Tier tier;

    public static BadgeImage from(MultipartFile file, Tier tier, String filePath) {
        throwIfNotAImageFile(file);

        String fileName = file.getOriginalFilename();

        return BadgeImage.builder()
                .uuid(UUID.randomUUID().toString())
                .fileName(fileName)
                .handlingType(HandlingType.IMAGE)
                .tier(tier)
                .size(file.getSize())
                .filePath(filePath)
                .build();
    }

    private static void throwIfNotAImageFile(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            throw new RestException(ErrorCode.FILE_NOT_IMAGE);
        }

        String[] splitted = fileName.split("\\.");
        if (splitted.length < 2) {
            throw new RestException(ErrorCode.FILE_NOT_IMAGE);
        }

        String extension = splitted[splitted.length - 1];
        if (!List.of("JPG", "JPEG", "PNG").contains(extension.toUpperCase())) {
            throw new RestException(ErrorCode.FILE_NOT_IMAGE);
        }
    }
}
