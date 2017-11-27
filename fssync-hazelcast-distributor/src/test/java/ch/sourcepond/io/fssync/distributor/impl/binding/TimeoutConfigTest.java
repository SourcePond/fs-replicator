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
package ch.sourcepond.io.fssync.distributor.impl.binding;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class TimeoutConfigTest {
    private static final TimeUnit EXPECTED_UNIT = SECONDS;
    private static final long EXPECTED_TIMEOUT = 1000L;
    private final TimeoutConfig config = new TimeoutConfig(EXPECTED_UNIT, EXPECTED_TIMEOUT);

    @Test
    public void getUnit() {
        assertSame(EXPECTED_UNIT, config.getUnit());
    }

    @Test
    public void getTimeout() {
        assertEquals(EXPECTED_TIMEOUT, config.getTimeout());
    }
}
