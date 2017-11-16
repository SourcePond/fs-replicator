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
package ch.sourcepond.io.distributor.impl.common;

import ch.sourcepond.io.distributor.impl.common.master.MasterResponseListener;
import ch.sourcepond.io.distributor.impl.common.master.StatusResponse;
import com.hazelcast.core.Cluster;
import com.hazelcast.core.ITopic;

import java.util.concurrent.TimeoutException;

public abstract class ResponseAwaitingManager {
    protected final Cluster cluster;
    private final ITopic<StatusResponse> responseTopic;

    protected ResponseAwaitingManager(final Cluster pCluster, final ITopic<StatusResponse> pResponseTopic) {
        cluster = pCluster;
        responseTopic = pResponseTopic;
    }

    protected <T, E extends Exception> void performAction(final ITopic<T> pSenderTopic,
                                                          final T pMessage,
                                                          final MasterResponseListener<E> pListener)
            throws TimeoutException, E {
        final String membershipId = cluster.addMembershipListener(pListener);
        try {
            final String registrationId = responseTopic.addMessageListener(pListener);
            try {
                pSenderTopic.publish(pMessage);
                pListener.awaitNodeAnswers();
            } finally {
                responseTopic.removeMessageListener(registrationId);
            }
        } finally {
            cluster.removeMembershipListener(membershipId);
        }
    }
}
