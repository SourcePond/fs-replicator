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
package ch.sourcepond.io.fssync.compound;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;

import static java.lang.String.format;
import static java.lang.reflect.Proxy.newProxyInstance;
import static java.util.Objects.requireNonNull;
import static org.osgi.framework.Constants.OBJECTCLASS;

public class CompoundServiceFactory {
    private final BundleContext context;
    private final ExecutorService executor;

    public CompoundServiceFactory(final BundleContext pContext, final ExecutorService pExecutor) {
        context = requireNonNull(pContext, "Bundle-Context is null");
        executor = requireNonNull(pExecutor, "Executor is null");
    }

    private static void validateMethods(final Class<?> pInterface, final Class<?> pExceptionClass) {
        for (final Method method : pInterface.getMethods()) {
            if (!void.class.equals(method.getReturnType())) {
                throw new IllegalArgumentException(format("Only void methods are permitted, illegal method: %s", method));
            }
            boolean exceptionDeclared = false;
            for (final Class<?> exceptionType : method.getExceptionTypes()) {
                exceptionDeclared = exceptionType.isAssignableFrom(pExceptionClass);
                if (exceptionDeclared) {
                    break;
                }
            }
            if (!exceptionDeclared) {
                throw new IllegalArgumentException(format("Every method must throw %s, illegal method: %s",
                        pExceptionClass.getName(), method));
            }
        }
    }

    private static <E extends Throwable> Constructor<E> findConstructor(final Class<E> pExceptionClass) {
        try {
            return pExceptionClass.getConstructor(String.class);
        } catch (final NoSuchMethodException e) {
            throw new IllegalArgumentException(
                    format("%s must have a public constructor which takes exactly one string as argument",
                            pExceptionClass.getName()));
        }
    }

    public <T, E extends Throwable> T create(final Class<T> pInterface, final Class<E> pExceptionClass) {
        requireNonNull(pInterface, "Interface is null");
        if (!pInterface.isInterface()) {
            throw new IllegalArgumentException(format("%s is not an interface", pInterface.getName()));
        }
        validateMethods(pInterface, pExceptionClass);

        final CompoundServiceHandler<T, E> handler = new CompoundServiceHandler<>(context,
                findConstructor(pExceptionClass), executor);
        final String filter = format("(%s=%s)", OBJECTCLASS, pInterface.getName());
        try {
            context.addServiceListener(handler, filter);
            final ServiceReference<?>[] references = context.getServiceReferences(pInterface.getName(), filter);
            if (references != null) {
                for (final ServiceReference<?> reference : references) {
                    handler.registerService((ServiceReference<T>) reference);
                }
            }
        } catch (final InvalidSyntaxException e) {
            // This should never happen
            throw new IllegalStateException(e);
        }

        return (T) newProxyInstance(pInterface.getClassLoader(),
                new Class<?>[]{pInterface}, handler);
    }
}
