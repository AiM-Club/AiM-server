package targeter.aim.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import targeter.aim.common.auditor.TimeStampedEntity;
import targeter.aim.domain.file.entity.BadgeImage;

import java.util.ArrayList;
import java.util.List;

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

    @OneToMany(mappedBy = "tier")
    @Builder.Default
    private List<User> users = new ArrayList<>();

    @OneToOne(mappedBy = "tier", cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "badge_image_id")
    private BadgeImage badgeImage;
}
