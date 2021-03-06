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
package ch.sourcepond.io.fssync.distributor.api;

import ch.sourcepond.io.fssync.common.api.SyncPath;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * This interface provides access to the underlying distribution mechanism.
 */
public interface Distributor {

    /**
     * Tries to lock the path specified in the network. If successful, this method simply returns.
     *
     * @param pSyncPath Path to be locked, must not be {@code null}
     * @throws IOException          Thrown, if the path specified could not be locked for some reason
     *                              (timeout, I/O failure etc.)
     * @throws NullPointerException Thrown, if the path specified is {@code null}.
     */
    boolean tryLock(SyncPath pSyncPath) throws IOException;

    /**
     * Unlocks the path specified in the network. The underlying implementation must make its best effort to unlock
     * resources even in failure case. If no exceptions where detected while unlocking, this method simply returns.
     *
     * @param pSyncPath Path to be unlocked, must not be {@code null}.
     * @throws IOException          Thrown, if exceptions occurred while unlocking.
     * @throws NullPointerException Thrown, if the path specified is {@code null}.
     */
    void unlock(SyncPath pSyncPath) throws IOException;

    /**
     * Deletes the path specified from the network. If successful, this method simply returns. Before calling this
     * method, {@link #tryLock(SyncPath)} should have been executed successfully.
     *
     * @param pSyncPath Path to be deleted, must not be {@code null}.
     * @throws IOException          Thrown, if the path specified could not be deleted for some reason
     *                              (timeout, I/O failure etc.)
     * @throws NullPointerException Thrown, if the path specified is {@code null}.
     */
    void delete(SyncPath pSyncPath) throws IOException;

    /**
     * Transfers the data specified for the path specified to the network. If successful, this method simply returns.
     * Before calling this method, {@link #tryLock(SyncPath)} should have been executed successfully. The newly
     * transferred data is <em>not</em> visible on the clients until {@link #store(SyncPath, byte[])} has been called.
     *
     * @param pSyncPath Path to which the data belongs to, must not be {@code null}.
     * @param pData     ByteBuffer containing the data to be transferred, must not be {@code null}
     * @throws IOException          Thrown, if the data for the path specified could not be transferred for some reason
     *                              (timeout, I/O failure etc.)
     * @throws NullPointerException Thrown, if the path specified is {@code null}.
     */
    void transfer(SyncPath pSyncPath, ByteBuffer pData) throws IOException;

    /**
     * Discards the transferred data (see {@link #transfer(SyncPath, ByteBuffer)}) for the path specified which has not been
     * stored yet (see {@link #transfer(SyncPath, ByteBuffer)}). Before calling this method,
     * {@link #tryLock(SyncPath)} should have been executed successfully.
     *
     * @param pSyncPath Path to which the data to be discarded belongs to, must not be {@code null}.
     * @param pFailure  IOException thrown during reading the file to be synced, must not be {@code null}.
     * @throws IOException          Thrown, if the data for the path specified could not be discarded for some reason
     *                              (timeout, I/O failure etc.)
     * @throws NullPointerException Thrown, if the path specified is {@code null}.
     */
    void discard(SyncPath pSyncPath, IOException pFailure) throws IOException;

    /**
     * Stores the transferred data to the path specified. If the store was successful, the global
     * checksum will be updated with the checksum specified and this method returns. Before calling this method,
     * {@link #tryLock(SyncPath)} should have been executed successfully. After calling this method, the newly transferred
     * data is visible on the clients (see {@link #transfer(SyncPath, ByteBuffer)}).
     *
     * @param pSyncPath Path to which the data belongs to, must not be {@code null}.
     * @param pChecksum Updated checksum to set, must be not {@code null}.
     * @throws IOException          Thrown, if the data for the path specified could not be stored for some reason
     *                              (timeout, I/O failure etc.)
     * @throws NullPointerException Thrown, if the path specified is {@code null}.
     */
    void store(SyncPath pSyncPath, byte[] pChecksum) throws IOException;

    /**
     * Returns the checksum of the path specified which was set during the last {@link #store(SyncPath, byte[])}
     * operation. If the path has no checksum yet, an empty array will be returned. Before calling this method,
     * {@link #tryLock(SyncPath)} should have been executed successfully.
     *
     * @param pSyncPath Path, must not be {@code null}.
     * @return Checksum as byte-array, never {@code null}
     * @throws NullPointerException Thrown, if the path specified is {@code null}
     */
    byte[] getChecksum(SyncPath pSyncPath);
}
