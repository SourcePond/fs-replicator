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
package ch.sourcepond.io.distributor.impl.binding;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static ch.sourcepond.io.distributor.impl.binding.Validations.mandatory;

class TimeoutConfigFactory {
    static final String LOCK_TIMEOUT_UNIT = "hazelcast.lock.timeoutUnit";
    static final String LOCK_TIMEOUT = "hazelcast.lock.timeout";
    static final String RESPONSE_TIMEOUT_UNIT = "hazelcast.response.timeoutUnit";
    static final String RESPONSE_TIMEOUT = "hazelcast.response.timeout";

    private static TimeoutConfig create(final String pUnitKey, final String pTimeoutKey, final Map<String, String> pInstantiationProperties) {
        return new TimeoutConfig(mandatory(pUnitKey, pInstantiationProperties, TimeUnit::valueOf),
                mandatory(pTimeoutKey, pInstantiationProperties, Long::valueOf));
    }

    public TimeoutConfig createLockConfig(final Map<String, String> pInstantiationProperties) {
        return create(LOCK_TIMEOUT_UNIT, LOCK_TIMEOUT, pInstantiationProperties);
    }

    public TimeoutConfig createResponseConfig(final Map<String, String> pInstantiationProperties) {
        return create(RESPONSE_TIMEOUT_UNIT, RESPONSE_TIMEOUT, pInstantiationProperties);
    }
}
