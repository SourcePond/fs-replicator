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

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.WatchKey;

final class Directory implements Closeable {
    private final Directory parentOrNull;
    private final WatchKey watchKey;

    public Directory(final Directory parentOrNull, final WatchKey watchKey) {
        this.parentOrNull = parentOrNull;
        this.watchKey = watchKey;
    }

    public Directory getParentOrNull() {
        return parentOrNull;
    }

    @Override
    public void close() throws IOException {
        watchKey.cancel();
    }
}
