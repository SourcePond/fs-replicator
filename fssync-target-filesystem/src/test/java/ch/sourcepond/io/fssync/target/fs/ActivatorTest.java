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

import ch.sourcepond.io.fssync.compound.BaseActivatorTest;
import org.junit.Before;
import org.junit.Test;

import static ch.sourcepond.io.fssync.target.fs.Activator.FACTORY_PID;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ActivatorTest extends BaseActivatorTest<TargetDirectory, Activator, Config> {
    private final TargetDirectoryFactory targetDirectoryFactory = mock(TargetDirectoryFactory.class);
    private final TargetDirectory syncTarget = mock(TargetDirectory.class);

    @Before
    public void setup() throws Exception {
        when(targetDirectoryFactory.create()).thenReturn(syncTarget);
        config = mock(Config.class, inv -> inv.getMethod().getDefaultValue());
        when(configBuilderFactory.create(Config.class, props)).thenReturn(configBuilder);
        when(configBuilder.build()).thenReturn(config);
        when(config.syncDir()).thenReturn(EXPECTED_UNIQUE_ID);
        activator = new Activator(configBuilderFactory, targetDirectoryFactory);
        super.setup();
    }

    @Test
    public void verifyDefaultConstructor() {
        new Activator();
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
}
