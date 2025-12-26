package targeter.aim.domain.auth.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import targeter.aim.domain.auth.service.AuthService;
import targeter.aim.system.exception.model.ErrorCode;
import targeter.aim.system.exception.model.RestException;
import targeter.aim.system.security.model.JwtDto;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;

    //추후 프론트 주소로 변경
    private static final String FRONT_CALLBACK_URL = "http://localhost:8080/oauth/callback";


    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        OAuth2User principal = token.getPrincipal();
        Map<String, Object> attributes = principal.getAttributes();

        String email = asString(attributes.get("email"));

        // 구글은 보통 OIDC로 sub를 줌. 그런데 환경/설정에 따라 attributes에 없을 수도 있어서 fallback 처리
        String sub = asString(attributes.get("sub"));
        if (sub == null || sub.isBlank()) {
            // nameAttributeKey가 sub인 경우 principal.getName()이 sub가 됨
            sub = principal.getName();
        }

        if (email == null || email.isBlank()) {
            throw new RestException(ErrorCode.GLOBAL_BAD_REQUEST);
        }
        if (sub == null || sub.isBlank()) {
            throw new RestException(ErrorCode.GLOBAL_BAD_REQUEST);
        }

        JwtDto.TokenInfo jwt = authService.issueTokenByGoogle(email, sub);

        String redirectUrl = FRONT_CALLBACK_URL
                + "?accessToken=" + URLEncoder.encode(jwt.getAccessToken(), StandardCharsets.UTF_8)
                + "&refreshToken=" + URLEncoder.encode(jwt.getRefreshToken(), StandardCharsets.UTF_8);

        response.sendRedirect(redirectUrl);
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
