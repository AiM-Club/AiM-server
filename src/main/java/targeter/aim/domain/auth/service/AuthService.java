package targeter.aim.domain.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import targeter.aim.domain.auth.dto.AuthDto;
import targeter.aim.domain.auth.token.entity.RefreshToken;
import targeter.aim.domain.auth.token.entity.dto.RefreshTokenDto;
import targeter.aim.domain.auth.token.repository.RefreshTokenCacheRepository;
import targeter.aim.domain.auth.token.repository.RefreshTokenRepository;
import targeter.aim.domain.auth.token.validator.RefreshTokenValidator;
import targeter.aim.domain.user.dto.UserDto;
import targeter.aim.domain.user.entity.Gender;
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

import java.time.LocalDate;

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

    @Transactional
    public UserDto.UserResponse signUp(AuthDto.SignUpRequest request) {
        boolean isExisting = userRepository.existsByLoginId(request.getLoginId());
        if (isExisting) {
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

        if (!passwordEncoder.matches(request.getPassword(), found.getPassword())) {
            throw new RestException(ErrorCode.AUTH_PASSWORD_NOT_MATCH);
        }

        var userDetails = UserDetails.from(found);
        var tokenPair = jwtTokenProvider.createTokenPair(userDetails);

        String refreshTokenString = tokenPair.getRefreshToken().getTokenString();
        String refreshUuid = jwtTokenResolver.resolveTokenFromString(refreshTokenString).getRefreshUuid();

        RefreshToken refreshToken = RefreshTokenDto.toEntity(
                refreshUuid,
                userDetails.getKey(),
                tokenPair.getRefreshToken().getExpireAt()
        );

        refreshTokenRepository.save(refreshToken);
        refreshTokenCacheRepository.cacheRefreshUuid(refreshUuid, userDetails.getKey());

        return AuthDto.SignInResponse.of(
                UserDto.UserResponse.from(found),
                JwtDto.TokenInfo.of(tokenPair)
        );
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
    public JwtDto.TokenInfo issueTokenByGoogle(String email, String googleSub) {
        if (email == null || email.isBlank()) {
            throw new RestException(ErrorCode.GLOBAL_BAD_REQUEST);
        }
        if (googleSub == null || googleSub.isBlank()) {
            throw new RestException(ErrorCode.GLOBAL_BAD_REQUEST);
        }

        User user = userRepository.findByLoginId(email)
                .orElseGet(() -> createGoogleUser(email, googleSub));

        var userDetails = UserDetails.from(user);
        var tokenPair = jwtTokenProvider.createTokenPair(userDetails);

        String refreshTokenString = tokenPair.getRefreshToken().getTokenString();
        String refreshUuid = jwtTokenResolver.resolveTokenFromString(refreshTokenString).getRefreshUuid();

        RefreshToken refreshToken = RefreshTokenDto.toEntity(
                refreshUuid,
                userDetails.getKey(),
                tokenPair.getRefreshToken().getExpireAt()
        );

        refreshTokenRepository.save(refreshToken);
        refreshTokenCacheRepository.cacheRefreshUuid(refreshUuid, userDetails.getKey());

        return JwtDto.TokenInfo.of(tokenPair);
    }

    private User createGoogleUser(String email, String googleSub) {
        Tier bronze = tierRepository.findByName("BRONZE")
                .orElseThrow(() -> new RestException(ErrorCode.TIER_NOT_FOUND));

        User user = User.builder()
                .email(email)
                .loginId(email)
                .nickname("google_" + System.currentTimeMillis())
                .password("")
                .socialLogin(SocialLogin.GOOGLE)
                .socialId(googleSub)
                .birthday(LocalDate.of(2000, 1, 1))
                .gender(Gender.UNKNOWN)
                .tier(bronze)
                .build();

        return userRepository.save(user);
    }

    private String getAccessTokenFromRequest(HttpServletRequest request) {
        return jwtTokenResolver.parseTokenFromRequest(request)
                .orElseThrow(() -> new RestException(ErrorCode.AUTH_TOKEN_MISSING));
    }
}