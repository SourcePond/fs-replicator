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

import ch.sourcepond.io.fssync.target.api.SyncTarget;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static java.time.Instant.now;

public class TestCustomizer implements ServiceTrackerCustomizer<SyncTarget, SyncTarget> {
    public final Set<SyncTarget> targets = new HashSet<>();
    private final BundleContext context;

    public TestCustomizer(final BundleContext pContext) {
        context = pContext;
    }

    @Override
    public SyncTarget addingService(final ServiceReference<SyncTarget> serviceReference) {
        final SyncTarget service = context.getService(serviceReference);
        synchronized (targets) {
            targets.add(service);
            targets.notifyAll();
        }
        return service;
    }

    @Override
    public void modifiedService(final ServiceReference<SyncTarget> serviceReference, final SyncTarget syncTarget) {

    }

    @Override
    public void removedService(final ServiceReference<SyncTarget> serviceReference, final SyncTarget syncTarget) {
        targets.remove(syncTarget);
    }

    public void waitForRegistrations() throws InterruptedException {
        synchronized (targets) {
            final Instant threshold = now().plusMillis(5000);
            while (2 > targets.size() && threshold.isAfter(now())) {
                targets.wait(threshold.minusMillis(now().toEpochMilli()).toEpochMilli());
            }
        }
    }
}
