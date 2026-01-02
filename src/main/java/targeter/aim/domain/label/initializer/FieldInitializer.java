package targeter.aim.domain.label.initializer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import targeter.aim.domain.label.entity.Field;
import targeter.aim.domain.label.repository.FieldRepository;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FieldInitializer implements CommandLineRunner {

    private final FieldRepository fieldRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // 중복 생성 방지
        if(fieldRepository.count() > 0) {
            log.info("[FieldInitializer] 분야 정보가 이미 있으므로 초기화 건너뜀.");
            return;
        }

        log.info("[FieldInitializer] 분야 정보 초기화 진행중...");

        // 분야 정보 초기화
        List<Field> fields = Arrays.asList(
                Field.builder().name("IT").build(),         // IT
                Field.builder().name("BUSINESS").build(),   // 경영
                Field.builder().name("ECONOMICS").build(),  // 경제
                Field.builder().name("POLITICS").build(),   // 정치
                Field.builder().name("LANGUAGE").build(),   // 어문
                Field.builder().name("SCIENCE").build(),    // 자연
                Field.builder().name("DESIGN").build(),     // 디자인
                Field.builder().name("SPORTS").build(),     // 체육
                Field.builder().name("MUSIC").build()       // 음악
        );

        fieldRepository.saveAll(fields);
        log.info("[FieldInitializer] 분야 정보 초기화 완료. (size: {})", fields.size());
    }
}
