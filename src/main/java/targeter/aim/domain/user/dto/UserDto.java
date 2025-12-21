package targeter.aim.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import targeter.aim.domain.file.dto.FileDto;
import targeter.aim.domain.file.entity.ProfileImage;
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
        @Schema(description = "아이디", example = "user1234")
        private String loginId;

        @Schema(description = "사용자 닉네임", example = "nickname")
        private String nickname;

        @Schema(description = "사용자 생일", example = "YYYY-MM-DD")
        private LocalDate birthday;

        @Schema(description = "사용자 성별", example = "MALE | FEMAIL | OTHER")
        private Gender gender;

        @Schema(
                description = "소셜 로그인 방식 (일반 회원가입 시 null)",
                example = "KAKAO",
                allowableValues = {"KAKAO", "GOOGLE"},
                nullable = true
        )
        private SocialLogin socialLogin;

        @Schema(description = "사용자 프로필 이미지")
        private FileDto.FileResponse profileImage;

        @Schema(description = "계정 생성 시간", example = "ISO Datetime")
        private LocalDateTime createdAt;

        @Schema(description = "마지막 정보 수정 시간", example = "ISO Datetime")
        private LocalDateTime lastModifiedAt;

        public static UserResponse from(User user) {
            return UserResponse.builder()
                    .loginId(user.getLoginId())
                    .nickname(user.getNickname())
                    .birthday(user.getBirthday())
                    .gender(user.getGender())
                    .socialLogin(user.getSocialLogin() == null ? null : user.getSocialLogin())
                    .createdAt(user.getCreatedAt())
                    .lastModifiedAt(user.getLastModifiedAt())
                    .build();
        }
    }

}
