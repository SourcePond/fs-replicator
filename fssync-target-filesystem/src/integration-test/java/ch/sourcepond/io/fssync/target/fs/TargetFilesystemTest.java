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

import ch.sourcepond.io.fssync.target.api.NodeInfo;
import ch.sourcepond.io.fssync.common.api.SyncPath;
import ch.sourcepond.io.fssync.target.api.SyncTarget;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Hashtable;

import static ch.sourcepond.io.fssync.target.fs.Activator.FACTORY_PID;
import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.lang.Thread.sleep;
import static java.nio.file.FileSystems.getDefault;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.walkFileTree;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.osgi.framework.Constants.SERVICE_PID;

@RunWith(PaxExam.class)
public class TargetFilesystemTest {
    private static final String EXPECTED_LOCAL_NODE = "expectedLocalNode";
    private static final String EXPECTED_REMOTE_NODE = "expectedRemoteNode";

    @Inject
    private SyncTarget defaultSyncTarget;

    @Inject
    private ConfigurationAdmin configAdmin;

    @Inject
    private BundleContext context;

    private TestCustomizer customizer;
    private ServiceTracker<SyncTarget, SyncTarget> tracker;

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
                mavenBundle("org.apache.felix", "org.apache.felix.configadmin").version("1.8.16")
        };
    }

    @Before
    public void setup() throws Exception {
        customizer = new TestCustomizer(context);
        tracker = new ServiceTracker<SyncTarget, SyncTarget>(context,
                context.createFilter(format("(&(%s=%s)(!(%s=%s)))", OBJECTCLASS, SyncTarget.class.getName(), SERVICE_PID, FACTORY_PID)),
                customizer);
        tracker.open();
    }

    @After
    public void tearDown() throws Exception {
        final Path path = getDefault().getPath(getProperty("user.dir"), "temp_testdata");
        if (Files.exists(path)) {
            walkFileTree(path, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                    delete(file);
                    return CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
                    delete(dir);
                    return CONTINUE;
                }
            });
        }
        tracker.close();
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
        org.osgi.service.cm.Configuration config = configAdmin.createFactoryConfiguration(FACTORY_PID, null);
        final Hashtable<String, Object> values = new Hashtable<>();
        values.put("syncDir", getProperty("user.dir") + "/temp_testdata");
        config.update(values);

        // A new service should have been registered
        tracker.open();
        final SyncTarget syncTarget1 = tracker.waitForService(10000);
        assertNotNull(syncTarget1);
        assertNotEquals(defaultSyncTarget, syncTarget1);

        config = configAdmin.createFactoryConfiguration(FACTORY_PID, null);
        values.put("syncDir", getProperty("user.dir") + "/temp_testdata2");
        config.update(values);

        final SyncTarget syncTarget2 = tracker.waitForService(10000);
        assertNotNull(syncTarget2);
        assertNotEquals(defaultSyncTarget, syncTarget2);

        customizer.waitForRegistrations();

        assertEquals(2, customizer.targets.size());
        assertTrue(customizer.targets.contains(syncTarget1));
        assertTrue(customizer.targets.contains(syncTarget2));
    }
}
