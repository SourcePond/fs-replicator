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

import ch.sourcepond.io.distributor.api.DeletionException;
import ch.sourcepond.io.distributor.api.Distributor;
import ch.sourcepond.io.distributor.api.ModificationException;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

public class DistributorImpl implements Distributor {

    @Override
    public void lockGlobally(String pPath, TimeUnit pTimeoutUnit, long pTimeout) {

    }

    @Override
    public void unlockGlobally(String pPath, TimeUnit pTimeoutUnit, long pTimeout) {

    }

    @Override
    public void delete(String pPath) throws DeletionException {

    }

    @Override
    public void modify(String pPath, ByteBuffer pData) throws ModificationException {

    }

    @Override
    public String getLocalNode() {
        return null;
    }

    @Override
    public byte[] getGlobalChecksum(String pFile) {
        return new byte[0];
    }
}
