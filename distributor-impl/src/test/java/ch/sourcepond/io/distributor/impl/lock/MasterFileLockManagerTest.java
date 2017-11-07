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
package ch.sourcepond.io.distributor.impl.lock;

import com.hazelcast.core.Cluster;
import com.hazelcast.core.ITopic;

import static org.mockito.Mockito.mock;

public class MasterFileLockManagerTest {
    private final Cluster cluster = mock(Cluster.class);
    private final ITopic<String> sendFileLockRequestTopic = mock(ITopic.class);
    private final ITopic<LockMessage> receiveFileLockResponseTopic = mock(ITopic.class);
    private final ITopic<String> sendFileUnlockRequstTopic = mock(ITopic.class);
    private final ITopic<String> receiveFileUnlockResponseTopic = mock(ITopic.class);



}
