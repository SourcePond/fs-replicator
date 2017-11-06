package ch.sourcepond.io.distributor.api;

import java.io.IOException;

public class GlobalLockException extends IOException {

    public GlobalLockException(String message) {
        super(message);
    }

    public GlobalLockException(String message, Throwable cause) {
        super(message, cause);
    }
}
