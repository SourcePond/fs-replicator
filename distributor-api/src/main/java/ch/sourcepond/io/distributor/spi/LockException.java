package ch.sourcepond.io.distributor.spi;

import java.io.IOException;

public class LockException extends IOException {

    public LockException(String message) {
        super(message);
    }

    public LockException(String message, Throwable cause) {
        super(message, cause);
    }
}
