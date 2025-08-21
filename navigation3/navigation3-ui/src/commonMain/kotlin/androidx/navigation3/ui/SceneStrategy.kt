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

package androidx.navigation3.ui

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry

/**
 * A strategy that tries to calculate a [Scene] given a list of [NavEntry].
 *
 * If the list of [NavEntry] does not result in a [Scene] for this strategy, `null` will be returned
 * instead to delegate to another strategy.
 */
public fun interface SceneStrategy<T : Any> {
    /**
     * Given a back stack of [entries], calculate whether this [SceneStrategy] should take on the
     * task of rendering one or more of those entries.
     *
     * By returning a non-null [Scene], your [Scene] takes on the responsibility of rendering the
     * set of entries you declare in [Scene.entries]. If you return `null`, the next available
     * [SceneStrategy] will be called.
     *
     * @param entries The entries on the back stack that should be considered valid to render via a
     *   returned Scene.
     * @param onBack a callback that should be connected to any internal handling of system back
     *   done by the returned [Scene]. The passed [Int] should be the number of entries were popped.
     */
    @Composable
    public fun calculateScene(entries: List<NavEntry<T>>, onBack: (count: Int) -> Unit): Scene<T>?

    /**
     * Chains this [SceneStrategy] with another [sceneStrategy] to return a combined
     * [SceneStrategy].
     */
    public infix fun then(sceneStrategy: SceneStrategy<T>): SceneStrategy<T> =
        SceneStrategy { entries, onBack ->
            calculateScene(entries, onBack) ?: sceneStrategy.calculateScene(entries, onBack)
        }
}
