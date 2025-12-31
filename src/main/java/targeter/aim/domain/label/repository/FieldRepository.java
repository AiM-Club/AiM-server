package targeter.aim.domain.label.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import targeter.aim.domain.label.entity.Field;

public interface FieldRepository extends JpaRepository<Field, Long> {
}
