package targeter.aim.domain.user.file.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import targeter.aim.common.auditor.TimeStampedEntity;

import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
@SuperBuilder(toBuilder = true)
public abstract class AttachedFile extends TimeStampedEntity {

    @Id
    @Column(nullable = false, updatable = false, length = 36)
    protected String uuid;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private Long size;

    @Column(nullable = false)
    private String filePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "handling_type", nullable = false)
    private HandlingType handlingType;

    // === SuperBuilder용 전체 필드 생성자 ===
    protected AttachedFile(
            String uuid,
            String fileName,
            Long size,
            String filePath,
            HandlingType handlingType
    ) {
        this.uuid = uuid;
        this.fileName = fileName;
        this.size = size;
        this.filePath = filePath;
        this.handlingType = handlingType;
    }

    // === 비즈니스 로직용 생성자 ===
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
}
