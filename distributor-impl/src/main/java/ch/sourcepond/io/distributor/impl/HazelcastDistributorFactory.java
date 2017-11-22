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
package ch.sourcepond.io.distributor.impl;

import ch.sourcepond.io.distributor.api.Distributor;
import ch.sourcepond.io.distributor.api.DistributorFactory;
import ch.sourcepond.io.distributor.spi.Receiver;
import ch.sourcepond.io.distributor.spi.TimeoutConfig;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import java.util.Map;
import java.util.Objects;

import static com.hazelcast.core.Hazelcast.getHazelcastInstanceByName;
import static com.hazelcast.core.Hazelcast.getOrCreateHazelcastInstance;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class HazelcastDistributorFactory implements DistributorFactory {
    public static final String HAZELCAST_INSTANCE_NAME = "hazelcast.instance.name";
    public static final String HAZELCAST_NEW_INSTANCE_CONFIG = "hazelcast.new.instance.config";

    private static <T> T validateType(final Object pValue, final Class<?> pExpectedType) {
        if (!pExpectedType.equals(pValue.getClass())) {
            throw new IllegalArgumentException(format("Value of property with key %s must be of type %s",
                    HAZELCAST_NEW_INSTANCE_CONFIG, Config.class.getName()));
        }
        return (T) pValue;
    }

    @Override
    public Distributor create(final Receiver pReceiver, final TimeoutConfig pTimeoutConfig, final Map<String, Object> pInstantiationProperties) {
        final Object nameObject = requireNonNull(pInstantiationProperties.get(HAZELCAST_INSTANCE_NAME),
                format("No property set with key %s", HAZELCAST_INSTANCE_NAME));
        final String name = validateType(nameObject, String.class);
        final Object configObject = pInstantiationProperties.get(HAZELCAST_NEW_INSTANCE_CONFIG);
        final HazelcastInstance hci;
        if (configObject != null) {
            hci = getOrCreateHazelcastInstance(validateType(configObject, Config.class));
        } else {
            hci = requireNonNull(getHazelcastInstanceByName(name),
                    format("No Hazelcast instance found with name %s", name));
        }



        return null;
    }
}
