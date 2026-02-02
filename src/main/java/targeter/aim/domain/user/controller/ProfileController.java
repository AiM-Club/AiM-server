package targeter.aim.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import targeter.aim.domain.user.dto.ProfileDto;
import targeter.aim.domain.user.service.ProfileService;
import targeter.aim.system.exception.model.ErrorCode;
import targeter.aim.system.exception.model.RestException;
import targeter.aim.system.security.model.UserDetails;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Tag(name = "Profile", description = "프로필 조회/수정 API")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/me/profile")
    @Operation(summary = "내 프로필 조회", description = "내 프로필(유저 정보 + 통계 + 관심사/분야)을 조회합니다.")
    public ProfileDto.ProfileResponse getMyProfile(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (userDetails == null) {
            throw new RestException(ErrorCode.AUTH_LOGIN_REQUIRED);
        }

        return profileService.getProfile(userDetails.getUser().getId(), userDetails);
    }

    @GetMapping("/{userId}/profile")
    @Operation(summary = "유저 프로필 조회", description = "특정 유저의 프로필(유저 정보 + 통계 + 관심사/분야)을 조회합니다.")
    public ProfileDto.ProfileResponse getUserProfile(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return profileService.getProfile(userId, userDetails);
    }

    @PatchMapping(value = "/me/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "내 프로필 수정", description = "내 프로필(닉네임/관심사/관심분야/프로필사진)을 수정합니다.")
    public ProfileDto.ProfileResponse updateMyProfile(
            @ModelAttribute ProfileDto.ProfileUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (userDetails == null) {
            throw new RestException(ErrorCode.AUTH_LOGIN_REQUIRED);
        }

        return profileService.updateMyProfile(request, userDetails);
    }
}
