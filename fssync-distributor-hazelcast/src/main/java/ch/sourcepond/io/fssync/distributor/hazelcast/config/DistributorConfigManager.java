package ch.sourcepond.io.fssync.distributor.hazelcast.config;

import ch.sourcepond.osgi.cmpn.metatype.ConfigBuilderFactory;
import com.hazelcast.config.Config;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MulticastConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.ReliableTopicConfig;
import com.hazelcast.config.RingbufferConfig;
import com.hazelcast.config.TcpIpConfig;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Dictionary;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

import static ch.sourcepond.io.fssync.distributor.hazelcast.config.DistributorConfig.DEFAULT_CONFIG;
import static com.hazelcast.topic.TopicOverloadPolicy.BLOCK;
import static java.lang.String.format;
import static java.lang.reflect.Proxy.newProxyInstance;
import static java.util.Arrays.asList;
import static org.slf4j.LoggerFactory.getLogger;

class DistributorConfigManager implements ManagedServiceFactory {
    static final String FACTORY_PID = "ch.sourcepond.io.fssync.distributor.hazelcast.config.DistributorConfig";
    private static final Logger LOG = getLogger(DistributorConfigManager.class);
    private static final DistributorTopicConfig DEFAULT_TOPIC_CONFIG = (DistributorTopicConfig) newProxyInstance(DistributorTopicConfig.class.getClassLoader(),
            new Class<?>[]{DistributorTopicConfig.class}, (proxy, method, args) -> method.getDefaultValue());
    private static final String NAME_PATTERN = "__fssync_distributor.%s.%s";
    private static final String TOPIC_CONFIG_PID_POSTFIX = "TopicConfigPID";
    private static final String RESPONSE_POSTFIX = "response";
    private static final String DELETE_POSTFIX = "delete";
    private static final String TRANSFER_POSTFIX = "transfer";
    private static final String DISCARD_POSTFIX = "discard";
    private static final String STORE_POSTFIX = "store";
    private static final String LOCK_POSTFIX = "lock";
    private static final String UNLOCK_POSTFIX = "unlock";
    private final ConcurrentMap<String, DistributorConfig> configs = new ConcurrentHashMap<>();
    private final ExecutorService executorService;
    private final ConfigChangeObserver observer;
    private final ConfigBuilderFactory configBuilderFactory;
    private final ConfigurationAdmin configurationAdmin;

    public DistributorConfigManager(final ExecutorService pExecutor,
                                    final ConfigChangeObserver pObserver,
                                    final ConfigBuilderFactory pConfigBuilderFactory,
                                    final ConfigurationAdmin pConfigurationAdmin) {
        executorService = pExecutor;
        observer = pObserver;
        configBuilderFactory = pConfigBuilderFactory;
        configurationAdmin = pConfigurationAdmin;
    }

    private <T extends Annotation> T getConfig(final Class<T> pConfigInterface, final String pFieldName, final String pPid)
            throws ConfigurationException, IOException {
        final Configuration config = configurationAdmin.getConfiguration(pPid, null);
        final Dictionary<String, ?> props = config.getProperties();

        if (props == null) {
            throw new ConfigurationException(format("%s"), format("No config found for pid %s", pPid));
        }

        return configBuilderFactory.create(pConfigInterface, props).build();
    }

    private static String toTopicConfigPidName(final String pPostfix) {
        return format("%s%s", pPostfix, TOPIC_CONFIG_PID_POSTFIX);
    }

    private void addTopicConfig(final Config pConfig, final String pInstanceName, final String pPostfix, final String pPid)
            throws ConfigurationException {
        final String name = format(NAME_PATTERN, pInstanceName, pPostfix);
        final DistributorTopicConfig topicConfig;
        try {
            topicConfig = DEFAULT_CONFIG.equals(pPid) ? DEFAULT_TOPIC_CONFIG :
                    getConfig(DistributorTopicConfig.class, toTopicConfigPidName(pPostfix), pPid);
        } catch (final IOException e) {
            // TODO: Translate this
            throw new ConfigurationException(name, "Configuration could not be loaded", e);
        }
        final ReliableTopicConfig reliableTopicConfig = new ReliableTopicConfig(name);
        reliableTopicConfig.setReadBatchSize(topicConfig.readBatchSize());
        reliableTopicConfig.setStatisticsEnabled(topicConfig.statisticsEnabled());
        reliableTopicConfig.setTopicOverloadPolicy(BLOCK);

        final RingbufferConfig ringbufferConfig = new RingbufferConfig(name);
        ringbufferConfig.setCapacity(topicConfig.capacity());
        ringbufferConfig.setBackupCount(topicConfig.backupCount());
        ringbufferConfig.setAsyncBackupCount(topicConfig.asyncBackupCount());
        ringbufferConfig.setTimeToLiveSeconds(topicConfig.timeToLiveSeconds());

        pConfig.addReliableTopicConfig(reliableTopicConfig);
        pConfig.addRingBufferConfig(ringbufferConfig);
    }

    private void addTopicConfig(final Config config, DistributorConfig instance) throws ConfigurationException {
        addTopicConfig(config, instance.instanceName(), RESPONSE_POSTFIX, instance.responseTopicConfigPID());
        addTopicConfig(config, instance.instanceName(), DELETE_POSTFIX, instance.deleteTopicConfigPID());
        addTopicConfig(config, instance.instanceName(), TRANSFER_POSTFIX, instance.transferTopicConfigPID());
        addTopicConfig(config, instance.instanceName(), DISCARD_POSTFIX, instance.discardTopicConfigPID());
        addTopicConfig(config, instance.instanceName(), STORE_POSTFIX, instance.storeTopicConfigPID());
        addTopicConfig(config, instance.instanceName(), LOCK_POSTFIX, instance.lockTopicConfigPID());
        addTopicConfig(config, instance.instanceName(), UNLOCK_POSTFIX, instance.unlockTopicConfigPID());
    }

    @Override
    public String getName() {
        // TODO: Translate this
        return FACTORY_PID;
    }

    private Config createConfig(final DistributorConfig pDistributorConfig) throws ConfigurationException {
        final Config config = new Config();
        config.setInstanceName(pDistributorConfig.instanceName());
        config.setGroupConfig(new GroupConfig().setName(pDistributorConfig.groupName()).setPassword(pDistributorConfig.groupPassword()));

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
        final DistributorConfig distributorConfig = configBuilderFactory.create(DistributorConfig.class, pProperties).build();
        final Config config = createConfig(distributorConfig);
        configs.put(pPid, distributorConfig);
        executorService.execute(() -> observer.configUpdated(config));
    }

    @Override
    public void deleted(final String pPid) {
        final DistributorConfig distributorConfig = configs.remove(pPid);
        if (distributorConfig != null) {
            executorService.execute(() -> observer.configDeleted(distributorConfig.instanceName()));
        }
    }

    private static boolean usesTopicConfigPid(final DistributorConfig pConfig, final String pDistributorTopicConfigPid) {
        return pDistributorTopicConfigPid.equals(pConfig.responseTopicConfigPID()) ||
                pDistributorTopicConfigPid.equals(pConfig.deleteTopicConfigPID()) ||
                pDistributorTopicConfigPid.equals(pConfig.transferTopicConfigPID()) ||
                pDistributorTopicConfigPid.equals(pConfig.discardTopicConfigPID()) ||
                pDistributorTopicConfigPid.equals(pConfig.storeTopicConfigPID()) ||
                pDistributorTopicConfigPid.equals(pConfig.lockTopicConfigPID()) ||
                pDistributorTopicConfigPid.equals(pConfig.unlockTopicConfigPID());
    }

    void configUpdated(final String pDistributorTopicConfigPid) throws ConfigurationException {
        for (final Map.Entry<String, DistributorConfig> entry : configs.entrySet()) {
            final DistributorConfig config = entry.getValue();
            if (usesTopicConfigPid(config, pDistributorTopicConfigPid)) {
                try {
                    updated(entry.getKey(), configurationAdmin.getConfiguration(entry.getKey(), null).getProperties());
                } catch (final IOException | ConfigurationException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        }
    }

    void configDeleted(final String pDistributorTopicConfigPid) {
        for (final Map.Entry<String, DistributorConfig> entry : configs.entrySet()) {
            final DistributorConfig config = entry.getValue();
            if (usesTopicConfigPid(config, pDistributorTopicConfigPid)) {
                try {
                    final Configuration configuration = configurationAdmin.getConfiguration(entry.getKey(), null);
                    final Dictionary<String, Object> props = configuration.getProperties();

                    if (pDistributorTopicConfigPid.equals(config.responseTopicConfigPID())) {
                        props.put(toTopicConfigPidName(RESPONSE_POSTFIX), DEFAULT_CONFIG);
                    }
                    if (pDistributorTopicConfigPid.equals(config.deleteTopicConfigPID())) {
                        props.put(toTopicConfigPidName(DELETE_POSTFIX), DEFAULT_CONFIG);
                    }
                    if (pDistributorTopicConfigPid.equals(config.transferTopicConfigPID())) {
                        props.put(toTopicConfigPidName(TRANSFER_POSTFIX), DEFAULT_CONFIG);
                    }
                    if (pDistributorTopicConfigPid.equals(config.discardTopicConfigPID())) {
                        props.put(toTopicConfigPidName(DISCARD_POSTFIX), DEFAULT_CONFIG);
                    }
                    if (pDistributorTopicConfigPid.equals(config.storeTopicConfigPID())) {
                        props.put(toTopicConfigPidName(STORE_POSTFIX), DEFAULT_CONFIG);
                    }
                    if (pDistributorTopicConfigPid.equals(config.lockTopicConfigPID())) {
                        props.put(toTopicConfigPidName(LOCK_POSTFIX), DEFAULT_CONFIG);
                    }
                    if (pDistributorTopicConfigPid.equals(config.unlockTopicConfigPID())) {
                        props.put(toTopicConfigPidName(UNLOCK_POSTFIX), DEFAULT_CONFIG);
                    }
                    configuration.update(props);
                } catch (final IOException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        }
    }
}
