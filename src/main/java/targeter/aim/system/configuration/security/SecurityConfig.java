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

import java.util.*;
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
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigSrc()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/api/auth/**",
                                "/error",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/h2-console/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/files/**", "/api/challenges/**", "/api/posts/**", "/api/users/rank/**").permitAll()
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
                    // auth는 JWT 인증 제외 (로그인/회원가입/소셜로그인 등)
                    it.exclude("/api/auth/**", ApiPathPattern.METHODS.GET);
                    it.exclude("/api/auth/**", ApiPathPattern.METHODS.POST);
                    it.exclude("/api/auth/**", ApiPathPattern.METHODS.PUT);
                    it.exclude("/api/auth/**", ApiPathPattern.METHODS.PATCH);
                    it.exclude("/api/auth/**", ApiPathPattern.METHODS.DELETE);
                    it.exclude("/api/auth/**", ApiPathPattern.METHODS.OPTIONS);
                    it.exclude("/api/**", ApiPathPattern.METHODS.OPTIONS);

                    // 기존 유지
                    it.include("/api/**", ApiPathPattern.METHODS.GET);
                    it.include("/api/**", ApiPathPattern.METHODS.POST);
                    it.include("/api/**", ApiPathPattern.METHODS.PUT);
                    it.include("/api/**", ApiPathPattern.METHODS.PATCH);
                    it.include("/api/**", ApiPathPattern.METHODS.DELETE);
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
        corsConfiguration.setAllowedHeaders(List.of("*"));
        corsConfiguration.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}