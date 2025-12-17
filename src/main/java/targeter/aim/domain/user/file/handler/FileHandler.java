package targeter.aim.domain.user.file.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import targeter.aim.domain.user.file.entity.AttachedFile;
import targeter.aim.domain.user.file.exception.FileStorageException;
import targeter.aim.domain.user.file.repository.AttachedFileRepository;
import targeter.aim.system.exception.model.ErrorCode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FileHandler {

    private final AttachedFileRepository fileRepository;

    public record FileResource(AttachedFile attachedFile, Resource resource) {
    }

    public Optional<FileResource> loadFileAndMetadata(String fileUuid) {
        AttachedFile attachedFile = fileRepository.findByUuid(fileUuid)
                .orElse(null);

        if (attachedFile == null) {
            return Optional.empty();
        }

        Path path = Paths.get(attachedFile.getFilePath());

        if (!Files.exists(path)) {
            throw new FileStorageException(ErrorCode.FILE_METADATA_BUT_DISK_NOT_FOUND);
        }

        try {
            Resource resource = new FileSystemResource(path.toFile());

            if (resource.exists() && resource.isReadable()) {
                return Optional.of(new FileResource(attachedFile, resource));
            } else {
                throw new FileStorageException(ErrorCode.FILE_NOT_READABLE);
            }

        } catch (SecurityException e) {
            throw new FileStorageException(ErrorCode.FILE_ACCESS_DENIED);
        }
    }

    public ResponseEntity<Resource> createViewImageResponse(FileResource fileResource) {
        Resource resource = fileResource.resource();

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(resource);
    }

    public ResponseEntity<Resource> createDownloadResponse(FileResource fileResource) {
        AttachedFile file = fileResource.attachedFile();
        Resource resource = fileResource.resource();

        String contentDisposition = "attachment; filename=\"" + file.getFileName() + "\"";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(resource);
    }
}
