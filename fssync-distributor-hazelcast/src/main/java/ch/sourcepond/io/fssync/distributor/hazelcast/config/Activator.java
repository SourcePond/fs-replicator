/*Copyright (C) 2018 Roland Hauser, <sourcepond@gmail.com>

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

import ch.sourcepond.io.fssync.compound.CompoundServiceFactory;
import ch.sourcepond.io.fssync.distributor.api.Distributor;
import ch.sourcepond.io.fssync.distributor.hazelcast.HazelcastDistributor;
import ch.sourcepond.io.fssync.distributor.hazelcast.HazelcastDistributorModule;
import ch.sourcepond.io.fssync.target.api.SyncTarget;
import ch.sourcepond.osgi.cmpn.metatype.ConfigBuilderFactory;
import com.hazelcast.config.Config;
import com.hazelcast.osgi.HazelcastOSGiService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedServiceFactory;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static ch.sourcepond.io.fssync.distributor.hazelcast.config.ServiceListenerImpl.registerListener;
import static com.google.inject.Guice.createInjector;
import static java.lang.String.format;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.osgi.framework.Constants.OBJECTCLASS;

public class Activator implements BundleActivator {
    private final Map<String, HazelcastDistributor> distributors = new HashMap<>();
    private final ConfigManager configManager;
    private final TopicConfigManager topicConfigManager;
    private final CompoundServiceFactory compoundServiceFactory;
    private final ExecutorService executor;
    private volatile ConfigurationAdmin configurationAdmin;
    private volatile HazelcastOSGiService hazelcastOSGiService;
    private volatile SyncTarget compoundSyncTarget;
    private volatile ServiceRegistration<ManagedServiceFactory> configManagerRegistration;
    private volatile ServiceRegistration<ManagedServiceFactory> topicConfigManagerRegistration;
    private volatile BundleContext bundleContext;

    public Activator() {
        final ConfigBuilderFactory factory = new ConfigBuilderFactory();
        configManager = new ConfigManager(this, factory);
        topicConfigManager = new TopicConfigManager(configManager, factory);
        compoundServiceFactory = new CompoundServiceFactory();
        executor = /* TODO: Implement more flexible solution here */ newSingleThreadExecutor();
    }

    public Activator(final ConfigManager pConfigManager,
                     final TopicConfigManager pTopicConfigManager,
                     final CompoundServiceFactory pCompoundServiceFactory,
                     final ExecutorService pExecutor) {
        configManager = pConfigManager;
        topicConfigManager = pTopicConfigManager;
        compoundServiceFactory = pCompoundServiceFactory;
        executor = pExecutor;
    }

    public void configUpdated(final Config pHazelcastConfig, final DistributorConfig pDistributorConfig) {
        synchronized (distributors) {
            configDeleted(pDistributorConfig.instanceName());
            final HazelcastDistributor distributor = createInjector(
                    new HazelcastDistributorModule(pDistributorConfig, pHazelcastConfig, compoundSyncTarget)).
                    getInstance(HazelcastDistributor.class);
            distributor.setRegistration(bundleContext.registerService(Distributor.class, distributor, null));
            distributors.put(pDistributorConfig.instanceName(), distributor);
        }
    }

    public void configDeleted(final String pInstanceName) {
        synchronized (distributors) {
            HazelcastDistributor distributor = distributors.remove(pInstanceName);
            if (distributor != null) {
                distributor.close();
            }
        }
    }

    @Override
    public void start(final BundleContext pBundleContext) throws Exception {
        bundleContext = pBundleContext;
        compoundSyncTarget = compoundServiceFactory.create(pBundleContext, executor, SyncTarget.class);
        registerListener(pBundleContext, this::setConfigAdmin, this::unsetConfigAdmin, ConfigurationAdmin.class);
        registerListener(pBundleContext, this::setHazelcastOSGiService, this::unsetHazelcastOSGiService, HazelcastOSGiService.class);
    }

    private void setConfigAdmin(final ConfigurationAdmin pConfigAdmin) {
        configurationAdmin = pConfigAdmin;
        registerConfigManager();
    }

    private void unsetConfigAdmin() {
        configurationAdmin = null;
        configManager.setConfigAdmin(null);
        unregisterConfigManager();
    }

    private void setHazelcastOSGiService(final HazelcastOSGiService pHazelcastOSGiService) {
        hazelcastOSGiService = pHazelcastOSGiService;
        registerConfigManager();
    }

    private void unsetHazelcastOSGiService() {
        hazelcastOSGiService = null;
        unregisterConfigManager();
    }

    private synchronized void unregisterConfigManager() {
        topicConfigManagerRegistration.unregister();
        configManagerRegistration.unregister();
        configManagerRegistration = null;
        topicConfigManagerRegistration = null;
    }

    private synchronized void registerConfigManager() {
        if (configurationAdmin != null && hazelcastOSGiService != null) {
            configManager.setConfigAdmin(configurationAdmin);
            configManagerRegistration = register(configManager, ConfigManager.FACTORY_PID);
            topicConfigManagerRegistration = register(topicConfigManager, TopicConfigManager.FACTORY_PID);
        }
    }

    @Override
    public void stop(final BundleContext bundleContext) {
        synchronized (distributors) {
            distributors.values().forEach(distributor -> distributor.close());
        }
    }

    private ServiceRegistration<ManagedServiceFactory> register(final ManagedServiceFactory pFactory, final String pPid) {
        final Dictionary<String, Object> props = new Hashtable<>();
        props.put(Constants.SERVICE_PID, pPid);
        return bundleContext.registerService(ManagedServiceFactory.class, pFactory, props);
    }
}
