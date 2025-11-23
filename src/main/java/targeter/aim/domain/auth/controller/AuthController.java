package targeter.aim.domain.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import targeter.aim.domain.auth.dto.AuthDto;
import targeter.aim.domain.auth.service.AuthService;
import targeter.aim.domain.user.dto.UserDto;
import targeter.aim.system.security.annotation.NoJwtAuth;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    @NoJwtAuth("회원가입은 인증이 필요하지 않음")
    @PostMapping("/register")
    @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다.")
    @ApiResponse(responseCode = "200", description = "회원가입 성공")
    public UserDto.UserResponse signUp(@ModelAttribute @Valid AuthDto.SignUpRequest request) {
        return authService.signUp(request);
    }
}
