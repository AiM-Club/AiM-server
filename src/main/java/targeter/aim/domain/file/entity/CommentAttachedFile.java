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

import java.util.UUID;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@DiscriminatorValue("COMMENT_FILE")
public class CommentAttachedFile extends AttachedFile {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    private Comment comment;

    public static CommentAttachedFile from(MultipartFile file, Comment comment) {
        throwIfInvalidFile(file);

        String uuId = UUID.randomUUID().toString();
        String original = file.getOriginalFilename();
        String ext = extractExt(original);
        String filePath = (ext == null) ? uuId : (uuId + "." + ext.toLowerCase());

        return CommentAttachedFile.builder()
                .uuid(uuId)
                .handlingType(HandlingType.DOWNLOADABLE)
                .fileName(safeFilename(original))
                .size(file.getSize())
                .filePath(filePath)
                .comment(comment)
                .build();
    }

    private static void throwIfInvalidFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RestException(ErrorCode.FILE_NOT_FOUND);
        }
    }

    private static String extractExt(String fileName) {
        if (fileName == null) return null;
        int idx = fileName.lastIndexOf('.');
        if (idx < 0 || idx == fileName.length() - 1) return null;
        return fileName.substring(idx + 1);
    }

    private static String safeFilename(String fileName) {
        if (fileName == null || fileName.isBlank()) return "file";
        return fileName.replace("\r", "_")
                .replace("\n", "_")
                .replace(" ", "_");
    }
}