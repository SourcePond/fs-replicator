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
import ch.sourcepond.osgi.cmpn.metatype.ConfigBuilderFactory;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MulticastConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.ReliableTopicConfig;
import com.hazelcast.config.RingbufferConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.core.DuplicateInstanceNameException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Dictionary;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static ch.sourcepond.io.fssync.distributor.hazelcast.config.DistributorConfig.DEFAULT_CONFIG;
import static java.lang.String.format;
import static java.lang.reflect.Proxy.newProxyInstance;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

class ConfigManager implements ManagedServiceFactory {
    static final String FACTORY_PID = "ch.sourcepond.io.fssync.distributor.hazelcast.Config";
    private static final Logger LOG = getLogger(ConfigManager.class);
    private static final TopicConfig DEFAULT_TOPIC_CONFIG = (TopicConfig) newProxyInstance(TopicConfig.class.getClassLoader(),
            new Class<?>[]{TopicConfig.class}, (proxy, method, args) -> method.getDefaultValue());
    private static final String TOPIC_CONFIG_PID_POSTFIX = "TopicConfigPID";
    private final ConcurrentMap<String, DistributorConfig> configs = new ConcurrentHashMap<>();
    private final Activator observer;
    private final ConfigBuilderFactory configBuilderFactory;
    private volatile ConfigurationAdmin configurationAdmin;

    public ConfigManager(final Activator pObserver,
                         final ConfigBuilderFactory pConfigBuilderFactory) {
        observer = pObserver;
        configBuilderFactory = pConfigBuilderFactory;
    }

    private <T extends Annotation> T getConfig(final Class<T> pConfigInterface, final String pFieldName, final String pPid)
            throws ConfigurationException {
        final Configuration config;
        try {
            config = requireNonNull(configurationAdmin, "ConfigurationAdmin service not set!").getConfiguration(pPid, null);
        } catch (final IOException e) {
            // TODO: Translate this
            throw new ConfigurationException(pFieldName, "Configuration could not be loaded", e);
        }
        final Dictionary<String, ?> props = config.getProperties();

        if (props == null) {
            throw new ConfigurationException(pFieldName, format("No config found for pid %s", pPid));
        }

        return configBuilderFactory.create(pConfigInterface, props).build();
    }

    private static String toTopicConfigPidName(final String pPostfix) {
        return format("%s%s", pPostfix, TOPIC_CONFIG_PID_POSTFIX);
    }

    private void addTopicConfig(final com.hazelcast.config.Config pConfig, final String pName, final String pPid)
            throws ConfigurationException {
        final TopicConfig topicConfig = DEFAULT_CONFIG.equals(pPid) ? DEFAULT_TOPIC_CONFIG :
                getConfig(TopicConfig.class, toTopicConfigPidName(pName), pPid);
        final ReliableTopicConfig reliableTopicConfig = new ReliableTopicConfig(pName);
        reliableTopicConfig.setReadBatchSize(topicConfig.readBatchSize());
        reliableTopicConfig.setStatisticsEnabled(topicConfig.statisticsEnabled());

        final RingbufferConfig ringbufferConfig = new RingbufferConfig(pName);
        ringbufferConfig.setCapacity(topicConfig.capacity());
        ringbufferConfig.setBackupCount(topicConfig.backupCount());
        ringbufferConfig.setAsyncBackupCount(topicConfig.asyncBackupCount());
        ringbufferConfig.setTimeToLiveSeconds(topicConfig.timeToLiveSeconds());

        pConfig.addReliableTopicConfig(reliableTopicConfig);
        pConfig.addRingBufferConfig(ringbufferConfig);
    }

    private void addTopicConfig(final com.hazelcast.config.Config config, DistributorConfig instance) throws ConfigurationException {
        addTopicConfig(config, Response.NAME, instance.responseTopicConfigPID());
        addTopicConfig(config, Delete.NAME, instance.deleteTopicConfigPID());
        addTopicConfig(config, Transfer.NAME, instance.transferTopicConfigPID());
        addTopicConfig(config, Discard.NAME, instance.discardTopicConfigPID());
        addTopicConfig(config, Store.NAME, instance.storeTopicConfigPID());
        addTopicConfig(config, Lock.NAME, instance.lockTopicConfigPID());
        addTopicConfig(config, Unlock.NAME, instance.unlockTopicConfigPID());
    }

    @Override
    public String getName() {
        // TODO: Translate this
        return FACTORY_PID;
    }

    private com.hazelcast.config.Config createConfig(final DistributorConfig pDistributorConfig) throws ConfigurationException {
        final com.hazelcast.config.Config config = new com.hazelcast.config.Config();
        config.setClassLoader(getClass().getClassLoader());
        config.setInstanceName(pDistributorConfig.instanceName());
        config.setGroupConfig(new GroupConfig().setName(pDistributorConfig.instanceName()));

        final NetworkConfig networkConfig = new NetworkConfig().setPort(pDistributorConfig.port()).setPortAutoIncrement(pDistributorConfig.portAutoIncrement());
        networkConfig.setOutboundPortDefinitions(asList(pDistributorConfig.outboundPorts()));
        networkConfig.setPortCount(pDistributorConfig.portCount());

        final JoinConfig joinConfig = new JoinConfig();

        final MulticastConfig multicastConfig = new MulticastConfig();
        multicastConfig.setEnabled(pDistributorConfig.multicastEnabled());
        multicastConfig.setMulticastGroup(pDistributorConfig.multicastGroup());
        multicastConfig.setMulticastPort(pDistributorConfig.multicastPort());
        multicastConfig.setMulticastTimeToLive(pDistributorConfig.multicastTimeToLive());
        multicastConfig.setMulticastTimeoutSeconds(pDistributorConfig.multicastTimeoutSeconds());
        joinConfig.setMulticastConfig(multicastConfig);

        final TcpIpConfig tcpIpConfig = new TcpIpConfig();
        tcpIpConfig.setMembers(asList(pDistributorConfig.tcpipMembers()));
        tcpIpConfig.setEnabled(pDistributorConfig.tcpipEnabled());
        joinConfig.setTcpIpConfig(tcpIpConfig);

        networkConfig.setJoin(joinConfig);
        config.setNetworkConfig(networkConfig);

        addTopicConfig(config, pDistributorConfig);
        return config;
    }

    @Override
    public void updated(final String pPid, final Dictionary<String, ?> pProperties) throws ConfigurationException {
        final DistributorConfig distributorDistributorConfig = configBuilderFactory.create(DistributorConfig.class, pProperties).build();
        final com.hazelcast.config.Config config = createConfig(distributorDistributorConfig);
        configs.put(pPid, distributorDistributorConfig);

        try {
            observer.configUpdated(config, distributorDistributorConfig);
        } catch (final DuplicateInstanceNameException e) {
            throw new ConfigurationException("instanceName", e.getMessage(), e);
        }
    }

    @Override
    public void deleted(final String pPid) {
        final DistributorConfig distributorConfig = configs.remove(pPid);
        if (distributorConfig != null) {
            observer.configDeleted(distributorConfig.instanceName());
        }
    }

    private static boolean usesTopicConfigPid(final DistributorConfig pDistributorConfig, final String pDistributorTopicConfigPid) {
        return pDistributorTopicConfigPid.equals(pDistributorConfig.responseTopicConfigPID()) ||
                pDistributorTopicConfigPid.equals(pDistributorConfig.deleteTopicConfigPID()) ||
                pDistributorTopicConfigPid.equals(pDistributorConfig.transferTopicConfigPID()) ||
                pDistributorTopicConfigPid.equals(pDistributorConfig.discardTopicConfigPID()) ||
                pDistributorTopicConfigPid.equals(pDistributorConfig.storeTopicConfigPID()) ||
                pDistributorTopicConfigPid.equals(pDistributorConfig.lockTopicConfigPID()) ||
                pDistributorTopicConfigPid.equals(pDistributorConfig.unlockTopicConfigPID());
    }

    void topicConfigUpdated(final String pDistributorTopicConfigPid) {
        for (final Map.Entry<String, DistributorConfig> entry : configs.entrySet()) {
            final DistributorConfig distributorConfig = entry.getValue();
            if (usesTopicConfigPid(distributorConfig, pDistributorTopicConfigPid)) {
                try {
                    updated(entry.getKey(), configurationAdmin.getConfiguration(entry.getKey(), null).getProperties());
                } catch (final IOException | ConfigurationException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        }
    }

    private static void putTopicConfigPid(final String pDistributorTopicConfigPid,
                                          final String pTopicConfigPid,
                                          final String pPostfix,
                                          final Dictionary<String, Object> props) {
        if (pDistributorTopicConfigPid.equals(pTopicConfigPid)) {
            props.put(toTopicConfigPidName(pPostfix), DEFAULT_CONFIG);
        }
    }

    void topicConfigDeleted(final String pDistributorTopicConfigPid) {
        for (final Map.Entry<String, DistributorConfig> entry : configs.entrySet()) {
            final DistributorConfig distributorConfig = entry.getValue();
            if (usesTopicConfigPid(distributorConfig, pDistributorTopicConfigPid)) {
                try {
                    final Configuration configuration = configurationAdmin.getConfiguration(entry.getKey(), null);
                    final Dictionary<String, Object> props = configuration.getProperties();

                    putTopicConfigPid(pDistributorTopicConfigPid, distributorConfig.responseTopicConfigPID(), Response.NAME, props);
                    putTopicConfigPid(pDistributorTopicConfigPid, distributorConfig.deleteTopicConfigPID(), Delete.NAME, props);
                    putTopicConfigPid(pDistributorTopicConfigPid, distributorConfig.transferTopicConfigPID(), Transfer.NAME, props);
                    putTopicConfigPid(pDistributorTopicConfigPid, distributorConfig.discardTopicConfigPID(), Discard.NAME, props);
                    putTopicConfigPid(pDistributorTopicConfigPid, distributorConfig.storeTopicConfigPID(), Store.NAME, props);
                    putTopicConfigPid(pDistributorTopicConfigPid, distributorConfig.lockTopicConfigPID(), Lock.NAME, props);
                    putTopicConfigPid(pDistributorTopicConfigPid, distributorConfig.unlockTopicConfigPID(), Unlock.NAME, props);
                    configuration.update(props);
                } catch (final IOException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        }
    }

    public void setConfigAdmin(final ConfigurationAdmin configAdmin) {
        configurationAdmin = configAdmin;
    }
}
