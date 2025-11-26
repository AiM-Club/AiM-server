package targeter.aim.domain.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import targeter.aim.domain.auth.dto.AuthDto;
import targeter.aim.domain.auth.service.AuthService;
import targeter.aim.domain.user.dto.UserDto;
import targeter.aim.system.security.annotation.NoJwtAuth;
import targeter.aim.system.security.model.UserDetails;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "인증/인가 API")
public class AuthController {
    private final AuthService authService;

    @NoJwtAuth("회원가입은 인증이 필요하지 않음")
    @PostMapping("/register")
    @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다.")
    @ApiResponse(responseCode = "200", description = "회원가입 성공")
    public UserDto.UserResponse signUp(@ModelAttribute @Valid AuthDto.SignUpRequest request) {
        return authService.signUp(request);
    }

    @NoJwtAuth("로그인은 인증이 필요하지 않음")
    @PostMapping("/login")
    @Operation(summary = "로그인", description = "아이디와 비밀번호로 로그인하여 JWT 토큰을 발급받습니다.")
    @ApiResponse(responseCode = "200", description = "로그인 성공")
    public AuthDto.SignInResponse signIn(@RequestBody @Valid AuthDto.SignInRequest request) {
        return authService.signIn(request);
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "현재 사용자의 Refresh Token을 삭제하며 로그아웃 처리합니다.")
    @ApiResponse(responseCode = "200", description = "로그아웃 성공")
    public ResponseEntity<String> logout(
            @Parameter (hidden = true) @AuthenticationPrincipal UserDetails userDetails, HttpServletRequest request) {
        authService.logout(userDetails, request);
        return ResponseEntity.ok("Logout successful.");
    }
}
