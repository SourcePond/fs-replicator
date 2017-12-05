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
package ch.sourcepond.io.fssync.target.api;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class NodeInfoTest {
    private static final String EXPECTED_SENDER = "expectedSenderNode";
    private static final String EXPECTED_LOCAL = "expectedLocalNode";
    private final NodeInfo nodeInfo = new NodeInfo(EXPECTED_SENDER, EXPECTED_LOCAL);

    @Test
    public void hashCodeShouldBeConsistent() {
        final int hashCode = nodeInfo.hashCode();
        for (int i = 0; i < 10000000; i++) {
            assertEquals(hashCode, nodeInfo.hashCode());
        }
    }

    @Test
    public void hashCodeMustBeEqualOnEqualnodeInfos() {
        assertEquals(nodeInfo.hashCode(), new NodeInfo(EXPECTED_SENDER, EXPECTED_LOCAL).hashCode());
    }

    @Test
    public void hashCodeShouldNotBeEqualOnNonEqualnodeInfos() {
        assertNotEquals(nodeInfo.hashCode(), new NodeInfo("otherNode", EXPECTED_LOCAL));
        assertNotEquals(nodeInfo.hashCode(), new NodeInfo(EXPECTED_SENDER, "othernodeInfo"));
        assertNotEquals(nodeInfo.hashCode(), new NodeInfo("otherNode", "othernodeInfo"));
    }

    @Test
    public void verifyEquals() {
        assertTrue(nodeInfo.equals(nodeInfo));
        assertTrue(nodeInfo.equals(new NodeInfo(EXPECTED_SENDER, EXPECTED_LOCAL)));
    }

    @Test
    public void verifyNotEquals() {
        assertFalse(nodeInfo.equals(null));
        assertFalse(nodeInfo.equals("SomeDifferentObject"));
        assertFalse(nodeInfo.equals(new NodeInfo("otherNode", EXPECTED_LOCAL)));
        assertFalse(nodeInfo.equals(new NodeInfo(EXPECTED_SENDER, "othernodeInfo")));
        assertFalse(nodeInfo.equals(new NodeInfo("otherNode", "othernodeInfo")));
    }

    @Test
    public void getSender() {
        assertEquals(EXPECTED_SENDER, nodeInfo.getSender());
    }

    @Test
    public void getLocal() {
        assertEquals(EXPECTED_LOCAL, nodeInfo.getLocal());
    }

    @Test
    public void verifyToString() {
        assertEquals("[sender:expectedSenderNode, local:expectedLocalNode]", nodeInfo.toString());
    }
}
