package targeter.aim.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import targeter.aim.domain.user.dto.MyPageDto;
import targeter.aim.domain.user.service.MyPageService;
import targeter.aim.system.security.model.UserDetails;

@Tag(name = "MyPage", description = "마이페이지 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mypage")
public class MyPageController {

    private final MyPageService myPageService;

    @GetMapping
    @Operation(
            summary = "마이페이지 레벨/티어 조회",
            description = "로그인한 사용자의 레벨, 티어, 티어 진행률, 다음 티어 정보를 조회합니다."
    )
    public MyPageDto.MyPageResponse getMyPage(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return myPageService.getMyPage(userDetails);
    }
}