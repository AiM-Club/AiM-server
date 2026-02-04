package targeter.aim.domain.challenge.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import targeter.aim.domain.challenge.dto.RankDto;
import targeter.aim.domain.challenge.service.ChallengeRankService;
import targeter.aim.system.security.annotation.NoJwtAuth;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/challenges")
@Tag(name = "Challenge (Rank)", description = "랭킹 조회 API")
public class ChallengeRankController {

    private final ChallengeRankService challengeRankService;

    @NoJwtAuth
    @GetMapping("/rank/top20")
    @Operation(
            summary = "랭킹 TOP 20 조회",
            description = "레벨이 가장 높은 유저 20명을 랭킹으로 조회합니다. 1~3위는 all/solo/vs 기록, 4~20위는 all 기록만 포함됩니다."
    )
    public List<RankDto.Top20RankResponse> getTop20Rank() {
        return challengeRankService.getTop20Rank();
    }
}