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

import ch.sourcepond.io.checksum.api.Checksum;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.util.concurrent.locks.Lock;

import static java.lang.System.getProperty;
import static java.nio.channels.FileChannel.open;
import static java.nio.file.FileSystems.getDefault;
import static java.nio.file.StandardOpenOption.READ;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DigestingChannelTest {
    private static final String EXPECTED_CHECKSUM = "0be0b1c29bc463b8c16a681b511bad8cb05134d802da91db770bf0dd0a49786e";
    private final ReadableByteChannel delegate = mock(ReadableByteChannel.class);
    private final Lock lock = mock(Lock.class);
    private final DigestingChannel channel = new DigestingChannel(lock);

    @Test
    public void verifyDefaultConstructor() throws IOException {
        try (final DigestingChannel ch = new DigestingChannelFactory().create().lock(delegate)) {
            // should not throw an exception
        }
    }

    @Test
    public void lock() {
        assertSame(channel, channel.lock(delegate));
        verify(lock).lock();
    }

    @Test
    public void close() throws IOException {
        channel.lock(delegate).close();
        verify(lock).unlock();
    }

    @Test
    public void closeInExceptionCase() throws IOException {
        doThrow(IOException.class).when(delegate).close();
        try {
            channel.lock(delegate).close();
            fail("Exception expected here");
        } catch (final IOException e) {
            // expected
        }
        verify(lock).unlock();
    }

    @Test
    public void isOpen() {
        when(delegate.isOpen()).thenReturn(true);
        assertTrue(channel.lock(delegate).isOpen());
    }

    @Test
    public void verifyReadChecksum() throws IOException {
        final Path sourcePath = getDefault().getPath(getProperty("user.dir")).
                resolve("src").resolve("test").resolve("resources").
                resolve(getClass().getSimpleName() + ".txt");
        try (final ReadableByteChannel ch = open(sourcePath, READ)) {
            channel.lock(ch);
            final ByteBuffer buffer = ByteBuffer.allocate(16);
            while (channel.read(buffer) != -1) {
                buffer.flip();
            }
        }
        assertEquals(EXPECTED_CHECKSUM, Checksum.toHexString(channel.digest()));
    }
}
