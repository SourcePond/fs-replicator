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
 * Listener to acquire a local file-lock.
 */
class ClientLockListener implements MessageListener<String> {
    private static final Logger LOG = getLogger(ClientLockListener.class);
    private final Receiver receiver;
    private final ITopic<FileLockResponse> sendFileLockResponseTopic;

    public ClientLockListener(final Receiver pReceiver, final ITopic<FileLockResponse> pSendFileLockResponseTopic) {
        receiver = pReceiver;
        sendFileLockResponseTopic = pSendFileLockResponseTopic;
    }

    @Override
    public void onMessage(final Message<String> message) {
        final String path = message.getMessageObject();
        try {
            receiver.lockLocally(path);
            sendFileLockResponseTopic.publish(new FileLockResponse(path));
        } catch (final IOException e) {
            LOG.error(e.getMessage(), e);
            sendFileLockResponseTopic.publish(new FileLockResponse(path, e));
        }
    }
}
