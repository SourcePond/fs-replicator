package ch.sourcepond.io.distributor.api;

import java.util.concurrent.TimeUnit;

public interface DistributorFactory {

    Distributor create(String pSeparator, TimeUnit pTimeoutUnit, long pTimeout);
}
