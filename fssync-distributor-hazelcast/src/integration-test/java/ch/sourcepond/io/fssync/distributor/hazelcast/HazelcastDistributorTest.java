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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceListener;
import org.osgi.service.cm.ConfigurationAdmin;

import javax.inject.Inject;

import java.util.Dictionary;
import java.util.Hashtable;

import static java.lang.String.format;
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.bootDelegationPackage;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.osgi.framework.Constants.OBJECTCLASS;

@RunWith(PaxExam.class)
public class HazelcastDistributorTest {
    static final String FACTORY_PID = "ch.sourcepond.io.fssync.distributor.hazelcast.Config";

    @Inject
    private BundleContext context;

    @Inject
    private ConfigurationAdmin configAdmin;

    private Bundle distributorBundle;
    private DistributorListener listener;

    @Configuration
    public Option[] configure() {
        return new Option[]{
                junitBundles(),
                bootDelegationPackage("com.sun.*"),
                mavenBundle("com.google.inject", "guice").classifier("no_aop").version("4.1.0"),
                mavenBundle("com.google.guava", "guava").version("19.0"),
                mavenBundle("com.hazelcast", "hazelcast").version("3.8.6"),
                mavenBundle("ch.sourcepond.osgi.cmpn", "metatype-builder-lib").version("0.1-SNAPSHOT"),
                mavenBundle("ch.sourcepond.io.fssync", "fssync-compound-lib").version("0.1-SNAPSHOT"),
                mavenBundle("ch.sourcepond.io.fssync", "fssync-distributor-api").version("0.1-SNAPSHOT"),
                mavenBundle("ch.sourcepond.io.fssync", "fssync-target-api").version("0.1-SNAPSHOT"),
                mavenBundle("ch.sourcepond.io.fssync", "fssync-distributor-hazelcast").version("0.1-SNAPSHOT"),
                mavenBundle("org.apache.felix", "org.apache.felix.metatype").version("1.1.6"),
                mavenBundle("org.apache.felix", "org.apache.felix.configadmin").version("1.8.16")
        };
    }

    @Before
    public void setup() throws Exception {
        for (final Bundle bundle : context.getBundles()) {
            if ("fssync-distributor-hazelcast".equals(bundle.getSymbolicName())) {
                distributorBundle = bundle;
                break;
            }
        }
        assertNotNull("Bundle not found", distributorBundle);

        listener = new DistributorListener(context);
        listener.register();
    }

    @After
    public void tearDown() throws Exception {
        listener.unregister();
    }

    @Test
    public void verifyGetDistributor() throws Exception {
        final org.osgi.service.cm.Configuration config = configAdmin.createFactoryConfiguration(FACTORY_PID, null);
        final Dictionary<String, Object> props = new Hashtable<>();
        props.put("instanceName", "testInstance");
        config.update(props);

        final Distributor distributor = listener.getDistributor();
    }
}
