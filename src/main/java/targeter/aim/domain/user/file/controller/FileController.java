package targeter.aim.domain.user.file.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import targeter.aim.domain.user.file.service.FileService;
import targeter.aim.system.security.annotation.NoJwtAuth;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/files")
@Tag(name = "File", description = "파일 다운로드/조회 API")
public class FileController {

    private final FileService fileService;


    @NoJwtAuth("이미지 조회는 공개 리소스로 인증이 필요하지 않음")
    @GetMapping("/images/{file_uuId}")
    @Operation(summary = "이미지 조회", description = "특정 아이디로 저장된 이미지를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "이미지 조회 성공")
    public ResponseEntity<Resource> viewImage(@PathVariable("file_uuId") String fileUuid) {
        return fileService.getImage(fileUuid);
    }


    @NoJwtAuth("파일 다운로드는 공개 리소스로 인증이 필요하지 않음")
    @GetMapping("/downloads/{file_uuId}")
    @Operation(summary = "파일 다운로드", description = "특정 아이디로 저장된 파일을 다운로드합니다.")
    @ApiResponse(responseCode = "200", description = "파일 다운로드 성공")
    public ResponseEntity<Resource> downloadFile(@PathVariable("file_uuId") String fileUuid) {
        return fileService.downloadFile(fileUuid);
    }
}
