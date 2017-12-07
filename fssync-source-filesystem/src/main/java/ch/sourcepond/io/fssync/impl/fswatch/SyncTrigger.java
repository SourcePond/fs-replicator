package ch.sourcepond.io.fssync.impl.fswatch;

import ch.sourcepond.io.fssync.distributor.api.Distributor;
import ch.sourcepond.io.fssync.impl.config.Config;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;

import static org.slf4j.LoggerFactory.getLogger;

class SyncTrigger implements Runnable {
    private static final Logger LOG = getLogger(SyncTrigger.class);
    private final ScheduledExecutorService executor;
    private final Distributor distributor;
    private final Config config;
    private final SyncTriggerFunction trigger;
    private final String syncDir;
    private final String path;

    public SyncTrigger(final ScheduledExecutorService pExecutor,
                       final Distributor pDistributor,
                       final Config pConfig,
                       final String pSyncDir,
                       final String pPath,
                       final SyncTriggerFunction pTrigger) {
        executor = pExecutor;
        distributor = pDistributor;
        config = pConfig;
        syncDir = pSyncDir;
        path = pPath;
        trigger = pTrigger;
    }

    @Override
    public void run() {
        try {
            if (distributor.tryLock(syncDir, path)) {
                try {
                    trigger.process(syncDir, path);
                } finally {
                    distributor.unlock(syncDir, path);
                }
            } else {
                executor.schedule(this, config.retryDelay(), config.retryDelayUnit());
            }
        } catch (final IOException e) {
            LOG.error(e.getMessage(), e);
        }
    }
}
