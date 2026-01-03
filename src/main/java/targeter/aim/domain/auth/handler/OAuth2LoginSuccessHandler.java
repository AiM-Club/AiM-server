package targeter.aim.domain.auth.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import targeter.aim.domain.auth.dto.AuthDto;
import targeter.aim.domain.auth.service.AuthService;
import targeter.aim.system.exception.model.ErrorCode;
import targeter.aim.system.exception.model.RestException;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;

    private static final String FRONT_CALLBACK_URL = "http://localhost:8080/oauth/callback";//추후 프론트 연동

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

        String sub = asString(attributes.get("sub"));
        if (sub == null || sub.isBlank()) {
            sub = principal.getName();
        }

        if (email == null || email.isBlank()) {
            throw new RestException(ErrorCode.GLOBAL_BAD_REQUEST);
        }
        if (sub == null || sub.isBlank()) {
            throw new RestException(ErrorCode.GLOBAL_BAD_REQUEST);
        }

        AuthDto.SocialSignInResponse signIn = authService.issueTokenByGoogle(email, sub);

        String redirectUrl = FRONT_CALLBACK_URL
                + "?accessToken=" + URLEncoder.encode(signIn.getToken().getAccessToken(), StandardCharsets.UTF_8)
                + "&refreshToken=" + URLEncoder.encode(signIn.getToken().getRefreshToken(), StandardCharsets.UTF_8)
                + "&isNewUser=" + URLEncoder.encode(String.valueOf(signIn.getIsNewUser()), StandardCharsets.UTF_8);

        response.sendRedirect(redirectUrl);
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}