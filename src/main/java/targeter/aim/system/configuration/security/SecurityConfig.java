package targeter.aim.system.configuration.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
                        .requestMatchers(
                                "/api/auth/**",
                                "/error",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/h2-console/**"
                        ).permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )
                // REST 방식
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        jwtAutoConfigurerFactory.create(userLoadServiceImpl)
                .pathConfigure(it -> {
                    // auth는 JWT 인증 제외 (로그인/회원가입/소셜로그인 등)
                    it.exclude("/api/auth/**", ApiPathPattern.METHODS.GET);
                    it.exclude("/api/auth/**", ApiPathPattern.METHODS.POST);
                    it.exclude("/api/auth/**", ApiPathPattern.METHODS.PUT);
                    it.exclude("/api/auth/**", ApiPathPattern.METHODS.PATCH);
                    it.exclude("/api/auth/**", ApiPathPattern.METHODS.DELETE);
                    it.exclude("/api/auth/**", ApiPathPattern.METHODS.OPTIONS);

                    // 기존 유지
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
        corsConfiguration.setAllowedOrigins(List.of(allowedOrigins));
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
