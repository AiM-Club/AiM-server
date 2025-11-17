package targeter.aim.domain.auth.token.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Repository;
import targeter.aim.system.configuration.cache.CacheNames;

@Repository
@RequiredArgsConstructor
public class RefreshTokenCacheRepository {
    private final CacheManager cacheManager;

    public void cacheRefreshUuid(String uuid, String userKey) {
        cacheManager.getCache(CacheNames.REFRESH_TOKEN_UUID).put(uuid, userKey);
    }

    public String getCachedRefreshUuid(String uuid) {
        return cacheManager.getCache(CacheNames.REFRESH_TOKEN_UUID).get(uuid, String.class);
    }

    public void evictRefreshUuid(String uuid) {
        cacheManager.getCache(CacheNames.REFRESH_TOKEN_UUID).evict(uuid);
    }
}