package targeter.aim.domain.file.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import targeter.aim.domain.file.entity.ProfileImage;

public interface ProfileImageRepository extends JpaRepository<ProfileImage, String> {
}
