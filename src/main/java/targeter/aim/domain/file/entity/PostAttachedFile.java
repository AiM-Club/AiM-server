package targeter.aim.domain.file.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.web.multipart.MultipartFile;
import targeter.aim.domain.post.entity.Post;
import targeter.aim.system.exception.model.ErrorCode;
import targeter.aim.system.exception.model.RestException;

import java.util.UUID;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@DiscriminatorValue("POST_ATTACHED_FILE")
public class PostAttachedFile extends AttachedFile {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    public static PostAttachedFile from(MultipartFile file, Post post) {
        throwIfFileIsInvalid(file);

        String uuid = UUID.randomUUID().toString();
        String filePath = uuid + "." + extractExt(file.getOriginalFilename()).toLowerCase();

        return PostAttachedFile.builder()
                .uuid(uuid)
                .handlingType(HandlingType.DOWNLOADABLE)
                .fileName(safeFilename(file.getOriginalFilename()))
                .size(file.getSize())
                .filePath(filePath)
                .post(post)
                .build();
    }

    private static void throwIfFileIsInvalid(MultipartFile file) {
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