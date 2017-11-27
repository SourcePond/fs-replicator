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
package ch.sourcepond.io.fssync.distributor.impl.common;

import ch.sourcepond.io.fssync.distributor.api.GlobalPath;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;

final class ClientMessageListener<T extends Serializable> implements MessageListener<T> {
    private static final Logger LOG = LoggerFactory.getLogger(ClientMessageListener.class);
    private final ClientMessageProcessor<T> processor;
    private final ITopic<StatusMessage> sendResponseTopic;

    public ClientMessageListener(final ClientMessageProcessor<T> pProcessor, final ITopic<StatusMessage> pSendResponseTopic) {
        processor = pProcessor;
        sendResponseTopic = pSendResponseTopic;
    }

    @Override
    public final void onMessage(final Message<T> message) {
        final T payload = message.getMessageObject();
        final String path = processor.toPath(payload);
        final GlobalPath globalPath = new GlobalPath(message.getPublishingMember().getUuid(), path);

        try {
            processor.processMessage(globalPath, payload);
            sendResponseTopic.publish(new StatusMessage(path));
        } catch (final IOException e) {
            LOG.error(e.getMessage(), e);
            sendResponseTopic.publish(new StatusMessage(path, e));
        }
    }
}
