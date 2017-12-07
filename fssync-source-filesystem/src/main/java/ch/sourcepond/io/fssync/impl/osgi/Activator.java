package ch.sourcepond.io.fssync.impl.osgi;

import ch.sourcepond.io.fssync.distributor.api.DistributorFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator, ServiceListener {
    private volatile BundleContext context;
    private volatile DistributorFactory distributorFactory;

    @Override
    public void start(final BundleContext pContext) throws Exception {
        context = pContext;
        final ServiceReference<DistributorFactory> ref = context.getServiceReference(DistributorFactory.class);
        if (ref != null) {

        }
    }

    private DistributorFactory getService(final ServiceReference<DistributorFactory> pRef) {
        return context.getService(pRef);
    }

    @Override
    public void stop(final BundleContext context) throws Exception {

    }

    @Override
    public void serviceChanged(final ServiceEvent event) {

    }
}
