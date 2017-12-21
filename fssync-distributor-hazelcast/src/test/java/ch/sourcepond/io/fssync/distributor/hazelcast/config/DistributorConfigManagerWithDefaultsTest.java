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

import ch.sourcepond.osgi.cmpn.metatype.ConfigBuilder;
import ch.sourcepond.osgi.cmpn.metatype.ConfigBuilderFactory;
import com.hazelcast.config.MulticastConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.ReliableTopicConfig;
import com.hazelcast.config.RingbufferConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.topic.TopicOverloadPolicy;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import java.util.Collection;
import java.util.Dictionary;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

public class DistributorConfigManagerWithDefaultsTest {
    private static final String EXPECTED_RESPONSE_TOPIC_NAME = "__fssync_distributor.TEST.response";
    private static final String EXPECTED_DELETE_TOPIC_NAME = "__fssync_distributor.TEST.delete";
    private static final String EXPECTED_TRANSFER_TOPIC_NAME = "__fssync_distributor.TEST.transfer";
    private static final String EXPECTED_DISCARD_TOPIC_NAME = "__fssync_distributor.TEST.discard";
    private static final String EXPECTED_STORE_TOPIC_NAME = "__fssync_distributor.TEST.store";
    private static final String EXPECTED_LOCK_TOPIC_NAME = "__fssync_distributor.TEST.lock";
    private static final String EXPECTED_UNLOCK_TOPIC_NAME = "__fssync_distributor.TEST.unlock";

    private static final String EXPECTED_PID = "expectedPid";
    private static final String EXPECTED_INSTANCE_NAME = "TEST";
    private final ConfigurationAdmin configurationAdmin = mock(ConfigurationAdmin.class);
    private final Configuration configuration = mock(Configuration.class);
    private final AtomicReference<com.hazelcast.config.Config> hazelcastConfig = new AtomicReference<>();
    private final ConfigChangeObserver observer = mock(ConfigChangeObserver.class, withSettings().defaultAnswer(inv -> {
        hazelcastConfig.set(inv.getArgument(0));
        return null;
    }));
    private final ConfigBuilderFactory configBuilderFactory = mock(ConfigBuilderFactory.class);
    private final ConfigBuilder<DistributorConfig> configBuilder = mock(ConfigBuilder.class);
    private final DistributorConfig distributorConfig = mock(DistributorConfig.class, withSettings().defaultAnswer(inv -> inv.getMethod().getDefaultValue()));
    private final Dictionary<String, ?> properties = mock(Dictionary.class);
    private final ConfigManager manager = new ConfigManager(observer, configBuilderFactory, configurationAdmin);

    @Before
    public void setup() throws Exception {
        when(distributorConfig.instanceName()).thenReturn(EXPECTED_INSTANCE_NAME);
        when(configBuilderFactory.create(DistributorConfig.class, properties)).thenReturn(configBuilder);
        when(configBuilder.build()).thenReturn(distributorConfig);
    }

    private void verifyTopicConfig(final String pExpectedName) {
        final com.hazelcast.config.Config cfg = hazelcastConfig.get();
        final ReliableTopicConfig topicConfig = cfg.getReliableTopicConfigs().get(pExpectedName);
        assertNotNull(topicConfig);
        assertEquals(pExpectedName, topicConfig.getName());
        assertEquals(50, topicConfig.getReadBatchSize());
        assertFalse(topicConfig.isStatisticsEnabled());

        final RingbufferConfig ringbufferConfig = cfg.getRingbufferConfig(pExpectedName);
        assertEquals(2000, ringbufferConfig.getCapacity());
        assertEquals(1, ringbufferConfig.getBackupCount());
        assertEquals(0, ringbufferConfig.getAsyncBackupCount());
        assertEquals(300, ringbufferConfig.getTimeToLiveSeconds());
    }

    @Test
    public void verifyDefaultHazelcastConfig() throws Exception {
        manager.updated(EXPECTED_PID, properties);
        final com.hazelcast.config.Config cfg = hazelcastConfig.get();
        assertNotNull(cfg);
        assertEquals(EXPECTED_INSTANCE_NAME, cfg.getInstanceName());
        assertEquals(EXPECTED_INSTANCE_NAME, cfg.getGroupConfig().getName());

        final NetworkConfig networkConfig = cfg.getNetworkConfig();
        assertEquals(6701, networkConfig.getPort());
        assertTrue(networkConfig.isPortAutoIncrement());
        assertEquals(100, networkConfig.getPortCount());

        final Collection<String> outboundPortsDefinitions = networkConfig.getOutboundPortDefinitions();
        assertEquals(1, outboundPortsDefinitions.size());
        assertEquals("*", outboundPortsDefinitions.iterator().next());

        final MulticastConfig multicastConfig = networkConfig.getJoin().getMulticastConfig();
        assertTrue(multicastConfig.isEnabled());
        assertEquals("224.2.2.3", multicastConfig.getMulticastGroup());
        assertEquals(54327, multicastConfig.getMulticastPort());
        assertEquals(32, multicastConfig.getMulticastTimeToLive());
        assertEquals(2, multicastConfig.getMulticastTimeoutSeconds());

        final TcpIpConfig tcpIpConfig = networkConfig.getJoin().getTcpIpConfig();
        assertFalse(tcpIpConfig.isEnabled());
        assertArrayEquals(new String[0], tcpIpConfig.getMembers().toArray());

        verifyTopicConfig(EXPECTED_RESPONSE_TOPIC_NAME);
        verifyTopicConfig(EXPECTED_DELETE_TOPIC_NAME);
        verifyTopicConfig(EXPECTED_TRANSFER_TOPIC_NAME);
        verifyTopicConfig(EXPECTED_DISCARD_TOPIC_NAME);
        verifyTopicConfig(EXPECTED_STORE_TOPIC_NAME);
        verifyTopicConfig(EXPECTED_LOCK_TOPIC_NAME);
        verifyTopicConfig(EXPECTED_UNLOCK_TOPIC_NAME);
    }
}
