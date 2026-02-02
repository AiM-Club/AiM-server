package targeter.aim.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import targeter.aim.domain.user.dto.UserDto;
import targeter.aim.domain.user.service.UserService;
import targeter.aim.system.exception.model.ErrorCode;
import targeter.aim.system.exception.model.RestException;
import targeter.aim.system.security.annotation.NoJwtAuth;
import targeter.aim.system.security.model.UserDetails;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Tag(name = "User", description = "유저 관련 API")
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    @Operation(summary = "프로필 조회(테스트용, 실사용 X)", description = "현재 로그인된 사용자의 프로필을 조회합니다. 연동 테스트용이기 때문에 수정이 필요합니다.")
    public UserDto.UserResponse getUserProfile(@Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        return userService.getUserProfile(userDetails.getUser().getId());
    }

    @NoJwtAuth
    @GetMapping("/rank/top10")
    @Operation(summary = "TOP 10 유저 랭킹 조회", description = "홈 화면에 노출되는 레벨 상위 10명의 유저 랭킹을 반환합니다.")
    public List<UserDto.RankTop10Response> getTop10UserRank() {
        return userService.getTop10UserRank();
    }

    @GetMapping("/mypage")
    @Operation(
            summary = "마이페이지 레벨/티어 조회",
            description = "로그인한 사용자의 레벨, 티어, 티어 진행률, 다음 티어 정보를 조회합니다."
    )
    public UserDto.MyPageResponse getMyPage(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return userService.getMyPage(userDetails);
    }

    @GetMapping("/me/profile")
    @Operation(summary = "내 프로필 조회", description = "내 프로필(유저 정보 + 통계 + 관심사/분야)을 조회합니다.")
    public UserDto.ProfileResponse getMyProfile(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (userDetails == null) {
            throw new RestException(ErrorCode.AUTH_LOGIN_REQUIRED);
        }

        return userService.getProfile(userDetails.getUser().getId(), userDetails);
    }

    @GetMapping("/{userId}/profile")
    @Operation(summary = "유저 프로필 조회", description = "특정 유저의 프로필(유저 정보 + 통계 + 관심사/분야)을 조회합니다.")
    public UserDto.ProfileResponse getUserProfile(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return userService.getProfile(userId, userDetails);
    }
}
