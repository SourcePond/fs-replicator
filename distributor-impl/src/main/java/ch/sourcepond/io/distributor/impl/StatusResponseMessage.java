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
package ch.sourcepond.io.distributor.impl;

import ch.sourcepond.io.distributor.impl.DistributionMessage;

import java.io.IOException;

/**
 * An instance of this class is sent by a node as response to a received request.
 */
public class StatusResponseMessage extends DistributionMessage {
    private final IOException failureOrNull;

    /**
     *
     * @param pPath
     */
    public StatusResponseMessage(final String pPath) {
        this(pPath, null);
    }

    public StatusResponseMessage(final String pPath, final IOException pFailureOrNull) {
        super(pPath);
        failureOrNull = pFailureOrNull;
    }

    /**
     * If the acquisition of the {@link java.nio.channels.FileLock} failed on the node, this
     * method returns the thrown {@link IOException}. If the acquisition was successful, this method
     * returns {@code null}.
     *
     * @return Failure or {@code} when successful
     */
    public IOException getFailureOrNull() {
        return failureOrNull;
    }
}
