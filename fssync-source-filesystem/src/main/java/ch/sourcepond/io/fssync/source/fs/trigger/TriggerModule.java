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
package ch.sourcepond.io.fssync.source.fs.trigger;

import ch.sourcepond.io.fssync.common.api.SyncPathFactory;
import com.google.inject.AbstractModule;

public class TriggerModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(MessageDigestFactory.class);
        bind(ReplicationTrigger.class);
        bind(SyncTriggerFactory.class);
        bind(SyncPathFactory.class);
    }
}
