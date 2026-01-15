package targeter.aim.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    @Schema(description = "구글 로그인 요청 DTO")
    public static class GoogleLoginRequest {
        @NotBlank(message = "인가 코드를 입력해주세요.")
        @Schema(description = "Google 인증 서버로부터 받은 인가 코드", example = "4/0AfJohX...")
        private String code;

        @NotBlank(message = "redirectUri를 입력해주세요.")
        @Schema(description = "구글 콘솔에 등록된 Redirect URI (검증용)", example = "http://localhost:5173/oauth/google/callback")
        private String redirectUri;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "회원가입 요청 DTO", requiredProperties = {"login_id", "nickname", "password", "birthday", "gender"})
    public static class SignUpRequest {
        @NotBlank(message = "아이디를 입력해주세요.")
        @Schema(description = "아이디", example = "user1234")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[0-9])[a-z0-9]{8,16}$", message = "아이디는 영문 소문자와 숫자를 각각 1자 이상 포함하며, 8~16자여야 합니다.")
        private String loginId;

        @NotBlank(message = "닉네임을 입력해주세요.")
        @Schema(description = "닉네임", example = "nickname")
        @Pattern(regexp = "^[a-zA-Z0-9가-힣]{1,10}$",
                message = "닉네임은 한글, 영문, 숫자로만 구성되며 1~10자여야 합니다.")
        private String nickname;

        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Schema(description = "비밀번호", example = "password123!")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[0-9])(?=.*[!@#$%^&*?_])[a-z0-9!@#$%^&*?_]{8,16}$", message = "비밀번호는 영문 소문자, 숫자, 특수문자를 각각 1자 이상 포함하며 8~16자여야 합니다.")
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
                    .loginId(loginId)
                    .nickname(nickname)
                    .password(encoder.encode(password))
                    .birthday(birthday)
                    .gender(gender)
                    .build();
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "로그인 요청 DTO")
    public static class SignInRequest {
        @NotBlank(message = "아이디를 입력해주세요.")
        @Schema(description = "아이디", example = "user1234")
        private String loginId;

        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[0-9])(?=.*[!@#$%^&*?_])[a-z0-9!@#$%^&*?_]{8,16}$", message = "비밀번호는 영문 소문자, 숫자, 특수문자를 각각 1자 이상 포함하며 8~16자여야 합니다.")
        @Schema(description = "아이디", example = "password123!")
        private String password;
    }

    //로그인 응답 DTO
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

        public static SignInResponse of(UserDto.UserResponse user, JwtDto.TokenInfo token) {
            return SignInResponse.builder()
                    .user(user)
                    .token(token)
                    .build();
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "소셜 로그인 응답 DTO")
    public static class SocialSignInResponse {
        @Schema(description = "로그인한 사용자 정보", implementation = UserDto.UserResponse.class)
        private UserDto.UserResponse user;

        @Schema(description = "발급된 토큰 정보", implementation = JwtDto.TokenInfo.class)
        private JwtDto.TokenInfo token;

        @Schema(description = "신규 소셜유저 여부(추가 정보 입력 필요)", example = "true")
        private Boolean isNewUser;

        public static SocialSignInResponse of(UserDto.UserResponse user, JwtDto.TokenInfo token, Boolean isNewUser) {
            return SocialSignInResponse.builder()
                    .user(user)
                    .token(token)
                    .isNewUser(isNewUser)
                    .build();
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "아이디 중복 검사 응답 DTO")
    public static class IdExistResponse {
        @Schema(description = "아이디 존재 여부 (true/false)", example = "false")
        private Boolean isExist;

        public static IdExistResponse from(Boolean isExist) {
            return IdExistResponse.builder()
                    .isExist(isExist)
                    .build();
        }
    }

    //중복검사DTO
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "닉네임 중복 검사 응답 DTO")
    public static class NicknameExistResponse {
        @Schema(description = "닉네임 존재 여부 (true/false)", example = "false")
        private Boolean isExist;

        public static NicknameExistResponse from(Boolean isExist) {
            return NicknameExistResponse.builder()
                    .isExist(isExist)
                    .build();
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "카카오 로그인 요청 DTO")
    public static class KakaoLoginRequest {
        @NotBlank(message = "인가 코드를 입력해주세요.")
        @Schema(description = "카카오 인증 서버로부터 받은 인가 코드", example = "authorization_code")
        private String code;

        @NotBlank(message = "redirectUri를 입력해주세요.")
        @Schema(description = "카카오 developers에 등록된 Redirect URI (검증용)", example = "http://localhost:5173/oauth/kakao/callback")
        private String redirectUri;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "카카오 로그인 응답 DTO")
    public static class AuthResponse {
        @Schema(description = "서비스 전용 JWT Access Token")
        private String accessToken;

        @Schema(description = "서비스 전용 JWT Refresh Token")
        private String refreshToken;

        @JsonProperty("isNewUser")
        @Schema(description = "최초 가입 여부")
        private boolean isNewUser;

        @Schema(description = "유저 정보")
        private AuthUserResponse user;

        public static AuthResponse of(
                String accessToken,
                String refreshToken,
                boolean isNewUser,
                AuthUserResponse user
        ) {
            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .isNewUser(isNewUser)
                    .user(user)
                    .build();
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "카카오 로그인 유저 응답 DTO")
    public static class AuthUserResponse {
        private Long id;
        private String email;
        private String nickname;
        private String profileUrl;

        public static AuthUserResponse of(Long id, String email, String nickname, String profileUrl) {
            return AuthUserResponse.builder()
                    .id(id)
                    .email(email)
                    .nickname(nickname)
                    .profileUrl(profileUrl)
                    .build();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KakaoTokenResponse {
        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("token_type")
        private String tokenType;

        @JsonProperty("refresh_token")
        private String refreshToken;

        @JsonProperty("expires_in")
        private Integer expiresIn;

        @JsonProperty("refresh_token_expires_in")
        private Integer refreshTokenExpiresIn;

        private String scope;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KakaoUserResponse {
        private Long id;

        @JsonProperty("kakao_account")
        private KakaoAccount kakaoAccount;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class KakaoAccount {
            private String email;

            private Profile profile;

            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            @Builder
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Profile {
                private String nickname;

                @JsonProperty("profile_image_url")
                private String profileImageUrl;
            }
        }
    }
}