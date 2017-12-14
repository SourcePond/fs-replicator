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
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import java.lang.annotation.Retention;
import java.util.concurrent.TimeUnit;

import static ch.sourcepond.io.fssync.distributor.hazelcast.Activator.FACTORY_PID;
import static com.hazelcast.topic.TopicOverloadPolicy.BLOCK;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.concurrent.TimeUnit.MINUTES;

@Retention(RUNTIME)
@ObjectClassDefinition(name = "Fssync Hazelcast distributor", description = "Configuration for Hazelcast distributor",
        factoryPid = {FACTORY_PID})
public @interface Config {

    @AttributeDefinition(
            description = "Name of the existing Hazelcast instance to be used"
    )
    String existingInstanceName();

    @AttributeDefinition
    String checksumMapName() default "__fs_replicator.checksums";

    @AttributeDefinition
    TimeUnit lockTimeoutUnit() default MINUTES;

    @AttributeDefinition
    long lockTimeout() default 1;

    @AttributeDefinition
    TimeUnit leaseTimeUnit() default MINUTES;

    @AttributeDefinition
    long leaseTime() default 3;

    @AttributeDefinition
    TimeUnit responseTimeoutUnit() default MINUTES;

    @AttributeDefinition
    long responseTimeout() default 1;

    @AttributeDefinition
    String responseTopicName() default "__fs_distributor.response";

    @AttributeDefinition
    TopicOverloadPolicy responseTopicOverloadPolicy() default BLOCK;

    @AttributeDefinition
    int responseTopicReadBatchSize() default 50;

    @AttributeDefinition
    boolean responseTopicStatisticsEnabled() default false;

    @AttributeDefinition
    String deleteTopicName() default "__fs_distributor.delete";

    @AttributeDefinition
    TopicOverloadPolicy deleteTopicOverloadPolicy() default BLOCK;

    @AttributeDefinition
    int deleteTopicReadBatchSize() default 50;

    @AttributeDefinition
    boolean deleteTopicStatisticsEnabled() default false;

    @AttributeDefinition
    String transferTopicName() default "__fs_distributor.transfer";

    @AttributeDefinition
    TopicOverloadPolicy transferTopicOverloadPolicy() default BLOCK;

    @AttributeDefinition
    int transferTopicReadBatchSize() default 50;

    @AttributeDefinition
    boolean transferTopicStatisticsEnabled() default false;

    @AttributeDefinition
    String discardTopicName() default "__fs_distributor.discard";

    @AttributeDefinition
    TopicOverloadPolicy discardTopicOverloadPolicy() default BLOCK;

    @AttributeDefinition
    int discardTopicReadBatchSize() default 50;

    @AttributeDefinition
    boolean discardTopicStatisticsEnabled() default false;

    @AttributeDefinition
    String storeTopicName() default "__fs_distributor.store";

    @AttributeDefinition
    TopicOverloadPolicy storeTopicOverloadPolicy() default BLOCK;

    @AttributeDefinition
    int storeTopicReadBatchSize() default 50;

    @AttributeDefinition
    boolean storeTopicStatisticsEnabled() default false;

    @AttributeDefinition
    String lockTopicName() default "__fs_distributor.lock";

    @AttributeDefinition
    TopicOverloadPolicy lockTopicOverloadPolicy() default BLOCK;

    @AttributeDefinition
    int lockTopicReadBatchSize() default 50;

    @AttributeDefinition
    boolean lockTopicStatisticsEnabled() default false;

    @AttributeDefinition
    String unlockTopicName() default "__fs_distributor.unlock";

    @AttributeDefinition
    TopicOverloadPolicy unlockTopicOverloadPolicy() default BLOCK;

    @AttributeDefinition
    int unlockTopicReadBatchSize() default 50;

    @AttributeDefinition
    boolean unlockTopicStatisticsEnabled() default false;
}
