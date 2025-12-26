package targeter.aim.system.configuration.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import targeter.aim.domain.auth.handler.OAuth2LoginSuccessHandler;
import targeter.aim.system.security.configurer.JwtAutoConfigurerFactory;
import targeter.aim.system.security.model.ApiPathPattern;
import targeter.aim.system.security.service.UserLoadServiceImpl;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAutoConfigurerFactory jwtAutoConfigurerFactory;

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity httpSecurity,
            UserLoadServiceImpl userLoadServiceImpl,
            OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler
    ) throws Exception {

        // JWT 관련 설정 (기존 로직 유지)
        jwtAutoConfigurerFactory.create(userLoadServiceImpl)
                .pathConfigure((it) -> {
                    it.include("/api/**", ApiPathPattern.METHODS.GET);
                    it.include("/api/**", ApiPathPattern.METHODS.POST);
                    it.include("/api/**", ApiPathPattern.METHODS.PUT);
                    it.include("/api/**", ApiPathPattern.METHODS.PATCH);
                    it.include("/api/**", ApiPathPattern.METHODS.DELETE);
                    it.include("/api/**", ApiPathPattern.METHODS.OPTIONS);
                })
                .configure(httpSecurity);

        httpSecurity.cors(cors -> cors.configurationSource(corsConfigSrc()));

        httpSecurity.oauth2Login(oauth -> oauth
                .successHandler(oAuth2LoginSuccessHandler)
                .failureHandler((request, response, exception) -> {
                    exception.printStackTrace();
                    response.sendRedirect("/login?error");
                })
        );

        return httpSecurity.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigSrc() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowCredentials(true);
        corsConfiguration.setAllowedMethods(
                Arrays.asList("HEAD", "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        );
        corsConfiguration.setAllowedHeaders(
                Arrays.asList("Authorization", "Cache-Control", "Content-Type")
        );
        corsConfiguration.setAllowedOriginPatterns(
                Arrays.asList(
                        "http://localhost:3000",
                        "http://localhost:8080"
                )
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


