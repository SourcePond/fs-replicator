package ch.sourcepond.io.distributor.impl;

import java.io.Serializable;

class DistributorMessage implements Serializable {
    private final String path;

    public DistributorMessage(final String pPath) {
        path = pPath;
    }

    public final String getPath() {
        return path;
    }
}
