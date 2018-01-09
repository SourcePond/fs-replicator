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
package ch.sourcepond.io.fssync.common.api;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class SyncPathTest {
    private static final String EXPECTED_SEPARATOR = "/";
    private static final String EXPECTED_SYNC_DIR = "/expectedSyncDir";
    private static final String EXPECTED_PATH = "expectedPath";
    private final SyncPath path = new SyncPath(EXPECTED_SEPARATOR, EXPECTED_SYNC_DIR, EXPECTED_PATH);

    @Test(expected = NullPointerException.class)
    public void separatorIsNull() {
        new SyncPath(null, EXPECTED_SYNC_DIR, EXPECTED_PATH);
    }

    @Test(expected = IllegalArgumentException.class)
    public void separatorIsEmpty() {
        new SyncPath("", EXPECTED_SYNC_DIR, EXPECTED_PATH);
    }

    @Test(expected = NullPointerException.class)
    public void syncDirIsNull() {
        new SyncPath(EXPECTED_SEPARATOR, null, EXPECTED_PATH);
    }

    @Test(expected = IllegalArgumentException.class)
    public void syncDirIsNotAbsolute() {
        new SyncPath(EXPECTED_SEPARATOR, EXPECTED_SYNC_DIR.substring(1), EXPECTED_PATH);
    }

    @Test(expected = IllegalArgumentException.class)
    public void pathIsEmpty() {
        new SyncPath(EXPECTED_SEPARATOR, EXPECTED_SYNC_DIR, "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void pathIsNotRelative() {
        new SyncPath(EXPECTED_SEPARATOR, EXPECTED_SYNC_DIR, EXPECTED_SEPARATOR + EXPECTED_PATH);
    }

    @Test
    public void hashCodeShouldBeConsistent() {
        final int hashCode = path.hashCode();
        for (int i = 0; i < 10000000; i++) {
            assertEquals(hashCode, path.hashCode());
        }
    }

    @Test
    public void hashCodeMustBeEqualOnEqualPaths() {
        assertEquals(path.hashCode(), new SyncPath(EXPECTED_SEPARATOR, EXPECTED_SYNC_DIR, EXPECTED_PATH).hashCode());
    }

    @Test
    public void hashCodeShouldNotBeEqualOnNonEqualPaths() {
        assertNotEquals(path.hashCode(), new SyncPath("//", "/" + EXPECTED_SYNC_DIR, EXPECTED_PATH));
        assertNotEquals(path.hashCode(), new SyncPath(EXPECTED_SEPARATOR, "/otherNode", EXPECTED_PATH));
        assertNotEquals(path.hashCode(), new SyncPath(EXPECTED_SEPARATOR, EXPECTED_SYNC_DIR, "otherPath"));
        assertNotEquals(path.hashCode(), new SyncPath(EXPECTED_SEPARATOR, "/otherNode", "otherPath"));
    }

    @Test
    public void verifyEquals() {
        assertTrue(path.equals(path));
        assertTrue(path.equals(new SyncPath(EXPECTED_SEPARATOR, EXPECTED_SYNC_DIR, EXPECTED_PATH)));
    }

    @Test
    public void verifyNotEquals() {
        assertFalse(path.equals(null));
        assertFalse(path.equals("SomeDifferentObject"));
        assertFalse(path.equals(new SyncPath("//", "/" + EXPECTED_SYNC_DIR, EXPECTED_PATH)));
        assertFalse(path.equals(new SyncPath(EXPECTED_SEPARATOR, "/otherNode", EXPECTED_PATH)));
        assertFalse(path.equals(new SyncPath(EXPECTED_SEPARATOR, EXPECTED_SYNC_DIR, "otherPath")));
        assertFalse(path.equals(new SyncPath(EXPECTED_SEPARATOR, "/otherNode", "otherPath")));
    }

    @Test
    public void getSeparator() {
        assertEquals(EXPECTED_SEPARATOR, path.getSeparator());
    }

    @Test
    public void getSyncDir() {
        assertEquals(EXPECTED_SYNC_DIR, path.getSyncDir());
    }

    @Test
    public void getPath() {
        assertEquals(EXPECTED_PATH, path.getRelativePath());
    }

    @Test
    public void toAbsolutePath() {
        assertEquals("/expectedSyncDir/expectedPath", path.toAbsolutePath());
    }

    @Test
    public void verifyToString() {
        assertEquals("[/expectedSyncDir]/expectedPath", path.toString());
    }

    @Test
    public void serializeDeserialize() throws Exception {
        final ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        final ObjectOutputStream out = new ObjectOutputStream(bOut);
        out.writeObject(path);

        final ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bOut.toByteArray()));
        assertEquals(path, in.readObject());
    }
}
