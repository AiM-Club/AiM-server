package targeter.aim.system.security.configurer;

import lombok.RequiredArgsConstructor;
import org.springframework.web.servlet.HandlerExceptionResolver;
import targeter.aim.domain.auth.token.validator.RefreshTokenValidator;
import targeter.aim.system.security.initializer.JwtAuthPathInitializer;
import targeter.aim.system.security.service.UserLoadService;
import targeter.aim.system.security.utility.jwt.JwtTokenResolver;

@RequiredArgsConstructor
public class JwtAutoConfigurerFactory {
    private final HandlerExceptionResolver handlerExceptionResolver;
    private final JwtTokenResolver jwtTokenResolver;
    private final RefreshTokenValidator refreshTokenValidator;
    private final JwtAuthPathInitializer jwtAuthPathInitializer;

    public JwtAutoConfigurer create(UserLoadService userLoadService) {
        return new JwtAutoConfigurer(jwtTokenResolver, userLoadService, handlerExceptionResolver, refreshTokenValidator, jwtAuthPathInitializer);
    }
}