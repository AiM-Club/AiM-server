package targeter.aim.domain.user.file.service;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import targeter.aim.domain.user.file.entity.AttachedFile;
import targeter.aim.domain.user.file.entity.HandlingType;
import targeter.aim.domain.user.file.exception.FileHandlingException;
import targeter.aim.domain.user.file.exception.FileNotFoundException;
import targeter.aim.domain.user.file.handler.FileHandler;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FileService {

    private final FileHandler fileHandler;

    public ResponseEntity<Resource> getImage(String uuid) {
        FileHandler.FileResource fileResource = fileHandler.loadFileAndMetadata(uuid)
                .orElseThrow(() ->
                        new FileNotFoundException("이미지 파일을 찾을 수 없습니다. (UUID: " + uuid + ")"));

        AttachedFile file = fileResource.attachedFile();

        if (file.getHandlingType() != HandlingType.IMAGE) {
            throw new FileHandlingException(
                    "요청된 파일은 이미지 타입이 아닌 " + file.getHandlingType() + " 타입입니다.");
        }

        // image/jpeg + body 설정은 FileHandler에 위임
        return fileHandler.createViewImageResponse(fileResource);
    }


    public ResponseEntity<Resource> downloadFile(String uuid) {
        FileHandler.FileResource fileResource = fileHandler.loadFileAndMetadata(uuid)
                .orElseThrow(() ->
                        new FileNotFoundException("다운로드 파일을 찾을 수 없습니다. (UUID: " + uuid + ")"));

        AttachedFile file = fileResource.attachedFile();

        if (file.getHandlingType() != HandlingType.DOWNLOADABLE) {
            throw new FileHandlingException(
                    "요청된 파일은 다운로드 가능한 타입이 아닌 " + file.getHandlingType() + " 타입입니다.");
        }

        // Content-Disposition, application/octet-stream 설정은 FileHandler에 위임
        return fileHandler.createDownloadResponse(fileResource);
    }
}
