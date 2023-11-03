/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.wear.protolayout.expression;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Represent a {@link DynamicDataKey} that references real-time data from the platform.
 *
 * <p>The [namespace, key] tuple creates the actual reference, so that a single key can refer to two
 * different sources in two different namespaces.
 *
 * <p>The namespace must not be empty. Additionally, the "protolayout" namespace (and its lowercase
 * and uppercase variations) are reserved for the default platform data sources and should not be
 * used for any custom OEM data source. To make sure namespaces are unique, any custom namespace is
 * expected to follow Java style naming {@code com.company.foo}.
 *
 * @param <T> The data type of the dynamic values that this key is bound to.
 */
public final class PlatformDataKey<T extends DynamicBuilders.DynamicType>
        extends DynamicDataKey<T> {
    @NonNull private static final String RESERVED_NAMESPACE = "protolayout";

    /**
     * Create a {@link PlatformDataKey} with the specified key in the given namespace.
     *
     * @param namespace The namespace of the key for the platform data source.
     * @param key The key that references the platform data source.
     */
    public PlatformDataKey(@NonNull String namespace, @NonNull String key) {
        super(namespace, key);
        if (namespace.isEmpty()) {
            throw new IllegalArgumentException("Custom data source namespace must not be empty.");
        }

        if (RESERVED_NAMESPACE.equalsIgnoreCase(namespace)) {
            throw new IllegalArgumentException(
                    String.format(
                            "Custom data source must not use the reserved namespace:%s",
                            RESERVED_NAMESPACE));
        }
    }

    /**
     * Create a {@link PlatformDataKey} with the specified key in the reserved namespace. This
     * should only be used by protolayout library internally for default platform data sources.
     *
     * @param key The key that references the platform data source
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public PlatformDataKey(@NonNull String key) {
        super(RESERVED_NAMESPACE, key);
    }
}
