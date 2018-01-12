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

import ch.sourcepond.io.checksum.api.Resource;
import ch.sourcepond.io.checksum.api.UpdateObserver;
import ch.sourcepond.io.fssync.common.api.SyncPath;

import java.io.IOException;
import java.nio.file.Path;

import static java.nio.channels.FileChannel.open;
import static java.nio.file.StandardOpenOption.READ;

public class RegularFile {
    private final DigestingChannel channel;
    private final Resource resource;
    private final SyncPath syncPath;
    private final Path absolutePath;

    RegularFile(final DigestingChannel pChannel,
                final Resource pResource,
                final SyncPath pSyncPath,
                final Path pAbsolutePath) {
        channel = pChannel;
        resource = pResource;
        syncPath = pSyncPath;
        absolutePath = pAbsolutePath;
    }

    public DigestingChannel startDigest() throws IOException {
        return channel.lock(open(absolutePath, READ));
    }

    public SyncPath getSyncPath() {
        return syncPath;
    }

    void update(final UpdateObserver pObserver) throws IOException {
        resource.update(pObserver);
    }
}
