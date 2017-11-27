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
package ch.sourcepond.io.fssync.distributor.impl.request;

import ch.sourcepond.io.fssync.distributor.api.GlobalPath;
import ch.sourcepond.io.fssync.distributor.impl.common.ClientMessageProcessor;
import ch.sourcepond.io.fssync.distributor.impl.common.StatusMessage;
import ch.sourcepond.io.fssync.distributor.spi.Receiver;

import javax.inject.Inject;
import java.io.IOException;

final class DiscardRequestProcessor extends ClientMessageProcessor<StatusMessage> {

    @Inject
    DiscardRequestProcessor(final Receiver pReceiver) {
        super(pReceiver);
    }

    @Override
    protected void processMessage(final GlobalPath pPath, final StatusMessage pMessage) throws IOException {
        receiver.discard(pPath, pMessage.getFailureOrNull());
    }

    @Override
    protected String toPath(final StatusMessage pMessage) {
        return pMessage.getPath();
    }
}
