/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.appsearch.localstorage.util;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.util.Map;

/**
 * A utility class to avoid the clutter of checking whether or not {@link Map#getOrDefault} is
 * available.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class MapUtil {
    private MapUtil() {}

    /**
     * @return the value in map for key, or defaultValue if key is not present in map.
     */
    public static <K, V> V getOrDefault(@NonNull Map<K, V> map, K key, V defaultValue) {
        V value = map.get(key);
        return (value != null) ? value : defaultValue;
    }
}
