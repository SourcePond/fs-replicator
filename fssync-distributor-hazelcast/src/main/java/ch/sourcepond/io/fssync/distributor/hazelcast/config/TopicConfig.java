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
package ch.sourcepond.io.fssync.distributor.hazelcast.config;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import static ch.sourcepond.io.fssync.distributor.hazelcast.config.TopicConfigManager.FACTORY_PID;

@ObjectClassDefinition(name = "fssync-distributor-hazelcast Reliable Topic configuration",
        description = "Configuration of a Reliable Topic (incl. Ringbuffer)",
        factoryPid = {FACTORY_PID})
public @interface TopicConfig {

    // TODO: Use translation service for this
    @AttributeDefinition(description = "Minimum number of messages that Reliable Topic will try to read in batches.")
    int readBatchSize() default 50;

    @AttributeDefinition(description = "Enables or disables the statistics collection for the Reliable Topic.")
    boolean statisticsEnabled() default false;

    @AttributeDefinition(description = "A Ringbuffer is configured with a capacity of n items. This creates an array with a size of n. " +
            "If a time-to-live is configured, then an array of longs is also created that stores the expiration time for every item.")
    int capacity() default 2000;

    @AttributeDefinition(description = "You can control the Ringbuffer backup just like most of the other Hazelcast distributed data " +
            "structures by setting the synchronous backups")
    int backupCount() default 1;

    @AttributeDefinition(description = "You can control the Ringbuffer backup just like most of the other Hazelcast distributed data " +
            "structures by setting the asynchronous backups")
    int asyncBackupCount() default 0;

    @AttributeDefinition(description = "You can configure Hazelcast Ringbuffer with a time to live in seconds. Using this setting, " +
            "you can control how long the items remain in the Ringbuffer before they are expired.")
    int timeToLiveSeconds() default 300;
}
