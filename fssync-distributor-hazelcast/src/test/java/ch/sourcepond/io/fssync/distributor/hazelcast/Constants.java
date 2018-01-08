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
package ch.sourcepond.io.fssync.distributor.hazelcast;

import ch.sourcepond.io.fssync.target.api.NodeInfo;
import org.mockito.ArgumentMatcher;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MINUTES;

public class Constants {
    public static final long EXPECTED_LOCK_TIMEOUT = 5L;
    public static final TimeUnit EXPECTED_LEASE_TIME_UNIT = MINUTES;
    public static final long EXPECTED_LEASE_TIME = 15;
    public static final TimeUnit EXPECTED_LOCK_TIMEOUT_UNIT = MINUTES;
    public static final String EXPECTED_SENDER_NODE = "someSenderNode";
    public static final String EXPECTED_LOCAL_NODE = "someLocalNode";
    public static final byte[] EXPECTED_DATA = new byte[]{1, 2, 3, 4, 5};
    public static final IOException EXPECTED_EXCEPTION = new IOException();
    public static final ArgumentMatcher<NodeInfo> IS_EQUAL_TO_EXPECTED_NODE_INFO = ni -> EXPECTED_SENDER_NODE.equals(ni.getSender()) && EXPECTED_LOCAL_NODE.equals(ni.getLocal());
}
