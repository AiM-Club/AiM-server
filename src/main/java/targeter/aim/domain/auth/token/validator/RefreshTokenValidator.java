package targeter.aim.domain.auth.token.validator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import targeter.aim.domain.auth.token.repository.RefreshTokenCacheRepository;
import targeter.aim.domain.auth.token.repository.RefreshTokenRepository;
import targeter.aim.system.exception.model.ErrorCode;
import targeter.aim.system.exception.model.RestException;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenValidator {
    private final RefreshTokenCacheRepository cacheRepository;
    private final RefreshTokenRepository repository;

    public void validateOrThrow(String userKey, String refreshUuid) {
        if (refreshUuid == null || refreshUuid.isBlank()) {
            log.warn("Invalid refreshUuid: null or blank.");
            throw new RestException(ErrorCode.AUTH_TOKEN_INVALID);
        }

        if (!isValid(userKey, refreshUuid)) {
            log.warn("RefreshToken invalid: userKey={}, refreshUuid={}", userKey, refreshUuid);
            throw new RestException(ErrorCode.AUTH_TOKEN_INVALID);
        }
    }

    private boolean isValid(String userKey, String refreshUuid) {
        String cachedUserKey = cacheRepository.getCachedRefreshUuid(refreshUuid);
        if (cachedUserKey != null) return cachedUserKey.equals(userKey);

        return repository.findByUuid(refreshUuid)
                .map(token -> token.getUserKey().equals(userKey))
                .orElse(false);
    }
}
