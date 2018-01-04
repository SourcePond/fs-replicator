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
package ch.sourcepond.io.fssync.distributor.hazelcast.config;

import ch.sourcepond.io.fssync.distributor.hazelcast.annotations.Delete;
import ch.sourcepond.io.fssync.distributor.hazelcast.annotations.Discard;
import ch.sourcepond.io.fssync.distributor.hazelcast.annotations.Lock;
import ch.sourcepond.io.fssync.distributor.hazelcast.annotations.Response;
import ch.sourcepond.io.fssync.distributor.hazelcast.annotations.Store;
import ch.sourcepond.io.fssync.distributor.hazelcast.annotations.Transfer;
import ch.sourcepond.io.fssync.distributor.hazelcast.annotations.Unlock;

public class DefaultDistributorConfigManagerTest extends DistributorConfigManagerTest {

    @Override
    protected ExpectedValues expectedValues() {
        final ExpectedValues values = new ExpectedValues();

        values.expectedPort = 6701;
        values.expectedPortAutoIncrement = true;
        values.expectedPortCount = 100;
        values.expectedOutboundDefinitions = new String[]{"*"};
        values.expectedMulticastEnabled = true;
        values.expectedMulticastGroup = "224.2.2.3";
        values.expectedMulticastPort = 54327;
        values.expectedMulitcastTimeToLive = 32;
        values.expectedMulticastTimeoutSeconds = 2;
        values.expectedTcpipEnabled = false;
        values.expectedTcpipMembers = new String[0];

        final ExpectedTopicValues responseTopicValues = new ExpectedTopicValues();
        responseTopicValues.expectedReadBatchSize = 50;
        responseTopicValues.expectedStatisticsEnabled = false;
        responseTopicValues.expectedAsyncBackupCount = 0;
        responseTopicValues.expectedBackupCount = 1;
        responseTopicValues.expectedCapacity = 2000;
        responseTopicValues.expectedName = Response.NAME;
        responseTopicValues.expectedTimeToLiveSeconds = 300;

        final ExpectedTopicValues deleteTopicValues = responseTopicValues.clone();
        deleteTopicValues.expectedName = Delete.NAME;

        final ExpectedTopicValues transferTopicValues = responseTopicValues.clone();
        deleteTopicValues.expectedName = Transfer.NAME;

        final ExpectedTopicValues discardTopicValues = responseTopicValues.clone();
        deleteTopicValues.expectedName = Discard.NAME;

        final ExpectedTopicValues storeTopicValues = responseTopicValues.clone();
        deleteTopicValues.expectedName = Store.NAME;

        final ExpectedTopicValues lockTopicValues = responseTopicValues.clone();
        deleteTopicValues.expectedName = Lock.NAME;

        final ExpectedTopicValues unlockTopicValues = responseTopicValues.clone();
        deleteTopicValues.expectedName = Unlock.NAME;

        values.expectedTopicValues.put(Response.NAME, responseTopicValues);
        values.expectedTopicValues.put(Delete.NAME, deleteTopicValues);
        values.expectedTopicValues.put(Transfer.NAME, transferTopicValues);
        values.expectedTopicValues.put(Discard.NAME, discardTopicValues);
        values.expectedTopicValues.put(Store.NAME, storeTopicValues);
        values.expectedTopicValues.put(Lock.NAME, lockTopicValues);
        values.expectedTopicValues.put(Unlock.NAME, unlockTopicValues);

        return values;
    }
}
