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
package ch.sourcepond.io.fssync.source.fs;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static ch.sourcepond.io.fssync.source.fs.Activator.FACTORY_PID;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 *
 */
@ObjectClassDefinition(name = "File-system synchronization configuration",
        description = "Service configuration definition",
        factoryPid = {FACTORY_PID})
public @interface Config {

    @AttributeDefinition(description = "You can specify how many threads shall be used when checksum are calculated.",
            min = "1")
    int checksumConcurrency() default 3;

    @AttributeDefinition(min = "0")
    long retryDelay() default 5;

    @AttributeDefinition
    TimeUnit retryDelayUnit() default SECONDS;

    @AttributeDefinition(min = "1")
    int retryAttempts() default 5;

    @AttributeDefinition
    String watchedDirectory();

    @AttributeDefinition
    int readBufferSize() default 1024;

    @AttributeDefinition(min = "1")
    int triggerConcurrency() default 3;
}
