package targeter.aim.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import targeter.aim.common.auditor.TimeStampedEntity;

import java.time.LocalDate;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
@Table(name = "USER_ACCOUNT", uniqueConstraints = {
        @UniqueConstraint(name = "USER_LOGIN_ID", columnNames = "login_id"),
        @UniqueConstraint(name = "USER_SOCIAL_ID", columnNames = "social_id"),
        @UniqueConstraint(name = "USER_NICKNAME", columnNames = "nickname")
})
public class User extends TimeStampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(length = 50, name = "login_id")
    private String loginId;     // 자체 로그인 계정 아이디, 소셜 로그인 시 null

    @Column(length = 60)
    private String password;    // 소셜 로그인 시 null

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

    @Column(name = "social_id")
    private String socialId;    // 구글 / 카카오 연동 아이디

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "tier_id")
    private Tier tier;  // 1~30 : Bronze, 31~60 : Silver, 61~80 : Gold, 81~100 : Diamond

//    프로필 이미지 컬럼. File 도메인 완성 시 구현
//    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
//    @JoinColumn(name = "profile_image_id")
//    private ProfileImage profileImage;

    public boolean isLocalUser() {
        return this.socialLogin == null;
    }

    public boolean isSocialUser() {
        return this.socialLogin != null;
    }
}
