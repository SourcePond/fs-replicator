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
package ch.sourcepond.io.fssync.distributor.impl.binding;

import ch.sourcepond.io.fssync.distributor.impl.annotations.Delete;
import ch.sourcepond.io.fssync.distributor.impl.annotations.Discard;
import ch.sourcepond.io.fssync.distributor.impl.annotations.Lock;
import ch.sourcepond.io.fssync.distributor.impl.annotations.Response;
import ch.sourcepond.io.fssync.distributor.impl.annotations.Store;
import ch.sourcepond.io.fssync.distributor.impl.annotations.Transfer;
import ch.sourcepond.io.fssync.distributor.impl.annotations.Unlock;
import ch.sourcepond.io.fssync.distributor.impl.common.StatusMessage;
import ch.sourcepond.io.fssync.distributor.impl.request.TransferRequest;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.hazelcast.config.ReliableTopicConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ITopic;
import com.hazelcast.topic.TopicOverloadPolicy;

import javax.inject.Singleton;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static ch.sourcepond.io.fssync.distributor.impl.binding.Validations.mandatory;
import static ch.sourcepond.io.fssync.distributor.impl.binding.Validations.optional;
import static com.hazelcast.core.Hazelcast.getHazelcastInstanceByName;
import static com.hazelcast.topic.TopicOverloadPolicy.BLOCK;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class BindingModule extends AbstractModule {
    public static final String EXISTING_INSTANCE_NAME = "hazelcast.existing.instance.name";
    public static final String DEFAULT_CHECKSUM_MAP_NAME = "__fs_replicator.checksums";
    public static final String CHECKSUM_MAP_NAME = "hazelcast.checksum.mapName";
    public static final String LOCK_TIMEOUT_UNIT = "hazelcast.lock.timeoutUnit";
    public static final String LOCK_TIMEOUT = "hazelcast.lock.timeout";
    public static final String RESPONSE_TIMEOUT_UNIT = "hazelcast.response.timeoutUnit";
    public static final String RESPONSE_TIMEOUT = "hazelcast.response.timeout";

    public static final String RESPONSE_TOPIC_DEFAULT_NAME = "__fs_distributor.response";
    public static final String RESPONSE_TOPIC_NAME = "hazelcast.response.topic.name";
    public static final String RESPONSE_TOPIC_OVERLOAD_POLICY = "hazelcast.response.topic.policy";
    public static final int RESPONSE_TOPIC_DEFAULT_READ_BATCH_SIZE = 50;
    public static final String RESPONSE_TOPIC_READ_BATCH_SIZE = "hazelcast.response.topic.readBatchSize";
    public static final boolean RESPONSE_TOPIC_DEFAULT_STATISTICS_ENABLED = false;
    public static final String RESPONSE_TOPIC_STATISTICS_ENABLED = "hazelcast.response.topic.statisticsEnabled";

    public static final String DELETE_TOPIC_DEFAULT_NAME = "__fs_distributor.delete";
    public static final String DELETE_TOPIC_NAME = "hazelcast.delete.topic.name";
    public static final String DELETE_TOPIC_OVERLOAD_POLICY = "hazelcast.delete.topic.policy";
    public static final int DELETE_TOPIC_DEFAULT_READ_BATCH_SIZE = 50;
    public static final String DELETE_TOPIC_READ_BATCH_SIZE = "hazelcast.delete.topic.readBatchSize";
    public static final boolean DELETE_TOPIC_DEFAULT_STATISTICS_ENABLED = false;
    public static final String DELETE_TOPIC_STATISTICS_ENABLED = "hazelcast.delete.topic.statisticsEnabled";

    public static final String TRANSFER_TOPIC_DEFAULT_NAME = "__fs_distributor.transfer";
    public static final String TRANSFER_TOPIC_NAME = "hazelcast.transfer.topic.name";
    public static final String TRANSFER_TOPIC_OVERLOAD_POLICY = "hazelcast.transfer.topic.policy";
    public static final int TRANSFER_TOPIC_DEFAULT_READ_BATCH_SIZE = 50;
    public static final String TRANSFER_TOPIC_READ_BATCH_SIZE = "hazelcast.transfer.topic.readBatchSize";
    public static final boolean TRANSFER_TOPIC_DEFAULT_STATISTICS_ENABLED = false;
    public static final String TRANSFER_TOPIC_STATISTICS_ENABLED = "hazelcast.transfer.topic.statisticsEnabled";

    public static final String DISCARD_TOPIC_DEFAULT_NAME = "__fs_distributor.discard";
    public static final String DISCARD_TOPIC_NAME = "hazelcast.discard.topic.name";
    public static final String DISCARD_TOPIC_OVERLOAD_POLICY = "hazelcast.discard.topic.policy";
    public static final int DISCARD_TOPIC_DEFAULT_READ_BATCH_SIZE = 50;
    public static final String DISCARD_TOPIC_READ_BATCH_SIZE = "hazelcast.discard.topic.readBatchSize";
    public static final boolean DISCARD_TOPIC_DEFAULT_STATISTICS_ENABLED = false;
    public static final String DISCARD_TOPIC_STATISTICS_ENABLED = "hazelcast.discard.topic.statisticsEnabled";

    public static final String STORE_TOPIC_DEFAULT_NAME = "__fs_distributor.store";
    public static final String STORE_TOPIC_NAME = "hazelcast.store.topic.name";
    public static final String STORE_TOPIC_OVERLOAD_POLICY = "hazelcast.store.topic.policy";
    public static final int STORE_TOPIC_DEFAULT_READ_BATCH_SIZE = 50;
    public static final String STORE_TOPIC_READ_BATCH_SIZE = "hazelcast.store.topic.readBatchSize";
    public static final boolean STORE_TOPIC_DEFAULT_STATISTICS_ENABLED = false;
    public static final String STORE_TOPIC_STATISTICS_ENABLED = "hazelcast.store.topic.statisticsEnabled";

    public static final String LOCK_TOPIC_DEFAULT_NAME = "__fs_distributor.lock";
    public static final String LOCK_TOPIC_NAME = "hazelcast.lock.topic.name";
    public static final String LOCK_TOPIC_OVERLOAD_POLICY = "hazelcast.lock.topic.policy";
    public static final int LOCK_TOPIC_DEFAULT_READ_BATCH_SIZE = 50;
    public static final String LOCK_TOPIC_READ_BATCH_SIZE = "hazelcast.lock.topic.readBatchSize";
    public static final boolean LOCK_TOPIC_DEFAULT_STATISTICS_ENABLED = true;
    public static final String LOCK_TOPIC_STATISTICS_ENABLED = "hazelcast.lock.topic.statisticsEnabled";

    public static final String UNLOCK_TOPIC_DEFAULT_NAME = "__fs_distributor.unlock";
    public static final String UNLOCK_TOPIC_NAME = "hazelcast.unlock.topic.name";
    public static final String UNLOCK_TOPIC_OVERLOAD_POLICY = "hazelcast.unlock.topic.policy";
    public static final int UNLOCK_TOPIC_DEFAULT_READ_BATCH_SIZE = 50;
    public static final String UNLOCK_TOPIC_READ_BATCH_SIZE = "hazelcast.unlock.topic.readBatchSize";
    public static final boolean UNLOCK_TOPIC_DEFAULT_STATISTICS_ENABLED = false;
    public static final String UNLOCK_TOPIC_STATISTICS_ENABLED = "hazelcast.unlock.topic.statisticsEnabled";

    private final Map<String, String> instantiationProperties;

    public BindingModule(final Map<String, String> pInstantiationProperties) {
        instantiationProperties = pInstantiationProperties;
    }

    private static TimeoutConfig create(final String pUnitKey, final String pTimeoutKey, final Map<String, String> pInstantiationProperties) {
        return new TimeoutConfig(mandatory(pUnitKey, pInstantiationProperties, TimeUnit::valueOf),
                mandatory(pTimeoutKey, pInstantiationProperties, Long::valueOf));
    }

    private static <T extends Serializable> ITopic<T> getTopic(final HazelcastInstance pHci, final ReliableTopicConfig pConfig) {
        return pHci.getTopic(pConfig.getName());
    }

    private static ReliableTopicConfig createTopicConfig(final HazelcastInstance pHci,
                                                         final String pName,
                                                         final TopicOverloadPolicy pOverloadPolicy,
                                                         final int pReadBatchSize,
                                                         final boolean pEnabledStatistics) {
        return pHci.getConfig().getReliableTopicConfig(pName)
                .setTopicOverloadPolicy(pOverloadPolicy)
                .setReadBatchSize(pReadBatchSize)
                .setStatisticsEnabled(pEnabledStatistics);
    }

    @Override
    protected void configure() {
        bind(new TypeLiteral<Map<String, String>>() {
        }).toInstance(instantiationProperties);
    }

    @Provides
    @Singleton
    HazelcastInstance hazelcastInstance(final Map<String, String> pInstantiationProperties) {
        final String name = mandatory(EXISTING_INSTANCE_NAME, pInstantiationProperties, Validations::same);
        return requireNonNull(getHazelcastInstanceByName(name),
                format("No Hazelcast instance found with name %s, property %s must specify an existing instance", name, EXISTING_INSTANCE_NAME));
    }

    @Provides
    @Singleton
    @Lock
    TimeoutConfig lockTimeoutConfig(final Map<String, String> pInstantiationProperties) {
        return create(LOCK_TIMEOUT_UNIT, LOCK_TIMEOUT, pInstantiationProperties);
    }

    @Provides
    @Singleton
    @Response
    TimeoutConfig responseTimeoutConfig(final Map<String, String> pInstantiationProperties) {
        return create(RESPONSE_TIMEOUT_UNIT, RESPONSE_TIMEOUT, pInstantiationProperties);
    }

    @Provides
    @Singleton
    @Response
    ReliableTopicConfig responseTopicConfig(final HazelcastInstance pHci, final Map<String, String> pInstantiationProperties) {
        return createTopicConfig(pHci,
                optional(RESPONSE_TOPIC_NAME, RESPONSE_TOPIC_DEFAULT_NAME, pInstantiationProperties, Validations::same),
                optional(RESPONSE_TOPIC_OVERLOAD_POLICY, BLOCK, pInstantiationProperties, TopicOverloadPolicy::valueOf),
                optional(RESPONSE_TOPIC_READ_BATCH_SIZE, RESPONSE_TOPIC_DEFAULT_READ_BATCH_SIZE, pInstantiationProperties, Integer::valueOf),
                optional(RESPONSE_TOPIC_STATISTICS_ENABLED, RESPONSE_TOPIC_DEFAULT_STATISTICS_ENABLED, pInstantiationProperties, Boolean::valueOf));
    }

    @Provides
    @Singleton
    @Delete
    ReliableTopicConfig deleteTopicConfig(final HazelcastInstance pHci, final Map<String, String> pInstantiationProperties) {
        return createTopicConfig(pHci,
                optional(DELETE_TOPIC_NAME, DELETE_TOPIC_DEFAULT_NAME, instantiationProperties, Validations::same),
                optional(DELETE_TOPIC_OVERLOAD_POLICY, BLOCK, instantiationProperties, TopicOverloadPolicy::valueOf),
                optional(DELETE_TOPIC_READ_BATCH_SIZE, DELETE_TOPIC_DEFAULT_READ_BATCH_SIZE, instantiationProperties, Integer::valueOf),
                optional(DELETE_TOPIC_STATISTICS_ENABLED, DELETE_TOPIC_DEFAULT_STATISTICS_ENABLED, instantiationProperties, Boolean::valueOf));
    }

    @Provides
    @Singleton
    @Transfer
    ReliableTopicConfig transferTopicConfig(final HazelcastInstance pHci, final Map<String, String> pInstantiationProperties) {
        return createTopicConfig(pHci, optional(TRANSFER_TOPIC_NAME, TRANSFER_TOPIC_DEFAULT_NAME, instantiationProperties, Validations::same),
                optional(TRANSFER_TOPIC_OVERLOAD_POLICY, BLOCK, instantiationProperties, TopicOverloadPolicy::valueOf),
                optional(TRANSFER_TOPIC_READ_BATCH_SIZE, TRANSFER_TOPIC_DEFAULT_READ_BATCH_SIZE, instantiationProperties, Integer::valueOf),
                optional(TRANSFER_TOPIC_STATISTICS_ENABLED, TRANSFER_TOPIC_DEFAULT_STATISTICS_ENABLED, instantiationProperties, Boolean::valueOf));
    }

    @Provides
    @Singleton
    @Discard
    ReliableTopicConfig discardTopicConfig(final HazelcastInstance pHci, final Map<String, String> pInstantiationProperties) {
        return createTopicConfig(pHci,
                optional(DISCARD_TOPIC_NAME, DISCARD_TOPIC_DEFAULT_NAME, instantiationProperties, Validations::same),
                optional(DISCARD_TOPIC_OVERLOAD_POLICY, BLOCK, instantiationProperties, TopicOverloadPolicy::valueOf),
                optional(DISCARD_TOPIC_READ_BATCH_SIZE, DISCARD_TOPIC_DEFAULT_READ_BATCH_SIZE, instantiationProperties, Integer::valueOf),
                optional(DISCARD_TOPIC_STATISTICS_ENABLED, DISCARD_TOPIC_DEFAULT_STATISTICS_ENABLED, instantiationProperties, Boolean::valueOf));
    }

    @Provides
    @Singleton
    @Store
    ReliableTopicConfig storeTopicConfig(final HazelcastInstance pHci, final Map<String, String> pInstantiationProperties) {
        return createTopicConfig(pHci,
                optional(STORE_TOPIC_NAME, STORE_TOPIC_DEFAULT_NAME, instantiationProperties, Validations::same),
                optional(STORE_TOPIC_OVERLOAD_POLICY, BLOCK, instantiationProperties, TopicOverloadPolicy::valueOf),
                optional(STORE_TOPIC_READ_BATCH_SIZE, STORE_TOPIC_DEFAULT_READ_BATCH_SIZE, instantiationProperties, Integer::valueOf),
                optional(STORE_TOPIC_STATISTICS_ENABLED, STORE_TOPIC_DEFAULT_STATISTICS_ENABLED, instantiationProperties, Boolean::valueOf));
    }

    @Provides
    @Singleton
    @Lock
    ReliableTopicConfig lockTopicConfig(final HazelcastInstance pHci, final Map<String, String> pInstantiationProperties) {
        return createTopicConfig(pHci,
                optional(LOCK_TOPIC_NAME, LOCK_TOPIC_DEFAULT_NAME, instantiationProperties, Validations::same),
                optional(LOCK_TOPIC_OVERLOAD_POLICY, BLOCK, instantiationProperties, TopicOverloadPolicy::valueOf),
                optional(LOCK_TOPIC_READ_BATCH_SIZE, LOCK_TOPIC_DEFAULT_READ_BATCH_SIZE, instantiationProperties, Integer::valueOf),
                optional(LOCK_TOPIC_STATISTICS_ENABLED, LOCK_TOPIC_DEFAULT_STATISTICS_ENABLED, instantiationProperties, Boolean::valueOf));
    }

    @Provides
    @Singleton
    @Unlock
    ReliableTopicConfig unlockTopicConfig(final HazelcastInstance pHci, final Map<String, String> pInstantiationProperties) {
        return createTopicConfig(pHci,
                optional(UNLOCK_TOPIC_NAME, UNLOCK_TOPIC_DEFAULT_NAME, instantiationProperties, Validations::same),
                optional(UNLOCK_TOPIC_OVERLOAD_POLICY, BLOCK, instantiationProperties, TopicOverloadPolicy::valueOf),
                optional(UNLOCK_TOPIC_READ_BATCH_SIZE, UNLOCK_TOPIC_DEFAULT_READ_BATCH_SIZE, instantiationProperties, Integer::valueOf),
                optional(UNLOCK_TOPIC_STATISTICS_ENABLED, UNLOCK_TOPIC_DEFAULT_STATISTICS_ENABLED, instantiationProperties, Boolean::valueOf));
    }

    @Provides
    @Singleton
    @Response
    ITopic<StatusMessage> responseTopic(final HazelcastInstance pHci, @Response final ReliableTopicConfig pConfig) {
        return getTopic(pHci, pConfig);
    }

    @Provides
    @Singleton
    @Delete
    ITopic<String> deleteRequestTopic(final HazelcastInstance pHci, @Delete final ReliableTopicConfig pConfig) {
        return getTopic(pHci, pConfig);
    }

    @Provides
    @Singleton
    @Transfer
    ITopic<TransferRequest> transferRequestTopic(final HazelcastInstance pHci, @Transfer final ReliableTopicConfig pConfig) {
        return getTopic(pHci, pConfig);
    }

    @Provides
    @Singleton
    @Discard
    ITopic<StatusMessage> discardRequestTopic(final HazelcastInstance pHci, @Discard final ReliableTopicConfig pConfig) {
        return getTopic(pHci, pConfig);
    }

    @Provides
    @Singleton
    @Store
    ITopic<String> storeRequestTopic(final HazelcastInstance pHci, @Store final ReliableTopicConfig pConfig) {
        return getTopic(pHci, pConfig);
    }

    @Provides
    @Singleton
    @Lock
    ITopic<String> lockRequestTopic(final HazelcastInstance pHci, @Lock final ReliableTopicConfig pConfig) {
        return getTopic(pHci, pConfig);
    }

    @Provides
    @Singleton
    @Unlock
    public ITopic<String> unlockRequestTopic(final HazelcastInstance pHci, @Unlock final ReliableTopicConfig pConfig) {
        return getTopic(pHci, pConfig);
    }

    @Provides
    @Singleton
    IMap<String, byte[]> checksumMap(final HazelcastInstance pHci, final Map<String, String> pInstantiationProperties) {
        return pHci.getMap(optional(CHECKSUM_MAP_NAME, DEFAULT_CHECKSUM_MAP_NAME, pInstantiationProperties, Validations::same));
    }
}
