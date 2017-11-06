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
package ch.sourcepond.io.replicator.impl.sender;

import ch.sourcepond.io.distributor.api.Distributor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static java.nio.ByteBuffer.allocate;
import static java.nio.channels.FileChannel.open;
import static java.nio.file.StandardOpenOption.READ;

class ReplicationTrigger {
    private final Distributor distributor;

    public ReplicationTrigger(final Distributor pDistributor) {
        distributor = pDistributor;
    }

    private void transfer(final Path pSource, final String pTarget) throws IOException {
        try (final ReadableByteChannel source = open(pSource, READ)) {
            try (final WritableByteChannel dest = distributor.openChannel(pTarget.toString())) {
                final ByteBuffer buffer = allocate(1024);
                while (source.read(buffer) != -1) {
                    buffer.flip();
                    dest.write(buffer);
                    buffer.rewind();
                }
            }
        }
    }

    void delete(final Path pFile) throws IOException {
        // TODO: make unit/timeout configurable
        distributor.lockGlobally(pFile.toString(), TimeUnit.SECONDS, 10);
        try {
            distributor.delete(pFile.toString());
        } finally {
            distributor.unlockGlobally(pFile.toString());
        }
    }

    void modify(final Path pSource, final byte[] pChecksum) throws IOException {
        final String target = pSource.toString();
        if (!Arrays.equals(distributor.getGlobalChecksum(target), pChecksum)) {
            // TODO: make unit/timeout configurable
            distributor.lockGlobally(pSource.toString(), TimeUnit.SECONDS, 10);
            try {
                transfer(pSource, target);
            } finally {
                distributor.unlockGlobally(pSource.toString());
            }
        }
    }
}
