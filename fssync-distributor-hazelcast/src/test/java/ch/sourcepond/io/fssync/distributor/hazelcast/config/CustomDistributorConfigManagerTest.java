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
import com.hazelcast.config.Config;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationException;

import java.io.IOException;
import java.util.Dictionary;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class CustomDistributorConfigManagerTest extends DistributorConfigManagerTest {
    private static final String EXPECTED_RESPONSE_TOPIC_PROPERTY = "responseTopicConfigPID";
    private static final String EXPECTED_DELETE_TOPIC_PROPERTY = "deleteTopicConfigPID";
    private static final String EXPECTED_TRANSFER_TOPIC_PROPERTY = "transferTopicConfigPID";
    private static final String EXPECTED_DISCARD_TOPIC_PROPERTY = "discardTopicConfigPID";
    private static final String EXPECTED_STORE_TOPIC_PROPERTY = "storeTopicConfigPID";
    private static final String EXPECTED_LOCK_TOPIC_PROPERTY = "lockTopicConfigPID";
    private static final String EXPECTED_UNLOCK_TOPIC_PROPERTY = "unlockTopicConfigPID";
    private static final String EXPECTED_RESPONSE_TOPIC_PID = "expectedResponseTopicPid";
    private static final String EXPECTED_DELETE_TOPIC_PID = "expectedDeleteTopicPid";
    private static final String EXPECTED_TRANSFER_TOPIC_PID = "expectedTransferTopicPid";
    private static final String EXPECTED_DISCARD_TOPIC_PID = "expectedDiscardTopicPid";
    private static final String EXPECTED_STORE_TOPIC_PID = "expectedStoreTopicPid";
    private static final String EXPECTED_LOCK_TOPIC_PID = "expectedLockTopicPid";
    private static final String EXPECTED_UNLOCK_TOPIC_PID = "expectedUnlockTopicPid";
    private static final int EXPECTED_PORT = 8701;
    private static final boolean EXPECTED_PORT_AUTO_INCREMENT = false;
    private static final int EXPECTED_PORT_COUNT = 200;
    private static final String[] EXPECTED_OUTBOUND_DEFINITIONS = new String[]{"8702", "8703"};
    private static final boolean EXPECTED_MULTICAST_ENABLED = false;
    private static final String EXPECTED_MULTICAST_GROUP = "225.3.3.4";
    private static final int EXPECTED_MULTICAST_PORT = 64327;
    private static final int EXPECTED_MULTICAST_TIME_TO_LIVE = 22;
    private static final int EXPECTED_MULTICAST_TIMEOUT_SECONDS = 4;
    private static final boolean EXPECTED_TCPIP_ENABLED = true;
    private static final String[] EXPECTED_TCPIP_MEMBERS = new String[]{"10.0.1.1", "10.0.1.2"};
    private static final int EXPECTED_READ_BATCH_SIZE = 60;
    private static final boolean EXPECTED_STATISTICS_ENABLED = true;
    private static final int EXPECTED_ASYNC_BACKUP_COUNT = 2;
    private static final int EXPECTED_BACKUP_COUNT = 2;
    private static final int EXPECTED_CAPACITY = 4000;
    private static final int EXPECTED_TIME_TO_LIVE_SECONDS = 400;
    private final Configuration responseConfig = mock(Configuration.class);
    private final Configuration deleteConfig = mock(Configuration.class);
    private final Configuration transferConfig = mock(Configuration.class);
    private final Configuration discardConfig = mock(Configuration.class);
    private final Configuration storeConfig = mock(Configuration.class);
    private final Configuration lockConfig = mock(Configuration.class);
    private final Configuration unlockConfig = mock(Configuration.class);
    private final Dictionary<String, Object> responseProps = mock(Dictionary.class);
    private final Dictionary<String, Object> deleteProps = mock(Dictionary.class);
    private final Dictionary<String, Object> transferProps = mock(Dictionary.class);
    private final Dictionary<String, Object> discardProps = mock(Dictionary.class);
    private final Dictionary<String, Object> storeProps = mock(Dictionary.class);
    private final Dictionary<String, Object> lockProps = mock(Dictionary.class);
    private final Dictionary<String, Object> unlockProps = mock(Dictionary.class);
    private final ConfigBuilder<TopicConfig> responseTopicConfigBuilder = mock(ConfigBuilder.class);
    private final ConfigBuilder<TopicConfig> deleteTopicConfigBuilder = mock(ConfigBuilder.class);
    private final ConfigBuilder<TopicConfig> transferTopicConfigBuilder = mock(ConfigBuilder.class);
    private final ConfigBuilder<TopicConfig> discardTopicConfigBuilder = mock(ConfigBuilder.class);
    private final ConfigBuilder<TopicConfig> storeTopicConfigBuilder = mock(ConfigBuilder.class);
    private final ConfigBuilder<TopicConfig> lockTopicConfigBuilder = mock(ConfigBuilder.class);
    private final ConfigBuilder<TopicConfig> unlockTopicConfigBuilder = mock(ConfigBuilder.class);
    private final TopicConfig responseTopicConfig = mock(TopicConfig.class);
    private final TopicConfig deleteTopicConfig = mock(TopicConfig.class);
    private final TopicConfig transferTopicConfig = mock(TopicConfig.class);
    private final TopicConfig discardTopicConfig = mock(TopicConfig.class);
    private final TopicConfig storeTopicConfig = mock(TopicConfig.class);
    private final TopicConfig lockTopicConfig = mock(TopicConfig.class);
    private final TopicConfig unlockTopicConfig = mock(TopicConfig.class);
    private final Configuration configuration = mock(Configuration.class);

    @Override
    protected ExpectedValues expectedValues() {
        final ExpectedValues values = new ExpectedValues();

        values.expectedPort = EXPECTED_PORT;
        values.expectedPortAutoIncrement = EXPECTED_PORT_AUTO_INCREMENT;
        values.expectedPortCount = EXPECTED_PORT_COUNT;
        values.expectedOutboundDefinitions = EXPECTED_OUTBOUND_DEFINITIONS;
        values.expectedMulticastEnabled = EXPECTED_MULTICAST_ENABLED;
        values.expectedMulticastGroup = EXPECTED_MULTICAST_GROUP;
        values.expectedMulticastPort = EXPECTED_MULTICAST_PORT;
        values.expectedMulitcastTimeToLive = EXPECTED_MULTICAST_TIME_TO_LIVE;
        values.expectedMulticastTimeoutSeconds = EXPECTED_MULTICAST_TIMEOUT_SECONDS;
        values.expectedTcpipEnabled = EXPECTED_TCPIP_ENABLED;
        values.expectedTcpipMembers = EXPECTED_TCPIP_MEMBERS;

        final ExpectedTopicValues responseTopicValues = new ExpectedTopicValues();
        responseTopicValues.expectedReadBatchSize = EXPECTED_READ_BATCH_SIZE;
        responseTopicValues.expectedStatisticsEnabled = EXPECTED_STATISTICS_ENABLED;
        responseTopicValues.expectedAsyncBackupCount = EXPECTED_ASYNC_BACKUP_COUNT;
        responseTopicValues.expectedBackupCount = EXPECTED_BACKUP_COUNT;
        responseTopicValues.expectedCapacity = EXPECTED_CAPACITY;
        responseTopicValues.expectedName = EXPECTED_RESPONSE_TOPIC_NAME;
        responseTopicValues.expectedTimeToLiveSeconds = EXPECTED_TIME_TO_LIVE_SECONDS;

        final ExpectedTopicValues deleteTopicValues = responseTopicValues.clone();
        deleteTopicValues.expectedName = EXPECTED_DELETE_TOPIC_NAME;

        final ExpectedTopicValues transferTopicValues = responseTopicValues.clone();
        deleteTopicValues.expectedName = EXPECTED_TRANSFER_TOPIC_NAME;

        final ExpectedTopicValues discardTopicValues = responseTopicValues.clone();
        deleteTopicValues.expectedName = EXPECTED_DISCARD_TOPIC_NAME;

        final ExpectedTopicValues storeTopicValues = responseTopicValues.clone();
        deleteTopicValues.expectedName = EXPECTED_STORE_TOPIC_NAME;

        final ExpectedTopicValues lockTopicValues = responseTopicValues.clone();
        deleteTopicValues.expectedName = EXPECTED_LOCK_TOPIC_NAME;

        final ExpectedTopicValues unlockTopicValues = responseTopicValues.clone();
        deleteTopicValues.expectedName = EXPECTED_UNLOCK_TOPIC_NAME;

        values.expectedTopicValues.put(EXPECTED_RESPONSE_TOPIC_NAME, responseTopicValues);
        values.expectedTopicValues.put(EXPECTED_DELETE_TOPIC_NAME, deleteTopicValues);
        values.expectedTopicValues.put(EXPECTED_TRANSFER_TOPIC_NAME, transferTopicValues);
        values.expectedTopicValues.put(EXPECTED_DISCARD_TOPIC_NAME, discardTopicValues);
        values.expectedTopicValues.put(EXPECTED_STORE_TOPIC_NAME, storeTopicValues);
        values.expectedTopicValues.put(EXPECTED_LOCK_TOPIC_NAME, lockTopicValues);
        values.expectedTopicValues.put(EXPECTED_UNLOCK_TOPIC_NAME, unlockTopicValues);

        return values;
    }

    @Before
    public void setup() throws Exception {
        super.setup();
        when(distributorConfig.port()).thenReturn(EXPECTED_PORT);
        when(distributorConfig.portAutoIncrement()).thenReturn(EXPECTED_PORT_AUTO_INCREMENT);
        when(distributorConfig.portCount()).thenReturn(EXPECTED_PORT_COUNT);
        when(distributorConfig.outboundPorts()).thenReturn(EXPECTED_OUTBOUND_DEFINITIONS);
        when(distributorConfig.multicastEnabled()).thenReturn(EXPECTED_MULTICAST_ENABLED);
        when(distributorConfig.multicastGroup()).thenReturn(EXPECTED_MULTICAST_GROUP);
        when(distributorConfig.multicastPort()).thenReturn(EXPECTED_MULTICAST_PORT);
        when(distributorConfig.multicastTimeToLive()).thenReturn(EXPECTED_MULTICAST_TIME_TO_LIVE);
        when(distributorConfig.multicastTimeoutSeconds()).thenReturn(EXPECTED_MULTICAST_TIMEOUT_SECONDS);
        when(distributorConfig.tcpipEnabled()).thenReturn(EXPECTED_TCPIP_ENABLED);
        when(distributorConfig.tcpipMembers()).thenReturn(EXPECTED_TCPIP_MEMBERS);
        when(distributorConfig.responseTopicConfigPID()).thenReturn(EXPECTED_RESPONSE_TOPIC_PID);
        when(distributorConfig.deleteTopicConfigPID()).thenReturn(EXPECTED_DELETE_TOPIC_PID);
        when(distributorConfig.transferTopicConfigPID()).thenReturn(EXPECTED_TRANSFER_TOPIC_PID);
        when(distributorConfig.discardTopicConfigPID()).thenReturn(EXPECTED_DISCARD_TOPIC_PID);
        when(distributorConfig.storeTopicConfigPID()).thenReturn(EXPECTED_STORE_TOPIC_PID);
        when(distributorConfig.lockTopicConfigPID()).thenReturn(EXPECTED_LOCK_TOPIC_PID);
        when(distributorConfig.unlockTopicConfigPID()).thenReturn(EXPECTED_UNLOCK_TOPIC_PID);
        when(configurationAdmin.getConfiguration(EXPECTED_RESPONSE_TOPIC_PID, null)).thenReturn(responseConfig);
        when(configurationAdmin.getConfiguration(EXPECTED_DELETE_TOPIC_PID, null)).thenReturn(deleteConfig);
        when(configurationAdmin.getConfiguration(EXPECTED_TRANSFER_TOPIC_PID, null)).thenReturn(transferConfig);
        when(configurationAdmin.getConfiguration(EXPECTED_DISCARD_TOPIC_PID, null)).thenReturn(discardConfig);
        when(configurationAdmin.getConfiguration(EXPECTED_STORE_TOPIC_PID, null)).thenReturn(storeConfig);
        when(configurationAdmin.getConfiguration(EXPECTED_LOCK_TOPIC_PID, null)).thenReturn(lockConfig);
        when(configurationAdmin.getConfiguration(EXPECTED_UNLOCK_TOPIC_PID, null)).thenReturn(unlockConfig);
        when(responseConfig.getProperties()).thenReturn(responseProps);
        when(deleteConfig.getProperties()).thenReturn(deleteProps);
        when(transferConfig.getProperties()).thenReturn(transferProps);
        when(discardConfig.getProperties()).thenReturn(discardProps);
        when(storeConfig.getProperties()).thenReturn(storeProps);
        when(lockConfig.getProperties()).thenReturn(lockProps);
        when(unlockConfig.getProperties()).thenReturn(unlockProps);
        when(configBuilderFactory.create(TopicConfig.class, responseProps)).thenReturn(responseTopicConfigBuilder);
        when(configBuilderFactory.create(TopicConfig.class, deleteProps)).thenReturn(deleteTopicConfigBuilder);
        when(configBuilderFactory.create(TopicConfig.class, transferProps)).thenReturn(transferTopicConfigBuilder);
        when(configBuilderFactory.create(TopicConfig.class, discardProps)).thenReturn(discardTopicConfigBuilder);
        when(configBuilderFactory.create(TopicConfig.class, storeProps)).thenReturn(storeTopicConfigBuilder);
        when(configBuilderFactory.create(TopicConfig.class, lockProps)).thenReturn(lockTopicConfigBuilder);
        when(configBuilderFactory.create(TopicConfig.class, unlockProps)).thenReturn(unlockTopicConfigBuilder);
        when(responseTopicConfigBuilder.build()).thenReturn(responseTopicConfig);
        when(deleteTopicConfigBuilder.build()).thenReturn(deleteTopicConfig);
        when(transferTopicConfigBuilder.build()).thenReturn(transferTopicConfig);
        when(discardTopicConfigBuilder.build()).thenReturn(discardTopicConfig);
        when(storeTopicConfigBuilder.build()).thenReturn(storeTopicConfig);
        when(lockTopicConfigBuilder.build()).thenReturn(lockTopicConfig);
        when(unlockTopicConfigBuilder.build()).thenReturn(unlockTopicConfig);
        setupTopicConfig(responseTopicConfig);
        setupTopicConfig(responseTopicConfig);
        setupTopicConfig(deleteTopicConfig);
        setupTopicConfig(transferTopicConfig);
        setupTopicConfig(discardTopicConfig);
        setupTopicConfig(storeTopicConfig);
        setupTopicConfig(lockTopicConfig);
        setupTopicConfig(unlockTopicConfig);
        when(configurationAdmin.getConfiguration(EXPECTED_PID, null)).thenReturn(configuration);
        when(configuration.getProperties()).thenReturn(properties);
    }

    private void setupTopicConfig(final TopicConfig pTopicConfig) {
        when(pTopicConfig.asyncBackupCount()).thenReturn(EXPECTED_ASYNC_BACKUP_COUNT);
        when(pTopicConfig.backupCount()).thenReturn(EXPECTED_BACKUP_COUNT);
        when(pTopicConfig.capacity()).thenReturn(EXPECTED_CAPACITY);
        when(pTopicConfig.readBatchSize()).thenReturn(EXPECTED_READ_BATCH_SIZE);
        when(pTopicConfig.statisticsEnabled()).thenReturn(EXPECTED_STATISTICS_ENABLED);
        when(pTopicConfig.timeToLiveSeconds()).thenReturn(EXPECTED_TIME_TO_LIVE_SECONDS);
    }

    @Test
    public void addTopicConfigIOExceptionOccured() throws Exception {
        final IOException expected = new IOException();
        doThrow(expected).when(configurationAdmin).getConfiguration(EXPECTED_RESPONSE_TOPIC_PID, null);
        try {
            manager.updated(EXPECTED_PID, properties);
            fail("Exception expected!");
        } catch (final ConfigurationException e) {
            assertEquals("responseTopicConfigPID", e.getProperty());
            assertSame(expected, e.getCause());
        }
    }

    @Test
    public void getConfigPropertiesAreNull() throws Exception {
        when(responseConfig.getProperties()).thenReturn(null);
        try {
            manager.updated(EXPECTED_PID, properties);
            fail("Exception expected!");
        } catch (final ConfigurationException e) {
            assertEquals("responseTopicConfigPID", e.getProperty());
        }
    }

    @Test
    public void topicConfigDeletedIOExceptionOccurred() throws Exception {
        manager.updated(EXPECTED_PID, properties);
        doThrow(IOException.class).when(configurationAdmin).getConfiguration(EXPECTED_PID, null);

        // No exception should be thrown here
        manager.topicConfigDeleted(EXPECTED_RESPONSE_TOPIC_PID);
        verifyZeroInteractions(properties, configuration);
    }

    private void topicConfigDeleted(final String pExpectedTopicPid, final String pExpectedTopicProperty) throws Exception {
        manager.updated(EXPECTED_PID, properties);
        manager.topicConfigDeleted(pExpectedTopicPid);
        final InOrder order = inOrder(properties, configuration);
        order.verify(properties).put(pExpectedTopicProperty, DistributorConfig.DEFAULT_CONFIG);
        order.verify(configuration).update(properties);
    }

    @Test
    public void responseTopicConfigDeleted() throws Exception {
        topicConfigDeleted(EXPECTED_RESPONSE_TOPIC_PID, EXPECTED_RESPONSE_TOPIC_PROPERTY);
    }

    @Test
    public void deleteTopicConfigDeleted() throws Exception {
        topicConfigDeleted(EXPECTED_DELETE_TOPIC_PID, EXPECTED_DELETE_TOPIC_PROPERTY);
    }

    @Test
    public void transferTopicConfigDeleted() throws Exception {
        topicConfigDeleted(EXPECTED_TRANSFER_TOPIC_PID, EXPECTED_TRANSFER_TOPIC_PROPERTY);
    }

    @Test
    public void discardTopicConfigDeleted() throws Exception {
        topicConfigDeleted(EXPECTED_DISCARD_TOPIC_PID, EXPECTED_DISCARD_TOPIC_PROPERTY);
    }

    @Test
    public void storeTopicConfigDeleted() throws Exception {
        topicConfigDeleted(EXPECTED_STORE_TOPIC_PID, EXPECTED_STORE_TOPIC_PROPERTY);
    }

    @Test
    public void lockTopicConfigDeleted() throws Exception {
        topicConfigDeleted(EXPECTED_LOCK_TOPIC_PID, EXPECTED_LOCK_TOPIC_PROPERTY);
    }

    @Test
    public void unlockTopicConfigDeleted() throws Exception {
        topicConfigDeleted(EXPECTED_UNLOCK_TOPIC_PID, EXPECTED_UNLOCK_TOPIC_PROPERTY);
    }

    @Test
    public void topicConfigUpdated() throws Exception {
        manager.updated(EXPECTED_PID, properties);
        hazelcastConfig.set(null);
        manager.topicConfigUpdated(EXPECTED_RESPONSE_TOPIC_PID);
        assertNotNull(hazelcastConfig.get());
        verify(observer).configUpdated(hazelcastConfig.get());
    }

    @Test
    public void topicConfigUpdatedIOExceptionOccurred() throws Exception {
        manager.updated(EXPECTED_PID, properties);
        verify(observer).configUpdated(hazelcastConfig.get());
        doThrow(IOException.class).when(configurationAdmin).getConfiguration(EXPECTED_PID, null);
        manager.topicConfigUpdated(EXPECTED_RESPONSE_TOPIC_PID);
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void deleted() throws Exception {
        manager.updated(EXPECTED_PID, properties);
        final Config cfg = hazelcastConfig.get();
        verify(observer).configUpdated(cfg);
        manager.deleted(EXPECTED_PID);
        verify(observer).configDeleted(EXPECTED_INSTANCE_NAME);
    }

    @Test
    public void deletedNoSuchConfig() {
        manager.deleted(EXPECTED_PID);
        verifyZeroInteractions(observer);
    }
}
