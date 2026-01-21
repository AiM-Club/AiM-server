package targeter.aim.domain.challenge.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import targeter.aim.domain.ai.llm.dto.RoutePayload;
import targeter.aim.domain.ai.llm.service.RouteGenerator;
import targeter.aim.domain.challenge.dto.ChallengeDto;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChallengeRouteGenerationService {

    private final RouteGenerator routeGenerator;

    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MAX_CONTENT_LENGTH = 500;

    public RoutePayload generateRoute(ChallengeDto.ChallengeCreateRequest req) {
        // 1. AI 호출 (Retry 최대 3회 수행)
        RoutePayload payload = retry(() ->
                routeGenerator.generate(
                        req.getName(),
                        listToString(req.getTags()),
                        listToString(req.getFields()),
                        req.getJob(),
                        req.getDuration(),
                        req.getStartedAt().format(DateTimeFormatter.ISO_LOCAL_DATE),
                        req.getUserRequest() != null ? req.getUserRequest() : ""
                ),
                3, 1000L    // 3회 시도, 1초 대기부터 시작
        );

        // 2. 데이터 정제 (공백 제거, 길이 자르기 등)
        sanitizePayload(payload);

        // 3. 결과 검증 (필수값 확인)
        validatePayload(payload, req.getDuration());

        return payload;
    }

    // [1] retry 로직
    private <T> T retry(Supplier<T> action, int maxAttempts, long initialDelayMs) {
        long delay = initialDelayMs;
        RuntimeException lastException = null;

        for(int i = 1; i <= maxAttempts; i++) {
            try{
                return action.get();
            } catch (RuntimeException ex) {
                lastException = ex;
                if(i < maxAttempts) {
                    sleep(delay);
                    delay = Math.min(delay * 2, 8000);
                }
            }
        }

        throw lastException != null ? lastException : new RuntimeException("Retry failed");
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    // [2] Sanitization 로직
    private void sanitizePayload(RoutePayload payload) {
        if (payload == null || payload.getWeeks() == null) return;

        for (RoutePayload.Week week : payload.getWeeks()) {
            // 앞뒤 공백 제거
            week.setTitle(normalize(week.getTitle()));
            week.setContent(normalize(week.getContent()));

            // 안전 장치: 제목이 DB 함량보다 길면 자름
            if (week.getTitle().length() > MAX_TITLE_LENGTH) {
                week.setTitle(week.getTitle().substring(0, MAX_TITLE_LENGTH));
            }
        }
    }

    // [3] Validation 로직
    private void validatePayload(RoutePayload payload, int expectedWeeks) {
        if (payload == null || payload.getWeeks() == null) {
            throw new IllegalStateException("AI 응답 데이터가 비어있습니다.");
        }
        if (payload.getWeeks().size() != expectedWeeks) {
            throw new IllegalStateException(
                    String.format("AI 생성 기간 불일치: 요청(%d주) != 응답(%d주)", expectedWeeks, payload.getWeeks().size())
            );
        }

        for(RoutePayload.Week week : payload.getWeeks()) {
            if(isBlank(week.getTitle()) || isBlank(week.getContent())) {
                throw new IllegalStateException("AI가 빈 내용(제목/본문)을 반환했습니다.");
            }
        }
    }

    // Help 로직
    private String listToString(List<String> list) {
        return (list == null || list.isEmpty()) ? "" : String.join(", ", list);
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ");
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
