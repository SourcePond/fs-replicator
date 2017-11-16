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
package ch.sourcepond.io.distributor.impl.dataflow;

import ch.sourcepond.io.distributor.impl.ClientListenerTest;
import org.junit.Test;
import org.mockito.InOrder;

import java.io.IOException;

import static ch.sourcepond.io.distributor.impl.Constants.EXPECTED_EXCEPTION;
import static ch.sourcepond.io.distributor.impl.Constants.EXPECTED_PATH;
import static ch.sourcepond.io.distributor.impl.Constants.FAILURE_RESPONSE_ARGUMENT_MATCHER;
import static ch.sourcepond.io.distributor.impl.Constants.GLOBAL_PATH_ARGUMENT_MATCHER;
import static ch.sourcepond.io.distributor.impl.Constants.SUCCESS_RESPONSE_ARGUMENT_MATCHER;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

public class ClientDeleteListenerTest extends ClientListenerTest<ClientDeleteListener, String> {

    @Override
    protected String createPayload() {
        return EXPECTED_PATH;
    }

    @Override
    protected ClientDeleteListener createListener() {
        return new ClientDeleteListener(receiver, sendResponseTopic);
    }

    @Test
    public void onMessageFailure() throws IOException {
        doThrow(EXPECTED_EXCEPTION).when(receiver).delete(argThat(GLOBAL_PATH_ARGUMENT_MATCHER));
        listener.onMessage(message);
        verify(sendResponseTopic).publish(argThat(FAILURE_RESPONSE_ARGUMENT_MATCHER));
    }

    @Test
    public void onMessageSuccess() throws IOException {
        listener.onMessage(message);
        final InOrder order = inOrder(receiver, sendResponseTopic);
        order.verify(receiver).delete(argThat(GLOBAL_PATH_ARGUMENT_MATCHER));
        order.verify(sendResponseTopic).publish(argThat(SUCCESS_RESPONSE_ARGUMENT_MATCHER));
    }
}
