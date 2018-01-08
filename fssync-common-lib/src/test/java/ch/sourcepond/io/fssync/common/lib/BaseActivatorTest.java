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
package ch.sourcepond.io.fssync.common.lib;

import ch.sourcepond.osgi.cmpn.metatype.ConfigBuilder;
import ch.sourcepond.osgi.cmpn.metatype.ConfigBuilderFactory;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import java.lang.annotation.Annotation;
import java.util.Dictionary;
import java.util.concurrent.ExecutorService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Base test for {@link BaseActivator} implementations.
 *
 * @param <S> Service-type
 * @param <T> Activator-type
 * @param <C> Config-type
 */
public abstract class BaseActivatorTest<S extends Configurable<C>, T extends BaseActivator<S, C>, C extends Annotation> {
    public static final String EXPECTED_UNIQUE_ID = "expectedUniqueId";
    protected final Dictionary<String, ?> props = mock(Dictionary.class);
    protected final CompoundServiceFactory compoundServiceFactory = mock(CompoundServiceFactory.class);
    protected final ConfigBuilderFactory configBuilderFactory = mock(ConfigBuilderFactory.class);
    protected final ConfigBuilder<C> configBuilder = mock(ConfigBuilder.class);
    protected final BundleContext context = mock(BundleContext.class);
    protected final ExecutorService executorService = mock(ExecutorService.class);
    protected C config;
    protected T activator;

    protected abstract Class<C> getConfigAnnotation();

    public abstract void verifyGetFactoryId();

    @Before
    public void setup() throws Exception  {
        activator.start(context);
    }

    @Test
    public void verifyConfiguration() throws Exception {
        assertSame(getConfigAnnotation(), activator.getConfigAnnotation());

        // There must be a corresponding method
        getConfigAnnotation().getMethod(activator.getUniqueIdName());
        assertEquals(EXPECTED_UNIQUE_ID, activator.getUniqueId(config));
    }

    @Test
    public void verifyService() throws Exception {
        final Class<?> serviceInterface = getClass().getClassLoader().loadClass(activator.getServiceInterfaceName());
        assertTrue(serviceInterface.isInterface());
        final S service = activator.createService(config);
        assertNotNull(service);
        assertTrue(serviceInterface.isAssignableFrom(service.getClass()));
    }
}
