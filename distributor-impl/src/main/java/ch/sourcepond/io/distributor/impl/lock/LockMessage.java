package ch.sourcepond.io.distributor.impl.lock;

import java.io.IOException;
import java.io.Serializable;

class LockMessage implements Serializable {
    private final String path;
    private final IOException failureOrNull;

    public LockMessage(final String pPath) {
        this(pPath, null);
    }

    public LockMessage(final String pPath, final IOException pFailureOrNull) {
        path = pPath;
        failureOrNull = pFailureOrNull;
    }

    public String getPath() {
        return path;
    }

    public IOException getFailureOrNull() {
        return failureOrNull;
    }
}
