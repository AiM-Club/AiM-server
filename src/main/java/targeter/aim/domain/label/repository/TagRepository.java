package targeter.aim.domain.label.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import targeter.aim.domain.label.entity.Tag;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByName(String name);

    List<Tag> findAllByNameIn(Collection<String> names);
}
