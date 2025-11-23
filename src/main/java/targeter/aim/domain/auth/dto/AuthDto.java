package targeter.aim.domain.auth.dto;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;
import targeter.aim.domain.user.entity.Gender;
import targeter.aim.domain.user.entity.User;

import java.time.LocalDate;

public class AuthDto {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class SignUpRequest {
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[0-9])[a-z0-9]{8,16}$", message = "아이디는 영문 소문자와 숫자를 각각 1자 이상 포함하며, 8~16자여야 합니다.")
        private String email;
        private String nickname;
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[0-9])(?=.*[!@#$%^&*?_])[a-z0-9!@#$%^&*?_]{8,16}$",  message = "비밀번호는 영문 소문자, 숫자, 특수문자를 각각 1자 이상 포함하며 8~16자여야 합니다.")
        private String password;
        private LocalDate birthday;
        private MultipartFile profileImage;
        private Gender gender;

        public User toEntity(PasswordEncoder encoder) {
            return User.builder()
                    .email(email)
                    .nickname(nickname)
                    .password(encoder.encode(password))
                    .birthday(birthday)
                    .gender(gender)
                    .build();
        }
    }
}
