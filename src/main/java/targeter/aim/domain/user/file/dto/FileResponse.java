package targeter.aim.domain.user.file.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import targeter.aim.domain.user.file.entity.AttachedFile;
import targeter.aim.domain.user.file.entity.HandlingType;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileResponse {

    private String uuid;
    private String fileName;
    private Long size;
    private String filePath;
    private HandlingType handlingType;

    public static FileResponse from(AttachedFile file) {
        return FileResponse.builder()
                .uuid(file.getUuid())
                .fileName(file.getFileName())
                .size(file.getSize())
                .filePath(file.getFilePath())
                .handlingType(file.getHandlingType())
                .build();
    }
}
