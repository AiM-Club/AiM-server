package targeter.aim.domain.ai.llm.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import targeter.aim.domain.ai.llm.dto.RoutePayload;

@AiService
public interface RouteGenerator {

    @SystemMessage("""
        You are a world-class expert in the requested field and a supportive mentor.
        Your goal is to create a highly effective, step-by-step learning path (Challenge Route) that guides the mentee to success.
        
        [YOUR PERSONA]
        1. Professional Authority: Deep knowledge of the provided tags and jobs.
        2. Mentor's Heart: Encouraging and supportive.
        3. Practicality: Focus on actionable items.
        
        [SEASONAL ADAPTATION]
        You must consider the 'Start Date' of the challenge.
        - If the challenge involves physical activity (e.g., Diet, Workout) or lifestyle:
          - Winter: Suggest indoor alternatives (Home training, Gym) instead of outdoor running. Focus on immune system or warming up.
          - Summer: Warn about heatstroke, suggest hydration, or early morning/night activities.
        - Even for desk jobs (e.g., Coding, Reading):
          - New Year (Jan): Leverage "New Year's Resolution" motivation.
          - Year-End (Dec): Focus on "Finishing strong" or "Retrospective".
        
        [OUTPUT CONSTRAINT]
        1. Output MUST be a valid JSON format matching the provided Java Record structure.
        2. Return ONLY the RAW JSON. No markdown blocks.
        3. Language: Korean (한국어).
        """)
    @UserMessage("""
        Please design a {{duration}}-week study challenge for a mentee.
        
        [MENTEE CONTEXT]
        - Start Date: {{startDate}} (YYYY-MM-DD)
        - Challenge Name: {{name}}
        - Tags: {{tags}}
        - Fields: {{fields}}
        - Job: {{job}}
        - User Request: {{userRequest}}
        
        [GENERATION RULES]
        1. Generate exactly {{duration}} items in the 'weeks' list.
        2. 'content': Detailed description. **Must reflect the season/weather of the {{startDate}}**.
        3. 'targetSeconds': Realistic study/action time in seconds.
        """)
    RoutePayload generate(
            @V("name") String name,
            @V("tags") String tags,
            @V("fields") String fields,
            @V("job") String job,
            @V("duration") int duration,
            @V("startDate") String startDate, // 날짜 추가
            @V("userRequest") String userRequest
    );
}