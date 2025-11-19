package targeter.aim.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import targeter.aim.common.auditor.TimeStampedEntity;

import java.time.LocalDate;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "USER_ACCOUNT", uniqueConstraints = {
        @UniqueConstraint(name = "USER_EMAIL", columnNames = "email"),
        @UniqueConstraint(name = "USER_NICKNAME", columnNames = "nickname")
})
public class User extends TimeStampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(nullable = false, length = 50)
    private String email;   // 계정 아이디 + 구글/카카오 이메일

    @Column(nullable = false, length = 50)
    private String password;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(nullable = false)
    private LocalDate birthday;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(length = 50, name = "social_login")
    private SocialLogin socialLogin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "tier_id")
    private Tier tier;  // 1~30 : Bronze, 31~60 : Silver, 61~80 : Gold, 81~100 : Diamond

//    프로필 이미지 컬럼. File 도메인 완성 시 구현
//    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
//    @JoinColumn(name = "profile_image_id")
//    private ProfileImage profileImage;
}
