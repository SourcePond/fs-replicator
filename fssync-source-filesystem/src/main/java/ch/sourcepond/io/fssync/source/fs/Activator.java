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
package ch.sourcepond.io.fssync.source.fs;

import ch.sourcepond.io.checksum.api.ResourceProducerFactory;
import ch.sourcepond.io.fssync.common.lib.CompoundServiceFactory;
import ch.sourcepond.io.fssync.common.lib.ServiceListenerRegistrar;
import ch.sourcepond.io.fssync.distributor.api.Distributor;
import ch.sourcepond.io.fssync.source.fs.fswatch.WatchServiceInstaller;
import ch.sourcepond.io.fssync.source.fs.fswatch.WatchServiceInstallerFactory;
import ch.sourcepond.osgi.cmpn.metatype.ConfigBuilderFactory;
import com.google.inject.Injector;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.WatchService;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static java.nio.file.FileSystems.getDefault;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.osgi.framework.Constants.SERVICE_PID;
import static org.slf4j.LoggerFactory.getLogger;

public class Activator implements ManagedServiceFactory, BundleActivator {
    static final String FACTORY_PID = "ch.sourcepond.io.fssync.source.fs.WatchService";
    private static final Logger LOG = getLogger(Activator.class);
    private final FileSystem fs;
    private final ConfigBuilderFactory configBuilderFactory;
    private final CompoundServiceFactory compoundServiceFactory;
    private final ExecutorService distributionExecutor;
    private final ServiceListenerRegistrar registrar;
    private final InjectorFactory injectorFactory;
    private final Map<String, Config> configs = new HashMap<>();
    private final Map<String, WatchServiceInstaller> installers = new HashMap<>();
    private volatile Distributor distributor;
    private ResourceProducerFactory resourceProducerFactory;

    public Activator() {
        this(getDefault(),
                new ConfigBuilderFactory(),
                new CompoundServiceFactory(),
                newSingleThreadExecutor(),
                new ServiceListenerRegistrar(),
                new InjectorFactory());
    }

    public Activator(final FileSystem pFs,
                     final ConfigBuilderFactory pConfigBuildFactory,
                     final CompoundServiceFactory pCompoundServiceFactory,
                     final ExecutorService pDistributionExecutor,
                     final ServiceListenerRegistrar pRegistrar,
                     final InjectorFactory pInjectorFactory) {
        fs = pFs;
        configBuilderFactory = pConfigBuildFactory;
        compoundServiceFactory = pCompoundServiceFactory;
        distributionExecutor = pDistributionExecutor;
        registrar = pRegistrar;
        injectorFactory = pInjectorFactory;
    }

    @Override
    public void start(final BundleContext bundleContext) {
        distributor = compoundServiceFactory.create(bundleContext, distributionExecutor, Distributor.class);
        registrar.registerListener(bundleContext, this::setResourceProducerFactory, this::unsetResourceProducerFactory, ResourceProducerFactory.class);
        final Dictionary<String, Object> props = new Hashtable<>();
        props.put(SERVICE_PID, FACTORY_PID);
        bundleContext.registerService(ManagedServiceFactory.class, this, props);
    }

    @Override
    public synchronized void stop(final BundleContext bundleContext) {
        stopInstallers();
        distributionExecutor.shutdown();
    }

    synchronized void setResourceProducerFactory(final ResourceProducerFactory pResourceProducerFactory) {
        resourceProducerFactory = pResourceProducerFactory;
        startInstallers();
    }

    synchronized void unsetResourceProducerFactory() {
        resourceProducerFactory = null;
        stopInstallers();
    }

    private void startInstallers() {
        if (resourceProducerFactory != null) {
            configs.entrySet().forEach(entry -> {
                if (!installers.containsKey(entry.getKey())) {
                    try {
                        installers.put(entry.getKey(), startInstaller(entry.getValue()));
                    } catch (final IOException e) {
                        LOG.error(e.getMessage(), e);
                    }
                }
            });
        }
    }

    private WatchServiceInstaller startInstaller(final Config pConfig) throws IOException {
        final WatchService watchService = fs.newWatchService();
        final Injector injector = injectorFactory.createInjector(pConfig, watchService, distributor,
                resourceProducerFactory);
        final Path watchedDirectory = fs.getPath(pConfig.watchedDirectory());
        final WatchServiceInstaller installer = injector.getInstance(WatchServiceInstallerFactory.class).create(watchedDirectory);
        installer.start();
        return installer;
    }

    private void stopInstallers() {
        installers.values().removeIf(installer -> stopInstaller(installer));
    }

    private boolean stopInstaller(final WatchServiceInstaller pInstaller) {
        if (pInstaller != null) {
            try {
                pInstaller.close();
            } catch (final IOException e) {
                LOG.warn(e.getMessage(), e);
            }
        }
        return true;
    }

    @Override
    public String getName() {
        // TODO: Replace this with translated value
        return FACTORY_PID;
    }

    @Override
    public synchronized void updated(final String pPid, final Dictionary<String, ?> pProps) throws ConfigurationException {
        final Config config = configBuilderFactory.create(Config.class, pProps).build();
        deleted(pPid);
        configs.put(pPid, config);
        startInstallers();
    }

    @Override
    public synchronized void deleted(final String pPid) {
        configs.remove(pPid);
        stopInstaller(installers.remove(pPid));
    }
}
