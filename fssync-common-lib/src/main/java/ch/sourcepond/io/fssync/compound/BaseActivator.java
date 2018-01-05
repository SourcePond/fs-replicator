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
package ch.sourcepond.io.fssync.compound;

import ch.sourcepond.osgi.cmpn.metatype.ConfigBuilderFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.ManagedServiceFactory;

import java.lang.annotation.Annotation;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.lang.String.format;
import static org.osgi.framework.Constants.SERVICE_PID;

public abstract class BaseActivator<T extends Configurable<C>, C extends Annotation>
        implements BundleActivator, ManagedService, ManagedServiceFactory {
    private final Map<String, String> uniqueIdToPid = new HashMap<>();
    private final ConcurrentMap<String, T> services = new ConcurrentHashMap<>();
    private final ConfigBuilderFactory configBuilderFactory;
    private volatile BundleContext context;

    protected BaseActivator(final ConfigBuilderFactory pConfigBuilderFactory) {
        configBuilderFactory = pConfigBuilderFactory;
    }

    @Override
    public void start(final BundleContext pContext) {
        context = pContext;
        final Hashtable<String, String> props = new Hashtable<>();
        props.put(SERVICE_PID, getFactoryPid());
        pContext.registerService(new String[]{ManagedService.class.getName(),
                ManagedServiceFactory.class.getName()}, this, props);
    }

    @Override
    public void stop(final BundleContext context) {
        services.values().forEach(t -> t.close());
    }

    @Override
    public String getName() {
        // TODO (RH): Use localized string here
        return getFactoryPid();
    }

    @Override
    public void updated(final Dictionary<String, ?> properties) throws ConfigurationException {
        updated(getFactoryPid(), properties);
    }

    protected abstract String getFactoryPid();

    protected abstract String getUniqueIdName();

    protected abstract String getUniqueId(C pConfig);

    protected abstract Class<C> getConfigAnnotation();

    protected abstract String getServiceInterfaceName();

    protected abstract T createService(C pConfig);

    @Override
    public void updated(final String pPid, final Dictionary<String, ?> pProperties) throws ConfigurationException {
        final C config = configBuilderFactory.create(getConfigAnnotation(), pProperties).build();

        synchronized (uniqueIdToPid) {
            final String uniqueIdName = getUniqueIdName();
            final String uniqueId = getUniqueId(config);
            final String pid = uniqueIdToPid.get(uniqueId);
            if (pid != null && !pid.equals(pPid)) {
                throw new ConfigurationException(uniqueIdName, format("'%s' with value %s is already used by PID %s", uniqueIdName, uniqueId, pid));
            }
            uniqueIdToPid.put(uniqueId, pPid);
        }

        final T service = services.computeIfAbsent(pPid, pid -> createService(config));
        service.update(config);
        final Hashtable<String, String> props = new Hashtable<>();
        props.put(SERVICE_PID, pPid);
        service.setRegistration(context.registerService(getServiceInterfaceName(), service, props));
    }

    @Override
    public void deleted(final String pPid) {
        synchronized (uniqueIdToPid) {
            final T service = services.remove(pPid);
            if (service != null) {
                service.close();
                uniqueIdToPid.remove(getUniqueId(service.getConfig()));
            }
        }
    }
}
