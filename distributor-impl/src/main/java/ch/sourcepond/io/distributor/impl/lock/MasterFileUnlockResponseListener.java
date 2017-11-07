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
import com.hazelcast.core.Message;

import java.util.Collection;
import java.util.Set;

class MasterFileUnlockResponseListener extends MasterResponseListener<String> {
    private final Collection<Member> members;

    public MasterFileUnlockResponseListener(final String pPath, final Collection<Member> pMembers) {
        super(pPath);
        members = pMembers;
    }

    @Override
    protected void memberRemoved(final Member pRemovedMember) {
        members.remove(pRemovedMember);
    }

    @Override
    protected boolean hasOpenAnswers() {
        return !members.isEmpty();
    }

    @Override
    protected String toPath(final String pMessage) {
        return pMessage;
    }

    @Override
    protected void processMessage(final Message<String> pMessage) {
        members.remove(pMessage.getPublishingMember());
    }
}
