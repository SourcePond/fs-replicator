package ch.sourcepond.io.distributor.impl;

import ch.sourcepond.io.distributor.api.Distributor;
import ch.sourcepond.io.distributor.api.GlobalLockException;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.MessageListener;

import java.nio.channels.WritableByteChannel;
import java.util.concurrent.TimeUnit;

public class DistributorImpl implements Distributor, MessageListener {
    private final HazelcastInstance hci;
    private final ITopic<AcquireLockMessage> lockTopic;
    private final ITopic<DataMessage> payloadTopic;

    public DistributorImpl(final HazelcastInstance pHci,
                           final ITopic<AcquireLockMessage> pLockTopic,
                           final ITopic<DataMessage> pPayloadTopic) {
        hci = pHci;
        lockTopic = pLockTopic;
        payloadTopic = pPayloadTopic;
    }

    @Override
    public String getLocalNode() {
        return hci.getLocalEndpoint().getUuid();
    }

    @Override
    public void delete(final String pPath) {
        hci.getTopic(pPath);
    }

    @Override
    public WritableByteChannel openChannel(String pPath) {
        return null;
    }

    @Override
    public void lockGlobally(final String pPath, final TimeUnit pTimeoutUnit, final long pTimeout) throws GlobalLockException {
        final ILock lock = hci.getLock(pPath);
        if (lock.tryLock(pTimeout, pTimeoutUnit)) {
            try {

            } finally {
                lock.unlock();
            }
        }
    }

    @Override
    public void unlockGlobally(String pPath) {

    }

    @Override
    public byte[] getGlobalChecksum(String pFile) {
        return new byte[0];
    }
}
