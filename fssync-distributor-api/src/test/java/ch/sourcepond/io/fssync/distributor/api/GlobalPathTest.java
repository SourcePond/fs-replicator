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
package ch.sourcepond.io.fssync.distributor.api;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class GlobalPathTest {
    private static final String EXPECTED_SENDING_NODE = "expectedNode";
    private static final String EXPECTED_PATH = "expectedPath";
    private final GlobalPath path = new GlobalPath(EXPECTED_SENDING_NODE, EXPECTED_PATH);

    @Test
    public void hashCodeShouldBeConsistent() {
        final int hashCode = path.hashCode();
        for (int i = 0 ; i < 10000000 ; i++) {
            assertEquals(hashCode, path.hashCode());
        }
    }

    @Test
    public void hashCodeMustBeEqualOnEqualPaths() {
        assertEquals(path.hashCode(), new GlobalPath(EXPECTED_SENDING_NODE, EXPECTED_PATH).hashCode());
    }

    @Test
    public void hashCodeShouldNotBeEqualOnNonEqualPaths() {
        assertNotEquals(path.hashCode(), new GlobalPath("otherNode", EXPECTED_PATH));
        assertNotEquals(path.hashCode(), new GlobalPath(EXPECTED_SENDING_NODE, "otherPath"));
        assertNotEquals(path.hashCode(), new GlobalPath("otherNode", "otherPath"));
    }

    @Test
    public void verifyEquals() {
        assertTrue(path.equals(path));
        assertTrue(path.equals(new GlobalPath(EXPECTED_SENDING_NODE, EXPECTED_PATH)));
    }

    @Test
    public void verifyNotEquals() {
        assertFalse(path.equals(null));
        assertFalse(path.equals("SomeDifferentObject"));
        assertFalse(path.equals(new GlobalPath("otherNode", EXPECTED_PATH)));
        assertFalse(path.equals(new GlobalPath(EXPECTED_SENDING_NODE, "otherPath")));
        assertFalse(path.equals(new GlobalPath("otherNode", "otherPath")));
    }

    @Test
    public void getSendingNode() {
        assertEquals(EXPECTED_SENDING_NODE, path.getSendingNode());
    }

    @Test
    public void getPath() {
        assertEquals(EXPECTED_PATH, path.getPath());
    }

    @Test
    public void verifyToString() {
        assertEquals("expectedNode://expectedPath", path.toString());
    }
}
