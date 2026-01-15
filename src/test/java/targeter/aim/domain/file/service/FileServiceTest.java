package targeter.aim.domain.file.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import targeter.aim.domain.file.entity.TestImageFile;
import targeter.aim.domain.file.entity.HandlingType;
import targeter.aim.domain.file.handler.FileHandler;
import targeter.aim.domain.file.repository.AttachedFileRepository;
import targeter.aim.system.exception.model.RestException;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class FileServiceTest {

    @TempDir
    Path tempDir;

    private FileHandler newHandlerWithTempRoot() {
        FileHandler handler = new FileHandler();
        ReflectionTestUtils.setField(handler, "savePath", tempDir.toString());
        return handler;
    }

    @Test
    @DisplayName("getImage(): DB 메타 조회 + 디스크 로드가 정상이어야 함")
    void getImage_success() {
        // given
        AttachedFileRepository repo = Mockito.mock(AttachedFileRepository.class);
        FileHandler handler = newHandlerWithTempRoot();
        FileService service = new FileService(repo, handler);

        MockMultipartFile multipart = new MockMultipartFile(
                "file",
                "ok.png",
                "image/png",
                new byte[]{1,2,3}
        );

        // 엔티티 생성(from()으로 생성)
        TestImageFile entity = TestImageFile.from(multipart);

        // 실제 디스크 저장
        handler.saveFile(multipart, entity);

        when(repo.findByUuid(entity.getUuid())).thenReturn(Optional.of(entity));

        // when
        FileService.FileResource fr = service.getImage(entity.getUuid());

        // then
        assertNotNull(fr);
        assertEquals(entity.getUuid(), fr.attachedFile().getUuid());
        assertNotNull(fr.resource());
        assertEquals("image", fr.mediaType().getType().toLowerCase());
    }

    @Test
    @DisplayName("getImage(): DB에는 있는데 filePath가 DOWNLOADABLE이면 INVALID_TYPE")
    void getImage_fail_whenHandlingTypeMismatch() {
        AttachedFileRepository repo = Mockito.mock(AttachedFileRepository.class);
        FileHandler handler = newHandlerWithTempRoot();
        FileService service = new FileService(repo, handler);

        TestImageFile entity = TestImageFile.builder()
                .uuid("u-200")
                .handlingType(HandlingType.DOWNLOADABLE) // mismatch
                .fileName("a.bin")
                .size(1L)
                .filePath("profile/u-200")
                .build();

        when(repo.findByUuid("u-200")).thenReturn(Optional.of(entity));

        // service.getImage() 내부에서 handler.loadAsResource()를 타므로
        // 디스크 파일이 없으면 먼저 FILE_METADATA_BUT_DISK_NOT_FOUND가 날 수 있음.
        // 타입 mismatch만 테스트하고 싶으면 디스크 파일을 먼저 만들어줘야 한다.
        // 여기서는 간단히 디스크 저장(더미) 만들어서 타입 mismatch로 떨어지게 한다.
        handler.saveFile(new MockMultipartFile("file", "a.bin", "application/octet-stream", new byte[]{1}), entity);

        assertThrows(RestException.class, () -> service.getImage("u-200"));
    }
}
