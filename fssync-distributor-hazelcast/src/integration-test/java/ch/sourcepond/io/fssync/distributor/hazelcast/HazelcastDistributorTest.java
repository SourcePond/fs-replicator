/*Copyright (C) 2018 Roland Hauser, <sourcepond@gmail.com>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/
package ch.sourcepond.io.fssync.distributor.hazelcast;

import ch.sourcepond.io.fssync.distributor.api.Distributor;
import ch.sourcepond.io.fssync.target.api.NodeInfo;
import ch.sourcepond.io.fssync.target.api.SyncPath;
import ch.sourcepond.io.fssync.target.api.SyncTarget;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InOrder;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.cm.ConfigurationAdmin;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;

import static java.lang.Thread.currentThread;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.ops4j.pax.exam.CoreOptions.bootDelegationPackage;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

@RunWith(PaxExam.class)
public class HazelcastDistributorTest {
    private static final String EXPECTED_FAILURE = "expectedFailure";
    private static final String EXPECTED_SYNC_DIR = "someExpectedSyncDir";
    private static final String EXPECTED_PATH = "someExpectedPath";
    private static final String FACTORY_PID = "ch.sourcepond.io.fssync.distributor.hazelcast.Config";
    private static final ArgumentMatcher<NodeInfo> NODE_MATCHER = inv -> inv.isLocalNode() && inv.getLocal().equals(inv.getSender());
    private static final ArgumentMatcher<SyncPath> PATH_MATCHER = inv -> EXPECTED_SYNC_DIR.equals(inv.getSyncDir()) && EXPECTED_PATH.equals(inv.getPath());
    private static final byte[] ARR_1 = new byte[]{1, 2, 3};
    private static final byte[] ARR_2 = new byte[]{4, 5, 6};
    private static final byte[] ARR_3 = new byte[]{7, 8, 9};

    @Inject
    private BundleContext context;

    @Inject
    private ConfigurationAdmin configAdmin;

    private SyncTarget target;
    private ServiceRegistration<SyncTarget> targetRegistration;

    private DistributorListener listener;
    private Distributor distributor;
    private org.osgi.service.cm.Configuration config;

    @Configuration
    public Option[] configure() {
        return new Option[]{
                junitBundles(),
                bootDelegationPackage("com.sun.*"),
                mavenBundle("com.google.inject", "guice").classifier("no_aop").version("4.1.0"),
                mavenBundle("com.google.inject.extensions", "guice-multibindings").version("4.1.0").noStart(),
                mavenBundle("com.google.guava", "guava").version("19.0"),
                mavenBundle("com.hazelcast", "hazelcast").version("3.8.6"),
                mavenBundle("ch.sourcepond.osgi.cmpn", "metatype-builder-lib").version("0.1-SNAPSHOT"),
                mavenBundle("ch.sourcepond.io.fssync", "fssync-common-lib").version("0.1-SNAPSHOT"),
                mavenBundle("ch.sourcepond.io.fssync", "fssync-distributor-api").version("0.1-SNAPSHOT"),
                mavenBundle("ch.sourcepond.io.fssync", "fssync-target-api").version("0.1-SNAPSHOT"),
                mavenBundle("ch.sourcepond.io.fssync", "fssync-distributor-hazelcast").version("0.1-SNAPSHOT"),
                mavenBundle("org.apache.felix", "org.apache.felix.metatype").version("1.1.6"),
                mavenBundle("org.apache.felix", "org.apache.felix.configadmin").version("1.8.16"),
                mavenBundle("org.objenesis", "objenesis").version("2.6"),
                mavenBundle("org.mockito", "mockito-core").version("2.13.0"),
                mavenBundle("net.bytebuddy", "byte-buddy").version("1.7.9"),
                mavenBundle("net.bytebuddy", "byte-buddy-agent").version("1.7.9")
        };
    }

    private Bundle getDistributorBundle() {
        for (final Bundle bundle : context.getBundles()) {
            if ("fssync-distributor-hazelcast".equals(bundle.getSymbolicName())) {
                return bundle;
            }
        }
        throw new AssertionError("Bundle not found");
    }

    @Before
    public void setup() throws Exception {
        final ClassLoader current = currentThread().getContextClassLoader();
        currentThread().setContextClassLoader(context.getBundle().adapt(BundleWiring.class).getClassLoader());
        try {
            target = mock(SyncTarget.class);
        } finally {
            currentThread().setContextClassLoader(current);
        }

        targetRegistration = context.registerService(SyncTarget.class, target, null);
        listener = new DistributorListener(context);
        listener.register();

        config = configAdmin.createFactoryConfiguration(FACTORY_PID, null);
        final Dictionary<String, Object> props = new Hashtable<>();
        props.put("instanceName", "testInstance");
        config.update(props);

        distributor = listener.getDistributor();
    }

    @After
    public void tearDown() throws Exception {
        distributor.unlock(EXPECTED_SYNC_DIR, EXPECTED_PATH);
        listener.unregister();
        targetRegistration.unregister();
        config.delete();
    }

    @Test
    public void delete() throws Exception {
        assertTrue(distributor.tryLock(EXPECTED_SYNC_DIR, EXPECTED_PATH));
        distributor.delete(EXPECTED_SYNC_DIR, EXPECTED_PATH);
        distributor.unlock(EXPECTED_SYNC_DIR, EXPECTED_PATH);
        final InOrder order = inOrder(target);
        order.verify(target).lock(argThat(NODE_MATCHER), argThat(PATH_MATCHER));
        order.verify(target).delete(argThat(NODE_MATCHER), argThat(PATH_MATCHER));
        order.verify(target).unlock(argThat(NODE_MATCHER), argThat(PATH_MATCHER));
        order.verifyNoMoreInteractions();
    }

    @Test
    public void transferStore() throws Exception {
        assertTrue(distributor.tryLock(EXPECTED_SYNC_DIR, EXPECTED_PATH));
        distributor.transfer(EXPECTED_SYNC_DIR, EXPECTED_PATH, ByteBuffer.wrap(ARR_1));
        distributor.transfer(EXPECTED_SYNC_DIR, EXPECTED_PATH, ByteBuffer.wrap(ARR_2));
        distributor.transfer(EXPECTED_SYNC_DIR, EXPECTED_PATH, ByteBuffer.wrap(ARR_3));
        distributor.store(EXPECTED_SYNC_DIR, EXPECTED_PATH, new byte[0]);
        distributor.unlock(EXPECTED_SYNC_DIR, EXPECTED_PATH);
        final InOrder order = inOrder(target);
        order.verify(target).lock(argThat(NODE_MATCHER), argThat(PATH_MATCHER));
        order.verify(target).transfer(argThat(NODE_MATCHER), argThat(PATH_MATCHER), argThat(inv -> Arrays.equals(ARR_1, inv.array())));
        order.verify(target).transfer(argThat(NODE_MATCHER), argThat(PATH_MATCHER), argThat(inv -> Arrays.equals(ARR_2, inv.array())));
        order.verify(target).transfer(argThat(NODE_MATCHER), argThat(PATH_MATCHER), argThat(inv -> Arrays.equals(ARR_3, inv.array())));
        order.verify(target).store(argThat(NODE_MATCHER), argThat(PATH_MATCHER));
        order.verify(target).unlock(argThat(NODE_MATCHER), argThat(PATH_MATCHER));
        order.verifyNoMoreInteractions();
    }

    @Test
    public void transferDiscard() throws Exception {
        final IOException expected = new IOException(EXPECTED_FAILURE);
        assertTrue(distributor.tryLock(EXPECTED_SYNC_DIR, EXPECTED_PATH));
        distributor.transfer(EXPECTED_SYNC_DIR, EXPECTED_PATH, ByteBuffer.wrap(ARR_1));
        distributor.discard(EXPECTED_SYNC_DIR, EXPECTED_PATH, expected);
        distributor.unlock(EXPECTED_SYNC_DIR, EXPECTED_PATH);
        final InOrder order = inOrder(target);
        order.verify(target).lock(argThat(NODE_MATCHER), argThat(PATH_MATCHER));
        order.verify(target).transfer(argThat(NODE_MATCHER), argThat(PATH_MATCHER), argThat(inv -> Arrays.equals(ARR_1, inv.array())));
        order.verify(target).discard(argThat(NODE_MATCHER), argThat(PATH_MATCHER), argThat(inv -> EXPECTED_FAILURE.equals(inv.getMessage())));
        order.verify(target).unlock(argThat(NODE_MATCHER), argThat(PATH_MATCHER));
        order.verifyNoMoreInteractions();
    }
}
