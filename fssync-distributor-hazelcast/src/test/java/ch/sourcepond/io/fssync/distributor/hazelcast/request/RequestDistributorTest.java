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

import ch.sourcepond.io.fssync.common.api.SyncPath;
import ch.sourcepond.io.fssync.distributor.hazelcast.common.DistributionMessage;
import ch.sourcepond.io.fssync.distributor.hazelcast.common.StatusMessage;
import ch.sourcepond.io.fssync.distributor.hazelcast.exception.DeletionException;
import ch.sourcepond.io.fssync.distributor.hazelcast.exception.DiscardException;
import ch.sourcepond.io.fssync.distributor.hazelcast.exception.StoreException;
import ch.sourcepond.io.fssync.distributor.hazelcast.exception.TransferException;
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
    private final SyncPath path = mock(SyncPath.class);
    private final ArgumentMatcher<TransferRequest> transferRequestMatcher = message -> path.equals(message.getPath()) && Arrays.equals(EXPECTED_DATA, message.getData());
    private final ArgumentMatcher<StatusMessage> discardRequestMatcher = message -> path.equals(message.getPath()) && message.getFailureOrNull() == EXPECTED_FAILURE;
    private final ClusterResponseBarrierFactory clusterResponseBarrierFactory = mock(ClusterResponseBarrierFactory.class);
    private final ITopic<DistributionMessage> deleteRequestTopic = mock(ITopic.class);
    private final ITopic<TransferRequest> transferRequestTopic = mock(ITopic.class);
    private final ITopic<StatusMessage> discardRequestTopic = mock(ITopic.class);
    private final ITopic<DistributionMessage> storeRequestTopic = mock(ITopic.class);
    private final ClusterResponseBarrier<DistributionMessage> deleteRequestBarrier = mock(ClusterResponseBarrier.class);
    private final ClusterResponseBarrier<TransferRequest> transferRequestBarrier = mock(ClusterResponseBarrier.class);
    private final ClusterResponseBarrier<StatusMessage> discardRequestBarrier = mock(ClusterResponseBarrier.class);
    private final ClusterResponseBarrier<DistributionMessage> storeRequestBarrier = mock(ClusterResponseBarrier.class);
    private final ArgumentMatcher<DistributionMessage> isEqualToExpectedDistributionMessage = msg -> path.equals(msg.getPath());
    private final RequestDistributor distributor = new RequestDistributor(clusterResponseBarrierFactory,
            deleteRequestTopic, transferRequestTopic, discardRequestTopic, storeRequestTopic);

    @Before
    public void setup() {
        when(clusterResponseBarrierFactory.create(path, deleteRequestTopic)).thenReturn(deleteRequestBarrier);
        when(clusterResponseBarrierFactory.create(path, transferRequestTopic)).thenReturn(transferRequestBarrier);
        when(clusterResponseBarrierFactory.create(path, discardRequestTopic)).thenReturn(discardRequestBarrier);
        when(clusterResponseBarrierFactory.create(path, storeRequestTopic)).thenReturn(storeRequestBarrier);
    }

    @Test
    public void transfer() throws Exception {
        final ByteBuffer data = wrap(EXPECTED_DATA);
        distributor.transfer(path, data);
        verify(transferRequestBarrier).awaitResponse(argThat(transferRequestMatcher));
    }

    @Test
    public void transferFailed() throws Exception {
        final ResponseException expected = new ResponseException("any");
        doThrow(expected).when(transferRequestBarrier).awaitResponse(argThat(transferRequestMatcher));
        try {
            distributor.transfer(path, wrap(EXPECTED_DATA));
            fail("Exception expected!");
        } catch (final TransferException e) {
            assertSame(expected, e.getCause());
        }
    }

    @Test
    public void discard() throws Exception {
        distributor.discard(path, EXPECTED_FAILURE);
        verify(discardRequestBarrier).awaitResponse(argThat(discardRequestMatcher));
    }

    @Test
    public void discardFailed() throws Exception {
        final ResponseException expected = new ResponseException("any");
        doThrow(expected).when(discardRequestBarrier).awaitResponse(argThat(discardRequestMatcher));
        try {
            distributor.discard(path, EXPECTED_FAILURE);
            fail("Exception expected!");
        } catch (final DiscardException e) {
            assertSame(expected, e.getCause());
        }
    }

    @Test
    public void store() throws Exception {
        distributor.store(path);
        verify(storeRequestBarrier).awaitResponse(argThat(isEqualToExpectedDistributionMessage));
    }

    @Test
    public void storeFailed() throws Exception {
        final ResponseException expected = new ResponseException("any");
        doThrow(expected).when(storeRequestBarrier).awaitResponse(argThat(isEqualToExpectedDistributionMessage));
        try {
            distributor.store(path);
            fail("Exception expected!");
        } catch (final StoreException e) {
            assertSame(expected, e.getCause());
        }
    }

    @Test
    public void delete() throws Exception {
        distributor.delete(path);
        verify(deleteRequestBarrier).awaitResponse(argThat(isEqualToExpectedDistributionMessage));
    }

    @Test
    public void deleteFailed() throws Exception {
        final ResponseException expected = new ResponseException("any");
        doThrow(expected).when(deleteRequestBarrier).awaitResponse(argThat(isEqualToExpectedDistributionMessage));
        try {
            distributor.delete(path);
            fail("Exception expected!");
        } catch (final DeletionException e) {
            assertSame(expected, e.getCause());
        }
    }
}
