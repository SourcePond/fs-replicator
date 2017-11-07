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
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

public class MasterResponseListenerFactoryTest {
    private static final String ANY_PATH = "anyPath";
    private final Collection<Member> members = new ArrayList<>();
    private final MasterResponseListenerFactory factory = new MasterResponseListenerFactory();

    private void verifyInstances(final MasterResponseListener<?> l1, final MasterResponseListener<?> l2) {
        assertNotNull(l1);
        assertNotNull(l2);
        assertSame(l1.getClass(), l2.getClass());
        assertNotSame(l1, l2);
    }

    @Test(expected = AssertionError.class)
    public void createLockListenerPathIsNull() {
        factory.createLockListener(null, members);
    }

    @Test(expected = AssertionError.class)
    public void createLockListenerMembersIsNull() {
        factory.createLockListener(ANY_PATH, null);
    }

    @Test(expected = AssertionError.class)
    public void createUnlockListenerPathIsNull() {
        factory.createUnlockListener(null, members);
    }

    @Test(expected = AssertionError.class)
    public void createUnlockListenerMembersIsNull() {
        factory.createUnlockListener(ANY_PATH, null);
    }

    @Test
    public void createLockListener() {
         verifyInstances(factory.createLockListener(ANY_PATH, members), factory.createLockListener(ANY_PATH, members));
    }

    @Test
    public void createUnlockListener() {
        verifyInstances(factory.createUnlockListener(ANY_PATH, members), factory.createUnlockListener(ANY_PATH, members));
    }
}
