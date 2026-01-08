package targeter.aim.domain.file.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import targeter.aim.domain.file.entity.ChallengeImage;
import targeter.aim.domain.file.entity.QChallengeImage;

import java.util.Optional;

@RequiredArgsConstructor
public class AttachedFileQueryRepositoryImpl implements AttachedFileQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<ChallengeImage> findChallengeImageByChallengeId(Long challengeId) {
        QChallengeImage ci = QChallengeImage.challengeImage;

        ChallengeImage result = queryFactory
                .selectFrom(ci)
                .where(ci.challenge.id.eq(challengeId))
                .fetchFirst();

        return Optional.ofNullable(result);
    }
}