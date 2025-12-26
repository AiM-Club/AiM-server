package targeter.aim.domain.auth.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class OAuthCallbackController {

    @GetMapping("/oauth/callback")
    public Map<String, String> callback(
            @RequestParam String accessToken,
            @RequestParam String refreshToken
    ) {
        return Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken
        );
    }
}

