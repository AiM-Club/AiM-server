package targeter.aim.domain.user.file.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import targeter.aim.domain.user.file.entity.AttachedFile;
import targeter.aim.domain.user.file.service.FileService;
import targeter.aim.system.security.annotation.NoJwtAuth;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/files")
@Tag(name = "File", description = "파일 다운로드/조회 API")
public class FileController {

    private final FileService fileService;

    @NoJwtAuth("이미지 조회는 공개 리소스로 인증이 필요하지 않음")
    @GetMapping("/images/{file_uuid}")
    @Operation(summary = "이미지 조회", description = "특정 아이디로 저장된 이미지를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "이미지 조회 성공")
    public ResponseEntity<Resource> viewImage(@PathVariable("file_uuid") String fileUuid) {
        FileService.FileResource fr = fileService.getImage(fileUuid);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(fr.resource());
    }

    @NoJwtAuth("파일 다운로드는 공개 리소스로 인증이 필요하지 않음")
    @GetMapping("/downloads/{file_uuid}")
    @Operation(summary = "파일 다운로드", description = "특정 아이디로 저장된 파일을 다운로드합니다.")
    @ApiResponse(responseCode = "200", description = "파일 다운로드 성공")
    public ResponseEntity<Resource> downloadFile(@PathVariable("file_uuid") String fileUuid) {
        FileService.FileResource fr = fileService.downloadFile(fileUuid);
        AttachedFile file = fr.attachedFile();

        String contentDisposition = "attachment; filename=\"" + file.getFileName() + "\"";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(fr.resource());
    }
}
