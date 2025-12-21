package targeter.aim.domain.file.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import targeter.aim.domain.file.entity.AttachedFile;

import java.util.Optional;

@Repository
public interface AttachedFileRepository extends JpaRepository<AttachedFile, String> {

    Optional<AttachedFile> findByUuid(String uuid);
}
