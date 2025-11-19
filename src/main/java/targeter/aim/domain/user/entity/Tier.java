package targeter.aim.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import targeter.aim.common.auditor.TimeStampedEntity;

@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "TIER")
public class Tier extends TimeStampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

//    뱃지 이미지 컬럼. File 도메인 완성 시 구현
//    @OneToOne(mappedBy = "tier", cascade = CascadeType.ALL, orphanRemoval = true)
//    @JoinColumn(name = "badge_image_id")
//    private BadgeImage badgeImage;
}
