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

import ch.sourcepond.io.fssync.distributor.api.Distributor;
import ch.sourcepond.osgi.cmpn.metatype.ConfigBuilderFactory;
import com.google.inject.Injector;
import com.hazelcast.core.HazelcastInstance;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.google.inject.Guice.createInjector;
import static org.osgi.framework.Constants.SERVICE_PID;

public class Activator implements BundleActivator, ManagedServiceFactory {
    private final ConcurrentMap<String, ServiceRegistration<Distributor>> distributors = new ConcurrentHashMap<>();
    private final ConfigBuilderFactory factory;
    private volatile BundleContext context;

    public Activator() {
        this(new ConfigBuilderFactory());
    }

    // Constructor for testing
    Activator(final ConfigBuilderFactory pFactory) {
        factory = pFactory;
    }

    @Override
    public void start(final BundleContext context) throws Exception {
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
    public void updated(final String pPid, final Dictionary<String, ?> pProperties) throws ConfigurationException {
        final Config config = factory.create(pProperties, Config.class).build();


        final Injector injector = createInjector(new HazelcastDistributorModule(config));
        injector.getInstance(HazelcastInstance.class);

        HazelcastDistributor distributor = createInjector(new HazelcastDistributorModule(config)).
                getInstance(HazelcastDistributor.class);

        distributor.setServiceRegistration(context.registerService(Distributor.class, distributor, null));
    }

    @Override
    public void deleted(final String pPid) {

    }
}
