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
package ch.sourcepond.io.fssync.target.fs;

import ch.sourcepond.io.fssync.distributor.api.Distributor;
import ch.sourcepond.io.fssync.distributor.api.GlobalPath;
import ch.sourcepond.io.fssync.distributor.spi.Client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

final class IgnoreLocalNodeHandler implements InvocationHandler {
    private final Distributor distributor;
    private final Client client;

    public IgnoreLocalNodeHandler(final Distributor pDistributor, final Client pClient) {
        distributor = pDistributor;
        client = pClient;
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        final Class<?> valueClass = args.length > 0 ? args[0].getClass() : null;
        if (GlobalPath.class.equals(valueClass)) {
            final GlobalPath globalPath = (GlobalPath) args[0];

            // This is important: do only trigger file operations on the original client if
            // they were NOT triggered locally!
            if (!distributor.getLocalNode().equals(globalPath.getSendingNode())) {
                return method.invoke(client, args);
            }
        }
        return method.invoke(client, args);
    }
}
