package targeter.aim.domain.file.handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import targeter.aim.domain.file.entity.HandlingType;
import targeter.aim.domain.file.entity.TestImageFile;
import targeter.aim.system.exception.model.RestException;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileHandlerTest {

    @TempDir
    Path tempDir;

    private FileHandler newHandlerWithTempRoot() {
        FileHandler handler = new FileHandler();
        ReflectionTestUtils.setField(handler, "savePath", tempDir.toString());
        return handler;
    }

    @Test
    @DisplayName("resolve(): ../ 로 루트 탈출 시도는 차단되어야 함")
    void resolve_blocksPathTraversal() {
        FileHandler handler = newHandlerWithTempRoot();

        TestImageFile malicious = TestImageFile.builder()
                .uuid("u-1")
                .handlingType(HandlingType.IMAGE)
                .fileName("a.png")
                .size(1L)
                .filePath("../outside") // 탈출 시도
                .build();

        assertThrows(RestException.class, () -> handler.resolve(malicious));
    }

    @Test
    @DisplayName("save() 후 loadAsResource(): 디스크에 저장되고 읽을 수 있어야 함")
    void save_and_load_success() throws Exception {
        FileHandler handler = newHandlerWithTempRoot();

        // given
        TestImageFile entity = TestImageFile.builder()
                .uuid("u-100")
                .handlingType(HandlingType.IMAGE)
                .fileName("x.png")
                .size(3L)
                .filePath("profile/u-100")
                .build();

        MockMultipartFile multipart = new MockMultipartFile(
                "file",
                "x.png",
                "image/png",
                new byte[]{1,2,3}
        );

        // when
        handler.saveFile(multipart, entity);

        // then (실제 파일 존재 확인)
        Path saved = tempDir.resolve("profile/u-100");
        assertTrue(Files.exists(saved), "saved file should exist on disk");

        var resource = handler.loadAsResource(entity);
        assertNotNull(resource);
        assertTrue(resource.exists());
        assertTrue(resource.isReadable());

        // MIME은 OS에 따라 조금 다를 수 있어 "image/*"만 확인
        var mt = handler.detectMediaType(entity);
        assertNotNull(mt);
        assertEquals("image", mt.getType().toLowerCase());
    }
}
