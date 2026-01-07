package targeter.aim.domain.label.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import targeter.aim.domain.label.entity.Tag;

import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByName(String name);
}
