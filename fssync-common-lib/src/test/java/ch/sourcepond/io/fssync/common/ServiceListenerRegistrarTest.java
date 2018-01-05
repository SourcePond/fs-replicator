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
package ch.sourcepond.io.fssync.common;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import java.io.Serializable;
import java.util.Collection;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServiceListenerRegistrarTest {
    private final BundleContext context = mock(BundleContext.class);
    private final Serializable service = mock(Serializable.class);
    private final Consumer<Serializable> registration = mock(Consumer.class);
    private final Runnable unregistration = mock(Runnable.class);
    private final ServiceReference<Object> reference = mock(ServiceReference.class);
    private final Collection<ServiceReference<Object>> references = asList(reference);
    private final ServiceListenerRegistrar registrar = new ServiceListenerRegistrar();

    @Before
    public void setup() throws Exception {
        when(context.getService(reference)).thenReturn(service);
        when(context.getServiceReferences(Serializable.class, null)).thenReturn((Collection) references);
    }

    @Test
    public void registerListener() throws Exception {
        registrar.registerListener(context, registration, unregistration, Serializable.class);
        final InOrder order = inOrder(context, registration);
        order.verify(context).addServiceListener(argThat(inv -> ServiceListenerImpl.class.equals(inv.getClass())), eq("(objectClass=java.io.Serializable)"));
        order.verify(registration).accept(service);
    }

    @Test
    public void invalidSyntaxException() throws Exception {
        final InvalidSyntaxException expected = new InvalidSyntaxException("", "");
        doThrow(expected).when(context).addServiceListener(any(), anyString());
        try {
            registrar.registerListener(context, registration, unregistration, Serializable.class);
            fail("Exception expected here");
        } catch (final IllegalStateException e) {
            assertSame(expected, e.getCause());
        }
    }
}
