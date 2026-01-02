package targeter.aim.domain.label.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import targeter.aim.domain.label.entity.Tag;

public interface TagRepository extends JpaRepository<Tag, Long> {
}
