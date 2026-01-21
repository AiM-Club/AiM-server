package targeter.aim.domain.auth.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import targeter.aim.domain.auth.dto.OAuth2Dto;
import targeter.aim.domain.auth.token.entity.RefreshToken;
import targeter.aim.domain.auth.token.entity.dto.RefreshTokenDto;
import targeter.aim.domain.auth.token.repository.RefreshTokenCacheRepository;
import targeter.aim.domain.auth.token.repository.RefreshTokenRepository;
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

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OAuth2Service {

    private final UserRepository userRepository;
    private final TierRepository tierRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtTokenResolver jwtTokenResolver;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenCacheRepository refreshTokenCacheRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;

    private static final String GOOGLE_TOKEN_URI = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USERINFO_URI = "https://openidconnect.googleapis.com/v1/userinfo";

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String kakaoClientId;

    @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
    private String kakaoClientSecret;

    @Value("${spring.security.oauth2.client.provider.kakao.token-uri}")
    private String kakaoTokenUri;

    @Value("${spring.security.oauth2.client.provider.kakao.user-info-uri}")
    private String kakaoUserInfoUri;

    @Transactional
    public OAuth2Dto.SocialSignInResponse loginGoogle(@Valid OAuth2Dto.GoogleLoginRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("code", request.getCode());
            body.add("client_id", googleClientId);
            body.add("client_secret", googleClientSecret);
            body.add("redirect_uri", request.getRedirectUri());
            body.add("grant_type", "authorization_code");

            HttpEntity<MultiValueMap<String, String>> tokenReq = new HttpEntity<>(body, headers);

            ResponseEntity<Map> tokenRes = restTemplate.exchange(
                    GOOGLE_TOKEN_URI,
                    HttpMethod.POST,
                    tokenReq,
                    Map.class
            );

            Map<String, Object> tokenMap = tokenRes.getBody();
            if (tokenMap == null) {
                throw new RestException(ErrorCode.GLOBAL_BAD_REQUEST);
            }

            Object accessTokenObj = tokenMap.get("access_token");
            if (accessTokenObj == null) {
                throw new RestException(ErrorCode.AUTH_AUTHENTICATION_FAILED);
            }

            String googleAccessToken = String.valueOf(accessTokenObj);

            HttpHeaders userinfoHeaders = new HttpHeaders();
            userinfoHeaders.setBearerAuth(googleAccessToken);

            HttpEntity<Void> userinfoReq = new HttpEntity<>(userinfoHeaders);

            ResponseEntity<Map> userinfoRes = restTemplate.exchange(
                    GOOGLE_USERINFO_URI,
                    HttpMethod.GET,
                    userinfoReq,
                    Map.class
            );

            Map<String, Object> userinfo = userinfoRes.getBody();
            if (userinfo == null) {
                throw new RestException(ErrorCode.GLOBAL_BAD_REQUEST);
            }

            String email = userinfo.get("email") == null ? null : String.valueOf(userinfo.get("email"));
            String sub = userinfo.get("sub") == null ? null : String.valueOf(userinfo.get("sub"));

            return issueTokenByGoogle(email, sub);

        } catch (RestClientResponseException e) {
            throw new RestException(ErrorCode.AUTH_AUTHENTICATION_FAILED);
        }
    }

    @Transactional
    public OAuth2Dto.SocialSignInResponse loginWithKakao(@Valid OAuth2Dto.KakaoLoginRequest request) {
        String kakaoAccessToken = requestKakaoAccessToken(request.getCode(), request.getRedirectUri());
        OAuth2Dto.KakaoUserResponse kakaoUser = requestKakaoUserInfo(kakaoAccessToken);

        String socialId = String.valueOf(kakaoUser.getId());

        Optional<User> existing = userRepository.findBySocialLoginAndSocialId(SocialLogin.KAKAO, socialId);

        String nicknameFromKakao = Optional.ofNullable(kakaoUser.getKakaoAccount())
                .map(OAuth2Dto.KakaoUserResponse.KakaoAccount::getProfile)
                .map(OAuth2Dto.KakaoUserResponse.KakaoAccount.Profile::getNickname)
                .orElse(null);

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
                    .level(1)
                    .build();

            user = userRepository.save(user);
        }

        var userDetails = UserDetails.from(user);
        var tokenPair = jwtTokenProvider.createTokenPair(userDetails);

        saveRefreshToken(userDetails, tokenPair);

        UserDto.UserResponse userRes = UserDto.UserResponse.from(user);
        JwtDto.TokenInfo tokenInfo = JwtDto.TokenInfo.of(tokenPair);

        return OAuth2Dto.SocialSignInResponse.of(
                userRes,
                tokenInfo,
                isNewUser
        );
    }

    @Transactional
    public OAuth2Dto.SocialSignInResponse issueTokenByGoogle(String email, String googleSub) {
        if (email == null || email.isBlank()) {
            throw new RestException(ErrorCode.GLOBAL_BAD_REQUEST);
        }
        if (googleSub == null || googleSub.isBlank()) {
            throw new RestException(ErrorCode.GLOBAL_BAD_REQUEST);
        }

        User user = findOrCreateGoogleUser(email, googleSub);

        var userDetails = UserDetails.from(user);
        var tokenPair = jwtTokenProvider.createTokenPair(userDetails);

        saveRefreshToken(userDetails, tokenPair);

        UserDto.UserResponse userRes = UserDto.UserResponse.from(user);
        return OAuth2Dto.SocialSignInResponse.of(
                userRes,
                JwtDto.TokenInfo.of(tokenPair),
                userRes.getIsNewUser()
        );
    }

    private User findOrCreateGoogleUser(String email, String googleSub) {
        var byEmail = userRepository.findByEmail(email);
        if (byEmail.isPresent()) {
            User existing = byEmail.get();

            if (existing.isSocialUser()
                    && existing.getSocialLogin() == SocialLogin.GOOGLE) {
                return existing;
            }

            throw new RestException(ErrorCode.DUPLICATE_EMAIL_PROVIDER);
        }

        var byLoginId = userRepository.findByLoginId(email);
        if (byLoginId.isPresent()) {
            throw new RestException(ErrorCode.DUPLICATE_EMAIL_PROVIDER);
        }

        return createGoogleUser(email, googleSub);
    }

    private User createGoogleUser(String email, String googleSub) {
        Tier bronze = tierRepository.findByName("BRONZE")
                .orElseThrow(() -> new RestException(ErrorCode.TIER_NOT_FOUND));

        User user = User.builder()
                .loginId(null)
                .email(email)
                .nickname("google_" + System.currentTimeMillis())
                .password(null)
                .socialLogin(SocialLogin.GOOGLE)
                .socialId(googleSub)
                .birthday(null)
                .gender(null)
                .tier(bronze)
                .level(1)
                .build();

        return userRepository.save(user);
    }

    private void saveRefreshToken(UserDetails userDetails, JwtDto.TokenPair tokenPair) {
        String accessTokenString = tokenPair.getAccessToken().getTokenString();
        String refreshUuid = jwtTokenResolver.resolveTokenFromString(accessTokenString).getRefreshUuid();

        if (refreshUuid == null || refreshUuid.isBlank()) {
            throw new RestException(ErrorCode.GLOBAL_BAD_REQUEST);
        }

        RefreshToken refreshToken = RefreshTokenDto.toEntity(
                refreshUuid,
                userDetails.getKey(),
                tokenPair.getRefreshToken().getExpireAt()
        );

        refreshTokenRepository.save(refreshToken);
        refreshTokenCacheRepository.cacheRefreshUuid(refreshUuid, userDetails.getKey());
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
            ResponseEntity<OAuth2Dto.KakaoTokenResponse> response =
                    restTemplate.exchange(kakaoTokenUri, HttpMethod.POST, request, OAuth2Dto.KakaoTokenResponse.class);

            OAuth2Dto.KakaoTokenResponse responseBody = response.getBody();
            if (responseBody == null || responseBody.getAccessToken() == null || responseBody.getAccessToken().isBlank()) {
                throw new RestException(ErrorCode.AUTH_KAKAO_TOKEN_REQUEST_FAILED);
            }
            return responseBody.getAccessToken();
        } catch (RestClientResponseException e) {
            throw new RestException(ErrorCode.AUTH_KAKAO_CODE_INVALID);
        }
    }

    private OAuth2Dto.KakaoUserResponse requestKakaoUserInfo(String kakaoAccessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(kakaoAccessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<OAuth2Dto.KakaoUserResponse> response =
                    restTemplate.exchange(kakaoUserInfoUri, HttpMethod.GET, request, OAuth2Dto.KakaoUserResponse.class);

            OAuth2Dto.KakaoUserResponse body = response.getBody();
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
