/*
 * Copyright 2026 The Android Open Source Project
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
package androidx.xr.compose.subspace.semantics

import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.getOrNull

/** Configuration of semantics properties for a spatial node. */
public class SubspaceSemanticsConfiguration
internal constructor(private val config: SemanticsConfiguration) :
    SubspaceSemanticsPropertyReceiver, Iterable<Map.Entry<SemanticsPropertyKey<*>, Any?>> {
    /** Returns true if the configuration contains the given [key]. */
    public operator fun <T> contains(key: SemanticsPropertyKey<T>): Boolean = key in config

    /** Returns the value for the given [key] if present, or the result of [defaultValue] if not. */
    public fun <T> getOrElseNullable(key: SemanticsPropertyKey<T>, defaultValue: () -> T?): T? =
        config.getOrElseNullable(key, defaultValue)

    /** Returns the value for the given [key] if present, or the result of [defaultValue] if not. */
    public fun <T> getOrElse(key: SemanticsPropertyKey<T>, defaultValue: () -> T): T =
        config.getOrElse(key, defaultValue)

    /**
     * Returns the value for the given [key] if present, or throws [IllegalStateException] if not.
     */
    public operator fun <T> get(key: SemanticsPropertyKey<T>): T = config[key]

    /** Returns the value for the given [key] if present, or null if not. */
    public fun <T> getOrNull(key: SemanticsPropertyKey<T>): T? = config.getOrNull(key)

    override operator fun <T> set(key: SemanticsPropertyKey<T>, value: T) {
        config[key] = value
    }

    override fun iterator(): Iterator<Map.Entry<SemanticsPropertyKey<*>, Any?>> = config.iterator()
}
