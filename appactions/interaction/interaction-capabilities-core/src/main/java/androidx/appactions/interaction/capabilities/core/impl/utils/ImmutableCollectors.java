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

package androidx.appactions.interaction.capabilities.core.impl.utils;

import static java.util.stream.Collectors.collectingAndThen;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Immutable collectors without guava dependencies. Collectors.toUnmodifiable*() function calls
 * should not be used because they are only available on API 33+.
 */
public final class ImmutableCollectors {

    private ImmutableCollectors() {
    }

    /** Collecting to immutable list. */
    @NonNull
    public static <E> Collector<E, ?, List<E>> toImmutableList() {
        return collectingAndThen(Collectors.toList(), Collections::unmodifiableList);
    }

    /** Collecting to immutable set. */
    @NonNull
    public static <E> Collector<E, ?, Set<E>> toImmutableSet() {
        return collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet);
    }

    /** Collecting to immutable map. */
    @NonNull
    public static <T, K, V> Collector<T, ?, Map<K, V>> toImmutableMap(
            @NonNull Function<? super T, ? extends K> keyFunction,
            @NonNull Function<? super T, ? extends V> valueFunction) {
        return collectingAndThen(
                Collectors.toMap(keyFunction, valueFunction), Collections::unmodifiableMap);
    }
}
