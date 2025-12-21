package targeter.aim.domain.file.service;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import targeter.aim.domain.file.entity.AttachedFile;
import targeter.aim.domain.file.entity.HandlingType;
import targeter.aim.domain.file.exception.FileHandlingException;
import targeter.aim.domain.file.exception.FileStorageException;
import targeter.aim.domain.file.repository.AttachedFileRepository;
import targeter.aim.system.exception.model.ErrorCode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FileService {

    private final AttachedFileRepository fileRepository;

    public record FileResource(AttachedFile attachedFile, Resource resource) {}

    public FileResource getImage(String uuid) {
        FileResource fr = loadFileAndMetadata(uuid);

        if (fr.attachedFile().getHandlingType() != HandlingType.IMAGE) {
            throw new FileHandlingException(ErrorCode.FILE_INVALID_TYPE);
        }
        return fr;
    }

    public FileResource downloadFile(String uuid) {
        FileResource fr = loadFileAndMetadata(uuid);

        if (fr.attachedFile().getHandlingType() != HandlingType.DOWNLOADABLE) {
            throw new FileHandlingException(ErrorCode.FILE_INVALID_TYPE);
        }
        return fr;
    }

    private FileResource loadFileAndMetadata(String fileUuid) {
        AttachedFile attachedFile = fileRepository.findByUuid(fileUuid)
                .orElseThrow(() -> new FileHandlingException(ErrorCode.FILE_NOT_FOUND));

        Path path = Paths.get(attachedFile.getFilePath());
        if (!Files.exists(path)) {
            throw new FileStorageException(ErrorCode.FILE_METADATA_BUT_DISK_NOT_FOUND);
        }

        try {
            Resource resource = new FileSystemResource(path.toFile());
            if (!resource.exists() || !resource.isReadable()) {
                throw new FileStorageException(ErrorCode.FILE_NOT_READABLE);
            }
            return new FileResource(attachedFile, resource);
        } catch (SecurityException e) {
            throw new FileStorageException(ErrorCode.FILE_ACCESS_DENIED);
        }
    }
}
