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
import java.util.concurrent.locks.Lock;

import static ch.sourcepond.io.checksum.api.Checksum.toHexString;
import static ch.sourcepond.io.fssync.source.fs.Constants.EXPECTED_CHECKSUM;
import static ch.sourcepond.io.fssync.source.fs.Constants.TEST_DATA_FILE;
import static java.nio.channels.FileChannel.open;
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
        try (final ReadableByteChannel ch = open(TEST_DATA_FILE, READ)) {
            channel.lock(ch);
            final ByteBuffer buffer = ByteBuffer.allocate(16);
            while (channel.read(buffer) != -1) {
                buffer.flip();
            }
        }
        assertEquals(EXPECTED_CHECKSUM, toHexString(channel.digest()));
    }
}
