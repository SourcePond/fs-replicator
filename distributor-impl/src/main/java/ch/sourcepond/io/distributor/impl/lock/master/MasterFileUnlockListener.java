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
import com.hazelcast.core.Member;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

class MasterFileUnlockListener extends AnswerValidatingMasterListener<FileUnlockException> {

    public MasterFileUnlockListener(final String pPath,
                                    final long pTimeout,
                                    final TimeUnit pUnit,
                                    final Collection<Member> pMembers) {
        super(pPath, pTimeout, pUnit, pMembers);
    }

    @Override
    protected void addValidationFailureMessage(final StringBuilder pBuilder) {
        pBuilder.append("Releasing file-lock failed on some node!");
    }

    @Override
    protected void throwValidationException(final String pMessage) throws FileUnlockException {
        throw new FileUnlockException(pMessage);
    }

    @Override
    protected void throwValidationException(final Exception pCause) throws FileUnlockException {
        throw new FileUnlockException(pCause);
    }
}
