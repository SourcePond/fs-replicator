package ch.sourcepond.io.distributor.impl.config;

import com.hazelcast.config.Config;
import com.hazelcast.topic.TopicOverloadPolicy;

public interface HazelcastConfig {

    interface TopicConfig {

        String getTopicName();

        TopicOverloadPolicy getTopicOverloadPolicy();

        int getTopicReadBatchSize();

        boolean isTopicStatisticsEnabled();
    }

    String INSTANCE_NAME = "hazelcast.instance.name";
    String NEW_INSTANCE_CONFIG = "hazelcast.new.instance.config";

    String RESPONSE_TOPIC_NAME = "hazelcast.response.topic.name";
    String RESPONSE_TOPIC_OVERLOAD_POLICY = "hazelcast.response.topic.policy";
    String RESPONSE_TOPIC_READ_BATCH_SIZE = "hazelcast.response.topic.readBatchSize";
    String RESPONSE_TOPIC_STATISTICS_ENABLED = "hazelcast.response.topic.statisticsEnabled";

    String DELETE_TOPIC_NAME = "hazelcast.delete.topic.name";
    String DELETE_TOPIC_OVERLOAD_POLICY = "hazelcast.delete.topic.policy";
    String DELETE_TOPIC_READ_BATCH_SIZE = "hazelcast.delete.topic.readBatchSize";
    String DELETE_TOPIC_STATISTICS_ENABLED = "hazelcast.delete.topic.statisticsEnabled";

    String getInstanceName();

    Config getNewInstanceConfigOrNull();

    TopicConfig getResponseTopicConfig();

    TopicConfig getDeleteTopicConfig();
}
