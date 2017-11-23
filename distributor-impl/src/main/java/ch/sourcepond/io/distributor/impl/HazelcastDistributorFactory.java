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
import ch.sourcepond.io.distributor.impl.binding.HazelcastBinding;
import ch.sourcepond.io.distributor.impl.binding.HazelcastBindingFactory;
import ch.sourcepond.io.distributor.spi.Receiver;
import ch.sourcepond.io.distributor.spi.TimeoutConfig;
import com.hazelcast.core.HazelcastInstance;

import java.util.Map;

public class HazelcastDistributorFactory implements DistributorFactory {
    private final HazelcastBindingFactory hazelcastBindingFactory;

    public HazelcastDistributorFactory(final HazelcastBindingFactory pHazelcastBindingFactory) {
        hazelcastBindingFactory = pHazelcastBindingFactory;
    }


    @Override
    public Distributor create(final Receiver pReceiver, final TimeoutConfig pTimeoutConfig, final Map<String, String> pInstantiationProperties) {
        final HazelcastBinding hazelcastBinding = hazelcastBindingFactory.create(pInstantiationProperties);
        final HazelcastInstance hci = hazelcastBinding.getHci();

        final ClusterResponseBarrierFactory clusterResponseBarrierFactory = new ClusterResponseBarrierFactory(hazelcastBinding.getResponseTopic(), pTimeoutConfig, hci.getCluster());


        return null;
    }
}
