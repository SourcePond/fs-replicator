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

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.osgi.service.metatype.annotations.AttributeType.PASSWORD;

@ObjectClassDefinition(name = "Fssync Hazelcast distributor", description = "Configuration for Hazelcast distributor",
        factoryPid = {""})
public @interface DistributorConfig {
    String DEFAULT_CONFIG = "default";

    @AttributeDefinition
    String instanceName();

    @AttributeDefinition
    String groupName();

    @AttributeDefinition(type = PASSWORD)
    String groupPassword();

    @AttributeDefinition
    int port() default 6701;

    @AttributeDefinition
    boolean portAutoIncrement() default true;

    @AttributeDefinition
    int portCount() default 100;

    @AttributeDefinition
    String[] outboundPorts() default { "*" };

    @AttributeDefinition
    boolean multicastEnabled() default true;

    @AttributeDefinition
    String multicastGroup() default "224.2.2.3";

    @AttributeDefinition
    int multicastPort() default 54327;

    @AttributeDefinition
    int multicastTimeToLive() default 32;

    @AttributeDefinition
    int multicastTimeoutSeconds() default 2;

    @AttributeDefinition
    boolean tcpipEnabled() default false;

    @AttributeDefinition
    String[] tcpipMembers() default {};

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
    String responseTopicConfigPID() default DEFAULT_CONFIG;

    @AttributeDefinition
    String deleteTopicConfigPID() default DEFAULT_CONFIG;

    @AttributeDefinition
    String transferTopicConfigPID() default DEFAULT_CONFIG;

    @AttributeDefinition
    String discardTopicConfigPID() default DEFAULT_CONFIG;

    @AttributeDefinition
    String storeTopicConfigPID() default DEFAULT_CONFIG;

    @AttributeDefinition
    String lockTopicConfigPID() default DEFAULT_CONFIG;

    @AttributeDefinition
    String unlockTopicConfigPID() default DEFAULT_CONFIG;
}
