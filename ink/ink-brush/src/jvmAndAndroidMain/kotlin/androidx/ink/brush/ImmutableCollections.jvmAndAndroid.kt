/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.ink.brush

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
actual public object ImmutableCollections {
    actual public fun <T> unmodifiableList(size: Int, indexToValue: (Int) -> T): List<T> =
        java.util.Collections.unmodifiableList(List(size, indexToValue))

    actual public fun <T> unmodifiableList(list: List<T>): List<T> =
        // unmodifiableList is a read-only view, so this also needs a defensive copy.
        java.util.Collections.unmodifiableList(list.toList())

    actual public fun <T> unmodifiableSet(set: Set<T>): Set<T> =
        // unmodifiableSet is a read-only view, so this also needs a defensive copy.
        java.util.Collections.unmodifiableSet(set.toSet())
}
