package targeter.aim.domain.file.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.web.multipart.MultipartFile;
import targeter.aim.domain.post.entity.Comment;
import targeter.aim.system.exception.model.ErrorCode;
import targeter.aim.system.exception.model.RestException;

import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@DiscriminatorValue("COMMENT_IMAGE")
public class CommentImage extends AttachedFile {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    public static CommentImage from(MultipartFile file, Comment comment) {
        throwIfNotAImageFile(file);

        String uuId = UUID.randomUUID().toString();
        String filePath = uuId + "." + extractExt(file.getOriginalFilename()).toLowerCase();

        return CommentImage.builder()
                .uuid(uuId)
                .handlingType(HandlingType.IMAGE)
                .fileName(safeFilename(file.getOriginalFilename()))
                .size(file.getSize())
                .filePath(filePath)
                .comment(comment)
                .build();
    }

    private static void throwIfNotAImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RestException(ErrorCode.FILE_NOT_FOUND);
        }

        String contentType = file.getContentType();
        if (contentType == null ||
                !contentType.toLowerCase().startsWith("image/")
                        && !contentType.equalsIgnoreCase("application/pdf")) {
            throw new RestException(ErrorCode.FILE_NOT_IMAGE);
        }

        String fileName = file.getOriginalFilename();
        String extension = extractExt(fileName);
        if (extension == null ||
                !List.of("JPG", "JPEG", "PNG", "PDF").contains(extension.toUpperCase())) {
            throw new RestException(ErrorCode.FILE_NOT_READABLE);
        }
    }

    private static String extractExt(String fileName) {
        if (fileName == null) return null;
        int idx = fileName.lastIndexOf('.');
        if (idx < 0 || idx == fileName.length() - 1) return null;
        return fileName.substring(idx + 1).toUpperCase();
    }

    private static String safeFilename(String fileName) {
        if (fileName == null || fileName.isBlank()) return "file";
        return fileName.replace("\r", "_")
                .replace("\n", "_")
                .replace(" ", "_");
    }
}