package targeter.aim.domain.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import targeter.aim.domain.auth.dto.OAuth2Dto;
import targeter.aim.domain.auth.service.OAuth2Service;
import targeter.aim.system.security.annotation.NoJwtAuth;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "OAuth2", description = "구글/카카오 로그인 API")
public class OAuth2Controller {

    private final OAuth2Service authService;

    @NoJwtAuth("구글 로그인은 인증이 필요하지 않음")
    @PostMapping("/login/google")
    @Operation(summary = "구글 로그인", description = "인가 코드를 구글에 전달해 토큰 교환 후 서비스 JWT를 발급합니다.")
    @ApiResponse(responseCode = "200", description = "구글 로그인 성공")
    public OAuth2Dto.SocialSignInResponse loginGoogle(@RequestBody @Valid OAuth2Dto.GoogleLoginRequest request) {
        return authService.loginGoogle(request);
    }

    @NoJwtAuth("카카오 로그인은 인증이 필요하지 않음")
    @PostMapping("/login/kakao")
    @Operation(summary = "카카오 로그인", description = "카카오 인가 코드로 로그인/회원가입을 처리하고 JWT 토큰을 발급합니다.")
    @ApiResponse(responseCode = "200", description = "카카오 로그인 성공")
    public OAuth2Dto.SocialSignInResponse loginWithKakao(@RequestBody @Valid OAuth2Dto.KakaoLoginRequest request) {
        return authService.loginWithKakao(request);
    }
}
