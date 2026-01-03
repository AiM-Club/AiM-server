package targeter.aim.domain.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import targeter.aim.domain.auth.dto.AuthDto;
import targeter.aim.domain.auth.token.entity.RefreshToken;
import targeter.aim.domain.auth.token.entity.dto.RefreshTokenDto;
import targeter.aim.domain.auth.token.repository.RefreshTokenCacheRepository;
import targeter.aim.domain.auth.token.repository.RefreshTokenRepository;
import targeter.aim.domain.auth.token.validator.RefreshTokenValidator;
import targeter.aim.domain.user.dto.UserDto;
import targeter.aim.domain.user.entity.SocialLogin;
import targeter.aim.domain.user.entity.Tier;
import targeter.aim.domain.user.entity.User;
import targeter.aim.domain.user.repository.TierRepository;
import targeter.aim.domain.user.repository.UserRepository;
import targeter.aim.system.exception.model.ErrorCode;
import targeter.aim.system.exception.model.RestException;
import targeter.aim.system.security.model.JwtDto;
import targeter.aim.system.security.model.UserDetails;
import targeter.aim.system.security.utility.jwt.JwtTokenProvider;
import targeter.aim.system.security.utility.jwt.JwtTokenResolver;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TierRepository tierRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtTokenResolver jwtTokenResolver;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenCacheRepository refreshTokenCacheRepository;
    private final RefreshTokenValidator refreshTokenValidator;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String kakaoClientId;

    @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
    private String kakaoClientSecret;

    @Value("${spring.security.oauth2.client.provider.kakao.token-uri}")
    private String kakaoTokenUri;

    @Value("${spring.security.oauth2.client.provider.kakao.user-info-uri}")
    private String kakaoUserInfoUri;

    @Transactional
    public UserDto.UserResponse signUp(AuthDto.SignUpRequest request) {
        boolean isExisting = userRepository.existsByLoginId(request.getLoginId());
        if (isExisting){
            throw new RestException(ErrorCode.USER_ALREADY_LOGIN_ID_EXISTS);
        }
        User toSave = request.toEntity(passwordEncoder);
        Tier bronze = tierRepository.findByName("BRONZE")
                .orElseThrow(() -> new RestException(ErrorCode.TIER_NOT_FOUND));
        toSave.setTier(bronze);

        User saved = userRepository.save(toSave);

        return UserDto.UserResponse.from(saved);
    }

    @Transactional
    public AuthDto.SignInResponse signIn(@Valid AuthDto.SignInRequest request) {
        var found = userRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> new RestException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), found.getPassword()))
            throw new RestException(ErrorCode.AUTH_PASSWORD_NOT_MATCH);

        var userDetails = UserDetails.from(found);
        var tokenPair = jwtTokenProvider.createTokenPair(userDetails);

        String refreshUuid = tokenPair.getRefreshToken().getTokenString();
        RefreshToken refreshToken = RefreshTokenDto.toEntity(
                refreshUuid,
                userDetails.getKey(),
                tokenPair.getRefreshToken().getExpireAt());

        refreshTokenRepository.save(refreshToken);
        refreshTokenCacheRepository.cacheRefreshUuid(refreshUuid,userDetails.getKey());

        return AuthDto.SignInResponse.of(UserDto.UserResponse.from(found), JwtDto.TokenInfo.of(tokenPair));
    }

    @Transactional(readOnly = true)
    public AuthDto.IdExistResponse checkId(String loginId) {
        boolean exists = userRepository.existsByLoginId(loginId);
        return AuthDto.IdExistResponse.from(exists);
    }

    @Transactional(readOnly = true)
    public AuthDto.NicknameExistResponse checkNickname(String nickname) {
        boolean exists = userRepository.existsByNickname(nickname);
        return AuthDto.NicknameExistResponse.from(exists);
    }

    @Transactional
    public void logout(UserDetails userDetails, HttpServletRequest request) {
        String accessToken = getAccessTokenFromRequest(request);
        String refreshUuid = jwtTokenResolver.resolveTokenFromString(accessToken).getRefreshUuid();

        refreshTokenValidator.validateOrThrow(userDetails.getKey(), refreshUuid);
        refreshTokenRepository.deleteByUuid(refreshUuid);
        refreshTokenCacheRepository.evictRefreshUuid(refreshUuid);
    }

    private String getAccessTokenFromRequest(HttpServletRequest request) {
        return jwtTokenResolver.parseTokenFromRequest(request)
                .orElseThrow(() -> new RestException(ErrorCode.AUTH_TOKEN_MISSING));
    }

    @Transactional
    public AuthDto.AuthResponse loginWithKakao(@Valid AuthDto.KakaoLoginRequest request) {
        String kakaoAccessToken = requestKakaoAccessToken(request.getCode(), request.getRedirectUri());
        AuthDto.KakaoUserResponse kakaoUser = requestKakaoUserInfo(kakaoAccessToken);

        String socialId = String.valueOf(kakaoUser.getId());

        Optional<User> existing = userRepository.findBySocialLoginAndSocialId(SocialLogin.KAKAO, socialId);

        String email = null;
        String nicknameFromKakao = null;
        String profileUrl = null;

        if (kakaoUser.getKakaoAccount() != null) {
            email = kakaoUser.getKakaoAccount().getEmail();
            if (kakaoUser.getKakaoAccount().getProfile() != null) {
                nicknameFromKakao = kakaoUser.getKakaoAccount().getProfile().getNickname();
                profileUrl = kakaoUser.getKakaoAccount().getProfile().getProfileImageUrl();
            }
        }

        boolean isNewUser = false;
        User user;

        if (existing.isPresent()) {
            user = existing.get();
        } else {
            isNewUser = true;

            Tier bronze = tierRepository.findByName("BRONZE")
                    .orElseThrow(() -> new RestException(ErrorCode.TIER_NOT_FOUND));

            user = User.builder()
                    .nickname(generateUniqueNickname(nicknameFromKakao))
                    .socialLogin(SocialLogin.KAKAO)
                    .socialId(socialId)
                    .tier(bronze)
                    .build();

            user = userRepository.save(user);
        }

        var userDetails = UserDetails.from(user);
        var tokenPair = jwtTokenProvider.createTokenPair(userDetails);

        String refreshUuid = tokenPair.getRefreshToken().getTokenString();
        RefreshToken refreshToken = RefreshTokenDto.toEntity(
                refreshUuid,
                userDetails.getKey(),
                tokenPair.getRefreshToken().getExpireAt());

        refreshTokenRepository.save(refreshToken);
        refreshTokenCacheRepository.cacheRefreshUuid(refreshUuid, userDetails.getKey());

        return AuthDto.AuthResponse.of(
                tokenPair.getAccessToken().getTokenString(),
                tokenPair.getRefreshToken().getTokenString(),
                isNewUser,
                AuthDto.AuthUserResponse.of(
                        user.getId(),
                        email,
                        user.getNickname(),
                        profileUrl
                )
        );
    }

    private String requestKakaoAccessToken(String code, String redirectUri) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", kakaoClientId);
        body.add("redirect_uri", redirectUri);
        body.add("code", code);

        if (kakaoClientSecret != null && !kakaoClientSecret.isBlank() && !"imsi-secret".equals(kakaoClientSecret)) {
            body.add("client_secret", kakaoClientSecret);
        }

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<AuthDto.KakaoTokenResponse> response =
                    restTemplate.exchange(kakaoTokenUri, HttpMethod.POST, request, AuthDto.KakaoTokenResponse.class);

            AuthDto.KakaoTokenResponse responseBody = response.getBody();
            if (responseBody == null || responseBody.getAccessToken() == null || responseBody.getAccessToken().isBlank()) {
                throw new RestException(ErrorCode.AUTH_KAKAO_TOKEN_REQUEST_FAILED);
            }
            return responseBody.getAccessToken();
        } catch (RestClientResponseException e) {
            throw new RestException(ErrorCode.AUTH_KAKAO_CODE_INVALID);
        }
    }

    private AuthDto.KakaoUserResponse requestKakaoUserInfo(String kakaoAccessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(kakaoAccessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<AuthDto.KakaoUserResponse> response =
                    restTemplate.exchange(kakaoUserInfoUri, HttpMethod.GET, request, AuthDto.KakaoUserResponse.class);

            AuthDto.KakaoUserResponse body = response.getBody();
            if (body == null || body.getId() == null) {
                throw new RestException(ErrorCode.AUTH_KAKAO_USERINFO_REQUEST_FAILED);
            }
            return body;
        } catch (RestClientResponseException e) {
            throw new RestException(ErrorCode.AUTH_KAKAO_USERINFO_REQUEST_FAILED);
        }
    }

    private String generateUniqueNickname(String baseNickname) {
        String base = (baseNickname == null || baseNickname.isBlank()) ? "kakao_user" : baseNickname;
        String candidate = base;

        int attempt = 0;
        while (userRepository.existsByNickname(candidate)) {
            attempt++;
            candidate = base + "_" + attempt;
            if (attempt > 50) {
                candidate = base + "_" + System.currentTimeMillis();
                break;
            }
        }
        return candidate;
    }
}
