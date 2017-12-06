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
package ch.sourcepond.io.fssync.distributor.hazelcast.lock;

import ch.sourcepond.io.fssync.distributor.api.LockException;
import ch.sourcepond.io.fssync.distributor.hazelcast.Config;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.String.format;

class Locks {
    private final Lock lock = new ReentrantLock();
    private final Condition shutdownDone = lock.newCondition();
    private final Map<String, ILock> locks = new HashMap<>();
    private final HazelcastInstance hci;
    private final Config config;
    private boolean shutdown;

    @Inject
    public Locks(final HazelcastInstance pHci, final Config pConfig) {
        hci = pHci;
        config = pConfig;
    }

    public boolean tryLock(final String pKey) throws LockException, InterruptedException {
        lock.lock();
        try {
            if (shutdown) {
                throw new LockException(format("Service is shutting down, lock for %s could not be acquired!", pKey));
            }

            final ILock globalLock = hci.getLock(pKey);
            if (globalLock.tryLock(config.lockTimeout(),
                    config.lockTimeoutUnit(),
                    config.leaseTime(),
                    config.leaseTimeUnit())) {
                locks.put(pKey, globalLock);
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    public void unlock(final String pKey) {
        lock.lock();
        try {
            final ILock globalLock = locks.remove(pKey);
            if (globalLock != null) {
                globalLock.unlock();
            }
        } finally {
            shutdownDone.signalAll();
            lock.unlock();
        }
    }

    public void shutdown() throws InterruptedException {
        lock.lock();
        try {
            shutdown = true;
            while (!locks.isEmpty()) {
                shutdownDone.await();
            }
        } finally {
            lock.unlock();
        }
    }
}
