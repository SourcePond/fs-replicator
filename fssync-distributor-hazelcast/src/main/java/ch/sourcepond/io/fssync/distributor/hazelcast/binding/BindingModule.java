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
package ch.sourcepond.io.fssync.distributor.hazelcast.binding;

import ch.sourcepond.io.fssync.distributor.hazelcast.Config;
import ch.sourcepond.io.fssync.distributor.hazelcast.annotations.Delete;
import ch.sourcepond.io.fssync.distributor.hazelcast.annotations.Discard;
import ch.sourcepond.io.fssync.distributor.hazelcast.annotations.Lock;
import ch.sourcepond.io.fssync.distributor.hazelcast.annotations.Response;
import ch.sourcepond.io.fssync.distributor.hazelcast.annotations.Store;
import ch.sourcepond.io.fssync.distributor.hazelcast.annotations.Transfer;
import ch.sourcepond.io.fssync.distributor.hazelcast.annotations.Unlock;
import ch.sourcepond.io.fssync.distributor.hazelcast.common.DistributionMessage;
import ch.sourcepond.io.fssync.distributor.hazelcast.common.StatusMessage;
import ch.sourcepond.io.fssync.distributor.hazelcast.request.TransferRequest;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.hazelcast.config.ReliableTopicConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ITopic;
import com.hazelcast.topic.TopicOverloadPolicy;

import javax.inject.Singleton;
import java.io.Serializable;

import static com.hazelcast.core.Hazelcast.getHazelcastInstanceByName;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class BindingModule extends AbstractModule {

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
        // noop
    }

    @Provides
    @Singleton
    HazelcastInstance hazelcastInstance(final Config pConfig) {
        return requireNonNull(getHazelcastInstanceByName(pConfig.existingInstanceName()),
                format("No Hazelcast instance found with name %s", pConfig.existingInstanceName()));
    }

    @Provides
    @Singleton
    @Response
    ReliableTopicConfig responseTopicConfig(final HazelcastInstance pHci, final Config pConfig) {
        return createTopicConfig(pHci,
                pConfig.responseTopicName(),
                pConfig.responseTopicOverloadPolicy(),
                pConfig.responseTopicReadBatchSize(),
                pConfig.responseTopicStatisticsEnabled());
    }

    @Provides
    @Singleton
    @Delete
    ReliableTopicConfig deleteTopicConfig(final HazelcastInstance pHci, final Config pConfig) {
        return createTopicConfig(pHci,
                pConfig.deleteTopicName(),
                pConfig.deleteTopicOverloadPolicy(),
                pConfig.deleteTopicReadBatchSize(),
                pConfig.deleteTopicStatisticsEnabled());
    }

    @Provides
    @Singleton
    @Transfer
    ReliableTopicConfig transferTopicConfig(final HazelcastInstance pHci, final Config pConfig) {
        return createTopicConfig(pHci,
                pConfig.transferTopicName(),
                pConfig.transferTopicOverloadPolicy(),
                pConfig.transferTopicReadBatchSize(),
                pConfig.transferTopicStatisticsEnabled());
    }

    @Provides
    @Singleton
    @Discard
    ReliableTopicConfig discardTopicConfig(final HazelcastInstance pHci, final Config pConfig) {
        return createTopicConfig(pHci,
                pConfig.discardTopicName(),
                pConfig.discardTopicOverloadPolicy(),
                pConfig.discardTopicReadBatchSize(),
                pConfig.discardTopicStatisticsEnabled());
    }

    @Provides
    @Singleton
    @Store
    ReliableTopicConfig storeTopicConfig(final HazelcastInstance pHci, final Config pConfig) {
        return createTopicConfig(pHci,
                pConfig.storeTopicName(),
                pConfig.storeTopicOverloadPolicy(),
                pConfig.storeTopicReadBatchSize(),
                pConfig.storeTopicStatisticsEnabled());
    }

    @Provides
    @Singleton
    @Lock
    ReliableTopicConfig lockTopicConfig(final HazelcastInstance pHci, final Config pConfig) {
        return createTopicConfig(pHci,
                pConfig.lockTopicName(),
                pConfig.lockTopicOverloadPolicy(),
                pConfig.lockTopicReadBatchSize(),
                pConfig.lockTopicStatisticsEnabled());
    }

    @Provides
    @Singleton
    @Unlock
    ReliableTopicConfig unlockTopicConfig(final HazelcastInstance pHci, final Config pConfig) {
        return createTopicConfig(pHci,
                pConfig.unlockTopicName(),
                pConfig.unlockTopicOverloadPolicy(),
                pConfig.unlockTopicReadBatchSize(),
                pConfig.unlockTopicStatisticsEnabled());
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
    ITopic<DistributionMessage> deleteRequestTopic(final HazelcastInstance pHci, @Delete final ReliableTopicConfig pConfig) {
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
    ITopic<DistributionMessage> storeRequestTopic(final HazelcastInstance pHci, @Store final ReliableTopicConfig pConfig) {
        return getTopic(pHci, pConfig);
    }

    @Provides
    @Singleton
    @Lock
    ITopic<DistributionMessage> lockRequestTopic(final HazelcastInstance pHci, @Lock final ReliableTopicConfig pConfig) {
        return getTopic(pHci, pConfig);
    }

    @Provides
    @Singleton
    @Unlock
    public ITopic<DistributionMessage> unlockRequestTopic(final HazelcastInstance pHci, @Unlock final ReliableTopicConfig pConfig) {
        return getTopic(pHci, pConfig);
    }

    @Provides
    @Singleton
    IMap<String, byte[]> checksumMap(final HazelcastInstance pHci, final Config pConfig) {
        return pHci.getMap(pConfig.checksumMapName());
    }
}
