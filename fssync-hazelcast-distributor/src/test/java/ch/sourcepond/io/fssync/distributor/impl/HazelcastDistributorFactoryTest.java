package ch.sourcepond.io.fssync.distributor.impl;

import ch.sourcepond.io.fssync.distributor.api.Distributor;
import ch.sourcepond.io.fssync.distributor.spi.Receiver;
import ch.sourcepond.io.fssync.distributor.impl.binding.BindingModule;
import com.hazelcast.config.Config;
import com.hazelcast.core.HazelcastInstance;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.hazelcast.core.Hazelcast.newHazelcastInstance;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.mockito.Mockito.mock;

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
public class HazelcastDistributorFactoryTest {
    private static final String TEST_INSTANCE_NAME = UUID.randomUUID().toString();
    private static final String TIMEOUT = "10";
    private final Receiver receiver = mock(Receiver.class);
    private final Map<String, String> instantiationProperties = new HashMap<>();
    private final HazelcastDistributorFactory factory = new HazelcastDistributorFactory();
    private HazelcastInstance instance;

    @Before
    public void setup() {
        instance = newHazelcastInstance(new Config().setInstanceName(TEST_INSTANCE_NAME));
        instantiationProperties.put(BindingModule.EXISTING_INSTANCE_NAME, TEST_INSTANCE_NAME);
        instantiationProperties.put(BindingModule.LOCK_TIMEOUT_UNIT, SECONDS.name());
        instantiationProperties.put(BindingModule.LOCK_TIMEOUT, TIMEOUT);
        instantiationProperties.put(BindingModule.RESPONSE_TIMEOUT_UNIT, SECONDS.name());
        instantiationProperties.put(BindingModule.RESPONSE_TIMEOUT, TIMEOUT);
    }

    @After
    public void tearDown() {
        instance.shutdown();
    }

    @Test
    public void create() throws Exception {
        final Distributor i1 = factory.create(receiver, instantiationProperties);
        assertNotNull(i1);
        final Distributor i2 = factory.create(receiver, instantiationProperties);
        assertNotNull(i2);
        assertNotSame(i1, i2);
    }
}
