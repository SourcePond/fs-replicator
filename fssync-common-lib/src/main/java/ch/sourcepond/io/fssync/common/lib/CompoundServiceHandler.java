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
package ch.sourcepond.io.fssync.common.lib;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static org.osgi.framework.ServiceEvent.MODIFIED_ENDMATCH;
import static org.osgi.framework.ServiceEvent.REGISTERED;
import static org.osgi.framework.ServiceEvent.UNREGISTERING;

class CompoundServiceHandler<T> implements ServiceListener, InvocationHandler {
    private final ConcurrentMap<ServiceReference<T>, T> targets = new ConcurrentHashMap<>();
    private final BundleContext context;
    private final ExecutorService executor;
    private final Set<Method> methodsWithIOException;

    public CompoundServiceHandler(final BundleContext pContext,
                                  final ExecutorService pExecutor,
                                  final Set<Method> pMethodsWithIOException) {
        context = pContext;
        executor = pExecutor;
        methodsWithIOException = pMethodsWithIOException;
    }

    @Override
    public void serviceChanged(final ServiceEvent pServiceEvent) {
        final ServiceReference<T> reference = (ServiceReference<T>) pServiceEvent.getServiceReference();

        switch (pServiceEvent.getType()) {
            case UNREGISTERING:
            case MODIFIED_ENDMATCH: {
                targets.remove(reference);
                context.ungetService(reference);
                break;
            }
            case REGISTERED: {
                registerService(reference);
                break;
            }
            default: {
                // noop
            }
        }
    }

    void registerService(final ServiceReference<T> pReference) {
        targets.computeIfAbsent(pReference, reference -> context.getService(reference));
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        final List<Future<?>> results = new LinkedList<>();
        StringBuilder failures = null;

        for (final T service : targets.values()) {
            results.add(executor.submit(() -> method.invoke(service, args)));
        }

        Throwable firstException = null;
        try {
            for (final Future<?> future : results) {
                future.get();
            }
        } catch (final ExecutionException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof InvocationTargetException) {
                final Throwable target = ((InvocationTargetException)cause).getTargetException();
                if (failures == null) {
                    failures = new StringBuilder("At least one exception occurred (only the first one is visible in stacktrace)!\n");
                    firstException = target;
                }
                failures.append("\t - ").append(target.getMessage()).append('\n');
            } else {
                throw cause;
            }
        }

        if (firstException != null) {
            if (methodsWithIOException.contains(method)) {
                throw new IOException(failures.toString(), firstException);
            } else {
                throw firstException;
            }
        }

        // Only void methods allowed, so it's safe to simply return null
        return null;
    }
}
