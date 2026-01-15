package targeter.aim.domain.challenge.timer;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChallengeRunningSessionManager {

    private final ConcurrentHashMap<String, LocalDateTime> runningSessions =
            new ConcurrentHashMap<>();

    public LocalDateTime start(Long challengeId, Long userId) {
        String key = key(challengeId, userId);
        LocalDateTime startedAt = LocalDateTime.now();

        if (runningSessions.putIfAbsent(key, startedAt) != null) {
            throw new IllegalStateException("이미 실행 중인 타이머입니다.");
        }

        return startedAt;
    }

    public long stop(Long challengeId, Long userId) {
        String key = key(challengeId, userId);

        LocalDateTime startedAt = runningSessions.remove(key);
        if (startedAt == null) {
            throw new IllegalStateException("이미 정지된 타이머입니다.");
        }

        long seconds = Duration.between(startedAt, LocalDateTime.now()).getSeconds();

        return seconds;
    }

    public boolean isRunning(Long challengeId, Long userId) {
        return runningSessions.containsKey(key(challengeId, userId));
    }

    private String key(Long challengeId, Long userId) {
        return challengeId + ":" + userId;
    }
}
