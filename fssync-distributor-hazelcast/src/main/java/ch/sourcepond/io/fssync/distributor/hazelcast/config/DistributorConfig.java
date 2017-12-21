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

import static ch.sourcepond.io.fssync.distributor.hazelcast.config.DistributorConfigManager.FACTORY_PID;
import static java.util.concurrent.TimeUnit.MINUTES;

@ObjectClassDefinition(name = "fssync-distributor-hazelcast Hazelcast Distributor configuration",
        description = "Configuration of a Hazelcast-Instance (incl. group-config, network, multicast and TCP/IP) for" +
                "used by a distinct Distributor",
        factoryPid = {FACTORY_PID})
public @interface DistributorConfig {
    String DEFAULT_CONFIG = "default";

    @AttributeDefinition(description = "The name of the associated Hazelcast Instance.")
    String instanceName();

    @AttributeDefinition(description = "You can separate your clusters in a simple way by specifying group names. A JVM " +
            "can host multiple Hazelcast instances. Each Hazelcast instance can only participate in one group. Each " +
            "Hazelcast instance only joins to its own group and does not interact with other groups.")
    String groupName();

    @AttributeDefinition(description = "You can specify the ports that Hazelcast will use to communicate between cluster members.")
    int port() default 6701;

    @AttributeDefinition(description = "In some cases you may want to choose to use only one port. In that case, you can disable the " +
            "auto-increment feature of port by setting auto-increment to false")
    boolean portAutoIncrement() default true;

    @AttributeDefinition(description = "By default, Hazelcast will try 100 ports to bind. Meaning that, if you set the " +
            "value of port as 6701, as members are joining to the cluster, Hazelcast tries to find ports between 6701 " +
            "and 6801. You can choose to change the port count in the cases like having large instances on a single " +
            "machine or willing to have only a few ports to be assigned. The parameter port-count is used for this purpose, " +
            "whose default value is 100")
    int portCount() default 100;

    @AttributeDefinition(description = "By default, Hazelcast lets the system pick up an ephemeral port during socket bind " +
            "operation. But security policies/firewalls may require you to restrict outbound ports to be used by " +
            "Hazelcast-enabled applications. To fulfill this requirement, you can configure Hazelcast to use only defined outbound ports")
    String[] outboundPorts() default {"*"};

    @AttributeDefinition(description = "With the multicast auto-discovery mechanism, Hazelcast allows cluster members to " +
            "find each other using multicast communication. The cluster members do not need to know the concrete addresses " +
            "of the other members, as they just multicast to all the other members for listening")
    boolean multicastEnabled() default true;

    @AttributeDefinition(description = "The multicast group IP address. Specify it when you want to create clusters within " +
            "the same network. Values can be between 224.0.0.0 and 239.255.255.255. Default value is 224.2.2.3")
    String multicastGroup() default "224.2.2.3";

    @AttributeDefinition(description = "The multicast socket port that the Hazelcast member listens to and sends " +
            "discovery messages through. Default value is 54327.")
    int multicastPort() default 54327;

    @AttributeDefinition(description = "Time-to-live value for multicast packets sent out to control the scope of multicasts.")
    int multicastTimeToLive() default 32;

    @AttributeDefinition(description = "Only when the members are starting up, this timeout (in seconds) specifies the " +
            "period during which a member waits for a multicast response from another member. For example, if you set " +
            "it as 60 seconds, each member will wait for 60 seconds until a leader member is selected. Its default value is 2 seconds.")
    int multicastTimeoutSeconds() default 2;

    @AttributeDefinition(description = "Specifies whether the TCP/IP discovery is enabled or not.")
    boolean tcpipEnabled() default false;

    @AttributeDefinition(description = "IP address(es) of one or more well known members. Once members are connected to " +
            "these well known ones, all member addresses will be communicated with each other.")
    String[] tcpipMembers() default {};

    @AttributeDefinition(description = "Time unit of the lockTimeout configuration property.")
    TimeUnit lockTimeoutUnit() default MINUTES;

    @AttributeDefinition(description = "Maximum time to wait for the lock of a specific global path.")
    long lockTimeout() default 1;

    @AttributeDefinition(description = "Time unit of the leaseTime configuration property.")
    TimeUnit leaseTimeUnit() default MINUTES;

    @AttributeDefinition(description = "Time to wait before a lock of a specific global path is forced to be released.")
    long leaseTime() default 3;

    @AttributeDefinition(description = "Time unit of the responseTimeout configuration property.")
    TimeUnit responseTimeoutUnit() default MINUTES;

    @AttributeDefinition(description = "Specifies how long a member should wait after sending a request to all " +
            "other cluster members for their responses.")
    long responseTimeout() default 1;

    @AttributeDefinition(description = "Reference PID to the configuration of the response Reliable Topic. " +
            "If not set, a sensible default will be used.")
    String responseTopicConfigPID() default DEFAULT_CONFIG;

    @AttributeDefinition(description = "Reference PID to the configuration of the delete Reliable Topic. " +
            "If not set, a sensible default will be used.")
    String deleteTopicConfigPID() default DEFAULT_CONFIG;

    @AttributeDefinition(description = "Reference PID to the configuration of the transfer Reliable Topic. " +
            "If not set, a sensible default will be used.")
    String transferTopicConfigPID() default DEFAULT_CONFIG;

    @AttributeDefinition(description = "Reference PID to the configuration of the discard Reliable Topic. " +
            "If not set, a sensible default will be used.")
    String discardTopicConfigPID() default DEFAULT_CONFIG;

    @AttributeDefinition(description = "Reference PID to the configuration of the store Reliable Topic. " +
            "If not set, a sensible default will be used.")
    String storeTopicConfigPID() default DEFAULT_CONFIG;

    @AttributeDefinition(description = "Reference PID to the configuration of the lock Reliable Topic. " +
            "If not set, a sensible default will be used.")
    String lockTopicConfigPID() default DEFAULT_CONFIG;

    @AttributeDefinition(description = "Reference PID to the configuration of the unlock Reliable Topic. " +
            "If not set, a sensible default will be used.")
    String unlockTopicConfigPID() default DEFAULT_CONFIG;
}
