package targeter.aim.domain.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import targeter.aim.domain.user.entity.Tier;

public interface TierRepository extends JpaRepository<Tier, Long> {


}
