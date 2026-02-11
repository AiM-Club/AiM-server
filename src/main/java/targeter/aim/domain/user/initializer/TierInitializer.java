package targeter.aim.domain.user.initializer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import targeter.aim.domain.user.entity.Tier;
import targeter.aim.domain.user.repository.TierRepository;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(1)
public class TierInitializer implements CommandLineRunner {

    private final TierRepository tierRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // 중복 생성 방지
        if(tierRepository.count() > 0) {
            log.info("[TierInitializer] 티어 정보가 이미 있으므로 초기화 건너뜀.");
            return;
        }

        log.info("[TierInitializer] 티어 정보 초기화 진행중...");

        // 티어 정보 초기화
        List<Tier> tiers = Arrays.asList(
                Tier.builder().name("BRONZE").build(),
                Tier.builder().name("SILVER").build(),
                Tier.builder().name("GOLD").build(),
                Tier.builder().name("DIAMOND").build()
        );

        tierRepository.saveAll(tiers);
        log.info("[TierInitializer] 티어 정보 초기화 완료. (size: {})", tiers.size());
    }
}
