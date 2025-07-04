/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.camera.viewfinder.core.impl.quirk

import androidx.annotation.VisibleForTesting

/** Wraps a list of [Quirk]s, allowing to easily retrieve a [Quirk] instance by its class. */
internal class Quirks(quirks: List<Quirk>) {
    private val quirks: MutableList<Quirk> = quirks.toMutableList()

    /**
     * Retrieves a [Quirk] instance given its type.
     *
     * Unlike [.contains], a quirk can only be retrieved by the exact class. If a superclass or
     * superinterface is provided, `null` will be returned, even if a quirk with the provided
     * superclass or superinterface exists in this collection.
     *
     * @return A [Quirk] instance of the provided type, or `null` if it isn't found.
     */
    inline fun <reified T : Quirk> get(): T? = quirks.firstOrNull { it::class == T::class } as? T

    /**
     * Returns whether this collection of quirks contains a quirk with the provided type.
     *
     * This checks whether the provided quirk type is the exact class, a superclass, or a
     * superinterface of any of the contained quirks, and will return true in all cases.
     *
     * @return `true` if this container contains a quirk with the given type, `false` otherwise.
     */
    inline fun <reified T : Quirk> contains(): Boolean = quirks.any { it is T }

    @VisibleForTesting
    fun reset(quirks: List<Quirk>) {
        this.quirks.clear()
        this.quirks.addAll(quirks)
    }
}
