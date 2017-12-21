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

import ch.sourcepond.osgi.cmpn.metatype.ConfigBuilderFactory;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

import java.util.Dictionary;

class TopicConfigManager implements ManagedServiceFactory {
    static final String FACTORY_PID = "ch.sourcepond.io.fssync.distributor.hazelcast.TopicConfig";
    private final ConfigManager observer;
    private final ConfigBuilderFactory configBuilderFactory;

    public TopicConfigManager(final ConfigManager pObserver, final ConfigBuilderFactory pConfigBuilderFactory) {
        observer = pObserver;
        configBuilderFactory = pConfigBuilderFactory;
    }

    @Override
    public String getName() {
        // TODO: Translate this
        return FACTORY_PID;
    }

    @Override
    public void updated(final String pPid, final Dictionary<String, ?> pProperties) throws ConfigurationException {
        // Validate configuration
        configBuilderFactory.create(TopicConfig.class, pProperties).build();
        observer.topicConfigUpdated(pPid);
    }

    @Override
    public void deleted(final String pPid) {
        observer.topicConfigDeleted(pPid);
    }
}
