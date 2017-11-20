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
package ch.sourcepond.io.distributor.impl;

import ch.sourcepond.io.distributor.api.GlobalPath;
import ch.sourcepond.io.distributor.impl.response.StatusResponse;
import org.mockito.ArgumentMatcher;

import java.io.IOException;

public class Constants {
    public static final String EXPECTED_NODE = "someNode";
    public static final String EXPECTED_PATH = "somePath";
    public static final IOException EXPECTED_EXCEPTION = new IOException();
    public static final ArgumentMatcher<GlobalPath> GLOBAL_PATH_ARGUMENT_MATCHER = globalPath -> {
        return EXPECTED_NODE.equals(globalPath.getSendingNode()) && EXPECTED_PATH.equals(EXPECTED_PATH);
    };
    public static final ArgumentMatcher<StatusResponse> SUCCESS_RESPONSE_ARGUMENT_MATCHER = response -> {
        return EXPECTED_PATH.equals(response.getPath()) && response.getFailureOrNull() == null;
    };
    public static final ArgumentMatcher<StatusResponse> FAILURE_RESPONSE_ARGUMENT_MATCHER = response -> {
        return EXPECTED_PATH.equals(response.getPath()) && EXPECTED_EXCEPTION.equals(response.getFailureOrNull());
    };
}
