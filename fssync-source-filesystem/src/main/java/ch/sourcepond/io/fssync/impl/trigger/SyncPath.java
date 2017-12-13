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
package ch.sourcepond.io.fssync.impl.trigger;

import java.nio.file.Path;

class SyncPath {
    private final Path syncDir;
    private final Path path;
    private final String syncDirAsString;
    private final String pathAsString;

    public SyncPath(final Path pSyncDir, final Path pPath) {
        syncDir = pSyncDir;
        path = pPath;
        syncDirAsString = pSyncDir.toString();
        pathAsString = pSyncDir.relativize(pPath).toString();
    }

    public Path getSyncDir() {
        return syncDir;
    }

    public Path getPath() {
        return path;
    }

    public String getSyncDirAsString() {
        return syncDirAsString;
    }

    public String getPathAsString() {
        return pathAsString;
    }
}
