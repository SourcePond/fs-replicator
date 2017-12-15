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
package ch.sourcepond.io.fssync.distributor.hazelcast;

import ch.sourcepond.io.fssync.compound.BaseActivatorTest;
import ch.sourcepond.io.fssync.target.api.SyncTarget;
import com.hazelcast.core.HazelcastInstance;
import org.junit.Before;
import org.junit.Test;

import static ch.sourcepond.io.fssync.distributor.hazelcast.Activator.FACTORY_PID;
import static com.hazelcast.core.Hazelcast.newHazelcastInstance;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ActivatorTest extends BaseActivatorTest<HazelcastDistributor, Activator, Config> {
    private final SyncTarget compoundSyncTarget = mock(SyncTarget.class);

    @Before
    public void setup() throws Exception {
        when(configBuilderFactory.create(Config.class, props)).thenReturn(configBuilder);
        when(configBuilder.build()).thenReturn(config);
        when(compoundServiceFactory.create(context, executorService, SyncTarget.class)).thenReturn(compoundSyncTarget);
        config = mock(Config.class, inv -> inv.getMethod().getDefaultValue());
        when(config.existingInstanceName()).thenReturn(EXPECTED_UNIQUE_ID);
        activator = new Activator(configBuilderFactory, compoundServiceFactory, executorService);
        super.setup();
    }

    @Override
    protected Class<Config> getConfigAnnotation() {
        return Config.class;
    }

    @Test
    @Override
    public void verifyGetFactoryId() {
        assertEquals(FACTORY_PID, activator.getFactoryPid());
    }

    @Test
    @Override
    public void verifyService() throws Exception {
        final HazelcastInstance hci = newHazelcastInstance(new com.hazelcast.config.Config(EXPECTED_UNIQUE_ID));
        try {
            super.verifyService();
        } finally {
            hci.shutdown();
        }
    }
}
