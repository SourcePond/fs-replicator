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
package ch.sourcepond.io.fssync.target.fs;

import ch.sourcepond.io.fssync.target.api.NodeInfo;
import ch.sourcepond.io.fssync.target.api.SyncPath;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystem;
import java.nio.file.Path;

import static java.nio.channels.FileChannel.open;
import static java.util.UUID.randomUUID;

class FileHandleFactory {
    private static final String TMP_FILE_PREFIX = "transfer";
    private static final String TMP_FILE_SUFFIX = "tmp";
    private final FileSystem fs;

    public FileHandleFactory(final FileSystem pFs) {
        fs = pFs;
    }

    public FileHandle create(final NodeInfo pNodeInfo, final SyncPath pPath) throws IOException {
        final Path syncDir = fs.getPath(pPath.getSyncDir());
        final Path targetFile = syncDir.resolve(pPath.getPath());
        final Path tmpFile = targetFile.getParent().resolve(String.format("%s_%s.%s",
                TMP_FILE_PREFIX, TMP_FILE_SUFFIX, randomUUID().toString()));
        final FileChannel channel = open(targetFile);
        channel.lock();
        return new FileHandle(pNodeInfo, channel, targetFile, tmpFile);
    }
}
