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

import org.junit.Test;

import static ch.sourcepond.io.distributor.impl.lock.master.BaseMasterResponseListenerTest.EXPECTED_PATH;
import static ch.sourcepond.io.distributor.impl.lock.master.BaseMasterResponseListenerTest.EXPECTED_TIMOUT;
import static ch.sourcepond.io.distributor.impl.lock.master.BaseMasterResponseListenerTest.EXPECTED_UNIT;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class MasterFileUnlockResponseListenerTest extends BaseMasterResponseListenerTest<String> {

    @Override
    protected BaseMasterResponseListener createListener() {
        return new MasterFileUnlockResponseListener(EXPECTED_PATH, EXPECTED_TIMOUT, EXPECTED_UNIT, members);
    }

    @Test
    @Override
    public void verifyHasOpenAnswersMemberRemoved() {
        assertTrue(listener.hasOpenAnswers());
        listener.memberRemoved(member);
        assertFalse(listener.hasOpenAnswers());
    }

    @Test
    @Override
    public void verifyHasOpenAnswers() {
        assertTrue(listener.hasOpenAnswers());
        listener.onMessage(message);
        assertFalse(listener.hasOpenAnswers());
    }
}
