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
package ch.sourcepond.io.fssync.impl;

import ch.sourcepond.io.fssync.api.FileSystemSync;
import ch.sourcepond.io.fssync.api.FileSystemSyncFactory;
import ch.sourcepond.io.fssync.impl.config.Config;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import java.io.IOException;
import java.nio.file.Path;

public class FileSystemSyncFactoryImpl implements FileSystemSyncFactory {

    @Override
    public FileSystemSync create(final Path pDirectory) throws IOException {
        return null;
    }

    @Activate
    public void activate(Config pConfig) {

    }

    @Deactivate
    public void deactivate() {

    }

    @Modified
    public void setConfig(final Config pConfig) {

    }
}
