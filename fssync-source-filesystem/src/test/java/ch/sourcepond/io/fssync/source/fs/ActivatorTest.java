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
package ch.sourcepond.io.fssync.source.fs;

import ch.sourcepond.io.checksum.api.ResourceProducer;
import ch.sourcepond.io.checksum.api.ResourceProducerFactory;
import ch.sourcepond.io.fssync.common.lib.CompoundServiceFactory;
import ch.sourcepond.io.fssync.common.lib.ServiceListenerRegistrar;
import ch.sourcepond.io.fssync.distributor.api.Distributor;
import ch.sourcepond.io.fssync.source.fs.fswatch.WatchServiceInstaller;
import ch.sourcepond.io.fssync.source.fs.fswatch.WatchServiceInstallerFactory;
import ch.sourcepond.osgi.cmpn.metatype.ConfigBuilder;
import ch.sourcepond.osgi.cmpn.metatype.ConfigBuilderFactory;
import com.google.inject.Injector;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ManagedServiceFactory;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.WatchService;
import java.util.Dictionary;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

import static ch.sourcepond.io.fssync.source.fs.Activator.FACTORY_PID;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.osgi.framework.Constants.SERVICE_PID;

public class ActivatorTest {
    private static final String EXPECTED_CONFIG_PID = "someExpectedConfigPid";
    private static final int EXPECTED_CONCURRENCY = 3;
    private static final String EXPECTED_WATCHED_DIRECTORY = "someExpectedWatchedDir";
    private final BundleContext context = mock(BundleContext.class);
    private final ResourceProducerFactory resourceProducerFactory = mock(ResourceProducerFactory.class);
    private final ResourceProducer resourceProducer = mock(ResourceProducer.class);
    private final FileSystem fs = mock(FileSystem.class);
    private final WatchService watchService = mock(WatchService.class);
    private final Path watchedDirectory = mock(Path.class);
    private final ScheduledExecutorService watchServiceExecutor = mock(ScheduledExecutorService.class);
    private final ExecutorService distributionExecutor = mock(ExecutorService.class);
    private final ServiceListenerRegistrar registrar = mock(ServiceListenerRegistrar.class);
    private final ConfigBuilderFactory configBuilderFactory = mock(ConfigBuilderFactory.class);
    private final ConfigBuilder<Config> configBuilder = mock(ConfigBuilder.class);
    private final Distributor distributor = mock(Distributor.class);
    private final CompoundServiceFactory compoundServiceFactory = mock(CompoundServiceFactory.class);
    private final Dictionary<String, Object> props = mock(Dictionary.class);
    private final Config config = mock(Config.class, withSettings().defaultAnswer(inv -> inv.getMethod().getDefaultValue()));
    private final InjectorFactory injectorFactory = mock(InjectorFactory.class);
    private final Injector injector = mock(Injector.class);
    private final WatchServiceInstallerFactory installerFactory = mock(WatchServiceInstallerFactory.class);
    private final WatchServiceInstaller installer = mock(WatchServiceInstaller.class);
    private final Activator activator = new Activator(fs, configBuilderFactory, compoundServiceFactory,
            distributionExecutor, registrar, injectorFactory);

    @Before
    public void setup() throws Exception {
        when(injectorFactory.createInjector(config, watchService, distributor, resourceProducerFactory)).thenReturn(injector);
        when(installerFactory.create(watchedDirectory)).thenReturn(installer);
        when(injector.getInstance(WatchServiceInstallerFactory.class)).thenReturn(installerFactory);
        when(fs.newWatchService()).thenReturn(watchService);
        when(config.watchedDirectory()).thenReturn(EXPECTED_WATCHED_DIRECTORY);
        when(compoundServiceFactory.create(context, distributionExecutor, Distributor.class)).thenReturn(distributor);
        when(configBuilderFactory.create(Config.class, props)).thenReturn(configBuilder);
        when(configBuilder.build()).thenReturn(config);
        when(resourceProducerFactory.create(EXPECTED_CONCURRENCY)).thenReturn(resourceProducer);
        when(fs.getPath(EXPECTED_WATCHED_DIRECTORY)).thenReturn(watchedDirectory);
        activator.setResourceProducerFactory(resourceProducerFactory);
        activator.start(context);
    }

    @Test
    public void verifyDefaultConstructor() {
        new Activator();
    }

    @Test
    public void unsetSetResourceProducerFactory() throws Exception {
        activator.updated(EXPECTED_CONFIG_PID, props);
        activator.unsetResourceProducerFactory();
        activator.setResourceProducerFactory(resourceProducerFactory);

        final InOrder order = inOrder(installer);
        order.verify(installer).start();
        order.verify(installer).close();
        order.verify(installer).start();
    }

    @Test
    public void stopInstallerCloseException() throws Exception {
        activator.updated(EXPECTED_CONFIG_PID, props);
        doThrow(IOException.class).when(installer).close();

        // This should not cause an exception
        activator.stop(context);
    }

    @Test
    public void stop() throws Exception {
        activator.updated(EXPECTED_CONFIG_PID, props);
        activator.stop(context);
        verify(installer).close();
        verify(distributionExecutor).shutdown();
    }

    @Test
    public void getName() {
        assertEquals(FACTORY_PID, activator.getName());
    }

    @Test
    public void startFailed() throws Exception {
        doThrow(IOException.class).when(fs).newWatchService();

        // This should not throw an exception
        activator.updated(EXPECTED_CONFIG_PID, props);
    }

    @Test
    public void start() {
        verify(context).registerService(Mockito.eq(ManagedServiceFactory.class), Mockito.same(activator), argThat(inv -> FACTORY_PID.equals(inv.get(SERVICE_PID))));
        final ArgumentCaptor<Consumer<ResourceProducerFactory>> registrationCaptor = ArgumentCaptor.forClass(Consumer.class);
        final ArgumentCaptor<Runnable> unregistrationCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(registrar).registerListener(eq(context), registrationCaptor.capture(), unregistrationCaptor.capture(), eq(ResourceProducerFactory.class));
    }
}
