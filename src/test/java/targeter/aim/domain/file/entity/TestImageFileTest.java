package targeter.aim.domain.file.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import targeter.aim.system.exception.model.RestException;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class TestImageFileTest {

    private static final Pattern PROFILE_PATH_PATTERN =
            Pattern.compile("^profile/[0-9a-fA-F\\-]{36}$");

    @Test
    @DisplayName("from(): 정상 이미지 파일이면 IMAGE 타입 + profile/{uuid} 경로 + 파일명 sanitize")
    void from_success() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "my pic.png",
                "image/png",
                new byte[]{1, 2, 3}
        );

        TestImageFile entity = TestImageFile.from(file);

        assertNotNull(entity);
        assertEquals(HandlingType.IMAGE, entity.getHandlingType());
        assertNotNull(entity.getUuid());
        assertNotNull(entity.getFileName());
        assertEquals(3L, entity.getSize());

        // filePath = profile/{uuid} 인지 검증
        assertTrue(PROFILE_PATH_PATTERN.matcher(entity.getFilePath()).matches(),
                "filePath should be like profile/{uuid}");

        // 공백이 _로 바뀌는지(너의 safeFilename 구현 기준)
        assertEquals("my_pic.png", entity.getFileName());
    }

    @Test
    @DisplayName("from(): file이 null이면 RestException(FILE_NOT_IMAGE)")
    void from_fail_whenNull() {
        assertThrows(RestException.class, () -> TestImageFile.from(null));
    }

    @Test
    @DisplayName("from(): empty 파일이면 RestException(FILE_NOT_IMAGE)")
    void from_fail_whenEmpty() {
        MockMultipartFile empty = new MockMultipartFile(
                "file",
                "a.png",
                "image/png",
                new byte[]{} // empty
        );
        assertThrows(RestException.class, () -> TestImageFile.from(empty));
    }

    @Test
    @DisplayName("from(): content-type이 image/*가 아니면 RestException(FILE_NOT_IMAGE)")
    void from_fail_whenNotImageContentType() {
        MockMultipartFile pdf = new MockMultipartFile(
                "file",
                "a.png",
                "application/pdf",
                new byte[]{1}
        );
        assertThrows(RestException.class, () -> TestImageFile.from(pdf));
    }

    @Test
    @DisplayName("from(): 확장자가 없으면 RestException(FILE_NOT_IMAGE)")
    void from_fail_whenNoExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "noext",
                "image/png",
                new byte[]{1}
        );
        assertThrows(RestException.class, () -> TestImageFile.from(file));
    }

    @Test
    @DisplayName("from(): 허용되지 않은 확장자면 RestException(FILE_NOT_IMAGE)")
    void from_fail_whenBadExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "evil.exe",
                "image/png",     // content-type만 image여도 확장자 검사에서 걸려야 함
                new byte[]{1}
        );
        assertThrows(RestException.class, () -> TestImageFile.from(file));
    }
}
