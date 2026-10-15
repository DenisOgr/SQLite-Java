package net.porplenko.sqlite.common.exceptions;

public class CorruptDatabaseException extends RuntimeException {
    public CorruptDatabaseException(String message) {
        super(message);
    }

    public CorruptDatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
