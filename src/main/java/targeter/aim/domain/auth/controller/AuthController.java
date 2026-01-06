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
import targeter.aim.domain.challenge.dto.ChallengeDto;
import targeter.aim.domain.challenge.service.ChallengeRoutePersistService;
import targeter.aim.domain.user.dto.UserDto;
import targeter.aim.system.security.annotation.NoJwtAuth;
import targeter.aim.system.security.model.UserDetails;
import targeter.aim.system.security.utility.validator.ValidatorUtil;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "Auth", description = "인증/인가 API")
public class AuthController {

    private final AuthService authService;
    private final ChallengeRoutePersistService challengeRoutePersistService;

    @NoJwtAuth("회원가입은 인증이 필요하지 않음")
    @PostMapping("/auth/register")
    @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다.")
    @ApiResponse(responseCode = "200", description = "회원가입 성공")
    public UserDto.UserResponse signUp(@ModelAttribute @Valid AuthDto.SignUpRequest request) {
        return authService.signUp(request);
    }

    @NoJwtAuth("로그인은 인증이 필요하지 않음")
    @PostMapping("/auth/login")
    @Operation(summary = "로그인", description = "아이디와 비밀번호로 로그인하여 JWT 토큰을 발급받습니다.")
    @ApiResponse(responseCode = "200", description = "로그인 성공")
    public AuthDto.SignInResponse signIn(@RequestBody @Valid AuthDto.SignInRequest request) {
        return authService.signIn(request);
    }

    @NoJwtAuth("구글 로그인은 인증이 필요하지 않음")
    @PostMapping("/auth/login/google")
    @Operation(summary = "구글 로그인", description = "인가 코드를 구글에 전달해 토큰 교환 후 서비스 JWT를 발급합니다.")
    @ApiResponse(responseCode = "200", description = "구글 로그인 성공")
    public AuthDto.SocialSignInResponse loginGoogle(@RequestBody @Valid AuthDto.GoogleLoginRequest request) {
        return authService.loginGoogle(request);
    }

    @NoJwtAuth("아이디 검증은 인증이 필요하지 않음")
    @GetMapping("/auth/id-exist")
    @Operation(summary = "아이디 중복 검사", description = "입력된 아이디의 중복 여부를 확인합니다.")
    @ApiResponse(responseCode = "200", description = "아이디 중복 검사 성공")
    public AuthDto.IdExistResponse checkId(@RequestParam("id") String loginId) {
        ValidatorUtil.validateId(loginId);
        return authService.checkId(loginId);
    }

    @NoJwtAuth("닉네임 검증은 인증이 필요하지 않음")
    @GetMapping("/auth/nickname-exist")
    @Operation(summary = "닉네임 중복 검사", description = "입력된 닉네임의 중복 여부를 확인합니다.")
    @ApiResponse(responseCode = "200", description = "닉네임 중복 검사 성공")
    public AuthDto.NicknameExistResponse checkNickname(@RequestParam("nickname") String nickname) {
        ValidatorUtil.validateNickname(nickname);
        return authService.checkNickname(nickname);
    }

    @PostMapping("/auth/logout")
    @Operation(summary = "로그아웃", description = "현재 사용자의 Refresh Token을 삭제하며 로그아웃 처리합니다.")
    @ApiResponse(responseCode = "200", description = "로그아웃 성공")
    public ResponseEntity<String> logout(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request
    ) {
        authService.logout(userDetails, request);
        return ResponseEntity.ok("Logout successful.");
    }

    @GetMapping("/challenge/vs/{challengeId}")
    @Operation(
            summary = "VS 챌린지 상세 조회",
            description = "특정 VS 챌린지의 상세 정보와 현재 주차 진행 현황, 상대방 상태를 조회합니다.",
            tags = {"Challenge"}
    )
    @ApiResponse(responseCode = "200", description = "VS 챌린지 상세 조회 성공")
    public ChallengeDto.VsChallengeDetailResponse getVsChallengeDetail(
            @PathVariable Long challengeId,
            @RequestParam(value = "filterType", required = false, defaultValue = "ALL") String filterType,
            @RequestParam(value = "sort", required = false, defaultValue = "created_at") String sort,
            @RequestParam(value = "order", required = false, defaultValue = "desc") String order,
            @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "16") Integer size,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails
    ) {
        return challengeRoutePersistService.getVsChallengeDetail(
                challengeId,
                userDetails,
                filterType,
                sort,
                order,
                page,
                size
        );
    }
}