package targeter.aim.domain.label.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import targeter.aim.domain.label.entity.Field;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FieldRepository extends JpaRepository<Field, Long> {

    Optional<Field> findByName(String name);

    List<Field> findAllByNameIn(Collection<String> names);
}