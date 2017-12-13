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
package ch.sourcepond.io.fssync.impl;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 *
 */
@ObjectClassDefinition(name = "Filesystem synchronization configuration", description = "Service configuration definition")
public @interface Config {

    @AttributeDefinition(
            min = "1",
            name = "Forice unlock timeout seconds",
            description = "Duration to wait until a file lock is forced to be released"
    )
    long getForceUnlockTimeoutSeconds();

    long retryDelay() default 500;

    TimeUnit retryDelayUnit() default MILLISECONDS;

    String[] ignoreFilePatterns() default {"\\.transfer_[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.tmp"};

    int retryAttempts();
}
