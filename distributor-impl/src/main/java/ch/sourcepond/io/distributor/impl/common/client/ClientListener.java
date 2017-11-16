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
package ch.sourcepond.io.distributor.impl.common.client;

import ch.sourcepond.io.distributor.api.GlobalPath;
import ch.sourcepond.io.distributor.impl.common.master.StatusResponse;
import ch.sourcepond.io.distributor.spi.Receiver;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import org.slf4j.Logger;

import java.io.IOException;

public abstract class ClientListener<T> implements MessageListener<T> {
    protected final Receiver receiver;
    private final ITopic<StatusResponse> sendResponseTopic;

    public ClientListener(final Receiver pReceiver, final ITopic<StatusResponse> pSendResponseTopic) {
        receiver = pReceiver;
        sendResponseTopic = pSendResponseTopic;
    }

    protected abstract Logger getLog();

    protected abstract void processMessage(GlobalPath pPath, T pPayload) throws IOException;

    protected abstract String toPath(T pPayload);

    @Override
    public final void onMessage(final Message<T> message) {
        final T payload = message.getMessageObject();
        final String path = toPath(payload);
        final GlobalPath globalPath = new GlobalPath(message.getPublishingMember().getUuid(), path);

        try {
            processMessage(globalPath, payload);
            sendResponseTopic.publish(new StatusResponse(path));
        } catch (final IOException e) {
            getLog().error(e.getMessage(), e);
            sendResponseTopic.publish(new StatusResponse(path, e));
        }
    }
}
