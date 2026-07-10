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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

/**
 * Entry maintains and stores the key and the content represented by that key. Entries should be
 * created as part of a [NavDisplay.entryProvider](reference/androidx/navigation/NavDisplay).
 *
 * @param T the type of the key for this NavEntry
 * @param key key for this entry
 * @param contentKey A unique, stable id that
 *     1. uniquely identifies the content of this NavEntry
 *     2. uniquely identifies any [NavEntryDecorator] states associated with this NavEntry.
 *
 *    NavEntries that share the same contentKey will be handled as sharing the same content and/or
 *    [NavEntryDecorator] state. To maximize stability, it should be derived from the [key]. The
 *    contentKey type must be saveable (i.e. on Android, it should be saveable via Android).
 *    Defaults to [key].toString().
 *
 * @param metadata provides information to the display
 * @param content content for this entry to be displayed when this entry is active
 */
@Immutable
public class NavEntry<T : Any>(
    private val key: T,
    public val contentKey: Any = defaultContentKey(key),
    public val metadata: Map<String, Any> = emptyMap(),
    private val content: @Composable (T) -> Unit,
) {
    /**
     * NavEntry constructor to create a NavEntry from another NavEntry
     *
     * @param navEntry The entry that provides the [key], [contentKey], and [metadata] for the new
     *   entry.
     * @param content content for this entry to be displayed when this entry is active
     */
    public constructor(
        navEntry: NavEntry<T>,
        content: @Composable (T) -> Unit,
    ) : this(navEntry.key, navEntry.contentKey, navEntry.metadata, content)

    /**
     * Invokes the composable content of this NavEntry with the key that was provided when
     * instantiating this NavEntry
     */
    @Composable
    public fun Content() {
        this.content(key)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as NavEntry<*>

        return key == other.key &&
            contentKey == other.contentKey &&
            metadata == other.metadata &&
            content === other.content
    }

    override fun hashCode(): Int {
        return key.hashCode() * 31 +
            contentKey.hashCode() * 31 +
            metadata.hashCode() * 31 +
            content.hashCode() * 31
    }

    override fun toString(): String {
        return "NavEntry(key=$key, contentKey=$contentKey, metadata=$metadata, content=$content)"
    }
}

@PublishedApi internal fun defaultContentKey(key: Any): Any = key.toString()
