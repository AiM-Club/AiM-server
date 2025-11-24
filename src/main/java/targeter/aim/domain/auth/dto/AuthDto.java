package targeter.aim.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;
import targeter.aim.domain.user.dto.UserDto;
import targeter.aim.domain.user.entity.Gender;
import targeter.aim.domain.user.entity.User;
import targeter.aim.system.security.model.JwtDto;

import java.time.LocalDate;

public class AuthDto {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "회원가입 요청 DTO",  requiredProperties = {"email", "nickname", "password", "birthday", "gender"})
    public static class SignUpRequest {
        @NotBlank(message = "아이디를 입력해주세요.")
        @Schema(description = "아이디", example = "user1234")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[0-9])[a-z0-9]{8,16}$", message = "아이디는 영문 소문자와 숫자를 각각 1자 이상 포함하며, 8~16자여야 합니다.")
        private String email;

        @NotBlank(message = "닉네임을 입력해주세요.")
        @Schema(description = "닉네임", example = "nickname")
        @Pattern(regexp = "^[a-zA-Z0-9가-힣]{1,10}$",
                message = "닉네임은 한글, 영문, 숫자로만 구성되며 1~10자여야 합니다.")
        private String nickname;

        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Schema(description = "비밀번호", example = "password123!")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[0-9])(?=.*[!@#$%^&*?_])[a-z0-9!@#$%^&*?_]{8,16}$",  message = "비밀번호는 영문 소문자, 숫자, 특수문자를 각각 1자 이상 포함하며 8~16자여야 합니다.")
        private String password;

        @NotNull(message = "생년월일을 입력해주세요.")
        @Schema(description = "생년월일", example = "yyyy-mm-dd")
        private LocalDate birthday;

        @Schema(description = "프로필 이미지")
        private MultipartFile profileImage;

        @NotNull(message = "성별을 입력해주세요.")
        @Schema(
                description = "사용자 성별",
                example = "MALE | FEMALE | OTHER",
                allowableValues = {"MALE", "FEMALE", "OTHER"}
        )
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

    // 로그인 요청 DTO
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "로그인 요청 DTO")
    public static class SignInRequest {
        @NotBlank(message = "아이디를 입력해주세요.")
        @Schema(description = "아이디", example = "user1234")
        private String email;

        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[0-9])(?=.*[!@#$%^&*?_])[a-z0-9!@#$%^&*?_]{8,16}$",  message = "비밀번호는 영문 소문자, 숫자, 특수문자를 각각 1자 이상 포함하며 8~16자여야 합니다.")
        @Schema(description = "아이디", example = "password123!")
        private String password;
    }

    // 로그인 응답 DTO
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "로그인 응답 DTO")
    public static class SignInResponse {
        @Schema(description = "로그인한 사용자 정보", implementation = UserDto.UserResponse.class)
        private UserDto.UserResponse user;

        @Schema(description = "발급된 토큰 정보", implementation = JwtDto.TokenInfo.class)
        private JwtDto.TokenInfo token;

        public static SignInResponse of(
                UserDto.UserResponse user,
                JwtDto.TokenInfo token
        ) {
            return SignInResponse.builder()
                    .user(user)
                    .token(token)
                    .build();
        }
    }
}
