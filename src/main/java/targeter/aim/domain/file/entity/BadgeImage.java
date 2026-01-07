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
    @JoinColumn(name = "tier_id")
    private Tier tier;

    public static BadgeImage from(MultipartFile file, Tier tier) {
        throwIfNotAImageFile(file);

        String uuId = UUID.randomUUID().toString();
        String filePath = "badge/" + uuId;

        return BadgeImage.builder()
                .uuid(UUID.randomUUID().toString())
                .fileName(safeFilename(file.getOriginalFilename()))
                .handlingType(HandlingType.IMAGE)
                .tier(tier)
                .size(file.getSize())
                .filePath(filePath)
                .build();
    }

    private static void throwIfNotAImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RestException(ErrorCode.FILE_NOT_IMAGE);
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new RestException(ErrorCode.FILE_NOT_IMAGE);
        }

        String fileName = file.getOriginalFilename();
        String extension = extractExt(fileName);
        if(extension == null || !List.of("JPG", "JPEG", "PNG", "WEBP").contains(extension.toUpperCase())) {
            throw new RestException(ErrorCode.FILE_NOT_IMAGE);
        }
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
