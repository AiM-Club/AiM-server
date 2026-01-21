package targeter.aim.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import targeter.aim.domain.user.dto.UserDto;
import targeter.aim.domain.user.service.UserService;
import targeter.aim.system.security.model.UserDetails;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Tag(name = "User", description = "유저 관련 API")
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    @Operation(summary = "프로필 조회(프로필 연동용)", description = "현재 로그인된 사용자의 프로필을 조회합니다. 연동 테스트용이기 때문에 수정이 필요합니다.")
    public UserDto.UserProfileResponse getUserProfile(@Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        return userService.getUserProfile(userDetails.getUser().getId());
    }
}
