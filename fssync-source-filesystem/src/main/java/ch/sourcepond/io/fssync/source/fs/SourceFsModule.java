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
package ch.sourcepond.io.fssync.source.fs;

import ch.sourcepond.io.checksum.api.ResourceProducer;
import ch.sourcepond.io.fssync.distributor.api.Distributor;
import ch.sourcepond.io.fssync.source.fs.fswatch.FswatchModule;
import ch.sourcepond.io.fssync.source.fs.trigger.TriggerModule;
import com.google.inject.AbstractModule;

import java.nio.file.WatchService;

public class SourceFsModule extends AbstractModule {
    private final Distributor distributor;
    private final ResourceProducer resourceProducer;
    private final WatchService watchService;
    private final Config config;

    public SourceFsModule(final Distributor pDistributor,
                          final ResourceProducer pResourceProducer,
                          final WatchService pWatchService,
                          final Config pConfig) {
        distributor = pDistributor;
        resourceProducer = pResourceProducer;
        watchService = pWatchService;
        config = pConfig;
    }

    @Override
    protected void configure() {
        bind(Distributor.class).toInstance(distributor);
        bind(ResourceProducer.class).toInstance(resourceProducer);
        bind(WatchService.class).toInstance(watchService);
        bind(Config.class).toInstance(config);
        install(new FswatchModule());
        install(new TriggerModule());
    }
}
