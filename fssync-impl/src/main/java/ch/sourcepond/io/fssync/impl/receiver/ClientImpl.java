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
package ch.sourcepond.io.fssync.impl.receiver;

import ch.sourcepond.io.fssync.distributor.api.GlobalPath;
import ch.sourcepond.io.fssync.distributor.spi.Client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ClientImpl implements Client {
    private final ConcurrentMap channels = new ConcurrentHashMap();

    @Override
    public void lock(final GlobalPath pPath) throws IOException {
        channels.computeIfAbsent(pPath)
    }

    @Override
    public void unlock(final GlobalPath pPath) throws IOException {

    }

    @Override
    public void delete(final GlobalPath pPath) throws IOException {

    }

    @Override
    public void transfer(final GlobalPath pPath, final ByteBuffer pBuffer) {

    }

    @Override
    public void discard(final GlobalPath pPath, final IOException pFailure) throws IOException {

    }

    @Override
    public void store(final GlobalPath pPath) throws IOException {

    }

    @Override
    public void cancel(final String pNode) {

    }
}
