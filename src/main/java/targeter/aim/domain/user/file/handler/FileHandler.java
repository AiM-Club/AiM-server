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
        // 1. 메타데이터(엔티티) 조회
        AttachedFile attachedFile = fileRepository.findByUuid(fileUuid)
                .orElse(null);

        if (attachedFile == null) {
            // 컨트롤러/서비스에서 404로 매핑할 수 있도록 Optional.empty()
            return Optional.empty();
        }

        // 2. 파일 시스템에서 Resource 로드
        Path path = Paths.get(attachedFile.getFilePath());

        if (!Files.exists(path)) {
            // DB에는 있지만 파일 시스템에 없는 경우 (일관성 오류)
            throw new FileStorageException("File metadata exists, but file not found on disk: " + attachedFile.getFilePath());
        }

        try {
            Resource resource = new FileSystemResource(path.toFile());

            if (resource.exists() && resource.isReadable()) {
                // 3. 파일 엔티티와 Resource를 함께 반환
                return Optional.of(new FileResource(attachedFile, resource));
            } else {
                // 파일이 존재하지만 읽기 권한이 없는 경우
                throw new FileStorageException("File exists but is not readable: " + attachedFile.getFilePath());
            }

        } catch (SecurityException e) {
            // 파일 권한 문제 등 런타임 예외가 발생할 경우
            throw new FileStorageException("Access denied or unexpected error loading file: " + attachedFile.getFilePath(), e);
        }
    }

    public ResponseEntity<Resource> createViewImageResponse(FileResource fileResource) {
        Resource resource = fileResource.resource();

        return ResponseEntity.ok()
                // 이미지 조회 스펙상 image/jpeg로 고정
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