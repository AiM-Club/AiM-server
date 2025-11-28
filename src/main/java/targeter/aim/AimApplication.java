package targeter.aim;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@EntityScan(basePackages = {
        // AttachedFile 및 TestImageFile 엔티티 경로
        "targeter.aim.domain.user.file.entity",
        // User 엔티티 경로
        "targeter.aim.domain.user.entity"
        // 프로젝트에 존재하는 다른 모든 엔티티 패키지 경로도 여기에 추가해야 합니다.
})
@SpringBootApplication
public class AimApplication {

    public static void main(String[] args) {
        // 💡 서버가 시작되자마자 종료되는 문제를 해결하기 위해 이 코드가 필수입니다.
        SpringApplication.run(AimApplication.class, args);
    }
}