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
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import java.time.Instant;

import static java.lang.String.format;
import static java.time.Instant.now;
import static org.junit.Assert.assertNotNull;
import static org.osgi.framework.Constants.OBJECTCLASS;

public class DistributorListener implements ServiceListener {
    private static final long TIMEOUT = 15000;
    private final BundleContext bundleContext;
    private volatile ServiceReference<?> reference;
    private Distributor distributor;

    public DistributorListener(final BundleContext pBundleContext) {
        bundleContext = pBundleContext;
    }

    public Distributor getDistributor() throws Exception {
        synchronized (this) {
            final Instant limit = now().plusMillis(TIMEOUT);
            while (distributor == null && limit.isAfter(now())) {
                wait(TIMEOUT);
            }
            assertNotNull("No distributor published!", distributor);
            return distributor;
        }
    }

    @Override
    public void serviceChanged(final ServiceEvent serviceEvent) {
        if (serviceEvent.getType() == ServiceEvent.REGISTERED) {
            reference = serviceEvent.getServiceReference();
            distributor = (Distributor) bundleContext.getService(reference);
        }
    }

    public void register() throws Exception {
        bundleContext.addServiceListener(this, format("(%s=%s)", OBJECTCLASS, Distributor.class.getName()));
    }

    public void unregister() throws Exception {
        if (reference != null) {
            bundleContext.ungetService(reference);
        }
        bundleContext.removeServiceListener(this);
    }
}
