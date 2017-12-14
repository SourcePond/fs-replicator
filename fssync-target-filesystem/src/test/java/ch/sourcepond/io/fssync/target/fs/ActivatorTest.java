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
package ch.sourcepond.io.fssync.target.fs;

import ch.sourcepond.io.fssync.target.api.SyncTarget;
import ch.sourcepond.osgi.cmpn.metatype.ConfigBuilder;
import ch.sourcepond.osgi.cmpn.metatype.ConfigBuilderFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.ManagedServiceFactory;

import java.util.Dictionary;

import static ch.sourcepond.io.fssync.target.fs.Activator.FACTORY_PID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.osgi.framework.Constants.SERVICE_PID;

public class ActivatorTest {
    private static final String EXPECTED_SYNC_DIR = "someExpectedSyncDir";
    private final ConfigBuilder<Config> configBuilder = mock(ConfigBuilder.class);
    private final ConfigBuilderFactory configBuilderFactory = mock(ConfigBuilderFactory.class);
    private final TargetDirectoryFactory targetDirectoryFactory = mock(TargetDirectoryFactory.class);
    private final TargetDirectory syncTarget = mock(TargetDirectory.class);
    private final Config config = mock(Config.class);
    private final Dictionary<String, Object> props = mock(Dictionary.class);
    private final BundleContext context = mock(BundleContext.class);
    private final ServiceRegistration<SyncTarget> registration = mock(ServiceRegistration.class);
    private final Activator activator = new Activator(configBuilderFactory, targetDirectoryFactory);

    @Before
    public void setup() throws Exception {
        when(config.syncDir()).thenReturn(EXPECTED_SYNC_DIR);
        when(configBuilderFactory.create(Config.class, props)).thenReturn(configBuilder);
        when(configBuilder.build()).thenReturn(config);
        when(targetDirectoryFactory.create()).thenReturn(syncTarget);
        when(syncTarget.getConfig()).thenReturn(config);
        activator.start(context);
    }

    @Test
    public void defaultConstructor() {
        new Activator();
    }

    @Test
    public void start() {
        verify(context).registerService(argThat((String[] arr) -> arr.length == 2 && ManagedService.class.getName().equals(arr[0]) && ManagedServiceFactory.class.getName().equals(arr[1])),
                same(activator),
                argThat(p -> p.size() == 1 && p.get(SERVICE_PID).equals(FACTORY_PID)));
    }

    @Test
    public void getName() {
        assertEquals(FACTORY_PID, activator.getName());
    }

    private void updated(final String pExpectedPid, final Update pUpdate) throws Exception {
        when(context.registerService(same(SyncTarget.class), same(syncTarget), argThat(p -> p.size() == 1 &&
                pExpectedPid.equals(p.get(SERVICE_PID))))).thenReturn(registration);
        pUpdate.update();
        final InOrder order = inOrder(syncTarget);
        order.verify(syncTarget).update(config);
        order.verify(syncTarget).setRegistration(registration);
    }

    @Test
    public void factoryUpdated() throws Exception {
        updated(FACTORY_PID, () -> activator.updated(props));
    }

    @Test
    public void updated() throws Exception {
        updated("somePid", () -> activator.updated("somePid", props));
        try {
            activator.updated("someOtherPid", props);
            fail("Exception expected here");
        } catch (final ConfigurationException e) {
            e.getMessage().contains(EXPECTED_SYNC_DIR);
        }
        activator.deleted("somePid");
        activator.updated("someOtherPid", props);
        activator.stop(context);
        verify(syncTarget, times(2)).close();
    }

    @Test
    public void deleteUnknownPid() {
        // No exception should be caused to be thrown
        activator.deleted("unknownPid");
    }
}
