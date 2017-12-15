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

import ch.sourcepond.io.fssync.compound.BaseActivator;
import ch.sourcepond.io.fssync.compound.CompoundServiceFactory;
import ch.sourcepond.io.fssync.distributor.api.Distributor;
import ch.sourcepond.io.fssync.target.api.SyncTarget;
import ch.sourcepond.osgi.cmpn.metatype.ConfigBuilderFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.ManagedServiceFactory;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static com.google.inject.Guice.createInjector;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.osgi.framework.Constants.SERVICE_PID;

public class Activator extends BaseActivator<HazelcastDistributor, Config> {
    static final String FACTORY_PID = "ch.sourcepond.io.fssync.distributor.hazelcast.configBuilderFactory";
    private final CompoundServiceFactory compoundServiceFactory;
    private final ExecutorService executor;
    private volatile BundleContext context;
    private volatile SyncTarget compoundSyncTarget;

    public Activator() {
        this(new ConfigBuilderFactory(), new CompoundServiceFactory(), /* TODO: Implement more flexible solution here */ newSingleThreadExecutor());
    }

    // Constructor for testing
    Activator(final ConfigBuilderFactory pConfigBuilderFactory,
              final CompoundServiceFactory pCompoundServiceFactory,
              final ExecutorService pExecutor) {
        super(pConfigBuilderFactory);
        compoundServiceFactory = pCompoundServiceFactory;
        executor = pExecutor;
    }

    @Override
    public void start(final BundleContext pContext) {
        compoundSyncTarget = compoundServiceFactory.create(pContext, executor, SyncTarget.class);
        super.start(pContext);
    }

    @Override
    protected String getFactoryPid() {
        return FACTORY_PID;
    }

    @Override
    protected String getUniqueIdName() {
        return "existingInstanceName";
    }

    @Override
    protected String determineUniqueId(final Config pConfig) {
        return pConfig.existingInstanceName();
    }

    @Override
    protected Class<Config> getConfigAnnotation() {
        return Config.class;
    }

    @Override
    protected String getServiceInterfaceName() {
        return Distributor.class.getName();
    }

    @Override
    protected HazelcastDistributor createService(final Config pConfig) {
        return createInjector(
                new HazelcastDistributorModule(pConfig, compoundSyncTarget)).
                getInstance(HazelcastDistributor.class);
    }
}
