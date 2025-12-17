package targeter.aim.domain.user.file.service;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import targeter.aim.domain.user.file.entity.AttachedFile;
import targeter.aim.domain.user.file.entity.HandlingType;
import targeter.aim.domain.user.file.exception.FileHandlingException;
import targeter.aim.domain.user.file.handler.FileHandler;
import targeter.aim.system.exception.model.ErrorCode;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FileService {

    private final FileHandler fileHandler;

    public ResponseEntity<Resource> getImage(String uuid) {
        FileHandler.FileResource fileResource = fileHandler.loadFileAndMetadata(uuid)
                .orElseThrow(() -> new FileHandlingException(ErrorCode.FILE_NOT_FOUND));

        AttachedFile file = fileResource.attachedFile();

        if (file.getHandlingType() != HandlingType.IMAGE) {
            throw new FileHandlingException(ErrorCode.FILE_INVALID_TYPE);
        }

        return fileHandler.createViewImageResponse(fileResource);
    }

    public ResponseEntity<Resource> downloadFile(String uuid) {
        FileHandler.FileResource fileResource = fileHandler.loadFileAndMetadata(uuid)
                .orElseThrow(() -> new FileHandlingException(ErrorCode.FILE_NOT_FOUND));

        AttachedFile file = fileResource.attachedFile();

        if (file.getHandlingType() != HandlingType.DOWNLOADABLE) {
            throw new FileHandlingException(ErrorCode.FILE_INVALID_TYPE);
        }

        return fileHandler.createDownloadResponse(fileResource);
    }
}
