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
package ch.sourcepond.io.fssync.distributor.hazelcast.request;

import ch.sourcepond.io.fssync.distributor.hazelcast.exception.DeletionException;
import ch.sourcepond.io.fssync.distributor.hazelcast.exception.DiscardException;
import ch.sourcepond.io.fssync.distributor.hazelcast.exception.StoreException;
import ch.sourcepond.io.fssync.distributor.hazelcast.exception.TransferException;
import ch.sourcepond.io.fssync.distributor.hazelcast.common.DistributionMessage;
import ch.sourcepond.io.fssync.distributor.hazelcast.common.StatusMessage;
import ch.sourcepond.io.fssync.distributor.hazelcast.response.ClusterResponseBarrier;
import ch.sourcepond.io.fssync.distributor.hazelcast.response.ClusterResponseBarrierFactory;
import ch.sourcepond.io.fssync.distributor.hazelcast.response.ResponseException;
import com.hazelcast.core.ITopic;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static ch.sourcepond.io.fssync.distributor.hazelcast.Constants.EXPECTED_PATH;
import static ch.sourcepond.io.fssync.distributor.hazelcast.Constants.EXPECTED_SYNC_DIR;
import static ch.sourcepond.io.fssync.distributor.hazelcast.Constants.IS_EQUAL_TO_EXPECTED_DISTRIBUTION_MESSAGE;
import static java.nio.ByteBuffer.wrap;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RequestDistributorTest {
    private static final byte[] EXPECTED_DATA = new byte[]{1, 2, 3, 4, 5};
    private static final IOException EXPECTED_FAILURE = new IOException();
    private static ArgumentMatcher<TransferRequest> TRANSFER_REQUEST_MATCHER = message -> EXPECTED_PATH.equals(message.getPath()) && Arrays.equals(EXPECTED_DATA, message.getData());
    private static ArgumentMatcher<StatusMessage> DISCARD_REQUEST_MATCHER = message -> EXPECTED_PATH.equals(message.getPath()) && message.getFailureOrNull() == EXPECTED_FAILURE;
    private final ClusterResponseBarrierFactory clusterResponseBarrierFactory = mock(ClusterResponseBarrierFactory.class);
    private final ITopic<DistributionMessage> deleteRequestTopic = mock(ITopic.class);
    private final ITopic<TransferRequest> transferRequestTopic = mock(ITopic.class);
    private final ITopic<StatusMessage> discardRequestTopic = mock(ITopic.class);
    private final ITopic<DistributionMessage> storeRequestTopic = mock(ITopic.class);
    private final ClusterResponseBarrier<DistributionMessage> deleteRequestBarrier = mock(ClusterResponseBarrier.class);
    private final ClusterResponseBarrier<TransferRequest> transferRequestBarrier = mock(ClusterResponseBarrier.class);
    private final ClusterResponseBarrier<StatusMessage> discardRequestBarrier = mock(ClusterResponseBarrier.class);
    private final ClusterResponseBarrier<DistributionMessage> storeRequestBarrier = mock(ClusterResponseBarrier.class);
    private final RequestDistributor distributor = new RequestDistributor(clusterResponseBarrierFactory,
            deleteRequestTopic, transferRequestTopic, discardRequestTopic, storeRequestTopic);

    @Before
    public void setup() {
        when(clusterResponseBarrierFactory.create(EXPECTED_PATH, deleteRequestTopic)).thenReturn(deleteRequestBarrier);
        when(clusterResponseBarrierFactory.create(EXPECTED_PATH, transferRequestTopic)).thenReturn(transferRequestBarrier);
        when(clusterResponseBarrierFactory.create(EXPECTED_PATH, discardRequestTopic)).thenReturn(discardRequestBarrier);
        when(clusterResponseBarrierFactory.create(EXPECTED_PATH, storeRequestTopic)).thenReturn(storeRequestBarrier);
    }

    @Test
    public void transfer() throws Exception {
        final ByteBuffer data = wrap(EXPECTED_DATA);
        distributor.transfer(EXPECTED_SYNC_DIR, EXPECTED_PATH, data);
        verify(transferRequestBarrier).awaitResponse(argThat(TRANSFER_REQUEST_MATCHER));
    }

    @Test
    public void transferFailed() throws Exception {
        final ResponseException expected = new ResponseException("any");
        doThrow(expected).when(transferRequestBarrier).awaitResponse(argThat(TRANSFER_REQUEST_MATCHER));
        try {
            distributor.transfer(EXPECTED_SYNC_DIR, EXPECTED_PATH, wrap(EXPECTED_DATA));
            fail("Exception expected!");
        } catch (final TransferException e) {
            assertSame(expected, e.getCause());
        }
    }

    @Test
    public void discard() throws Exception {
        distributor.discard(EXPECTED_SYNC_DIR, EXPECTED_PATH, EXPECTED_FAILURE);
        verify(discardRequestBarrier).awaitResponse(argThat(DISCARD_REQUEST_MATCHER));
    }

    @Test
    public void discardFailed() throws Exception {
        final ResponseException expected = new ResponseException("any");
        doThrow(expected).when(discardRequestBarrier).awaitResponse(argThat(DISCARD_REQUEST_MATCHER));
        try {
            distributor.discard(EXPECTED_SYNC_DIR, EXPECTED_PATH, EXPECTED_FAILURE);
            fail("Exception expected!");
        } catch (final DiscardException e) {
            assertSame(expected, e.getCause());
        }
    }

    @Test
    public void store() throws Exception {
        distributor.store(EXPECTED_SYNC_DIR, EXPECTED_PATH);
        verify(storeRequestBarrier).awaitResponse(argThat(IS_EQUAL_TO_EXPECTED_DISTRIBUTION_MESSAGE));
    }

    @Test
    public void storeFailed() throws Exception {
        final ResponseException expected = new ResponseException("any");
        doThrow(expected).when(storeRequestBarrier).awaitResponse(argThat(IS_EQUAL_TO_EXPECTED_DISTRIBUTION_MESSAGE));
        try {
            distributor.store(EXPECTED_SYNC_DIR, EXPECTED_PATH);
            fail("Exception expected!");
        } catch (final StoreException e) {
            assertSame(expected, e.getCause());
        }
    }

    @Test
    public void delete() throws Exception {
        distributor.delete(EXPECTED_SYNC_DIR, EXPECTED_PATH);
        verify(deleteRequestBarrier).awaitResponse(argThat(IS_EQUAL_TO_EXPECTED_DISTRIBUTION_MESSAGE));
    }

    @Test
    public void deleteFailed() throws Exception {
        final ResponseException expected = new ResponseException("any");
        doThrow(expected).when(deleteRequestBarrier).awaitResponse(argThat(IS_EQUAL_TO_EXPECTED_DISTRIBUTION_MESSAGE));
        try {
            distributor.delete(EXPECTED_SYNC_DIR, EXPECTED_PATH);
            fail("Exception expected!");
        } catch (final DeletionException e) {
            assertSame(expected, e.getCause());
        }
    }
}
