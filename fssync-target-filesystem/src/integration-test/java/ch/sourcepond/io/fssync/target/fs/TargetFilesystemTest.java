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
package ch.sourcepond.io.fssync.target.fs;

import ch.sourcepond.io.fssync.common.api.SyncPath;
import ch.sourcepond.io.fssync.target.api.NodeInfo;
import ch.sourcepond.io.fssync.target.api.SyncTarget;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static java.lang.System.getProperty;
import static java.nio.file.FileSystems.getDefault;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

@Ignore
@RunWith(PaxExam.class)
public class TargetFilesystemTest {
    private static final String EXPECTED_LOCAL_NODE = "expectedLocalNode";
    private static final String EXPECTED_REMOTE_NODE = "expectedRemoteNode";

    @Inject
    private SyncTarget defaultSyncTarget;

    @Configuration
    public Option[] configure() {
        return new Option[]{
                junitBundles(),
                mavenBundle("ch.sourcepond.osgi.cmpn", "metatype-builder-lib").version("0.1-SNAPSHOT"),
                mavenBundle("ch.sourcepond.io.fssync", "fssync-common-api").version("0.1-SNAPSHOT"),
                mavenBundle("ch.sourcepond.io.fssync", "fssync-common-lib").version("0.1-SNAPSHOT"),
                mavenBundle("ch.sourcepond.io.fssync", "fssync-target-api").version("0.1-SNAPSHOT"),
                mavenBundle("ch.sourcepond.io.fssync", "fssync-target-filesystem").version("0.1-SNAPSHOT"),
                mavenBundle("org.apache.felix", "org.apache.felix.metatype").version("1.1.6"),
                mavenBundle("org.apache.felix", "org.apache.felix.configadmin").version("1.8.16"),
                mavenBundle("commons-io", "commons-io").version("2.6")
        };
    }

    @Before
    public void setup() throws Exception {

    }

    @After
    public void tearDown() throws Exception {
        deleteDirectory(getDefault().getPath(getProperty("user.dir"), "temp_testdata").toFile());

    }

    @Test
    public void verifyDefaultSync() throws IOException {
        final NodeInfo ni = new NodeInfo(EXPECTED_REMOTE_NODE, EXPECTED_LOCAL_NODE);
        final SyncPath syncPath = new SyncPath(File.separator, getProperty("user.dir"), "temp_testdata/examfile.txt");
        defaultSyncTarget.lock(ni, syncPath);
        assertTrue(Files.exists(getDefault().getPath(syncPath.getSyncDir(), syncPath.getRelativePath())));
    }

    @Test
    public void registerAndUnregisterSyncTargets() throws Exception {

    }
}
