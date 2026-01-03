package targeter.aim.system.exception.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ErrorCode {

    // Global
    GLOBAL_BAD_REQUEST(400, "올바르지 않은 요청입니다."),
    GLOBAL_NOT_FOUND(404, "요청한 사항을 찾을 수 없습니다."),
    GLOBAL_ALREADY_EXIST(400, "요청의 대상이 이미 존재합니다."),
    GLOBAL_METHOD_NOT_ALLOWED(405, "허용되지 않는 Method 입니다."),
    GLOBAL_INVALID_PARAMETER(400, "올바르지 않은 파라미터입니다."),
    INVALID_INPUT_VALUE(400, "유효하지 않은 입력 값입니다."),

    // File
    FILE_UPLOAD_FAILED(400, "파일 업로드에 실패했습니다."),
    FILE_DELETE_FAILED(400, "파일 삭제에 실패했습니다."),
    FILE_NOT_FOUND(404, "파일을 찾을 수 없습니다."),
    FILE_INVALID_TYPE(400, "요청된 파일 타입이 올바르지 않습니다."),
    FILE_NOT_IMAGE(400, "이미지 파일이 아닙니다."),

    // File Storage
    FILE_METADATA_BUT_DISK_NOT_FOUND(404, "파일 메타데이터는 존재하지만 디스크에서 파일을 찾을 수 없습니다."),
    FILE_NOT_READABLE(400, "파일을 읽을 수 없습니다."),
    FILE_ACCESS_DENIED(403, "파일 접근 권한이 없습니다."),

    // Auth - Validation
    AUTH_ID_REQUIRED(400, "아이디는 필수 입력값입니다."),
    AUTH_INVALID_ID_FORMAT(400, "아이디 형식이 올바르지 않습니다."),
    AUTH_NICKNAME_REQUIRED(400, "닉네임은 필수 입력값입니다."),
    AUTH_INVALID_NICKNAME_FORMAT(400, "닉네임 형식이 올바르지 않습니다."),
    AUTH_PASSWORD_NOT_MATCH(401, "비밀번호가 올바르지 않습니다."),
    AUTH_FORBIDDEN(403, "접근 권한이 없습니다."),

    // Auth - Token/JWT
    AUTH_TOKEN_NOT_FOUND(401, "인증 토큰이 존재하지 않습니다."),
    AUTH_TOKEN_MISSING(401, "인증 토큰이 존재하지 않습니다."),
    AUTH_TOKEN_INVALID(401, "유효하지 않은 토큰입니다."),
    AUTH_TOKEN_EXPIRED(401, "만료된 토큰입니다."),
    AUTH_TOKEN_MALFORMED(401, "손상된 토큰입니다."),

    // Auth - Etc
    AUTH_AUTHENTICATION_FAILED(401, "인증에 실패했습니다."),
    AUTH_USER_NOT_FOUND(404, "등록된 유저를 찾을 수 없습니다."),
    AUTH_CANNOT_GENERATE_TOKEN(400, "인증키를 생성 할 수 없습니다."),

    // Auth - Kakao
    AUTH_KAKAO_CODE_INVALID(401, "카카오 인가 코드가 유효하지 않습니다."),
    AUTH_KAKAO_TOKEN_REQUEST_FAILED(401, "카카오 토큰 요청에 실패했습니다."),
    AUTH_KAKAO_USERINFO_REQUEST_FAILED(401, "카카오 사용자 정보 조회에 실패했습니다."),

    // Auth - Kakao
    AUTH_KAKAO_CODE_INVALID(401, "카카오 인가 코드가 유효하지 않습니다."),
    AUTH_KAKAO_TOKEN_REQUEST_FAILED(401, "카카오 토큰 요청에 실패했습니다."),
    AUTH_KAKAO_USERINFO_REQUEST_FAILED(401, "카카오 사용자 정보 조회에 실패했습니다."),

    // User
    USER_ALREADY_LOGIN_ID_EXISTS(409, "중복되는 아이디입니다."),
    USER_NOT_FOUND(404, "존재하지 않는 사용자입니다."),

    // Tier
    TIER_NOT_FOUND(404, "해당 티어를 찾을 수 없습니다."),

    // Other
    INTERNAL_SERVER_ERROR(500, "오류가 발생했습니다.");

    private final int statusCode;
    private final String message;
}

