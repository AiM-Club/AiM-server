package targeter.aim.domain.user.file.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@DiscriminatorValue("TEST_IMAGE") // attached_file.type 컬럼 값
@SuperBuilder(toBuilder = true)
public class TestImageFile extends AttachedFile {

    // 테스트용 이미지 엔티티: handlingType은 항상 IMAGE
    public TestImageFile(String fileName, Long size, String filePath) {
        super(fileName, size, filePath, HandlingType.IMAGE);
    }
}
