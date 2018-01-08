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
package ch.sourcepond.io.fssync.source.fs.fswatch;

import ch.sourcepond.io.checksum.api.ResourceProducer;
import ch.sourcepond.io.fssync.source.fs.trigger.ReplicationTrigger;
import org.mockito.Mockito;

import java.nio.file.WatchService;

import static org.mockito.Mockito.mock;

public class WatchServiceInstallerTest {
    private final ResourceProducer resourceProducer = mock(ResourceProducer.class);
    private final WatchService watchService = mock(WatchService.class);
    private final ReplicationTrigger replicationTrigger = mock(ReplicationTrigger.class);

}
