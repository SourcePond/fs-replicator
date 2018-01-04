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

import ch.sourcepond.io.fssync.distributor.hazelcast.annotations.Delete;
import ch.sourcepond.io.fssync.distributor.hazelcast.annotations.Discard;
import ch.sourcepond.io.fssync.distributor.hazelcast.annotations.Lock;
import ch.sourcepond.io.fssync.distributor.hazelcast.annotations.Response;
import ch.sourcepond.io.fssync.distributor.hazelcast.annotations.Store;
import ch.sourcepond.io.fssync.distributor.hazelcast.annotations.Transfer;
import ch.sourcepond.io.fssync.distributor.hazelcast.annotations.Unlock;
import ch.sourcepond.osgi.cmpn.metatype.ConfigBuilder;
import ch.sourcepond.osgi.cmpn.metatype.ConfigBuilderFactory;
import com.hazelcast.config.MulticastConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.ReliableTopicConfig;
import com.hazelcast.config.RingbufferConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.core.DuplicateInstanceNameException;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static ch.sourcepond.io.fssync.distributor.hazelcast.config.ConfigManager.FACTORY_PID;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

public abstract class DistributorConfigManagerTest {

    protected class ExpectedTopicValues implements Cloneable {

        String expectedName;

        int expectedReadBatchSize;

        boolean expectedStatisticsEnabled;

        int expectedCapacity;

        int expectedBackupCount;

        int expectedAsyncBackupCount;

        int expectedTimeToLiveSeconds;

        public ExpectedTopicValues clone() {
            final ExpectedTopicValues clone = new ExpectedTopicValues();
            clone.expectedName = expectedName;
            clone.expectedCapacity = expectedCapacity;
            clone.expectedBackupCount = expectedBackupCount;
            clone.expectedAsyncBackupCount = expectedAsyncBackupCount;
            clone.expectedReadBatchSize = expectedReadBatchSize;
            clone.expectedStatisticsEnabled = expectedStatisticsEnabled;
            clone.expectedTimeToLiveSeconds = expectedTimeToLiveSeconds;
            return clone;
        }
    }

    protected class ExpectedValues {

        int expectedPort;

        boolean expectedPortAutoIncrement;

        int expectedPortCount;

        String[] expectedOutboundDefinitions;

        boolean expectedMulticastEnabled;

        String expectedMulticastGroup;

        int expectedMulticastPort;

        int expectedMulitcastTimeToLive;

        int expectedMulticastTimeoutSeconds;

        boolean expectedTcpipEnabled;

        String[] expectedTcpipMembers;

        final Map<String, ExpectedTopicValues> expectedTopicValues = new HashMap<>();
    }

    public static final String EXPECTED_PID = "expectedPid";
    public static final String EXPECTED_INSTANCE_NAME = "TEST";

    protected final ConfigurationAdmin configurationAdmin = mock(ConfigurationAdmin.class);
    protected final Configuration configuration = mock(Configuration.class);
    protected final AtomicReference<com.hazelcast.config.Config> hazelcastConfig = new AtomicReference<>();
    protected final Activator observer = mock(Activator.class, withSettings().defaultAnswer(inv -> {
        hazelcastConfig.set(inv.getArgument(0));
        return null;
    }));
    protected final ConfigBuilderFactory configBuilderFactory = mock(ConfigBuilderFactory.class);
    protected final ConfigBuilder<DistributorConfig> configBuilder = mock(ConfigBuilder.class);
    protected final DistributorConfig distributorConfig = mock(DistributorConfig.class, withSettings().defaultAnswer(inv -> inv.getMethod().getDefaultValue()));
    protected final Dictionary<String, Object> properties = mock(Dictionary.class);
    protected final ConfigManager manager = new ConfigManager(observer, configBuilderFactory);
    protected final ExpectedValues expectedValues = expectedValues();

    protected abstract ExpectedValues expectedValues();

    @Before
    public void setup() throws Exception {
        manager.setConfigAdmin(configurationAdmin);
        when(distributorConfig.instanceName()).thenReturn(EXPECTED_INSTANCE_NAME);
        when(configBuilderFactory.create(DistributorConfig.class, properties)).thenReturn(configBuilder);
        when(configBuilder.build()).thenReturn(distributorConfig);
    }

    private void verifyTopicConfig(final String pExpectedName) {
        final ExpectedTopicValues values = expectedValues.expectedTopicValues.get(pExpectedName);
        assertNotNull(values);
        final com.hazelcast.config.Config cfg = hazelcastConfig.get();
        final ReliableTopicConfig topicConfig = cfg.getReliableTopicConfigs().get(values.expectedName);
        assertNotNull(topicConfig);
        assertEquals(values.expectedName, topicConfig.getName());
        assertEquals(values.expectedReadBatchSize, topicConfig.getReadBatchSize());
        assertEquals(values.expectedStatisticsEnabled, topicConfig.isStatisticsEnabled());

        final RingbufferConfig ringbufferConfig = cfg.getRingbufferConfig(values.expectedName);
        assertEquals(values.expectedCapacity, ringbufferConfig.getCapacity());
        assertEquals(values.expectedBackupCount, ringbufferConfig.getBackupCount());
        assertEquals(values.expectedAsyncBackupCount, ringbufferConfig.getAsyncBackupCount());
        assertEquals(values.expectedTimeToLiveSeconds, ringbufferConfig.getTimeToLiveSeconds());
    }

    @Test
    public void verifyDuplicateInstance() throws Exception {
        final DuplicateInstanceNameException expected = new DuplicateInstanceNameException("");
        doThrow(expected).when(observer).configUpdated(any(), any());
        try {
            manager.updated(EXPECTED_PID, properties);
            fail("Exception expected");
        } catch (final ConfigurationException e) {
            assertSame(expected, e.getCause());
        }
    }

    @Test
    public void verifyDefaultHazelcastConfig() throws Exception {
        manager.updated(EXPECTED_PID, properties);
        final com.hazelcast.config.Config cfg = hazelcastConfig.get();
        assertNotNull(cfg);
        assertEquals(EXPECTED_INSTANCE_NAME, cfg.getInstanceName());
        assertEquals(EXPECTED_INSTANCE_NAME, cfg.getGroupConfig().getName());

        final NetworkConfig networkConfig = cfg.getNetworkConfig();
        assertEquals(expectedValues.expectedPort, networkConfig.getPort());
        assertEquals(expectedValues.expectedPortAutoIncrement, networkConfig.isPortAutoIncrement());
        assertEquals(expectedValues.expectedPortCount, networkConfig.getPortCount());

        assertArrayEquals(expectedValues.expectedOutboundDefinitions, networkConfig.getOutboundPortDefinitions().toArray());

        final MulticastConfig multicastConfig = networkConfig.getJoin().getMulticastConfig();
        assertEquals(expectedValues.expectedMulticastEnabled, multicastConfig.isEnabled());
        assertEquals(expectedValues.expectedMulticastGroup, multicastConfig.getMulticastGroup());
        assertEquals(expectedValues.expectedMulticastPort, multicastConfig.getMulticastPort());
        assertEquals(expectedValues.expectedMulitcastTimeToLive, multicastConfig.getMulticastTimeToLive());
        assertEquals(expectedValues.expectedMulticastTimeoutSeconds, multicastConfig.getMulticastTimeoutSeconds());

        final TcpIpConfig tcpIpConfig = networkConfig.getJoin().getTcpIpConfig();
        assertEquals(expectedValues.expectedTcpipEnabled, tcpIpConfig.isEnabled());
        assertArrayEquals(expectedValues.expectedTcpipMembers, tcpIpConfig.getMembers().toArray());

        verifyTopicConfig(Response.NAME);
        verifyTopicConfig(Delete.NAME);
        verifyTopicConfig(Transfer.NAME);
        verifyTopicConfig(Discard.NAME);
        verifyTopicConfig(Store.NAME);
        verifyTopicConfig(Lock.NAME);
        verifyTopicConfig(Unlock.NAME);
    }

    @Test
    public void getName() {
        assertEquals(FACTORY_PID, manager.getName());
    }
}
