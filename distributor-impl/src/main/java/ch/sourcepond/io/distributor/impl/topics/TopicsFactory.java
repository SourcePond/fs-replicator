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
package ch.sourcepond.io.distributor.impl.topics;

import com.hazelcast.core.HazelcastInstance;

import java.util.Map;

import static ch.sourcepond.io.distributor.impl.topics.Validations.mandatory;
import static com.hazelcast.core.Hazelcast.getHazelcastInstanceByName;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class TopicsFactory {
    static final String EXISTING_INSTANCE_NAME = "hazelcast.existing.instance.name";
    private final TopicConfigsFactory factory;

    public TopicsFactory(final TopicConfigsFactory pFactory) {
        factory = pFactory;
    }

    public Topics create(final Map<String, String> pInstantiationProperties) {
        final String name = mandatory(EXISTING_INSTANCE_NAME, pInstantiationProperties, Validations::same);
        final HazelcastInstance hci = requireNonNull(getHazelcastInstanceByName(name),
                format("No Hazelcast instance found with name %s, property %s must specify an existing instance", name, EXISTING_INSTANCE_NAME));
        return new Topics(hci, factory.create(pInstantiationProperties));
    }
}
