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
package ch.sourcepond.io.distributor.api;

import ch.sourcepond.io.distributor.spi.Receiver;
import ch.sourcepond.io.distributor.spi.TimeoutConfig;

import java.util.Map;

/**
 * Factory to create a {@link Distributor} instance. This is the entry-point to the distributor sub-system. An instance
 * of this factory should be obtainable as service.
 */
public interface DistributorFactory {

    /**
     * Creates a new {@link Distributor} instance. The key/value pairs of the instantiation properties specified are
     * implementation depending and therefore not specified in this API.
     *
     * @param pReceiver
     * @param pTimeoutConfig Mutable binding which manages timeouts/time-units, must not be {@code null}
     * @param pInstantiationProperties Properties necessary to instantiate the distributor, must not be {@code null}
     * @return New distributor instance, never {@code null}
     * @throws NullPointerException Thrown, if either parameter is {@code null}.
     */
    Distributor create(Receiver pReceiver, TimeoutConfig pTimeoutConfig, Map<String, String> pInstantiationProperties);
}
