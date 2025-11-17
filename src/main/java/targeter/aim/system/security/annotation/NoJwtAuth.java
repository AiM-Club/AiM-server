package targeter.aim.system.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * JWT 인증이 필요 없는 API에 붙이는 어노테이션
 * (예: 로그인, 회원가입 등)
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface NoJwtAuth {

    String value() default ""; // 인증 제외 이유 (선택)
}
