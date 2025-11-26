package targeter.aim.system.security.utility.validator;

import org.springframework.util.StringUtils;
import targeter.aim.system.exception.model.ErrorCode;
import targeter.aim.system.exception.model.RestException;

import java.util.regex.Pattern;

public class ValidatorUtil {
    // 패턴 미리 상수화
    private static final String ID_REGEXP = "^(?=.*[a-z])(?=.*[0-9])[a-z0-9]{8,16}$";
    private static final String NICKNAME_REGEXP = "^[a-zA-Z0-9가-힣]{1,10}$";

    public static void validateId(String loginId) {
        // 중복 확인 때 비어있을 시 체크
        if(!StringUtils.hasText(loginId)) {
            throw new RestException(ErrorCode.AUTH_ID_REQUIRED);
        }

        if(!Pattern.matches(ID_REGEXP, loginId)) {
            throw new RestException(ErrorCode.AUTH_INVALID_ID_FORMAT);
        }
    }

    public static void validateNickname(String nickname) {
        if(!StringUtils.hasText(nickname)) {
            throw new RestException(ErrorCode.AUTH_NICKNAME_REQUIRED);
        }

        if(!Pattern.matches(NICKNAME_REGEXP, nickname)) {
            throw new RestException(ErrorCode.AUTH_INVALID_NICKNAME_FORMAT);
        }
    }
}
