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
package ch.sourcepond.io.fssync.distributor.hazelcast.common;

import ch.sourcepond.io.fssync.common.api.SyncPath;
import ch.sourcepond.io.fssync.target.api.NodeInfo;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import org.slf4j.Logger;

import java.io.IOException;

import static org.slf4j.LoggerFactory.getLogger;

final class ClientMessageListener<T extends DistributionMessage> implements MessageListener<T> {
    private static final Logger LOG = getLogger(ClientMessageListener.class);
    private final HazelcastInstance hci;
    private final ClientMessageProcessor<T> processor;
    private final ITopic<StatusMessage> sendResponseTopic;

    public ClientMessageListener(final HazelcastInstance pHci,
                                 final ClientMessageProcessor<T> pProcessor,
                                 final ITopic<StatusMessage> pSendResponseTopic) {
        hci = pHci;
        processor = pProcessor;
        sendResponseTopic = pSendResponseTopic;
    }

    @Override
    public final void onMessage(final Message<T> message) {
        final NodeInfo nodeInfo = new NodeInfo(message.getPublishingMember().getUuid(), hci.getLocalEndpoint().getUuid());
        final T payload = message.getMessageObject();
        final SyncPath syncPath = payload.getPath();

        try {
            processor.processMessage(nodeInfo, syncPath, payload);
            sendResponseTopic.publish(new StatusMessage(syncPath));
        } catch (final IOException e) {
            LOG.error(e.getMessage(), e);
            sendResponseTopic.publish(new StatusMessage(syncPath, e));
        }
    }
}
