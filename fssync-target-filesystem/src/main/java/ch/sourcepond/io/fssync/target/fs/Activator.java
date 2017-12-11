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
package ch.sourcepond.io.fssync.target.fs;

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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.lang.String.format;
import static org.osgi.framework.Constants.SERVICE_PID;

public class Activator implements BundleActivator, ManagedService, ManagedServiceFactory {
    static final String FACTORY_PID = "ch.sourcepond.io.fssync.target.fs.factory";
    private final Map<String, String> syncDirToPid = new HashMap<>();
    private final ConcurrentMap<String, TargetDirectory> targets = new ConcurrentHashMap<>();
    private final ConfigBuilderFactory configBuilderFactory;
    private final TargetDirectoryFactory targetDirectoryFactory;
    private volatile BundleContext context;

    public Activator() {
        this(new ConfigBuilderFactory(), new TargetDirectoryFactory());
    }

    // Constructor for testing
    Activator(final ConfigBuilderFactory pConfigBuilderFactory, final TargetDirectoryFactory pTargetDirectoryFactory) {
        configBuilderFactory = pConfigBuilderFactory;
        targetDirectoryFactory = pTargetDirectoryFactory;
    }

    @Override
    public void start(final BundleContext pContext) {
        context = pContext;
        final Hashtable<String, String> props = new Hashtable<>();
        props.put(SERVICE_PID, FACTORY_PID);
        context.registerService(new String[]{ManagedService.class.getName(),
                ManagedServiceFactory.class.getName()}, this, props);
    }

    @Override
    public void stop(final BundleContext context) {
        targets.values().forEach(t -> t.close());
    }

    @Override
    public String getName() {
        // TODO (RH): Use localized string here
        return FACTORY_PID;
    }

    @Override
    public void updated(final Dictionary<String, ?> properties) throws ConfigurationException {
        updated(FACTORY_PID, properties);
    }

    @Override
    public void updated(final String pPid, final Dictionary<String, ?> pProperties) throws ConfigurationException {
        final SyncTargetConfig config = configBuilderFactory.create(SyncTargetConfig.class, pProperties).build();

        synchronized (syncDirToPid) {
            final String pid = syncDirToPid.get(config.syncDir());
            if (pid != null && !pid.equals(pPid)) {
                throw new ConfigurationException("syncDir", format("Sync directory %s is already used by PID %s", config.syncDir(), pid));
            }
            syncDirToPid.put(config.syncDir(), pPid);
        }

        final TargetDirectory dir = targets.computeIfAbsent(pPid, pid -> targetDirectoryFactory.create());
        dir.update(config);
        final Hashtable<String, String> props = new Hashtable<>();
        props.put(SERVICE_PID, pPid);
        dir.setRegistration(context.registerService(SyncTarget.class, dir, props));
    }

    @Override
    public void deleted(final String pPid) {
        synchronized (syncDirToPid) {
            final TargetDirectory syncTarget = targets.remove(pPid);
            if (syncTarget != null) {
                syncTarget.close();
                syncDirToPid.remove(syncTarget.getConfig().syncDir());
            }
        }
    }
}
