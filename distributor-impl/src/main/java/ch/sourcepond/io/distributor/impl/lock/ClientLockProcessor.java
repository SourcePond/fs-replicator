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

import ch.sourcepond.io.distributor.api.GlobalPath;
import ch.sourcepond.io.distributor.impl.common.ClientMessageProcessor;
import ch.sourcepond.io.distributor.spi.Receiver;
import com.hazelcast.core.MemberAttributeEvent;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;

import javax.inject.Inject;
import java.io.IOException;

/**
 * Listener to acquire a local file-lock.
 */
final class ClientLockProcessor extends ClientMessageProcessor<String> implements MembershipListener {

    @Inject
    public ClientLockProcessor(final Receiver pReceiver) {
        super(pReceiver);
    }

    @Override
    public void processMessage(final GlobalPath pPath, final String pMessage) throws IOException {
        receiver.lockLocally(pPath);
    }

    @Override
    public String toPath(final String pMessage) {
        return pMessage;
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
