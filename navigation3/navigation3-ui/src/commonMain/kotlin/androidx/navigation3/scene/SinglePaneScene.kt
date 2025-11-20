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

package androidx.navigation3.scene

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry

internal data class SinglePaneScene<T : Any>(
    override val key: Any,
    val entry: NavEntry<T>,
    override val previousEntries: List<NavEntry<T>>,
) : Scene<T> {
    override val entries: List<NavEntry<T>> = listOf(entry)

    override val content: @Composable () -> Unit = { entry.Content() }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as SinglePaneScene<*>

        return key == other.key &&
            entry == other.entry &&
            previousEntries == other.previousEntries &&
            entries == other.entries
    }

    override fun hashCode(): Int {
        return key.hashCode() * 31 +
            entry.hashCode() * 31 +
            previousEntries.hashCode() * 31 +
            entries.hashCode() * 31
    }

    override fun toString(): String {
        return "SinglePaneScene(key=$key, entry=$entry, previousEntries=$previousEntries, entries=$entries)"
    }
}

/**
 * A [SceneStrategy] that always creates a 1-entry [Scene] simply displaying the last entry in the
 * list.
 */
public class SinglePaneSceneStrategy<T : Any> : SceneStrategy<T> {

    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T> {
        return SinglePaneScene(
            key = entries.last().contentKey,
            entry = entries.last(),
            previousEntries = entries.dropLast(1),
        )
    }
}

internal fun <T : Any> SceneStrategy<T>.calculateSceneWithSinglePaneFallback(
    scope: SceneStrategyScope<T>,
    entries: List<NavEntry<T>>,
): Scene<T> {
    return scope.calculateScene(entries)
        ?: with(SinglePaneSceneStrategy<T>()) { scope.calculateScene(entries) }
}
