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

import ch.sourcepond.io.distributor.impl.common.ResponseAwaitingManager;
import ch.sourcepond.io.distributor.impl.common.client.DataRequest;
import ch.sourcepond.io.distributor.impl.common.master.StatusResponse;
import ch.sourcepond.io.distributor.impl.dataflow.master.MasterDataSendResponseListener;
import com.hazelcast.core.Cluster;
import com.hazelcast.core.ITopic;

import java.nio.ByteBuffer;

public class DataflowManager extends ResponseAwaitingManager {
    private final ITopic<String> sendDeleteTopic;
    private final ITopic<DataRequest> sendDataTopic;
    private final ITopic<String> sendDataTransferFinishedTopic;

    public DataflowManager(final Cluster pCluster,
                           final ITopic<StatusResponse> pResponseTopic,
                           final ITopic<String> pSendDeleteTopic,
                           final ITopic<DataRequest> pSendDataTopic,
                           final ITopic<String> pSendDataTransferFinishedTopic) {
        super(pCluster, pResponseTopic);
        sendDeleteTopic = pSendDeleteTopic;
        sendDataTopic = pSendDataTopic;
        sendDataTransferFinishedTopic = pSendDataTransferFinishedTopic;
    }

    public void send(final String pPath, final ByteBuffer pData) {
        final byte[] data = new byte[pData.limit()];
        pData.get(data);
        performAction(sendDataTopic, new DataRequest(pPath, data), new MasterDataSendResponseListener());
    }

    public void delete(final String pPath) {
        sendDeleteTopic.publish(pPath);
    }
}
