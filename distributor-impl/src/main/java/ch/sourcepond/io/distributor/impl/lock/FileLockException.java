package ch.sourcepond.io.distributor.impl.lock;

public class FileLockException extends Exception {

    public FileLockException(final String message) {
        super(message);
    }

    public FileLockException(final Throwable cause) {
        super(cause);
    }
}
