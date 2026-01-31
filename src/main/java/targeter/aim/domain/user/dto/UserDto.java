package targeter.aim.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import targeter.aim.domain.file.dto.FileDto;
import targeter.aim.domain.user.entity.Gender;
import targeter.aim.domain.user.entity.SocialLogin;
import targeter.aim.domain.user.entity.User;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

        @Schema(description = "계정 생성 시간", example = "ISO Datetime")
        private LocalDateTime createdAt;

        @Schema(description = "마지막 정보 수정 시간", example = "ISO Datetime")
        private LocalDateTime lastModifiedAt;

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
                    .createdAt(user.getCreatedAt())
                    .lastModifiedAt(user.getLastModifiedAt())
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
}