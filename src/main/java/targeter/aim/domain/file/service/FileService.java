package targeter.aim.domain.file.service;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import targeter.aim.domain.file.entity.AttachedFile;
import targeter.aim.domain.file.entity.HandlingType;
import targeter.aim.domain.file.handler.FileHandler;
import targeter.aim.domain.file.repository.AttachedFileRepository;
import targeter.aim.system.exception.model.ErrorCode;
import targeter.aim.system.exception.model.RestException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FileService {

    private final AttachedFileRepository fileRepository;
    private final FileHandler fileHandler;

    public record FileResource(AttachedFile attachedFile, Resource resource, MediaType mediaType) {}

    public FileResource getImage(String uuid) {
        FileResource fr = loadFileAndMetadata(uuid);

        if (fr.attachedFile().getHandlingType() != HandlingType.IMAGE) {
            throw new RestException(ErrorCode.FILE_INVALID_TYPE);
        }
        if (!"image".equalsIgnoreCase(fr.mediaType().getType())) {
            throw new RestException(ErrorCode.FILE_INVALID_TYPE);
        }
        return fr;
    }

    public FileResource downloadFile(String uuid) {
        FileResource fr = loadFileAndMetadata(uuid);

        if (fr.attachedFile().getHandlingType() != HandlingType.DOWNLOADABLE) {
            throw new RestException(ErrorCode.FILE_INVALID_TYPE);
        }
        return fr;
    }

    private FileResource loadFileAndMetadata(String fileUuid) {
        AttachedFile file = fileRepository.findByUuid(fileUuid)
                .orElseThrow(() -> new RestException(ErrorCode.FILE_NOT_FOUND));

        Resource resource = fileHandler.loadAsResource(file);
        MediaType mediaType = fileHandler.detectMediaType(file);

        return new FileResource(file, resource, mediaType);
    }
}
