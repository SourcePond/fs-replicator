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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static java.lang.reflect.Proxy.newProxyInstance;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.osgi.framework.ServiceEvent.MODIFIED;
import static org.osgi.framework.ServiceEvent.MODIFIED_ENDMATCH;
import static org.osgi.framework.ServiceEvent.REGISTERED;
import static org.osgi.framework.ServiceEvent.UNREGISTERING;

public class CompoundServiceHandlerTest {
    private static final String EXPECTED_SYNC_DIR = "expectedSyncDir";
    private static final String EXPECTED_PATH = "expectedPath";
    private final BundleContext context = mock(BundleContext.class);
    private final ServiceReference<TestService> reference = mock(ServiceReference.class);
    private final TestService service = mock(TestService.class);
    private ExecutorService executor = newSingleThreadScheduledExecutor();
    private CompoundServiceHandler<TestService, TestException> handler;
    private TestService proxy;

    @Before
    public void setup() throws Exception {
        when(context.getService(reference)).thenReturn(service);
        handler = new CompoundServiceHandler<>(context,
                TestException.class.getConstructor(String.class), executor);
        proxy = (TestService) newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{TestService.class}, handler);
        handler.serviceChanged(new ServiceEvent(REGISTERED, reference));
    }

    @After
    public void tearDown() {
        executor.shutdown();
    }

    @Test
    public void registerService() throws Exception {
        proxy.start(EXPECTED_SYNC_DIR, EXPECTED_PATH);
        verify(service).start(EXPECTED_SYNC_DIR, EXPECTED_PATH);
    }

    @Test
    public void unregisterService() throws Exception {
        handler.serviceChanged(new ServiceEvent(UNREGISTERING, reference));
        proxy.start(EXPECTED_SYNC_DIR, EXPECTED_PATH);
        verifyZeroInteractions(service);
    }

    @Test
    public void unregisterServiceEndMatch() throws Exception {
        handler.serviceChanged(new ServiceEvent(MODIFIED_ENDMATCH, reference));
        proxy.start(EXPECTED_SYNC_DIR, EXPECTED_PATH);
        verifyZeroInteractions(service);
    }

    @Test
    public void ignoreModified() throws Exception {
        handler.serviceChanged(new ServiceEvent(MODIFIED, reference));
        proxy.start(EXPECTED_SYNC_DIR, EXPECTED_PATH);
        verify(service).start(EXPECTED_SYNC_DIR, EXPECTED_PATH);
    }

    @Test
    public void exceptionOccured() throws Exception {
        final TestException expected = new TestException("Some message");
        doThrow(expected).when(service).start(EXPECTED_SYNC_DIR, EXPECTED_PATH);
        try {
            proxy.start(EXPECTED_SYNC_DIR, EXPECTED_PATH);
            fail("Exception expected!");
        } catch (final TestException e) {
            assertEquals("At least one exception occurred (only the first one is visible in stacktrace)!\n\t - " +
                    "Some message\n", e.getMessage());
            final Throwable cause = e.getCause();
            assertSame(expected, cause);
        }
    }

    @Test(expected = UndeclaredThrowableException.class)
    public void undeclaredExceptionOccured() throws Exception {
        executor.shutdown();
        executor = mock(ExecutorService.class);
        final Future<?> future = mock(Future.class);
        when(executor.submit((Callable) Mockito.any())).thenReturn(future);

        final ClassNotFoundException undeclaredException = new ClassNotFoundException();
        final ExecutionException exception = new ExecutionException(undeclaredException);
        doThrow(exception).when(future).get();

        setup();
        proxy.start(EXPECTED_SYNC_DIR, EXPECTED_PATH);
    }
}
