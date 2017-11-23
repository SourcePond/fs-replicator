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
package ch.sourcepond.io.distributor.impl.binding;

import java.util.Map;
import java.util.function.Function;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

final class Validations {

    private static <T> T validateType(final String pKey, final String pValue, final Function<String, T> pConverter) {
        try {
            return pConverter.apply(pValue);
        } catch (final Exception e) {
            throw new IllegalArgumentException(format("Value of property with key %s could not be converted!", pKey), e);
        }
    }

    public static String same(final String pValue) {
        return pValue;
    }

    public static <T> T optional(final String pKey, final T pDefault, final Map<String, String> pValues, final Function<String, T> pConverter) {
        if (pValues != null) {
            return validateType(pKey, pValues.get(pKey), pConverter);
        }
        return pDefault;
    }

    public static <T> T mandatory(final String pKey, final Map<String, String> pProperties, final Function<String, T> pConverter) {
        return validateType(pKey, requireNonNull(pProperties.get(pKey), format("No property set with key %s", pKey)), pConverter);
    }
}
