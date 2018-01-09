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
package ch.sourcepond.io.fssync.source.fs.trigger;

import ch.sourcepond.io.fssync.common.api.SyncPath;
import ch.sourcepond.io.fssync.distributor.api.Distributor;
import ch.sourcepond.io.fssync.source.fs.Config;

import javax.inject.Inject;
import java.util.concurrent.ScheduledExecutorService;

class SyncTriggerFactory {
    private final Distributor distributor;
    private final ScheduledExecutorService executor;
    private final Config config;

    @Inject
    public SyncTriggerFactory(final Distributor pDistributor,
                              final ScheduledExecutorService pExecutor,
                              final Config pConfig) {
        distributor = pDistributor;
        executor = pExecutor;
        config = pConfig;
    }

    public SyncTrigger create(final SyncPath pPath,
                              final SyncTriggerFunction pTrigger) {
        return new SyncTrigger(executor, distributor, config, pPath, pTrigger);
    }
}
