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
import ch.sourcepond.io.fssync.target.api.SyncPath;
import ch.sourcepond.io.fssync.target.api.SyncTarget;
import com.hazelcast.core.HazelcastInstance;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.Objects.requireNonNull;

public class SyncTargets implements ServiceListener {

    @FunctionalInterface
    private interface TargetFunction {

        void process(NodeInfo pNodeInfo, SyncPath pPath, SyncTarget pTarget) throws IOException;
    }

    private final ConcurrentMap<ServiceReference<SyncTarget>, SyncTarget> targets = new ConcurrentHashMap<>();
    private final ConcurrentMap<SyncPath, Collection<SyncTarget>> lockedTargets = new ConcurrentHashMap<>();
    private final HazelcastInstance hci;

    SyncTargets(final HazelcastInstance pHci) {
        hci = pHci;
    }

    @Override
    public void serviceChanged(final ServiceEvent pServiceEvent) {
        final ServiceReference<SyncTarget> reference = (ServiceReference<SyncTarget>) pServiceEvent.getServiceReference();

        switch (pServiceEvent.getType()) {
            case ServiceEvent.UNREGISTERING:
            case ServiceEvent.MODIFIED_ENDMATCH: {
                targets.remove(reference);
                break;
            }
            case ServiceEvent.REGISTERED: {
                registerService(reference);
                break;
            }
            default: {
                // noop
            }
        }
    }

    void registerService(ServiceReference<SyncTarget> reference) {
        final BundleContext context = reference.getBundle().getBundleContext();
        final SyncTarget service = context.getService(reference);
        targets.put(reference, service);
    }

    private void process(final NodeInfo pNodeInfo, final SyncPath pPath, final TargetFunction pTargetFunction) throws IOException {
        try {
            requireNonNull(lockedTargets.get(pPath), "Sync has not been locked").forEach(t -> {
                try {
                    pTargetFunction.process(pNodeInfo, pPath, t);
                } catch (final IOException e) {
                    throw new UncheckedIOException(e.getMessage(), e);
                }
            });
        } catch (final UncheckedIOException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    public void lock(final NodeInfo pNodeInfo, final SyncPath pPath) throws IOException {
        lockedTargets.computeIfAbsent(pPath, p -> new CopyOnWriteArrayList<>(targets.values()));
        process(pNodeInfo, pPath, (nodeInfo, syncPath, syncTarget) -> syncTarget.lock(nodeInfo, syncPath));
    }

    public void unlock(final NodeInfo pNodeInfo, final SyncPath pPath) throws IOException {
        try {
            process(pNodeInfo, pPath, (nodeInfo, syncPath, syncTarget) -> syncTarget.unlock(nodeInfo, syncPath));
        } finally {
            lockedTargets.remove(pPath);
        }
    }

    public void delete(final NodeInfo pNodeInfo, final SyncPath pPath) throws IOException {
        process(pNodeInfo, pPath, (nodeInfo, syncPath, syncTarget) -> syncTarget.delete(nodeInfo, syncPath));
    }

    public void transfer(final NodeInfo pNodeInfo, final SyncPath pPath, final ByteBuffer pBuffer) throws IOException {
        process(pNodeInfo, pPath, (nodeInfo, syncPath, syncTarget) -> syncTarget.transfer(nodeInfo, syncPath, pBuffer));
    }

    public void discard(final NodeInfo pNodeInfo, final SyncPath pPath, final IOException pFailure) throws IOException {
        process(pNodeInfo, pPath, (nodeInfo, syncPath, syncTarget) -> syncTarget.discard(nodeInfo, syncPath, pFailure));
    }

    public void store(final NodeInfo pNodeInfo, final SyncPath pPath) throws IOException {
        process(pNodeInfo, pPath, (nodeInfo, syncPath, syncTarget) -> syncTarget.store(nodeInfo, syncPath));
    }

    public void cancel(final NodeInfo pNodeInfo) {
        lockedTargets.values().forEach(l -> l.forEach(a -> a.cancel(pNodeInfo)));
    }
}
