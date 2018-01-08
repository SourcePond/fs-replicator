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

import ch.sourcepond.osgi.cmpn.metatype.ConfigBuilderFactory;

public class TestActivator extends BaseActivator<DefaultTestService, TestConfig> {
    static final String FACTORY_PID = "someFactoryPid";
    private final DefaultTestService service;

    public TestActivator(final ConfigBuilderFactory pConfigBuilderFactory, final DefaultTestService pService) {
        super(pConfigBuilderFactory);
        service = pService;
    }

    @Override
    protected String getFactoryPid() {
        return FACTORY_PID;
    }

    @Override
    protected String getUniqueIdName() {
        return "someString";
    }

    @Override
    protected String getUniqueId(final TestConfig pConfig) {
        return pConfig.someString();
    }

    @Override
    protected Class<TestConfig> getConfigAnnotation() {
        return TestConfig.class;
    }

    @Override
    protected String getServiceInterfaceName() {
        return TestService.class.getName();
    }

    @Override
    protected DefaultTestService createService(final TestConfig pConfig) {
        return service;
    }
}
