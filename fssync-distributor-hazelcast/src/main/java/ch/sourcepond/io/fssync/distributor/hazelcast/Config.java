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
package ch.sourcepond.io.fssync.distributor.hazelcast;

import com.hazelcast.topic.TopicOverloadPolicy;

import java.util.concurrent.TimeUnit;

import static com.hazelcast.topic.TopicOverloadPolicy.BLOCK;

public @interface Config {

    String existingInstanceName();

    String checksumMapName() default "__fs_replicator.checksums";

    TimeUnit lockTimeoutUnit();

    long lockTimeout();

    TimeUnit responseTimeoutUnit();

    long responseTimeout();

    String responseTopicName() default "__fs_distributor.response";

    TopicOverloadPolicy responseTopicOverloadPolicy() default BLOCK;

    int responseTopicReadBatchSize() default 50;

    boolean responseTopicStatisticsEnabled() default false;

    String deleteTopicName() default "__fs_distributor.delete";

    TopicOverloadPolicy deleteTopicOverloadPolicy() default BLOCK;

    int deleteTopicReadBatchSize() default 50;

    boolean deleteTopicStatisticsEnabled() default false;

    String transferTopicName() default "__fs_distributor.transfer";

    TopicOverloadPolicy transferTopicOverloadPolicy() default BLOCK;

    int transferTopicReadBatchSize() default 50;

    boolean transferTopicStatisticsEnabled() default false;

    String discardTopicName() default "__fs_distributor.discard";

    TopicOverloadPolicy discardTopicOverloadPolicy() default BLOCK;

    int discardTopicReadBatchSize() default 50;

    boolean discardTopicStatisticsEnabled() default false;

    String storeTopicName() default "__fs_distributor.store";

    TopicOverloadPolicy storeTopicOverloadPolicy() default BLOCK;

    int storeTopicReadBatchSize() default 50;

    boolean storeTopicStatisticsEnabled() default false;

    String lockTopicName() default "__fs_distributor.lock";

    TopicOverloadPolicy lockTopicOverloadPolicy() default BLOCK;

    int lockTopicReadBatchSize() default 50;

    boolean lockTopicStatisticsEnabled() default false;

    String unlockTopicName() default "__fs_distributor.unlock";

    TopicOverloadPolicy unlockTopicOverloadPolicy() default BLOCK;

    int unlockTopicReadBatchSize() default 50;

    boolean unlockTopicStatisticsEnabled() default false;
}
