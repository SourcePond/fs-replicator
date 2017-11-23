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
package ch.sourcepond.io.distributor.impl.binding;

import ch.sourcepond.io.distributor.impl.common.StatusMessage;
import ch.sourcepond.io.distributor.impl.request.TransferRequest;
import com.hazelcast.config.ReliableTopicConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ITopic;

import java.io.Serializable;

public class HazelcastBinding {
    private final HazelcastInstance hci;
    private final IMap<String, byte[]> checksumMap;
    private final TopicConfigs configs;
    private final TimeoutConfig lockConfig;
    private final TimeoutConfig responseConfig;

    public HazelcastBinding(final HazelcastInstance pHci,
                            final IMap<String, byte[]> pChecksumMap,
                            final TopicConfigs pConfigs,
                            final TimeoutConfig pLockConfig,
                            final TimeoutConfig pResponseConfig) {
        hci = pHci;
        checksumMap = pChecksumMap;
        configs = pConfigs;
        lockConfig = pLockConfig;
        responseConfig = pResponseConfig;
    }

    private <T extends Serializable> ITopic<T> getTopic(final ReliableTopicConfig pConfig) {
        return hci.getTopic(pConfig.getName());
    }

    public IMap<String, byte[]> getChecksums() {
        return checksumMap;
    }

    public HazelcastInstance getHci() {
        return hci;
    }

    public TimeoutConfig getLockConfig() {
        return lockConfig;
    }

    public TimeoutConfig getResponseConfig() {
        return responseConfig;
    }

    public ITopic<StatusMessage> getResponseTopic() {
        return getTopic(configs.getResponseTopicConfig());
    }

    public ITopic<String> getDeleteRequestTopic() {
        return getTopic(configs.getDeleteTopicConfig());
    }

    public ITopic<TransferRequest> getTransferRequestTopic() {
        return getTopic(configs.getTransferTopicConfig());
    }

    public ITopic<StatusMessage> getStoreRequestTopic() {
        return getTopic(configs.getStoreTopicConfig());
    }

    public ITopic<String> getLockRequestTopic() {
        return getTopic(configs.getLockTopicConfig());
    }

    public ITopic<String> getUnlockRequestTopic() {
        return getTopic(configs.getUnlockTopicConfig());
    }
}
