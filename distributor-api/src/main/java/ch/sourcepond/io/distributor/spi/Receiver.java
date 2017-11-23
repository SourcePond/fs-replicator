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
package ch.sourcepond.io.distributor.spi;

import ch.sourcepond.io.distributor.api.GlobalPath;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface Receiver {

    /**
     * Locks the path specified on the local host.
     *
     * @param pPath
     * @throws IOException
     */
    void lockLocally(GlobalPath pPath) throws IOException;

    void unlockLocally(GlobalPath pPath) throws IOException;

    void delete(GlobalPath pPath) throws IOException;

    /**
     * Receives the data specified and temporarily store it. The temporary file is not yet visible
     * and needs to be moved to its destination location after all data has been received
     * (see {@link #store(GlobalPath, IOException)}).
     *
     * @param pPath
     * @param pBuffer
     */
    void receive(GlobalPath pPath, ByteBuffer pBuffer);

    void store(GlobalPath pPath, IOException pFailureOrNull) throws IOException;

    /**
     * Unlocks and removes any state which associated with the node-id specified. This method should be called
     * when the sending node goes offline unexpectedly.
     *
     * @param pNode
     */
    void kill(String pNode);
}
