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
package ch.sourcepond.io.distributor.impl;

import ch.sourcepond.io.distributor.api.exception.DeletionException;
import ch.sourcepond.io.distributor.api.Distributor;
import ch.sourcepond.io.distributor.api.exception.LockException;
import ch.sourcepond.io.distributor.api.exception.ModificationException;
import ch.sourcepond.io.distributor.api.exception.StoreException;
import ch.sourcepond.io.distributor.api.exception.UnlockException;
import ch.sourcepond.io.distributor.impl.lock.LockManager;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

public class DistributorImpl implements Distributor {

    @Override
    public void lock(String pPath) throws LockException {

    }

    @Override
    public void unlock(String pPath) throws UnlockException {

    }

    @Override
    public void delete(String pPath) throws DeletionException {

    }

    @Override
    public void transfer(String pPath, ByteBuffer pData) throws ModificationException {

    }

    @Override
    public void store(String pPath, byte[] pChecksum) throws StoreException {

    }

    @Override
    public String getLocalNode() {
        return null;
    }

    @Override
    public byte[] getChecksum(String pPath) {
        return new byte[0];
    }
}
