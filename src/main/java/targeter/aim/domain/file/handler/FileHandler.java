package targeter.aim.domain.file.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import targeter.aim.domain.file.entity.AttachedFile;
import targeter.aim.system.exception.model.ErrorCode;
import targeter.aim.system.exception.model.RestException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Component
@RequiredArgsConstructor
public class FileHandler {

    @Value("${file.save-path}")
    private String savePath;

    private Path uploadRootPath() {
        return Paths.get(savePath).toAbsolutePath().normalize();
    }

    // filePath를 실제 디스크 Path로 변환
    public Path resolve(AttachedFile file) {
        Path root = uploadRootPath();
        Path resolved = root.resolve(file.getFilePath()).normalize();

        // 디렉토리 탈출 방지
        if(!resolved.startsWith(root)) {
            throw new RestException(ErrorCode.FILE_ACCESS_DENIED);
        }

        return resolved;
    }

    public void saveFile(MultipartFile multipartFile, AttachedFile file) {
        Path target = resolve(file);

        try {
            Files.createDirectories(target.getParent());

            if(Files.exists(target)) {
                throw new RestException(ErrorCode.FILE_ALREADY_EXIST);
            }

            try (InputStream in = multipartFile.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new RestException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    public Resource loadAsResource(AttachedFile file) {
        Path target = resolve(file);

        if (!Files.exists(target)) {
            throw new RestException(ErrorCode.FILE_METADATA_BUT_DISK_NOT_FOUND);
        }

        Resource resource = new FileSystemResource(target.toFile());
        if (!resource.exists() || !resource.isReadable()) {
            throw new RestException(ErrorCode.FILE_NOT_READABLE);
        }

        return resource;
    }

    public void deleteIfExists(AttachedFile file) {
        Path target = resolve(file);

        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new RestException(ErrorCode.FILE_DELETE_FAILED);
        }
    }

    // MIME 타입 추정
    public MediaType detectMediaType(AttachedFile file) {
        Path path = resolve(file);

        // 1. OS 기반 추적
        try {
            String probed = Files.probeContentType(path);
            if(probed != null && !probed.isBlank()) {
                return MediaType.parseMediaType(probed);
            }
        } catch (IOException ignored) {}

        // 2. 원본 파일명 기반 추적
        return MediaTypeFactory.getMediaType(file.getFileName())
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
    }
}
