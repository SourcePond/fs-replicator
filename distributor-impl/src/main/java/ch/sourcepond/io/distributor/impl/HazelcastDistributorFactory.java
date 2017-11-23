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

import ch.sourcepond.io.distributor.api.CreationException;
import ch.sourcepond.io.distributor.api.Distributor;
import ch.sourcepond.io.distributor.api.DistributorFactory;
import ch.sourcepond.io.distributor.spi.Receiver;

import java.util.Map;

import static com.google.inject.Guice.createInjector;
import static java.lang.String.format;

public class HazelcastDistributorFactory implements DistributorFactory {

    @Override
    public Distributor create(final Receiver pReceiver, final Map<String, String> pInstantiationProperties) throws CreationException {
        try {
            return createInjector(new HazelcastDistributorModule(pReceiver, pInstantiationProperties)).
                    getInstance(HazelcastDistributor.class);
        } catch (final Exception e) {
            throw new CreationException(format("Instance could not be create with properties %s", pInstantiationProperties), e);
        }
    }
}
