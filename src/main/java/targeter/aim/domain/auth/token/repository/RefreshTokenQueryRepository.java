package targeter.aim.domain.auth.token.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import targeter.aim.domain.auth.token.entity.QRefreshToken;

@Repository
@RequiredArgsConstructor
public class RefreshTokenQueryRepository {

    private final JPAQueryFactory queryFactory;

    public long deleteAllByUuid(String uuid) {
        QRefreshToken refreshToken = QRefreshToken.refreshToken;
        return queryFactory
                .delete(refreshToken)
                .where(refreshToken.uuid.eq(uuid))
                .execute();
    }

    public long deleteAllByUserKey(String userKey) {
        QRefreshToken refreshToken = QRefreshToken.refreshToken;
        return queryFactory
                .delete(refreshToken)
                .where(refreshToken.userKey.eq(userKey))
                .execute();
    }
}
