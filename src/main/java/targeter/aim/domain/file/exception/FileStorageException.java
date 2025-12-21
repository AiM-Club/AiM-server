package targeter.aim.domain.file.exception;

import targeter.aim.system.exception.model.ErrorCode;
import targeter.aim.system.exception.model.RestException;

public class FileStorageException extends RestException {

    public FileStorageException(ErrorCode errorCode) {
        super(errorCode);
    }
}
