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
package ch.sourcepond.io.fssync.distributor.hazelcast.config;

import ch.sourcepond.osgi.cmpn.metatype.ConfigBuilder;
import ch.sourcepond.osgi.cmpn.metatype.ConfigBuilderFactory;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;

import java.util.Dictionary;
import java.util.Hashtable;

import static ch.sourcepond.io.fssync.distributor.hazelcast.config.DistributorTopicConfigManager.FACTORY_PID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class TopicConfigManagerTest {
    private static final String EXPECTED_PID = "expectedPid";
    private final DistributorConfigManager observer = mock(DistributorConfigManager.class);
    private final ConfigBuilderFactory configBuilderFactory = mock(ConfigBuilderFactory.class);
    private final ConfigBuilder<TopicConfig> builder = mock(ConfigBuilder.class);
    private final TopicConfig config = mock(TopicConfig.class);
    private final Dictionary<String, ?> properties = new Hashtable<>();
    private final DistributorTopicConfigManager manager = new DistributorTopicConfigManager(observer, configBuilderFactory);

    @Before
    public void setup() throws Exception {
        when(configBuilderFactory.create(TopicConfig.class, properties)).thenReturn(builder);
        when(builder.build()).thenReturn(config);
    }

    @Test
    public void getName() {
        assertEquals(FACTORY_PID, manager.getName());
    }

    @Test
    public void updated() throws Exception {
        manager.updated(EXPECTED_PID, properties);
        verify(observer).topicConfigUpdated(EXPECTED_PID);
    }

    @Test
    public void updatedValidationFailed() throws Exception {
        doThrow(ConfigurationException.class).when(builder).build();
        try {
            manager.updated(EXPECTED_PID, properties);
            fail("Exception expected");
        } catch (final ConfigurationException e) {
            // expected
        }
        verifyZeroInteractions(observer);
    }

    @Test
    public void deleted() {
        manager.deleted(EXPECTED_PID);
        verify(observer).topicConfigDeleted(EXPECTED_PID);
    }
}
