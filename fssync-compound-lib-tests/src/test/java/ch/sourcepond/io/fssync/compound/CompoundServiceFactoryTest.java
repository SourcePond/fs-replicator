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
package ch.sourcepond.io.fssync.compound;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.BundleContext;

import javax.inject.Inject;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

@RunWith(PaxExam.class)
public class CompoundServiceFactoryTest {

    private final ExecutorService executor = newSingleThreadExecutor();

    @Inject
    private BundleContext context;

    @ProbeBuilder
    public TestProbeBuilder probeConfiguration(final TestProbeBuilder pBuilder) {
        pBuilder.addTest(TestException.class);
        pBuilder.addTest(TestService.class);
        return pBuilder;
    }

    @Configuration
    public Option[] configure() {
        return new Option[]{
                junitBundles(),
                mavenBundle("ch.sourcepond.io.fssync", "fssync-compound-lib").version("0.1-SNAPSHOT")
        };
    }

    @Before
    public void tearDown() {
        executor.shutdown();
    }

    @Test
    public void verifyCreate() {
        final CompoundServiceFactory factory = new CompoundServiceFactory();
        final TestService proxy = factory.create(context, executor, TestService.class, TestException.class);
        assertNotNull(proxy);
    }
}
