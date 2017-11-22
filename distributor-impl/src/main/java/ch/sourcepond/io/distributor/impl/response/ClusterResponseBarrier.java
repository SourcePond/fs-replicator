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
package ch.sourcepond.io.distributor.impl.response;

import java.io.Serializable;
import java.util.concurrent.TimeoutException;

/**
 * A barrier which sends a message into the cluster and waits for responses of all active cluster members.
 *
 * @param <T> Type of the message to be distributed
 */
public interface ClusterResponseBarrier<T extends Serializable> {

    /**
     * Broadcasts the message specified into the cluster and blocks until all
     * members have sent an response to this barrier. Removing a cluster member while this method is blocking
     * is handled as a valid response. If one or more members produced a failure while processing the message, a
     * {@link ResponseException} is caused to be thrown. If one or more members did not send a response to this
     * barrier object within the defined timeout (see {@link ch.sourcepond.io.distributor.spi.TimeoutConfig}), a
     * {@link TimeoutException} is caused to be thrown.
     *
     * @param pMessage Message to be distributed to the cluster, must not be {@code null}
     * @throws TimeoutException Thrown, if one or more cluster members did not answer withing the defined timeout.
     * @throws ResponseException Thrown, if one or more cluster members produces failures while consuming the message.
     * @throws NullPointerException Thrown, if the message specified is {@code null}.
     */
    void awaitResponse(final T pMessage)  throws TimeoutException, ResponseException;
}
