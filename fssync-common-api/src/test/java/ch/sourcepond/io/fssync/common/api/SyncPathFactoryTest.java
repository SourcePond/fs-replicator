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

import org.junit.Before;
import org.junit.Test;

import java.nio.file.FileSystem;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

public class SyncPathFactoryTest {
    private static final String EXPECTED_SEPARATOR = "/";
    private static final String EXPECTED_SYNC_PATH = "/expectedSyncPath";
    private static final String EXPECTED_RELATIVE_PATH = "expectedRelativePath";
    private final FileSystem fs = mock(FileSystem.class);
    private final Path syncDir = mock(Path.class, withSettings().name(EXPECTED_SYNC_PATH));
    private final Path absolutePath = mock(Path.class);
    private final Path relativePath = mock(Path.class, withSettings().name(EXPECTED_RELATIVE_PATH));
    private final SyncPathFactory syncPathFactory = new SyncPathFactory();

    @Before
    public void setup() {
        when(syncDir.getFileSystem()).thenReturn(fs);
        when(fs.getSeparator()).thenReturn(EXPECTED_SEPARATOR);
    }

    @Test(expected = IllegalArgumentException.class)
    public void notAParentDirectory() {
        syncPathFactory.create(syncDir, absolutePath);
    }

    @Test
    public void create() {
        when(absolutePath.startsWith(syncDir)).thenReturn(true);
        when(syncDir.relativize(absolutePath)).thenReturn(relativePath);
        final SyncPath path = syncPathFactory.create(syncDir, absolutePath);
        assertNotNull(path);
        assertEquals(EXPECTED_SYNC_PATH, path.getSyncDir());
        assertEquals(EXPECTED_RELATIVE_PATH, path.getRelativePath());
    }
}
