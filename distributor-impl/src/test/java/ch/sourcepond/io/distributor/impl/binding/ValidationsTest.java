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
package ch.sourcepond.io.distributor.impl.binding;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static ch.sourcepond.io.distributor.impl.binding.Validations.mandatory;
import static ch.sourcepond.io.distributor.impl.binding.Validations.optional;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ValidationsTest {
    private static final String UNKNOWN_KEY = "unknownKey";
    private static final String EXPECTED_KEY = "expectedKey";
    private static final String EXPECTED_VALUE = "value";
    private final Map<String, String> properties = new HashMap<>();

    @Before
    public void setup() {
        properties.put(EXPECTED_KEY, EXPECTED_VALUE);
    }

    @Test
    public void mandatoryNoPropertySet() {
        try {
            mandatory(UNKNOWN_KEY, properties, Integer::valueOf);
            fail("Exception expected");
        } catch (final NullPointerException e) {
            assertTrue(e.getMessage().contains(UNKNOWN_KEY));
        }
    }

    @Test
    public void mandatoryInvalidType() {
        try {
            mandatory(EXPECTED_KEY, properties, Integer::valueOf);
            fail("Exception expected");
        } catch (final IllegalArgumentException e) {
            assertTrue(e.getMessage().contains(EXPECTED_KEY));
        }
    }

    @Test
    public void verifyMandatory() {
        assertEquals(EXPECTED_VALUE, mandatory(EXPECTED_KEY, properties, Validations::same));
    }

    @Test
    public void optionalNoPropertySet() {
        assertEquals(EXPECTED_VALUE, optional(UNKNOWN_KEY, EXPECTED_VALUE, properties, Validations::same));
    }

    @Test
    public void optionalInvalidType() {
        try {
            optional(EXPECTED_KEY, EXPECTED_VALUE, properties, Integer::valueOf);
            fail("Exception expected");
        } catch (final IllegalArgumentException e) {
            assertTrue(e.getMessage().contains(EXPECTED_KEY));
        }
    }

    @Test
    public void verifyOptional() {
        assertEquals(EXPECTED_VALUE,optional(EXPECTED_KEY, "shouldNotBeApplied", properties, Validations::same));
    }
}
