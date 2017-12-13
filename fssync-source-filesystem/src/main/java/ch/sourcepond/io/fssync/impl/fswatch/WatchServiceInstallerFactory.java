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
package ch.sourcepond.io.fssync.impl.fswatch;

import ch.sourcepond.io.checksum.api.ResourceProducer;
import ch.sourcepond.io.fssync.impl.trigger.ReplicationTrigger;

import javax.inject.Inject;
import java.nio.file.Path;
import java.nio.file.WatchService;

public class WatchServiceInstallerFactory {
    private final ResourceProducer resourceProducer;
    private final WatchService watchService;
    private final ReplicationTrigger replicationTrigger;

    @Inject
    WatchServiceInstallerFactory(final ResourceProducer pResourceProducer,
                                 final WatchService pWatchService,
                                 final ReplicationTrigger pReplicationTrigger) {
        resourceProducer = pResourceProducer;
        watchService = pWatchService;
        replicationTrigger = pReplicationTrigger;
    }

    public WatchServiceInstaller create(final Path pSyncDir) {
        return new WatchServiceInstaller(resourceProducer, watchService, replicationTrigger, pSyncDir);
    }
}
