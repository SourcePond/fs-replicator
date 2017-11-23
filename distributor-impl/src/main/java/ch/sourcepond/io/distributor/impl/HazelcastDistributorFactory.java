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
package ch.sourcepond.io.distributor.impl;

import ch.sourcepond.io.distributor.api.Distributor;
import ch.sourcepond.io.distributor.api.DistributorFactory;
import ch.sourcepond.io.distributor.impl.response.ClusterResponseBarrierFactory;
import ch.sourcepond.io.distributor.impl.topics.Topics;
import ch.sourcepond.io.distributor.impl.topics.TopicsFactory;
import ch.sourcepond.io.distributor.spi.Receiver;
import ch.sourcepond.io.distributor.spi.TimeoutConfig;
import com.hazelcast.core.HazelcastInstance;

import java.util.Map;

public class HazelcastDistributorFactory implements DistributorFactory {
    private final TopicsFactory topicsFactory;

    public HazelcastDistributorFactory(final  TopicsFactory pTopicsFactory) {
        topicsFactory = pTopicsFactory;
    }


    @Override
    public Distributor create(final Receiver pReceiver, final TimeoutConfig pTimeoutConfig, final Map<String, String> pInstantiationProperties) {
        final Topics topics = topicsFactory.create(pInstantiationProperties);
        final HazelcastInstance hci = topics.getHci();

        final ClusterResponseBarrierFactory clusterResponseBarrierFactory = new ClusterResponseBarrierFactory(topics.getResponseTopic(), pTimeoutConfig, hci.getCluster());


        return null;
    }
}
