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

package androidx.navigation3.runtime

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable

/**
 * Entry maintains and stores the key and the content represented by that key. Entries should be
 * created as part of a [NavDisplay.entryProvider](reference/androidx/navigation/NavDisplay).
 *
 * @param T the type of the key for this NavEntry
 * @param key key for this entry
 * @param contentKey A unique, stable id that uniquely identifies the content of this NavEntry. To
 *   maximize stability, it should be derived from the [key]. The contentKey type must be saveable
 *   (i.e. on Android, it should be saveable via Android). Defaults to [key].toString().
 * @param metadata provides information to the display
 * @param content content for this entry to be displayed when this entry is active
 */
public open class NavEntry<T : Any>(
    private val key: T,
    public val contentKey: Any = defaultContentKey(key),
    public open val metadata: Map<String, Any> = emptyMap(),
    private val content: @Composable (T) -> Unit,
) {
    /** Allows creating a NavEntry from another NavEntry while keeping [content] field private */
    internal constructor(
        navEntry: NavEntry<T>
    ) : this(navEntry.key, navEntry.contentKey, navEntry.metadata, navEntry.content)

    /**
     * Invokes the composable content of this NavEntry with the key that was provided when
     * instantiating this NavEntry
     */
    @Composable
    public open fun Content() {
        this.content(key)
    }

    /**
     * Returns true if this NavEntry is in the [backStack], false otherwise.
     *
     * @param [backStack] the backStack to check if it contains this NavEntry.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public fun isInBackStack(backStack: List<Any>): Boolean = backStack.contains(this.key)
}

@PublishedApi internal fun defaultContentKey(key: Any): Any = key.toString()
