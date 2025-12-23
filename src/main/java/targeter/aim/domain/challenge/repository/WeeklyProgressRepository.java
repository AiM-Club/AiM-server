package targeter.aim.domain.challenge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import targeter.aim.domain.challenge.entity.WeeklyProgress;

public interface WeeklyProgressRepository extends JpaRepository<WeeklyProgress, Long> {
}
