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

import org.junit.After;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class CompoundServiceFactoryTest {
    private final BundleContext context = mock(BundleContext.class);
    private final ExecutorService executor = newSingleThreadExecutor();
    private final TestService service = mock(TestService.class);
    private final CompoundServiceFactory factory = new CompoundServiceFactory();

    @After
    public void tearDown() {
        executor.shutdown();
    }

    @Test
    public void createReferencesAreNull() throws Exception {
        // Even in case when the service references are null this should work properly
        final TestService proxy = factory.create(context, executor, TestService.class);
        assertNotNull(proxy);
        proxy.start(Constants.EXPECTED_SYNC_DIR, Constants.EXPECTED_PATH);
        verifyZeroInteractions(service);
    }

    @Test
    public void create() throws Exception {
        final ServiceReference<TestService> reference = mock(ServiceReference.class);
        final ServiceReference<?>[] references = new ServiceReference<?>[]{reference};
        when(context.getServiceReferences(TestService.class.getName(), "(objectClass=ch.sourcepond.io.fssync.common.lib.TestService)")).thenReturn(references);
        when(context.getService(reference)).thenReturn(service);
        final TestService proxy = factory.create(context, executor, TestService.class);
        proxy.start(Constants.EXPECTED_SYNC_DIR, Constants.EXPECTED_PATH);
        verify(service).start(Constants.EXPECTED_SYNC_DIR, Constants.EXPECTED_PATH);
    }

    @Test
    public void createIllegalMethod() {
        try {
            factory.create(context, executor, TestServiceWithNonVoidMethods.class);
            fail("Exception expected!");
        } catch (final IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("void methods"));
        }
    }

    @Test
    public void createNotAnInterface() {
        try {
            factory.create(context, executor, Object.class);
            fail("Exception expected!");
        } catch (final IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("not an interface"));
        }
    }

    @Test(expected = IllegalStateException.class)
    public void invalidSyntaxException() throws Exception {
        final InvalidSyntaxException expected = new InvalidSyntaxException("", "");
        doThrow(expected).when(context).addServiceListener(any(), anyString());
        factory.create(context, executor, TestService.class);
    }
}
