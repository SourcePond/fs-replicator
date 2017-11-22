package ch.sourcepond.io.distributor.impl.config;

import ch.sourcepond.io.distributor.impl.config.HazelcastConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.ReliableTopicConfig;

class TopicFactory {
    private final Config hci;
    private final HazelcastConfig config;

    public TopicFactory(final Config pHci, final HazelcastConfig pConfig) {
        hci = pHci;
        config = pConfig;
    }

    public ReliableTopicConfig createResponseTopicConfig() {
        final HazelcastConfig.TopicConfig cfg = config.getResponseTopicConfig();
        return hci.getReliableTopicConfig(cfg.getTopicName())
                .setTopicOverloadPolicy(cfg.getTopicOverloadPolicy())
                .setReadBatchSize(cfg.getTopicReadBatchSize())
                .setStatisticsEnabled(cfg.isTopicStatisticsEnabled());
    }
}
