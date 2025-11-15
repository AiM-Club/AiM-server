package targeter.aim.system.security.configurer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import targeter.aim.domain.auth.token.validator.RefreshTokenValidator;
import targeter.aim.system.security.filter.JwtAuthenticationFilter;
import targeter.aim.system.security.initializer.JwtAuthPathInitializer;
import targeter.aim.system.security.service.UserLoadService;
import targeter.aim.system.security.utility.jwt.JwtTokenResolver;

@RequiredArgsConstructor
@Slf4j
public class JwtAutoConfigurer {
    private final JwtTokenResolver jwtTokenResolver;
    private final UserLoadService userLoadService;
    private final HandlerExceptionResolver handlerExceptionResolver;
    private final PathPatternConfigurer pathPatternConfigurer = new PathPatternConfigurer();
    private final RefreshTokenValidator refreshTokenValidator;
    private final JwtAuthPathInitializer jwtAuthPathInitializer;

    public JwtAutoConfigurer pathConfigure(Customizer<PathPatternConfigurer> customizer) {
        customizer.customize(this.pathPatternConfigurer);
        return this;
    }

    public void configure(HttpSecurity http) throws Exception {
        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(
                pathPatternConfigurer.getExcludePatternList(),
                pathPatternConfigurer.getIncludePatternList(),
                jwtTokenResolver,
                userLoadService,
                handlerExceptionResolver,
                refreshTokenValidator,
                jwtAuthPathInitializer
        );

        http
                .headers(it -> it.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .httpBasic(AbstractHttpConfigurer::disable)
                .cors(cors -> {})
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(it -> it.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        log.info("Jwt 인증 자동 설정이 완료되었습니다.");
    }
}