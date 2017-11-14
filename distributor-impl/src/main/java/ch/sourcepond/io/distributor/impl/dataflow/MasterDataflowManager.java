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

import com.hazelcast.core.ITopic;

import java.nio.ByteBuffer;

public class MasterDataflowManager {
    private final ITopic<String> sendDeleteTopic;
    private final ITopic<DataMessage> sendDataTopic;

    public MasterDataflowManager(final ITopic<String> pSendDeleteTopic,
                                 final ITopic<DataMessage> pSendDataTopic) {
        sendDeleteTopic = pSendDeleteTopic;
        sendDataTopic = pSendDataTopic;
    }

    public void send(final String pPath, final ByteBuffer pData) {
        final byte[] data = new byte[pData.limit()];
        pData.get(data);
        sendDataTopic.publish(new DataMessage(pPath, data));
    }

    public void delete(final String pPath) {
        sendDeleteTopic.publish(pPath);
    }
}
