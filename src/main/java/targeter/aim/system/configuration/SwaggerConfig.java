package targeter.aim.system.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String JWT_SCHEME_NAME = "JWT Auth";

    @Bean
    public OpenAPI openAPI() {
        Info info = new Info()
                .title("Aim API Document")
                .version("v1.0.0")
                .description("Aim의 API 명세서입니다.");

        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization");

        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList(JWT_SCHEME_NAME);

        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes(JWT_SCHEME_NAME, securityScheme))
                .addSecurityItem(securityRequirement)
                .addServersItem(new Server().url("/"))
                .info(info);
    }
}
