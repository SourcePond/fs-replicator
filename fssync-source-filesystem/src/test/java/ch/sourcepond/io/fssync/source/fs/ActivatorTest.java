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
import ch.sourcepond.io.fssync.common.CompoundServiceFactory;
import ch.sourcepond.io.fssync.common.ServiceListenerRegistrar;
import ch.sourcepond.io.fssync.distributor.api.Distributor;
import ch.sourcepond.osgi.cmpn.metatype.ConfigBuilder;
import ch.sourcepond.osgi.cmpn.metatype.ConfigBuilderFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ManagedServiceFactory;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.WatchService;
import java.util.Dictionary;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

import static ch.sourcepond.io.fssync.source.fs.Activator.FACTORY_PID;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
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
    private final Activator activator = new Activator(fs, configBuilderFactory, compoundServiceFactory,
            distributionExecutor, watchServiceExecutor, registrar);

    @Before
    public void setup() throws Exception {
        when(fs.newWatchService()).thenReturn(watchService);
        when(config.watchedDirectory()).thenReturn(EXPECTED_WATCHED_DIRECTORY);
        when(compoundServiceFactory.create(context, distributionExecutor, Distributor.class)).thenReturn(distributor);
        when(configBuilderFactory.create(Config.class, props)).thenReturn(configBuilder);
        when(configBuilder.build()).thenReturn(config);
        when(resourceProducerFactory.create(EXPECTED_CONCURRENCY)).thenReturn(resourceProducer);
        when(fs.getPath(EXPECTED_WATCHED_DIRECTORY)).thenReturn(watchedDirectory);
        activator.setResourceProducerFactory(resourceProducerFactory);
        activator.start(context);
        activator.updated(EXPECTED_CONFIG_PID, props);
    }

    @After
    public void tearDown() throws Exception {
        activator.stop(context);
    }

    @Test
    public void verifyStart() {
        verify(context).registerService(Mockito.eq(ManagedServiceFactory.class), Mockito.same(activator), argThat(inv -> FACTORY_PID.equals(inv.get(SERVICE_PID))));
        final ArgumentCaptor<Consumer<ResourceProducerFactory>> registrationCaptor = ArgumentCaptor.forClass(Consumer.class);
        final ArgumentCaptor<Runnable> unregistrationCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(registrar).registerListener(eq(context), registrationCaptor.capture(), unregistrationCaptor.capture(), eq(ResourceProducerFactory.class));
    }
}
