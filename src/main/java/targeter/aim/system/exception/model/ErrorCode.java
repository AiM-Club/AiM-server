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

    // File Storage
    FILE_METADATA_BUT_DISK_NOT_FOUND(404, "파일 메타데이터는 존재하지만 디스크에서 파일을 찾을 수 없습니다."),
    FILE_NOT_READABLE(400, "파일을 읽을 수 없습니다."),
    FILE_ACCESS_DENIED(403, "파일 접근 권한이 없습니다."),

    // Other
    INTERNAL_SERVER_ERROR(500, "오류가 발생했습니다.");

    private final int statusCode;
    private final String message;
}
