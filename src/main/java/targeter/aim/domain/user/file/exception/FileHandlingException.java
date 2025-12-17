package targeter.aim.domain.user.file.exception;

import targeter.aim.system.exception.model.ErrorCode;
import targeter.aim.system.exception.model.RestException;

public class FileHandlingException extends RestException {

    public FileHandlingException(ErrorCode errorCode) {
        super(errorCode);
    }
}
