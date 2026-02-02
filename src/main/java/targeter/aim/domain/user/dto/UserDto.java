package targeter.aim.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;
import targeter.aim.domain.file.dto.FileDto;
import targeter.aim.domain.user.entity.Gender;
import targeter.aim.domain.user.entity.SocialLogin;
import targeter.aim.domain.user.entity.User;

import java.time.LocalDate;
import java.util.List;

public class UserDto {

    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    public static class UserResponse {
        @Schema(description = "유저 아이디(DB PK)", example = "1")
        private Long id;

        @Schema(description = "아이디", example = "user1234")
        private String loginId;

        @Schema(description = "이메일", example = "user@gmail.com")
        private String email;

        @Schema(description = "사용자 닉네임", example = "nickname")
        private String nickname;

        @Schema(description = "사용자 생일", example = "YYYY-MM-DD", nullable = true)
        private LocalDate birthday;

        @Schema(description = "사용자 성별", example = "MALE | FEMALE | OTHER", nullable = true)
        private Gender gender;

        @Schema(description = "사용자 티어")
        private TierDto.TierResponse tier;

        @Schema(description = "사용자 레벨")
        private Integer level;

        @Schema(
                description = "소셜 로그인 방식 (일반 회원가입 시 null)",
                example = "KAKAO | GOOGLE",
                nullable = true
        )
        private SocialLogin socialLogin;

        @Schema(description = "신규 소셜유저 여부(추가 정보 입력 필요)", example = "true")
        private Boolean isNewUser;

        @Schema(description = "사용자 프로필 이미지")
        private FileDto.FileResponse profileImage;

        public static UserResponse from(User user) {
            boolean isNewUser = user.isSocialUser()
                    && (user.getBirthday() == null || user.getGender() == null);

            return UserResponse.builder()
                    .id(user.getId())
                    .loginId(user.getLoginId())
                    .email(user.getEmail())
                    .nickname(user.getNickname())
                    .birthday(user.getBirthday())
                    .gender(user.getGender())
                    .tier(TierDto.TierResponse.from(user.getTier()))
                    .level(user.getLevel())
                    .profileImage(FileDto.FileResponse.from(user.getProfileImage()))
                    .socialLogin(user.getSocialLogin() == null ? null : user.getSocialLogin())
                    .isNewUser(isNewUser)
                    .build();
        }
    }

    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    public static class RankTop10Response {

        @Schema(description = "순위", example = "1")
        private Integer rank;

        @Schema(description = "유저 아이디(DB PK)", example = "1")
        private Long userId;

        @Schema(description = "사용자 닉네임", example = "nickname")
        private String nickname;

        @Schema(description = "레벨", example = "10")
        private Integer level;

        public static RankTop10Response of(Integer rank, User user) {
            return RankTop10Response.builder()
                    .rank(rank)
                    .userId(user.getId())
                    .nickname(user.getNickname())
                    .level(user.getLevel())
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MyPageResponse {

        @Schema(description = "현재 레벨", example = "37")
        private int level;

        @Schema(description = "현재 티어", example = "SILVER")
        private TierDto.TierResponse tier;

        @Schema(description = "티어 진행률 (%)", example = "45")
        private int tierProgressPercent;

        @Schema(
                description = "다음 티어 (다이아몬드인 경우 null)",
                example = "GOLD",
                nullable = true
        )
        private TierDto.TierResponse nextTier;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "프로필 조회 응답")
    public static class ProfileResponse {

        @Schema(description = "유저 아이디(DB PK)", example = "1")
        private Long userId;

        @Schema(description = "프로필 이미지")
        private FileDto.FileResponse profileImage;

        @Schema(description = "유저 닉네임", example = "닉네임")
        private String nickname;

        @Schema(description = "로그인 아이디", example = "testuser1")
        private String loginId;

        @Schema(description = "티어 정보")
        private TierDto.TierResponse tier;

        @Schema(description = "레벨", example = "1")
        private Integer level;

        @Schema(description = "관심사(태그)", example = "[\"태그1\"]")
        private List<String> tags;

        @Schema(description = "관심 분야", example = "[\"IT\"]")
        private List<String> fields;

        @Schema(description = "전체 챌린지 기록")
        private ChallengeRecord allChallengeRecord;

        @Schema(description = "SOLO 기록")
        private ChallengeRecord soloChallengeRecord;

        @Schema(description = "VS 기록")
        private ChallengeRecord vsChallengeRecord;

        @Schema(description = "본인 프로필 여부")
        private Boolean isMine;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "챌린지 기록(시도/성공/실패/성공률)")
    public static class ChallengeRecord {

        @Schema(description = "성공률(0~100)")
        private Double successRate;

        @Schema(description = "시도 횟수", example = "30")
        private Long attemptCount;

        @Schema(description = "성공 횟수", example = "20")
        private Long successCount;

        @Schema(description = "실패 횟수", example = "10")
        private Long failCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "챌린지 기록(시도/성공/실패/성공률)")
    public static class UpdateProfileRequest {
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

        public void applyTo(User user, PasswordEncoder encoder) {
            if(loginId != null && !loginId.equals(user.getLoginId())) {
                user.setLoginId(loginId);
            }
            if(nickname != null && !nickname.equals(user.getNickname())) {
                user.setNickname(nickname);
            }
            if(password != null) {
                user.setPassword(encoder.encode(password));
            }
            if(birthday != null) {
                user.setBirthday(birthday);
            }
            if(gender != null) {
                user.setGender(gender);
            }
        }
    }
}