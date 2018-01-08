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
import ch.sourcepond.io.fssync.distributor.api.Distributor;
import ch.sourcepond.osgi.cmpn.metatype.ConfigBuilder;
import ch.sourcepond.osgi.cmpn.metatype.ConfigBuilderFactory;
import com.google.inject.Injector;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import java.nio.file.WatchService;
import java.util.Dictionary;
import java.util.concurrent.ExecutorService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

public class InjectorFactoryTest {
    private static final int EXPECTED_CONCURRENCY = 3;
    private final BundleContext context = mock(BundleContext.class);
    private final ResourceProducerFactory resourceProducerFactory = mock(ResourceProducerFactory.class);
    private final ResourceProducer resourceProducer = mock(ResourceProducer.class);
    private final WatchService watchService = mock(WatchService.class);
    private final ExecutorService distributionExecutor = mock(ExecutorService.class);
    private final ConfigBuilderFactory configBuilderFactory = mock(ConfigBuilderFactory.class);
    private final ConfigBuilder<Config> configBuilder = mock(ConfigBuilder.class);
    private final Distributor distributor = mock(Distributor.class);
    private final CompoundServiceFactory compoundServiceFactory = mock(CompoundServiceFactory.class);
    private final Dictionary<String, Object> props = mock(Dictionary.class);
    private final Config config = mock(Config.class, withSettings().defaultAnswer(inv -> inv.getMethod().getDefaultValue()));
    private final InjectorFactory injectorFactory = new InjectorFactory();

    @Before
    public void setup() throws Exception {
        when(compoundServiceFactory.create(context, distributionExecutor, Distributor.class)).thenReturn(distributor);
        when(configBuilderFactory.create(Config.class, props)).thenReturn(configBuilder);
        when(configBuilder.build()).thenReturn(config);
        when(resourceProducerFactory.create(EXPECTED_CONCURRENCY)).thenReturn(resourceProducer);
    }


    @Test
    public void createInjector() {
        final Injector injector = injectorFactory.createInjector(config, watchService, distributor, resourceProducerFactory);
    }
}
