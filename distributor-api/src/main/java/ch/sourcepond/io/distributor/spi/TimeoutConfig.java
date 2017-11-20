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
package ch.sourcepond.io.distributor.spi;

import ch.sourcepond.io.distributor.api.Distributor;

import java.util.concurrent.TimeUnit;

/**
 * TimeoutConfig object which provides all necessary timeouts specifications.
 */
public interface TimeoutConfig {

    /**
     * Returns the timeout-unit to be used when a lock is being acquired
     * (see {@link Distributor#lock(String)}).
     *
     * @return Timeout-unit, never {@code null}
     */
    TimeUnit getLockTimeoutUnit();

    /**
     * Returns the timeout to be used when a lock is being acquired
     * (see {@link Distributor#lock(String)}).
     *
     * @return Zero or positive long
     */
    long getLockTimeout();

    /**
     * Returns the timeout-unit to be used when a lock is being released
     * (see {@link Distributor#unlock(String)}).
     *
     * @return Timeout-unit, never {@code null}
     */
    TimeUnit getUnlockTimeoutUnit();

    /**
     * Returns the timeout to be used when a lock is being released
     * (see {@link Distributor#unlock(String)}).
     *
     * @return Timeout, zero or positive long
     */
    long getUnlockTimeout();

    /**
     * Returns the timeout-unit to be used when waiting for a response from the cluster.
     *
     * @return Timeout-unit, never {@code null}
     */
    TimeUnit getResponseTimeoutUnit();

    /**
     * Returns the timeout to be used when when waiting for a response from the cluster.
     *
     * @return Timeout, zero or positive long
     */
    long getResponseTimeout();
}
