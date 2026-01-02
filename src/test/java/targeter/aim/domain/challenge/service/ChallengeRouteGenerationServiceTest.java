package targeter.aim.domain.challenge.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import targeter.aim.domain.ai.llm.dto.RoutePayload;
import targeter.aim.domain.ai.llm.service.RouteGenerator;
import targeter.aim.domain.challenge.dto.ChallengeDto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChallengeRouteGenerationServiceTest {

    @InjectMocks
    private ChallengeRouteGenerationService generationService;

    @Mock
    private RouteGenerator routeGenerator;

    @Test
    @DisplayName("성공: AI 호출이 성공하고 데이터 정제(trim)가 수행된다")
    void generateRoute_Success() {
        // given
        int duration = 4;
        ChallengeDto.ProgressCreateRequest request = createRequest(duration);

        // AI 응답 Mock
        RoutePayload mockPayload = new RoutePayload();

        // 1주차: 공백 포함 (Sanitize 검증용)
        RoutePayload.Week week1 = createMockWeek(1);
        week1.setTitle("  Week 1 Title  ");
        week1.setContent("  Content  ");

        // 2~4주차: 정상 데이터 (검증 통과용) -> 여기가 수정됨!
        mockPayload.setWeeks(List.of(
                week1,
                createMockWeek(2),
                createMockWeek(3),
                createMockWeek(4)
        ));

        given(routeGenerator.generate(any(), any(), any(), any(), eq(duration), any(), any()))
                .willReturn(mockPayload);

        // when
        RoutePayload result = generationService.generateRoute(request);

        // then
        assertThat(result.getWeeks()).hasSize(duration);
        assertThat(result.getWeeks().get(0).getTitle()).isEqualTo("Week 1 Title"); // 공백 제거 확인
    }

    @Test
    @DisplayName("재시도: AI가 처음 2번 실패해도 3번째 성공하면 결과 반환")
    void generateRoute_Retry_Success() {
        // given
        int duration = 2;
        ChallengeDto.ProgressCreateRequest request = createRequest(duration);

        RoutePayload successPayload = new RoutePayload();
        // 모든 Week에 데이터가 채워져 있어야 함 -> 여기가 수정됨!
        successPayload.setWeeks(List.of(
                createMockWeek(1),
                createMockWeek(2)
        ));

        // 1,2번째는 에러 -> 3번째는 성공
        given(routeGenerator.generate(any(), any(), any(), any(), anyInt(), any(), any()))
                .willThrow(new RuntimeException("Network Error 1"))
                .willThrow(new RuntimeException("Network Error 2"))
                .willReturn(successPayload);

        // when
        RoutePayload result = generationService.generateRoute(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getWeeks()).hasSize(duration);
        // 총 3번 호출되었는지 검증
        verify(routeGenerator, times(3)).generate(any(), any(), any(), any(), anyInt(), any(), any());
    }

    @Test
    @DisplayName("실패: 주차(Week) 개수가 다르면 예외 발생")
    void generateRoute_Fail_Validation_Size() {
        // given
        ChallengeDto.ProgressCreateRequest request = createRequest(4); // 4주 요청

        RoutePayload invalidPayload = new RoutePayload();
        // 1주치만 옴 -> 개수 불일치
        invalidPayload.setWeeks(List.of(createMockWeek(1)));

        given(routeGenerator.generate(any(), any(), any(), any(), anyInt(), any(), any()))
                .willReturn(invalidPayload);

        // when & then
        assertThatThrownBy(() -> generationService.generateRoute(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("불일치");
    }

    @Test
    @DisplayName("실패: 내용이 비어있는 주차가 있으면 예외 발생")
    void generateRoute_Fail_Validation_EmptyContent() {
        // given
        ChallengeDto.ProgressCreateRequest request = createRequest(1);

        RoutePayload invalidPayload = new RoutePayload();
        RoutePayload.Week emptyWeek = new RoutePayload.Week(); // title, content가 null
        emptyWeek.setWeekNumber(1);
        invalidPayload.setWeeks(List.of(emptyWeek));

        given(routeGenerator.generate(any(), any(), any(), any(), anyInt(), any(), any()))
                .willReturn(invalidPayload);

        // when & then
        assertThatThrownBy(() -> generationService.generateRoute(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("빈 내용");
    }

    // --- Helper Methods ---

    private ChallengeDto.ProgressCreateRequest createRequest(int duration) {
        return ChallengeDto.ProgressCreateRequest.builder()
                .name("Test")
                .startedAt(LocalDate.now())
                .duration(duration)
                .tags(new ArrayList<>())
                .jobs(new ArrayList<>())
                .fields(new ArrayList<>())
                .build();
    }

    // 테스트용 Week 객체를 안전하게 만드는 헬퍼
    private RoutePayload.Week createMockWeek(int num) {
        RoutePayload.Week w = new RoutePayload.Week();
        w.setWeekNumber(num);
        w.setTitle("Valid Title " + num);
        w.setContent("Valid Content " + num);
        w.setTargetSeconds(3600);
        return w;
    }
}