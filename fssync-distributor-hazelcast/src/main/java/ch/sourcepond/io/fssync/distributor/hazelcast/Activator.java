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

import ch.sourcepond.io.fssync.compound.CompoundServiceFactory;
import ch.sourcepond.io.fssync.distributor.api.Distributor;
import ch.sourcepond.io.fssync.target.api.SyncTarget;
import ch.sourcepond.osgi.cmpn.metatype.ConfigBuilderFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import static com.google.inject.Guice.createInjector;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.osgi.framework.Constants.SERVICE_PID;

public class Activator implements BundleActivator, ManagedServiceFactory {
    private final Map<String, HazelcastDistributor> distributors = new ConcurrentHashMap<>();
    private final ConfigBuilderFactory factory;
    private final CompoundServiceFactory compoundServiceFactory;
    // TODO: Implement more flexible solution here
    private final ExecutorService executor = newSingleThreadExecutor();
    private volatile BundleContext context;
    private volatile SyncTarget compoundSyncTarget;

    public Activator() {
        this(new ConfigBuilderFactory(), new CompoundServiceFactory());
    }

    // Constructor for testing
    Activator(final ConfigBuilderFactory pFactory, final CompoundServiceFactory pCompoundServiceFactory) {
        factory = pFactory;
        compoundServiceFactory = pCompoundServiceFactory;
    }

    @Override
    public void start(final BundleContext context) throws Exception {
        compoundServiceFactory.create(context, executor, SyncTarget.class);
        final Hashtable<String, String> props = new Hashtable<>();
        props.put(SERVICE_PID, getClass().getPackage().getName());
        context.registerService(ManagedServiceFactory.class, this, props);
    }

    @Override
    public void stop(final BundleContext context) {
        // noop
    }

    @Override
    public String getName() {
        // TODO: Return a localized string here
        return getClass().getName();
    }

    @Override
    public synchronized void updated(final String pPid, final Dictionary<String, ?> pProperties) throws ConfigurationException {
        deleted(pPid);

        final Config config = factory.create(Config.class, pProperties).build();
        final HazelcastDistributor distributor = createInjector(
                new HazelcastDistributorModule(config, compoundSyncTarget)).
                getInstance(HazelcastDistributor.class);
        final Hashtable<String, String> props = new Hashtable<>();
        props.put(SERVICE_PID, pPid);
        distributor.setServiceRegistration(context.registerService(Distributor.class, distributor, props));
    }

    @Override
    public synchronized void deleted(final String pPid) {
        HazelcastDistributor distributor = distributors.remove(pPid);
        if (distributor != null) {
            distributor.close();
        }
    }
}
