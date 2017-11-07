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

import java.util.Collection;

/**
 * Factory for creating {@link MasterResponseListener} instances.
 */
class MasterResponseListenerFactory {

    /**
     * Creates a new instance of {@link MasterFileLockResponseListener}.
     *
     * @param pPath Path to be locked, must not be {@code null}
     * @param pMembers Currently active members of the cluster, must not be {@code null}
     * @return New instance, never {@code null}
     */
    MasterFileLockResponseListener createLockListener(final String pPath, final Collection<Member> pMembers) {
        assert pPath != null : "pPath is null";
        assert pMembers != null : "pMembers is null";
        return new MasterFileLockResponseListener(pPath, pMembers);
    }

    /**
     * Creates a new instance of {@link MasterFileUnlockResponseListener}.
     *
     * @param pPath Path to be unlocked, must not be {@code null}
     * @param pMembers Currently active members of the cluster, must not be {@code null}
     * @return New instance, never {@code null}
     */
    MasterFileUnlockResponseListener createUnlockListener(final String pPath, final Collection<Member> pMembers) {
        assert pPath != null : "pPath is null";
        assert pMembers != null : "pMembers is null";
        return new MasterFileUnlockResponseListener(pPath,pMembers);
    }
}
