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
import targeter.aim.system.security.utility.validator.ValidatorUtil;

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

    @NoJwtAuth("카카오 로그인은 인증이 필요하지 않음")
    @PostMapping("/login/kakao")
    @Operation(summary = "카카오 로그인", description = "카카오 인가 코드로 로그인/회원가입을 처리하고 JWT 토큰을 발급합니다.")
    @ApiResponse(responseCode = "200", description = "카카오 로그인 성공")
    public AuthDto.AuthResponse loginWithKakao(@RequestBody @Valid AuthDto.KakaoLoginRequest request) {
        return authService.loginWithKakao(request);
    }

    @NoJwtAuth("아이디 검증은 인증이 필요하지 않음")
    @GetMapping("/id-exist")
    @Operation(summary = "아이디 중복 검사", description = "입력된 아이디의 중복 여부를 확인합니다.")
    @ApiResponse(responseCode = "200", description = "아이디 중복 검사 성공")
    public AuthDto.IdExistResponse checkId(@RequestParam("id") String loginId) {
        ValidatorUtil.validateId(loginId);
        return authService.checkId(loginId);
    }

    @NoJwtAuth("닉네임 검증은 인증이 필요하지 않음")
    @GetMapping("/nickname-exist")
    @Operation(summary = "닉네임 중복 검사", description = "입력된 닉네임의 중복 여부를 확인합니다.")
    @ApiResponse(responseCode = "200", description = "닉네임 중복 검사 성공")
    public AuthDto.NicknameExistResponse checkNickname(@RequestParam("nickname") String nickname) {
        ValidatorUtil.validateNickname(nickname);
        return authService.checkNickname(nickname);
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "현재 사용자의 Refresh Token을 삭제하며 로그아웃 처리합니다.")
    @ApiResponse(responseCode = "200", description = "로그아웃 성공")
    public ResponseEntity<String> logout(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails, HttpServletRequest request) {
        authService.logout(userDetails, request);
        return ResponseEntity.ok("Logout successful.");
    }
}