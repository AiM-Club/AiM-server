package targeter.aim.domain.file.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import targeter.aim.domain.file.entity.AttachedFile;
import targeter.aim.domain.file.entity.ChallengeImage;

import java.util.Optional;

@Repository
public interface AttachedFileRepository extends JpaRepository<AttachedFile, String> {

    Optional<AttachedFile> findByUuid(String uuid);

    @Query("""
        select ci
        from ChallengeImage ci
        where ci.challenge.id = :challengeId
    """)
    Optional<ChallengeImage> findChallengeImageByChallengeId(Long challengeId); //챌린지 이미지가 여러 개라면 Optional 대신 List<ChallengeImage>로 추후변경

    @Query("""
        select af
        from AttachedFile af
        where af.uuid = (
            select w.uuid
            from WeeklyAuthFile w
            where w.weeklyProgress.id = :weeklyProgressId
        )
    """)
    Optional<AttachedFile> findWeeklyAuthFileByWeeklyProgressId(Long weeklyProgressId);
}