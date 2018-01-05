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

import org.junit.Test;
import org.mockito.InOrder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;

import java.util.function.Consumer;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.osgi.framework.ServiceEvent.MODIFIED;
import static org.osgi.framework.ServiceEvent.MODIFIED_ENDMATCH;
import static org.osgi.framework.ServiceEvent.REGISTERED;
import static org.osgi.framework.ServiceEvent.UNREGISTERING;

public class ServiceListenerImplTest {
    private final Object service = new Object();
    private final BundleContext context = mock(BundleContext.class);
    private final ServiceReference<Object> reference = mock(ServiceReference.class);
    private final Consumer<Object> registration = mock(Consumer.class);
    private final Runnable unregistration = mock(Runnable.class);
    private final ServiceListenerImpl<Object> listener = new ServiceListenerImpl<>(context, registration, unregistration);

    private void verifyUnregistering(final int pEventType) {
        listener.serviceChanged(new ServiceEvent(pEventType, reference));
        final InOrder order = inOrder(unregistration, context);
        order.verify(unregistration).run();
        order.verify(context).ungetService(reference);
        verifyZeroInteractions(registration);
    }

    @Test
    public void unregistering() {
        verifyUnregistering(UNREGISTERING);
    }

    @Test
    public void modifiedEndmatch() {
        verifyUnregistering(MODIFIED_ENDMATCH);
    }

    @Test
    public void registered() {
        when(context.getService(reference)).thenReturn(service);
        listener.serviceChanged(new ServiceEvent(REGISTERED, reference));
        verify(registration).accept(service);
        verifyZeroInteractions(unregistration);
    }

    @Test
    public void modified() {
        listener.serviceChanged(new ServiceEvent(MODIFIED, reference));
        verifyZeroInteractions(context, unregistration, registration);
    }
}
