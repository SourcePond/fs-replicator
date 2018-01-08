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

import ch.sourcepond.io.checksum.api.ResourceProducerFactory;
import ch.sourcepond.io.fssync.distributor.api.Distributor;
import com.google.inject.Guice;
import com.google.inject.Injector;

import java.nio.file.WatchService;

import static java.util.concurrent.Executors.newScheduledThreadPool;

class InjectorFactory {

    public Injector createInjector(final Config pConfig,
                                   final WatchService pWatchService,
                                   final Distributor pDistributor,
                                   final ResourceProducerFactory pResourceProducerFactory) {
        return Guice.createInjector(new SourceFsModule(pDistributor,
                pResourceProducerFactory.create(pConfig.checksumConcurrency()),
                pWatchService, newScheduledThreadPool(pConfig.triggerConcurrency()), pConfig));
    }
}
