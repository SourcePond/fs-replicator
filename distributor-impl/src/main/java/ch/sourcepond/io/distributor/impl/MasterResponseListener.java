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

import ch.sourcepond.io.distributor.impl.lock.master.FileLockException;
import com.hazelcast.core.MembershipListener;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

import java.util.concurrent.TimeoutException;

/**
 * Instances of this interface are responsible for receiving messages from a particular
 * topic (see {@link MessageListener} and {@link com.hazelcast.core.ITopic}. Additionally,
 * they are added to the cluster as {@link MembershipListener}, see {@link com.hazelcast.core.Cluster#addMembershipListener(MembershipListener)}
 *
 * @param <E> Type of the validation exception
 */
public interface MasterResponseListener<E extends Exception> extends MessageListener<StatusResponseMessage>, MembershipListener {

    /**
     * This method blocks until all nodes have responded (success or failure)
     * on a particular request.
     *
     * @throws TimeoutException
     * @throws E
     */
    void awaitNodeAnswers() throws TimeoutException, E;
}
