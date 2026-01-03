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
import org.springframework.web.client.HttpClientErrorException;
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

import java.util.Map;

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

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;

    private static final String GOOGLE_TOKEN_URI = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USERINFO_URI = "https://openidconnect.googleapis.com/v1/userinfo";

    @Transactional
    public UserDto.UserResponse signUp(AuthDto.SignUpRequest request) {
        boolean isExisting = userRepository.existsByLoginId(request.getLoginId());
        if (isExisting) {
            throw new RestException(ErrorCode.USER_ALREADY_LOGIN_ID_EXISTS);
        }

        User toSave = request.toEntity(passwordEncoder);

        Tier bronze = tierRepository.findByName("BRONZE")//초기설정
                .orElseThrow(() -> new RestException(ErrorCode.TIER_NOT_FOUND));
        toSave.setTier(bronze);

        User saved = userRepository.save(toSave);

        return UserDto.UserResponse.from(saved);
    }

    @Transactional
    public AuthDto.SignInResponse signIn(@Valid AuthDto.SignInRequest request) {
        var found = userRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> new RestException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), found.getPassword())) {
            throw new RestException(ErrorCode.AUTH_PASSWORD_NOT_MATCH);
        }

        var userDetails = UserDetails.from(found);
        var tokenPair = jwtTokenProvider.createTokenPair(userDetails);

        saveRefreshToken(userDetails, tokenPair);

        return AuthDto.SignInResponse.of(
                UserDto.UserResponse.from(found),
                JwtDto.TokenInfo.of(tokenPair)
        );
    }

    @Transactional
    public AuthDto.SocialSignInResponse loginGoogle(@Valid AuthDto.GoogleLoginRequest request) {
        RestTemplate restTemplate = new RestTemplate();

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

        } catch (HttpClientErrorException e) {
            // code 만료/무효 등은 401로
            throw new RestException(ErrorCode.AUTH_AUTHENTICATION_FAILED);
        }
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

    @Transactional
    public AuthDto.SocialSignInResponse issueTokenByGoogle(String email, String googleSub) {
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
        return AuthDto.SocialSignInResponse.of(
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

    private String getAccessTokenFromRequest(HttpServletRequest request) {
        return jwtTokenResolver.parseTokenFromRequest(request)
                .orElseThrow(() -> new RestException(ErrorCode.AUTH_TOKEN_MISSING));
    }
}