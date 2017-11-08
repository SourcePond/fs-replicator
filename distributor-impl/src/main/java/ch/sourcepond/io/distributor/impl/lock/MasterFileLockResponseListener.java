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
package ch.sourcepond.io.distributor.impl.lock;

import com.hazelcast.core.Member;
import com.hazelcast.core.MembershipListener;
import com.hazelcast.core.Message;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyMap;

/**
 * Processes {@link LockMessage} objects which are send as response from the cluster-nodes when
 * they are requested to acquire a {@link java.nio.channels.FileLock} for a particular path.
 *
 */
class MasterFileLockResponseListener extends BaseMasterResponseListener<LockMessage> implements MembershipListener {
    private final Map<Member, Object> responses = new HashMap<>();

    MasterFileLockResponseListener(final String pPath,
                                   final long pTimeout,
                                   final TimeUnit pUnit,
                                   final Collection<Member> pMembers) {
        super(pPath, pTimeout, pUnit);
        for (final Member member : pMembers) {
            responses.put(member, null);
        }
    }

    @Override
    protected void memberRemoved(final Member pRemovedMember) {
        responses.remove(pRemovedMember);
    }

    @Override
    protected String toPath(final LockMessage pMessage) {
        return pMessage.getPath();
    }

    @Override
    protected boolean hasOpenAnswers() {
        if (responses.isEmpty()) {
            return false;
        }
        for (final Object e : responses.values()) {
            if (e == null) {
                return true;
            }
        }
        return false;
    }

    private Map<Member, IOException> collectMemberExceptions() {
        Map<Member, IOException> exceptions = null;
        for (final Map.Entry<Member, Object> e : responses.entrySet()) {
            final Object value = e.getValue();
            if (value instanceof IOException) {
                if (exceptions == null) {
                    exceptions = new HashMap<>();
                }
                exceptions.put(e.getKey(), (IOException) value);
            }
        }
        return exceptions == null ? emptyMap() : exceptions;
    }

    @Override
    protected void validateAnswers() throws FileLockException {
        super.validateAnswers();
        final Map<Member, IOException> memberExceptions = collectMemberExceptions();
        if (!memberExceptions.isEmpty()) {
            final StringBuilder builder = new StringBuilder("Acquiring file-locks failed! Failures:\n\t");
            for (final Map.Entry<Member, IOException> entry : memberExceptions.entrySet()) {
                builder.append(entry.getKey()).append(": ").append(entry.getValue().getMessage()).append("\n\t");
            }
            builder.append("See logs on members for further information.");
            throw new FileLockException(builder.toString());
        }
    }

    @Override
    protected void processMessage(final Message<LockMessage> pMessage) {
        final LockMessage message = pMessage.getMessageObject();
        final IOException failure = message.getFailureOrNull();
        responses.replace(pMessage.getPublishingMember(), failure == null ? TRUE : failure);
    }
}
