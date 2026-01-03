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

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAutoConfigurerFactory jwtAutoConfigurerFactory;

    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity httpSecurity,
            UserLoadServiceImpl userLoadServiceImpl
    ) throws Exception {

        httpSecurity
                .cors(cors -> cors.configurationSource(corsConfigSrc()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/**",
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/error",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/h2-console/**"
                        ).permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )
                .oauth2Login(oauth -> {})
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));

        jwtAutoConfigurerFactory.create(userLoadServiceImpl)
                .pathConfigure(it -> {
                    //auth는 JWT 인증 제외 (로그인/회원가입/카카오로그인 등)
                    it.exclude("/api/auth/**", ApiPathPattern.METHODS.GET);
                    it.exclude("/api/auth/**", ApiPathPattern.METHODS.POST);
                    it.exclude("/api/auth/**", ApiPathPattern.METHODS.PUT);
                    it.exclude("/api/auth/**", ApiPathPattern.METHODS.PATCH);
                    it.exclude("/api/auth/**", ApiPathPattern.METHODS.DELETE);
                    it.exclude("/api/auth/**", ApiPathPattern.METHODS.OPTIONS);
                    //기존 유지
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

        corsConfiguration.setAllowedOrigins(Arrays.asList(allowedOrigins));
        corsConfiguration.setAllowedMethods(Arrays.asList(
                "HEAD", "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));
        corsConfiguration.setAllowedHeaders(Arrays.asList(
                "Authorization", "Cache-Control", "Content-Type"
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
