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
package ch.sourcepond.io.fssync.common;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static java.lang.String.format;
import static java.lang.reflect.Proxy.newProxyInstance;
import static java.util.Objects.requireNonNull;
import static org.osgi.framework.Constants.OBJECTCLASS;

public class CompoundServiceFactory {

    private static Set<Method> validateMethods(final Class<?> pInterface) {
        final Set<Method> methods = new HashSet<>();
        for (final Method method : pInterface.getMethods()) {
            if (!void.class.equals(method.getReturnType())) {
                throw new IllegalArgumentException(format("Only void methods are permitted, illegal method: %s", method));
            }
            addIfIOExceptionDeclared(methods, method);
        }
        return methods;
    }

    private static void addIfIOExceptionDeclared(final Set<Method> pMethods, final Method pMethod) {
        for (final Class<?> exceptionType : pMethod.getExceptionTypes()) {
            if (IOException.class.equals(exceptionType)) {
                pMethods.add(pMethod);
                break;
            }
        }
    }

    public <T> T create(final BundleContext pContext,
                        final ExecutorService pExecutor,
                        final Class<T> pInterface) {
        requireNonNull(pContext, "Bundle-Context is null");
        requireNonNull(pExecutor, "Executor is null");
        requireNonNull(pInterface, "Interface is null");
        if (!pInterface.isInterface()) {
            throw new IllegalArgumentException(format("%s is not an interface", pInterface.getName()));
        }
        final Set<Method> methodsWithIOException = validateMethods(pInterface);
        final CompoundServiceHandler<T> handler = new CompoundServiceHandler<>(pContext, pExecutor, methodsWithIOException);

        final String filter = format("(%s=%s)", OBJECTCLASS, pInterface.getName());
        try {
            pContext.addServiceListener(handler, filter);
            final ServiceReference<?>[] references = pContext.getServiceReferences(pInterface.getName(), filter);
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
