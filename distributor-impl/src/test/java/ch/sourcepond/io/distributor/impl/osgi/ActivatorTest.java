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
package ch.sourcepond.io.distributor.impl.osgi;

import ch.sourcepond.io.distributor.api.DistributorFactory;
import ch.sourcepond.io.distributor.impl.HazelcastDistributorFactory;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

public class ActivatorTest {
    private final BundleContext context = mock(BundleContext.class);
    private final Activator activator = new Activator();

    @Test
    public void start() {
        activator.start(context);
        verify(context).registerService(same(DistributorFactory.class), (DistributorFactory) argThat(factory -> factory != null && factory instanceof HazelcastDistributorFactory), isNull());
        verifyNoMoreInteractions(context);
    }

    @Test
    public void stop() {
        activator.stop(context);
        verifyZeroInteractions(context);
    }
}
