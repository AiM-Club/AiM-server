package targeter.aim.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import targeter.aim.common.auditor.TimeStampedEntity;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "UserAccout")
public class User extends TimeStampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    private String email;   // 계정 아이디 + 구글/카카오 이메일

    private String password;

    private String nickname;
}
