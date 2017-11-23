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

import com.hazelcast.config.Config;
import com.hazelcast.config.ReliableTopicConfig;
import com.hazelcast.topic.TopicOverloadPolicy;

import java.util.Map;

import static ch.sourcepond.io.distributor.impl.binding.Validations.optional;
import static com.hazelcast.topic.TopicOverloadPolicy.BLOCK;

class TopicConfigsFactory {

    static final String RESPONSE_TOPIC_DEFAULT_NAME = "__fs_distributor.response";
    static final String RESPONSE_TOPIC_NAME = "hazelcast.response.topic.name";
    static final String RESPONSE_TOPIC_OVERLOAD_POLICY = "hazelcast.response.topic.policy";
    static final int RESPONSE_TOPIC_DEFAULT_READ_BATCH_SIZE = 50;
    static final String RESPONSE_TOPIC_READ_BATCH_SIZE = "hazelcast.response.topic.readBatchSize";
    static final boolean RESPONSE_TOPIC_DEFAULT_STATISTICS_ENABLED = false;
    static final String RESPONSE_TOPIC_STATISTICS_ENABLED = "hazelcast.response.topic.statisticsEnabled";

    static final String DELETE_TOPIC_DEFAULT_NAME = "__fs_distributor.delete";
    static final String DELETE_TOPIC_NAME = "hazelcast.delete.topic.name";
    static final String DELETE_TOPIC_OVERLOAD_POLICY = "hazelcast.delete.topic.policy";
    static final int DELETE_TOPIC_DEFAULT_READ_BATCH_SIZE = 50;
    static final String DELETE_TOPIC_READ_BATCH_SIZE = "hazelcast.delete.topic.readBatchSize";
    static final boolean DELETE_TOPIC_DEFAULT_STATISTICS_ENABLED = false;
    static final String DELETE_TOPIC_STATISTICS_ENABLED = "hazelcast.delete.topic.statisticsEnabled";

    static final String TRANSFER_TOPIC_DEFAULT_NAME = "__fs_distributor.transfer";
    static final String TRANSFER_TOPIC_NAME = "hazelcast.transfer.topic.name";
    static final String TRANSFER_TOPIC_OVERLOAD_POLICY = "hazelcast.transfer.topic.policy";
    static final int TRANSFER_TOPIC_DEFAULT_READ_BATCH_SIZE = 50;
    static final String TRANSFER_TOPIC_READ_BATCH_SIZE = "hazelcast.transfer.topic.readBatchSize";
    static final boolean TRANSFER_TOPIC_DEFAULT_STATISTICS_ENABLED = false;
    static final String TRANSFER_TOPIC_STATISTICS_ENABLED = "hazelcast.transfer.topic.statisticsEnabled";

    static final String STORE_TOPIC_DEFAULT_NAME = "__fs_distributor.store";
    static final String STORE_TOPIC_NAME = "hazelcast.store.topic.name";
    static final String STORE_TOPIC_OVERLOAD_POLICY = "hazelcast.store.topic.policy";
    static final int STORE_TOPIC_DEFAULT_READ_BATCH_SIZE = 50;
    static final String STORE_TOPIC_READ_BATCH_SIZE = "hazelcast.store.topic.readBatchSize";
    static final boolean STORE_TOPIC_DEFAULT_STATISTICS_ENABLED = false;
    static final String STORE_TOPIC_STATISTICS_ENABLED = "hazelcast.store.topic.statisticsEnabled";

    static final String LOCK_TOPIC_DEFAULT_NAME = "__fs_distributor.lock";
    static final String LOCK_TOPIC_NAME = "hazelcast.lock.topic.name";
    static final String LOCK_TOPIC_OVERLOAD_POLICY = "hazelcast.lock.topic.policy";
    static final int LOCK_TOPIC_DEFAULT_READ_BATCH_SIZE = 50;
    static final String LOCK_TOPIC_READ_BATCH_SIZE = "hazelcast.lock.topic.readBatchSize";
    static final boolean LOCK_TOPIC_DEFAULT_STATISTICS_ENABLED = true;
    static final String LOCK_TOPIC_STATISTICS_ENABLED = "hazelcast.lock.topic.statisticsEnabled";

    static final String UNLOCK_TOPIC_DEFAULT_NAME = "__fs_distributor.unlock";
    static final String UNLOCK_TOPIC_NAME = "hazelcast.unlock.topic.name";
    static final String UNLOCK_TOPIC_OVERLOAD_POLICY = "hazelcast.unlock.topic.policy";
    static final int UNLOCK_TOPIC_DEFAULT_READ_BATCH_SIZE = 50;
    static final String UNLOCK_TOPIC_READ_BATCH_SIZE = "hazelcast.unlock.topic.readBatchSize";
    static final boolean UNLOCK_TOPIC_DEFAULT_STATISTICS_ENABLED = false;
    static final String UNLOCK_TOPIC_STATISTICS_ENABLED = "hazelcast.unlock.topic.statisticsEnabled";

    private Config config;

    TopicConfigsFactory(final Config pConfig) {
        config = pConfig;
    }

    private ReliableTopicConfig createTopicConfig(final String pName,
                                                  final TopicOverloadPolicy pOverloadPolicy,
                                                  final int pReadBatchSize,
                                                  final boolean pEnabledStatistics) {
        return config.getReliableTopicConfig(pName)
                .setTopicOverloadPolicy(pOverloadPolicy)
                .setReadBatchSize(pReadBatchSize)
                .setStatisticsEnabled(pEnabledStatistics);
    }

    public TopicConfigs create(final Map<String, String> pInstantiationProperties) {
        final TopicConfigs topicConfigs = new TopicConfigs();

        topicConfigs.setResponseTopicConfig(createTopicConfig(optional(RESPONSE_TOPIC_NAME, RESPONSE_TOPIC_DEFAULT_NAME, pInstantiationProperties, Validations::same),
                optional(RESPONSE_TOPIC_OVERLOAD_POLICY, BLOCK, pInstantiationProperties, TopicOverloadPolicy::valueOf),
                optional(RESPONSE_TOPIC_READ_BATCH_SIZE, RESPONSE_TOPIC_DEFAULT_READ_BATCH_SIZE, pInstantiationProperties, Integer::valueOf),
                optional(RESPONSE_TOPIC_STATISTICS_ENABLED, RESPONSE_TOPIC_DEFAULT_STATISTICS_ENABLED, pInstantiationProperties, Boolean::valueOf)));

        topicConfigs.setDeleteTopicConfig(createTopicConfig(optional(DELETE_TOPIC_NAME, DELETE_TOPIC_DEFAULT_NAME, pInstantiationProperties, Validations::same),
                optional(DELETE_TOPIC_OVERLOAD_POLICY, BLOCK, pInstantiationProperties, TopicOverloadPolicy::valueOf),
                optional(DELETE_TOPIC_READ_BATCH_SIZE, DELETE_TOPIC_DEFAULT_READ_BATCH_SIZE, pInstantiationProperties, Integer::valueOf),
                optional(DELETE_TOPIC_STATISTICS_ENABLED, DELETE_TOPIC_DEFAULT_STATISTICS_ENABLED, pInstantiationProperties, Boolean::valueOf)));

        topicConfigs.setTransferTopicConfig(createTopicConfig(optional(TRANSFER_TOPIC_NAME, TRANSFER_TOPIC_DEFAULT_NAME, pInstantiationProperties, Validations::same),
                optional(TRANSFER_TOPIC_OVERLOAD_POLICY, BLOCK, pInstantiationProperties, TopicOverloadPolicy::valueOf),
                optional(TRANSFER_TOPIC_READ_BATCH_SIZE, TRANSFER_TOPIC_DEFAULT_READ_BATCH_SIZE, pInstantiationProperties, Integer::valueOf),
                optional(TRANSFER_TOPIC_STATISTICS_ENABLED, TRANSFER_TOPIC_DEFAULT_STATISTICS_ENABLED, pInstantiationProperties, Boolean::valueOf)));

        topicConfigs.setStoreTopicConfig(createTopicConfig(optional(STORE_TOPIC_NAME, STORE_TOPIC_DEFAULT_NAME, pInstantiationProperties, Validations::same),
                optional(STORE_TOPIC_OVERLOAD_POLICY, BLOCK, pInstantiationProperties, TopicOverloadPolicy::valueOf),
                optional(STORE_TOPIC_READ_BATCH_SIZE, STORE_TOPIC_DEFAULT_READ_BATCH_SIZE, pInstantiationProperties, Integer::valueOf),
                optional(STORE_TOPIC_STATISTICS_ENABLED, STORE_TOPIC_DEFAULT_STATISTICS_ENABLED, pInstantiationProperties, Boolean::valueOf)));

        topicConfigs.setLockTopicConfig(createTopicConfig(optional(LOCK_TOPIC_NAME, LOCK_TOPIC_DEFAULT_NAME, pInstantiationProperties, Validations::same),
                optional(LOCK_TOPIC_OVERLOAD_POLICY, BLOCK, pInstantiationProperties, TopicOverloadPolicy::valueOf),
                optional(LOCK_TOPIC_READ_BATCH_SIZE, LOCK_TOPIC_DEFAULT_READ_BATCH_SIZE, pInstantiationProperties, Integer::valueOf),
                optional(LOCK_TOPIC_STATISTICS_ENABLED, LOCK_TOPIC_DEFAULT_STATISTICS_ENABLED, pInstantiationProperties, Boolean::valueOf)));

        topicConfigs.setUnlockTopicConfig(createTopicConfig(optional(UNLOCK_TOPIC_NAME, UNLOCK_TOPIC_DEFAULT_NAME, pInstantiationProperties, Validations::same),
                optional(UNLOCK_TOPIC_OVERLOAD_POLICY, BLOCK, pInstantiationProperties, TopicOverloadPolicy::valueOf),
                optional(UNLOCK_TOPIC_READ_BATCH_SIZE, UNLOCK_TOPIC_DEFAULT_READ_BATCH_SIZE, pInstantiationProperties, Integer::valueOf),
                optional(UNLOCK_TOPIC_STATISTICS_ENABLED, UNLOCK_TOPIC_DEFAULT_STATISTICS_ENABLED, pInstantiationProperties, Boolean::valueOf)));

        return topicConfigs;
    }
}
