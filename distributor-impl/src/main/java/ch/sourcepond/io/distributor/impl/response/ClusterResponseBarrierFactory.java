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
package ch.sourcepond.io.distributor.impl.response;

import ch.sourcepond.io.distributor.impl.common.StatusMessage;
import ch.sourcepond.io.distributor.spi.TimeoutConfig;
import com.hazelcast.core.Cluster;
import com.hazelcast.core.ITopic;

import java.io.Serializable;

/**
 * Factory to create {@link ClusterResponseBarrier} instances.
 */
public class ClusterResponseBarrierFactory {
    private final ITopic<StatusMessage> responseTopic;
    private final TimeoutConfig timeoutConfig;
    private final Cluster cluster;

    public ClusterResponseBarrierFactory(final ITopic<StatusMessage> pResponseTopic,
                                         final TimeoutConfig pTimeoutConfig,
                                         final Cluster pCluster) {
        assert pResponseTopic != null : "pResponseTopic is null";
        assert pTimeoutConfig != null : "pTimeoutConfig is null";
        assert pCluster != null : "pCluster cannot be null";
        responseTopic = pResponseTopic;
        timeoutConfig = pTimeoutConfig;
        cluster = pCluster;
    }

    public <T extends Serializable> ClusterResponseBarrier<T> create(final String pPath, final ITopic<T> pRequestTopic) {
        return new ClusterResponseBarrierImpl<T>(pPath, pRequestTopic, responseTopic, timeoutConfig, cluster);
    }
}