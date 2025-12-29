package targeter.aim.domain.ai.llm.dto;

import lombok.Data;

import java.util.List;

@Data
public class RoutePayload {
    private List<Week> weeks;

    @Data
    public static class Week {
        private Integer weekNumber; // 주차
        private String title;       // 제목
        private String content;     // 내용
        private Long targetSeconds; // 목표 시간초
    }
}
