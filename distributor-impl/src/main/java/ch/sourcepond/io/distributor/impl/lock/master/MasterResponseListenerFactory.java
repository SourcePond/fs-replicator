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

import ch.sourcepond.io.distributor.impl.common.master.AnswerValidatingMasterListener;
import ch.sourcepond.io.distributor.impl.common.master.ExceptionFactory;
import ch.sourcepond.io.distributor.impl.common.master.MasterListener;
import ch.sourcepond.io.distributor.impl.common.master.MasterResponseListener;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Member;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Factory for creating {@link MasterListener} instances.
 */
class MasterResponseListenerFactory {
    static final long DEFAULT_TIMEOUT = 30;
    static final TimeUnit DEFAULT_UNIT = SECONDS;
    private ITopic<String> sendFileLockRequestTopic;
    private ITopic<String> sendFileUnlockRequstTopic;

    public ITopic<String> getSendFileLockRequestTopic() {
        return sendFileLockRequestTopic;
    }

    public void setSendFileLockRequestTopic(ITopic<String> sendFileLockRequestTopic) {
        this.sendFileLockRequestTopic = sendFileLockRequestTopic;
    }

    public ITopic<String> getSendFileUnlockRequestTopic() {
        return sendFileUnlockRequstTopic;
    }

    public void setSendFileUnlockRequstTopic(ITopic<String> sendFileUnlockRequstTopic) {
        this.sendFileUnlockRequstTopic = sendFileUnlockRequstTopic;
    }

    private <E extends Exception> MasterResponseListener<E> createListener(final String pPath,
                                                                           final Collection<Member> pMembers,
                                                                           final ExceptionFactory<E> pExceptionFactory) {
        return new AnswerValidatingMasterListener<E>(pExceptionFactory,
                requireNonNull(pPath, "Path is null"),
                DEFAULT_TIMEOUT,
                DEFAULT_UNIT,
                requireNonNull(pMembers, "Members are null"));
    }

    /**
     * Creates a new response listener.
     *
     * @param pPath Path to be locked, must not be {@code null}
     * @return New instance, never {@code null}
     */
    public MasterResponseListener<FileLockException> createLockListener(final String pPath, final Collection<Member> pMembers) {
        return createListener(pPath, pMembers, message ->
                new FileLockException(message.insert(0, "Acquiring file-lock failed on some nodes!").toString()));
    }

    /**
     * Creates a new response listener.
     *
     * @param pPath Path to be unlocked, must not be {@code null}
     * @return New instance, never {@code null}
     */
    public MasterResponseListener<FileUnlockException> createUnlockListener(final String pPath, final Collection<Member> pMembers) {
        return createListener(pPath, pMembers, message ->
                new FileUnlockException(message.insert(0, "Releasing file-lock failed on some nodes!").toString()));
    }
}
