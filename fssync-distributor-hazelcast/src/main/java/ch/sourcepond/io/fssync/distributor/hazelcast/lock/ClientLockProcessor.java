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
package ch.sourcepond.io.fssync.distributor.hazelcast.lock;

import ch.sourcepond.io.fssync.distributor.hazelcast.common.ClientMessageProcessor;
import ch.sourcepond.io.fssync.distributor.hazelcast.common.DistributionMessage;
import ch.sourcepond.io.fssync.target.api.NodeInfo;
import ch.sourcepond.io.fssync.target.api.SyncPath;
import ch.sourcepond.io.fssync.target.api.SyncTarget;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.MemberAttributeEvent;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;

import javax.inject.Inject;
import java.io.IOException;

/**
 * Listener to acquire a local file-lock.
 */
final class ClientLockProcessor extends ClientMessageProcessor<DistributionMessage> implements MembershipListener {
    private final HazelcastInstance hci;

    @Inject
    public ClientLockProcessor(final HazelcastInstance pHci, final SyncTarget pSyncTarget) {
        super(pSyncTarget);
        hci = pHci;
    }

    @Override
    public void processMessage(final NodeInfo pNodeInfo, final SyncPath pPath, final DistributionMessage pMessage) throws IOException {
        syncTarget.lock(pNodeInfo, pPath);
    }

    @Override
    public void memberAdded(final MembershipEvent membershipEvent) {
        // noop
    }

    @Override
    public void memberRemoved(final MembershipEvent membershipEvent) {
        syncTarget.cancel(new NodeInfo(membershipEvent.getMember().getUuid(), hci.getLocalEndpoint().getUuid()));
    }

    @Override
    public void memberAttributeChanged(final MemberAttributeEvent memberAttributeEvent) {
        // noop
    }
}
