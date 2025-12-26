package targeter.aim.system.configuration.security;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerExceptionResolver;
import targeter.aim.domain.auth.token.validator.RefreshTokenValidator;
import targeter.aim.system.security.configurer.JwtAutoConfigurerFactory;
import targeter.aim.system.security.initializer.JwtAuthPathInitializer;
import targeter.aim.system.security.utility.jwt.JwtTokenProvider;
import targeter.aim.system.security.utility.jwt.JwtTokenResolver;

import java.security.Key;

@Slf4j
@Configuration
public class JwtConfig {

    private final Key secret;
    private final HandlerExceptionResolver handlerExceptionResolver;
    private final RefreshTokenValidator refreshTokenValidator;
    private final JwtAuthPathInitializer jwtAuthPathInitializer;

    public JwtConfig(
            @Value("${jwt.secret:#{null}}") String secretText,
            HandlerExceptionResolver handlerExceptionResolver,
            RefreshTokenValidator refreshTokenValidator,
            JwtAuthPathInitializer jwtAuthPathInitializer
    ) {

        if (StringUtils.hasText(secretText)) {
            byte[] keyBytes = Decoders.BASE64.decode(secretText);

            if (keyBytes.length < 32) {
                throw new IllegalStateException("Jwt Secret(디코딩 후)은 32바이트 이상이어야 합니다.");
            }

            this.secret = Keys.hmacShaKeyFor(keyBytes);
            log.info("JWT Secret(Base64) 로드 완료");
        } else {
            this.secret = Keys.secretKeyFor(SignatureAlgorithm.HS256);
            log.warn("JWT Secret이 설정되지 않았습니다. 임시 랜덤 키가 생성됩니다.");
        }

        this.handlerExceptionResolver = handlerExceptionResolver;
        this.refreshTokenValidator = refreshTokenValidator;
        this.jwtAuthPathInitializer = jwtAuthPathInitializer;
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtTokenProvider jwtTokenProvider(JwtProperties jwtProperties) {
        return new JwtTokenProvider(secret, jwtProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtTokenResolver jwtTokenResolver() {
        return new JwtTokenResolver(secret);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtAutoConfigurerFactory jwtAutoConfigurerFactory() {
        return new JwtAutoConfigurerFactory(
                handlerExceptionResolver,
                jwtTokenResolver(),
                refreshTokenValidator,
                jwtAuthPathInitializer
        );
    }
}
