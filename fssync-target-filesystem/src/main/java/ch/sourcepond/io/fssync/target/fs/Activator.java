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

import ch.sourcepond.io.fssync.common.BaseActivator;
import ch.sourcepond.io.fssync.target.api.SyncTarget;
import ch.sourcepond.osgi.cmpn.metatype.ConfigBuilderFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.ManagedServiceFactory;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.lang.String.format;
import static org.osgi.framework.Constants.SERVICE_PID;

public class Activator extends BaseActivator<TargetDirectory, Config> {
    static final String FACTORY_PID = "ch.sourcepond.io.fssync.target.fs.factory";
    private final TargetDirectoryFactory targetDirectoryFactory;

    public Activator() {
        this(new ConfigBuilderFactory(), new TargetDirectoryFactory());
    }

    // Constructor for testing
    Activator(final ConfigBuilderFactory pConfigBuilderFactory, final TargetDirectoryFactory pTargetDirectoryFactory) {
        super(pConfigBuilderFactory);
        targetDirectoryFactory = pTargetDirectoryFactory;
    }

    @Override
    protected String getFactoryPid() {
        return FACTORY_PID;
    }

    @Override
    protected String getUniqueIdName() {
        return "syncDir";
    }

    @Override
    protected String getUniqueId(Config pConfig) {
        return pConfig.syncDir();
    }

    @Override
    protected Class<Config> getConfigAnnotation() {
        return Config.class;
    }

    @Override
    protected String getServiceInterfaceName() {
        return SyncTarget.class.getName();
    }

    @Override
    protected TargetDirectory createService(Config pConfig) {
        return targetDirectoryFactory.create();
    }
}
