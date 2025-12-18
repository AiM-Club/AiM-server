package targeter.aim.domain.user.file.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(
        name = "type",
        discriminatorType = DiscriminatorType.STRING
)
@SuperBuilder(toBuilder = true)
public abstract class AttachedFile {

    // 외부 노출용 UUID (PK)
    @Id
    @Column(name = "uuid", nullable = false, updatable = false, length = 36)
    protected String uuid;

    // 원본 파일명
    @Column(nullable = false)
    private String fileName;

    // 파일 크기 (바이트)
    @Column(nullable = false)
    private Long size;

    // 서버 내 저장 경로
    @Column(nullable = false)
    private String filePath;

    // 파일 처리 유형 (DOWNLOADABLE or IMAGE)
    @Enumerated(EnumType.STRING)
    @Column(name = "handling_type", nullable = false)
    private HandlingType handlingType;

    // 파일 업로드 시각
    @Column(name = "uploaded_at", updatable = false)
    private LocalDateTime uploadedAt;

    // === SuperBuilder용 전체 필드 생성자 ===
    protected AttachedFile(
            String uuid,
            String fileName,
            Long size,
            String filePath,
            HandlingType handlingType,
            LocalDateTime uploadedAt
    ) {
        this.uuid = uuid;
        this.fileName = fileName;
        this.size = size;
        this.filePath = filePath;
        this.handlingType = handlingType;
        this.uploadedAt = uploadedAt;
    }

    // === 비즈니스 로직에서 사용하는 UUID 자동 생성 생성자 ===
    protected AttachedFile(
            String fileName,
            Long size,
            String filePath,
            HandlingType handlingType
    ) {
        this.uuid = UUID.randomUUID().toString();
        this.fileName = fileName;
        this.size = size;
        this.filePath = filePath;
        this.handlingType = handlingType;
    }

    // === 업로드 시간 자동 세팅 ===
    @PrePersist
    protected void onCreate() {
        if (this.uploadedAt == null) {
            this.uploadedAt = LocalDateTime.now();
        }
    }
}

