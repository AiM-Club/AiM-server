package targeter.aim.system.configuration.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import targeter.aim.system.security.configurer.JwtAutoConfigurerFactory;
import targeter.aim.system.security.model.ApiPathPattern;
import targeter.aim.system.security.service.UserLoadServiceImpl;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAutoConfigurerFactory jwtAutoConfigurerFactory;

    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity httpSecurity,
            UserLoadServiceImpl userLoadServiceImpl
    ) throws Exception {

        httpSecurity
                // CSRF 비활성화 (JWT 기반)
                .csrf(csrf -> csrf.disable())

                // CORS 설정
                .cors(cors -> cors.configurationSource(corsConfigSrc()))

                // 인가 정책
                .authorizeHttpRequests(auth -> auth
                        // 인증 없이 허용
                        .requestMatchers(
                                "/api/auth/**",
                                "/error",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/h2-console/**"
                        ).permitAll()

                        // 비로그인 허용: 챌린지 목록 조회
                        .requestMatchers(HttpMethod.GET, "/api/challenges").permitAll()

                        // 나머지 API는 JWT 필요
                        .requestMatchers("/api/**").authenticated()

                        .anyRequest().permitAll()
                )

                // 세션 미사용
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        // JWT Filter 설정
        jwtAutoConfigurerFactory.create(userLoadServiceImpl)
                .pathConfigure(it -> {
                    // auth 관련 API는 JWT 검사 제외
                    it.exclude("/api/auth/**", ApiPathPattern.METHODS.GET);
                    it.exclude("/api/auth/**", ApiPathPattern.METHODS.POST);
                    it.exclude("/api/auth/**", ApiPathPattern.METHODS.PUT);
                    it.exclude("/api/auth/**", ApiPathPattern.METHODS.PATCH);
                    it.exclude("/api/auth/**", ApiPathPattern.METHODS.DELETE);
                    it.exclude("/api/auth/**", ApiPathPattern.METHODS.OPTIONS);

                    // 그 외 API는 JWT 적용
                    it.include("/api/**", ApiPathPattern.METHODS.GET);
                    it.include("/api/**", ApiPathPattern.METHODS.POST);
                    it.include("/api/**", ApiPathPattern.METHODS.PUT);
                    it.include("/api/**", ApiPathPattern.METHODS.PATCH);
                    it.include("/api/**", ApiPathPattern.METHODS.DELETE);
                    it.include("/api/**", ApiPathPattern.METHODS.OPTIONS);
                })
                .configure(httpSecurity);

        return httpSecurity.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigSrc() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowCredentials(true);

        List<String> origins = Arrays.stream(allowedOrigins)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());

        corsConfiguration.setAllowedOrigins(origins);
        corsConfiguration.setAllowedMethods(
                Arrays.asList("HEAD", "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        );
        corsConfiguration.setAllowedHeaders(
                Arrays.asList("Authorization", "Cache-Control", "Content-Type")
        );

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}