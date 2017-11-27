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
package ch.sourcepond.io.distributor.impl.request;

import ch.sourcepond.io.distributor.api.DeletionException;
import ch.sourcepond.io.distributor.api.TransferException;
import ch.sourcepond.io.distributor.api.StoreException;
import ch.sourcepond.io.distributor.impl.common.StatusMessage;
import ch.sourcepond.io.distributor.impl.response.ClusterResponseBarrier;
import ch.sourcepond.io.distributor.impl.response.ClusterResponseBarrierFactory;
import ch.sourcepond.io.distributor.impl.response.ResponseException;
import com.hazelcast.core.ITopic;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static java.nio.ByteBuffer.wrap;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RequestDistributorTest {
    private static final String EXPECTED_PATH = "somePath";
    private static final byte[] EXPECTED_DATA = new byte[] {1, 2, 3, 4, 5};
    private static ArgumentMatcher<TransferRequest> TRANSFER_REQUEST_MATCHER = message -> EXPECTED_PATH.equals(message.getPath()) && Arrays.equals(EXPECTED_DATA, message.getData());
    private static ArgumentMatcher<StatusMessage> STORE_REQUEST_MATCHER = message -> EXPECTED_PATH.equals(message.getPath()) && message.getFailureOrNull() == null;
    private final ClusterResponseBarrierFactory clusterResponseBarrierFactory = mock(ClusterResponseBarrierFactory.class);
    private final ITopic<String> deleteRequestTopic = mock(ITopic.class);
    private final ITopic<TransferRequest> transferRequestTopic = mock(ITopic.class);
    private final ITopic<StatusMessage> storeRequestTopic = mock(ITopic.class);
    private final ClusterResponseBarrier<String> deleteRequestBarrier = mock(ClusterResponseBarrier.class);
    private final ClusterResponseBarrier<TransferRequest> transferRequestBarrier = mock(ClusterResponseBarrier.class);
    private final ClusterResponseBarrier<StatusMessage> storeRequestBarrier = mock(ClusterResponseBarrier.class);
    private final RequestDistributor distributor = new RequestDistributor(clusterResponseBarrierFactory,
            deleteRequestTopic, transferRequestTopic, storeRequestTopic);

    @Before
    public void setup() {
        when(clusterResponseBarrierFactory.create(EXPECTED_PATH, deleteRequestTopic)).thenReturn(deleteRequestBarrier);
        when(clusterResponseBarrierFactory.create(EXPECTED_PATH, transferRequestTopic)).thenReturn(transferRequestBarrier);
        when(clusterResponseBarrierFactory.create(EXPECTED_PATH, storeRequestTopic)).thenReturn(storeRequestBarrier);
    }

    @Test
    public void transfer() throws Exception {
        final ByteBuffer data = wrap(EXPECTED_DATA);
        distributor.transfer(EXPECTED_PATH, data);
        verify(transferRequestBarrier).awaitResponse(argThat(TRANSFER_REQUEST_MATCHER));
    }

    @Test
    public void transferFailed() throws Exception {
        final ResponseException expected = new ResponseException("any");
        doThrow(expected).when(transferRequestBarrier).awaitResponse(argThat(TRANSFER_REQUEST_MATCHER));
        try {
            distributor.transfer(EXPECTED_PATH, wrap(EXPECTED_DATA));
            fail("Exception expected!");
        } catch (final TransferException e) {
            assertSame(expected, e.getCause());
        }
    }

    @Test
    public void store() throws Exception {
        distributor.store(EXPECTED_PATH, null);
        verify(storeRequestBarrier).awaitResponse(argThat(STORE_REQUEST_MATCHER));
    }

    @Test
    public void storeWithFailure() throws Exception {
        final IOException expectedFailure = new IOException();
        distributor.store(EXPECTED_PATH, expectedFailure);
        verify(storeRequestBarrier).awaitResponse(argThat(message -> EXPECTED_PATH.equals(message.getPath()) && expectedFailure.equals(message.getFailureOrNull())));
    }

    @Test
    public void storeFailed() throws Exception {
        final ResponseException expected = new ResponseException("any");
        doThrow(expected).when(storeRequestBarrier).awaitResponse(argThat(STORE_REQUEST_MATCHER));
        try {
            distributor.store(EXPECTED_PATH, null);
            fail("Exception expected!");
        } catch (final StoreException e) {
            assertSame(expected, e.getCause());
        }
    }

    @Test
    public void delete() throws Exception {
        distributor.delete(EXPECTED_PATH);
        verify(deleteRequestBarrier).awaitResponse(EXPECTED_PATH);
    }

    @Test
    public void deleteFailed() throws Exception {
        final ResponseException expected = new ResponseException("any");
        doThrow(expected).when(deleteRequestBarrier).awaitResponse(EXPECTED_PATH);
        try {
            distributor.delete(EXPECTED_PATH);
            fail("Exception expected!");
        } catch (final DeletionException e) {
            assertSame(expected, e.getCause());
        }
    }
}
