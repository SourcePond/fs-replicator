package ch.sourcepond.io.fssync.impl.receiver;

import java.io.Closeable;
import java.nio.ByteBuffer;

interface Storage extends Closeable {

    void store(ByteBuffer pBuffer);
}
