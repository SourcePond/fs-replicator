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
package ch.sourcepond.io.fssync.distributor.api;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class ExceptionTest {
    private static final String EXPECTED_MESSAGE = "someMessage";
    private static final Exception EXPECTED_CAUSE = new Exception();

    private void verifyException(final Exception pException) {
        assertEquals(EXPECTED_MESSAGE, pException.getMessage());
        assertNull(pException.getCause());
    }

    private void verifyExceptionWithCause(final Exception pException) {
        assertEquals(EXPECTED_MESSAGE, pException.getMessage());
        assertSame(EXPECTED_CAUSE, pException.getCause());
    }

    @Test
    public void creationException() {
        verifyException(new CreationException(EXPECTED_MESSAGE));
        verifyExceptionWithCause(new CreationException(EXPECTED_MESSAGE, EXPECTED_CAUSE));
    }

    @Test
    public void deletionException() {
        verifyException(new DeletionException(EXPECTED_MESSAGE));
        verifyExceptionWithCause(new DeletionException(EXPECTED_MESSAGE, EXPECTED_CAUSE));
    }

    @Test
    public void storeException() {
        verifyException(new StoreException(EXPECTED_MESSAGE));
        verifyExceptionWithCause(new StoreException(EXPECTED_MESSAGE, EXPECTED_CAUSE));
    }

    @Test
    public void transferException() {
        verifyException(new TransferException(EXPECTED_MESSAGE));
        verifyExceptionWithCause(new TransferException(EXPECTED_MESSAGE, EXPECTED_CAUSE));
    }


    @Test
    public void lockException() {
        verifyException(new LockException(EXPECTED_MESSAGE));
        verifyExceptionWithCause(new LockException(EXPECTED_MESSAGE, EXPECTED_CAUSE));
    }

    @Test
    public void unlockException() {
        verifyException(new UnlockException(EXPECTED_MESSAGE));
        verifyExceptionWithCause(new UnlockException(EXPECTED_MESSAGE, EXPECTED_CAUSE));
    }
}
