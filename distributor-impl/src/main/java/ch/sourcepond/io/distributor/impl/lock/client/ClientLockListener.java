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
package ch.sourcepond.io.distributor.impl.lock.client;

import ch.sourcepond.io.distributor.api.GlobalPath;
import ch.sourcepond.io.distributor.impl.common.client.ClientListener;
import ch.sourcepond.io.distributor.impl.common.master.StatusResponse;
import ch.sourcepond.io.distributor.spi.Receiver;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.MemberAttributeEvent;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;
import org.slf4j.Logger;

import java.io.IOException;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Listener to acquire a local file-lock.
 */
class ClientLockListener extends ClientListener<String> implements MembershipListener {
    private static final Logger LOG = getLogger(ClientLockListener.class);

    public ClientLockListener(final Receiver pReceiver, final ITopic<StatusResponse> pSendFileLockResponseTopic) {
        super(pReceiver, pSendFileLockResponseTopic);
    }

    @Override
    protected Logger getLog() {
        return LOG;
    }

    @Override
    protected void processMessage(final GlobalPath pPath, final String pPayload) throws IOException {
        receiver.lockLocally(pPath);
    }

    @Override
    protected String toPath(final String pPayload) {
        return pPayload;
    }

    @Override
    public void memberAdded(final MembershipEvent membershipEvent) {
        // noop
    }

    @Override
    public void memberRemoved(final MembershipEvent membershipEvent) {
        receiver.kill(membershipEvent.getMember().getUuid());
    }

    @Override
    public void memberAttributeChanged(final MemberAttributeEvent memberAttributeEvent) {
        // noop
    }
}
