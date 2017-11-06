package ch.sourcepond.io.distributor.impl;

public class DataMessage extends DistributorMessage {
    private final byte[] payload;

    public DataMessage(final String pPath, final byte[] pPayload) {
        super(pPath);
        payload = pPayload;
    }
}
