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
package ch.sourcepond.io.distributor.api;

import ch.sourcepond.io.distributor.api.exception.DeletionException;
import ch.sourcepond.io.distributor.api.exception.LockException;
import ch.sourcepond.io.distributor.api.exception.ModificationException;
import ch.sourcepond.io.distributor.api.exception.StoreException;
import ch.sourcepond.io.distributor.api.exception.UnlockException;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * This interface provides access to the underlying distribution mechanism.
 */
public interface Distributor {

    /**
     * Tries to lock the path specified in the network. If successful, this method simply returns.
     *
     * @param pPath Path to be locked, must not be {@code null}
     * @throws LockException        Thrown, if the path specified could not be locked for some reason
     *                              (timeout, I/O failure etc.)
     * @throws NullPointerException Thrown, if the path specified is {@code null}.
     */
    void lock(String pPath) throws LockException;

    /**
     * Returns when the path specified is locked network-wide i.e. {@link #lock(String)} was successful and
     * {@link #unlock(String)} has not been called until now.
     *
     * @param pPath Path to check, must not be {@code null}.
     * @return {@code true} if the path specified is locked, {@code false} otherwise.
     */
    boolean isLocked(String pPath);

    /**
     * Unlocks the path specified in the network. The underlying implementation must make its best effort to unlock
     * resources even in failure case. If no exceptions where detected while unlocking, this method simply returns.
     *
     * @param pPath Path to be unlocked, must not be {@code null}.
     * @throws UnlockException      Thrown, if exceptions occurred while unlocking.
     * @throws NullPointerException Thrown, if the path specified is {@code null}.
     */
    void unlock(String pPath) throws UnlockException;

    /**
     * Deletes the path specified from the network. If successful, this method simply returns. Before calling this
     * method, {@link #lock(String)} should have been executed successfully.
     *
     * @param pPath Path to be deleted, must not be {@code null}.
     * @throws DeletionException      Thrown, if the path specified could not be deleted for some reason
     *                                (timeout, I/O failure etc.)
     * @throws NullPointerException   Thrown, if the path specified is {@code null}.
     */
    void delete(String pPath) throws DeletionException;

    /**
     * Transfers the data specified for the path specified to the network. If successful, this method simply returns.
     * Before calling this method, {@link #lock(String)} should have been executed successfully. The newly
     * transferred data is <em>not</em> visible on the clients until {@link #store(String, byte[], IOException)} has been called.
     *
     * @param pPath Path to which the data belongs to, must not be {@code null}.
     * @param pData ByteBuffer containing the data to be transferred, must not be {@code null}
     * @throws ModificationException  Thrown, if the data for the path specified could not be transferred for some reason
     *                                (timeout, I/O failure etc.)
     * @throws NullPointerException   Thrown, if the path specified is {@code null}.
     */
    void transfer(String pPath, ByteBuffer pData) throws ModificationException;

    /**
     * Stores the transferred data to the path specified. If the store was successful, the global
     * checksum will be updated with the checksum specified and this method returns. Before calling this method,
     * {@link #lock(String)} should have been executed successfully. After calling this method, the newly transferred
     * data is visible on the clients (see {@link #transfer(String, ByteBuffer)}).
     *
     * @param pPath     Path to which the data belongs to, must not be {@code null}.
     * @param pChecksum Updated checksum to set, must be not {@code null}.
     * @param pFailureOrNull Failure in case the I/O failed during reading the file to be replicated or {@code null}
     * @throws StoreException         Thrown, if the data for the path specified could not be stored for some reason
     *                                (timeout, I/O failure etc.)
     * @throws NullPointerException   Thrown, if the path specified is {@code null}.
     */
    void store(String pPath, byte[] pChecksum, IOException pFailureOrNull) throws StoreException;

    /**
     * Returns the local node identifier. This can be a name, a uuid or anything
     * else depending on the implementation.
     *
     * @return Local node identifier, never {@code null}.
     */
    String getLocalNode();

    /**
     * Returns the checksum of the path specified which was set during the last {@link #store(String, byte[], IOException)}
     * operation. If the path has no checksum yet, an empty array will be returned. Before calling this method,
     * {@link #lock(String)} should have been executed successfully.
     *
     * @param pPath Path, must not be {@code null}.
     * @return Checksum as byte-array, never {@code null}
     * @throws NullPointerException Thrown, if the path specified is {@code null}
     */
    byte[] getChecksum(String pPath);
}
