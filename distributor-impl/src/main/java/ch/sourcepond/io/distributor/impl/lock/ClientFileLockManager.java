/*Copyright (C) 2017 Roland Hauser, <sourcepond@gmail.com>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/
package ch.sourcepond.io.distributor.impl.lock;

import ch.sourcepond.io.distributor.spi.Receiver;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import org.slf4j.Logger;

import java.io.IOException;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * A singleton instance of this class receives requests for acquiring a file-lock for a specific path. If the lock
 * could be acquired, a success messages is sent back to the cluster. In case the lock could not be acquired, an error
 * message is sent back to the cluster.
 */
class ClientFileLockManager {

    /**
     * Listener to acquire a local file-lock.
     */
    private class LockListener implements MessageListener<FileLockResponse> {

        @Override
        public void onMessage(final Message<FileLockResponse> message) {
            final FileLockResponse msg = message.getMessageObject();
            final String path = msg.getPath();
            try {
                receiver.lockLocally(msg.getPath());
                sendFileLockResponseTopic.publish(new FileLockResponse(path));
            } catch (final IOException e) {
                LOG.error(e.getMessage(), e);
                sendFileLockResponseTopic.publish(new FileLockResponse(path, e));
            }
        }
    }

    /**
     * Listener to release a local file-lock.
     */
    private class UnlockListener implements MessageListener<String> {

        @Override
        public void onMessage(final Message<String> message) {
            final String path = message.getMessageObject();
            receiver.unlockLocally(path);
            sendFileUnlockResponseTopic.publish(path);
        }
    }

    private static final Logger LOG = getLogger(ClientFileLockManager.class);
    private final ITopic<FileLockResponse> sendFileLockResponseTopic;
    private final ITopic<String> sendFileUnlockResponseTopic;
    private final Receiver receiver;
    private volatile String lockListenerRegistrationId;
    private volatile String unlockListenerRegistrationId;

    /**
     * Creates a new instance of this class
     *
     * @param pSendFileLockResponseTopic
     * @param pSendFileUnlockResponseTopic
     * @param pReceiver
     */
    public ClientFileLockManager(final ITopic<FileLockResponse> pSendFileLockResponseTopic,
                                 final ITopic<String> pSendFileUnlockResponseTopic,
                                 final Receiver pReceiver) {
        sendFileLockResponseTopic = pSendFileLockResponseTopic;
        sendFileUnlockResponseTopic = pSendFileUnlockResponseTopic;
        receiver = pReceiver;
    }

    public void registerListeners() {
        lockListenerRegistrationId = sendFileLockResponseTopic.addMessageListener(new LockListener());
        unlockListenerRegistrationId = sendFileUnlockResponseTopic.addMessageListener(new UnlockListener());
    }

    private void unregisterListeners(final ITopic<?> pTopic, final String pRegistrationId) {
        if (pRegistrationId != null) {
            pTopic.removeMessageListener(pRegistrationId);
        }
    }

    public void unregisterListeners() {
        unregisterListeners(sendFileLockResponseTopic, lockListenerRegistrationId);
        unregisterListeners(sendFileUnlockResponseTopic, unlockListenerRegistrationId);
    }
}
