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
package ch.sourcepond.io.distributor.impl.lock.master;

import ch.sourcepond.io.distributor.impl.AnswerValidatingMasterListener;
import ch.sourcepond.io.distributor.impl.StatusResponseMessage;
import com.hazelcast.core.Member;
import com.hazelcast.core.MembershipListener;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Processes {@link StatusResponseMessage} objects which are send as response from the cluster-nodes when
 * they are requested to acquire a {@link java.nio.channels.FileLock} for a particular path.
 *
 */
class MasterFileLockListener extends AnswerValidatingMasterListener<FileLockException> implements MembershipListener {

    public MasterFileLockListener(final String pPath,
                                  final long pTimeout,
                                  final TimeUnit pUnit,
                                  final Collection<Member> pMembers) {
        super(pPath, pTimeout, pUnit, pMembers);
    }

    @Override
    protected void addValidationFailureMessage(final StringBuilder pBuilder) {
        pBuilder.append("Acquiring file-locks failed!");
    }

    @Override
    protected void throwValidationException(final String pMessage) throws FileLockException {
        throw new FileLockException(pMessage);
    }

    @Override
    protected void throwValidationException(final Exception pCause) throws FileLockException {
        throw new FileLockException(pCause);
    }
}
