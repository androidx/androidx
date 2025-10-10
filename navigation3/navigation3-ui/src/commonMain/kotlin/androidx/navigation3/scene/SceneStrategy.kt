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

import androidx.navigation3.runtime.NavEntry

/**
 * Scope used to create a [Scene] from a list of [NavEntry]s.
 *
 * This Scope should be provided to the [SceneStrategy.calculateScene] function to create Scenes.
 */
public class SceneStrategyScope<T : Any>(
    /**
     * A callback that should be connected to any internal handling of system back done by the
     * returned [Scene].
     *
     * For example, if your [Scene] uses a separate window that handles system back itself or if the
     * UI present in your [Scene] allows users to go back via a custom gesture or affordance, this
     * callback allows you to bubble up that event to the [SceneState] /
     * [androidx.navigation3.ui.NavDisplay] that interfaces with the developer owned back stack.
     *
     * @sample androidx.navigation3.scene.samples.SceneStrategyOnBackSample
     */
    public val onBack: () -> Unit = {}
)

/**
 * A strategy that tries to calculate a [Scene] given a list of [NavEntry].
 *
 * If the list of [NavEntry] does not result in a [Scene] for this strategy, `null` will be returned
 * instead to delegate to another strategy.
 */
public fun interface SceneStrategy<T : Any> {
    /**
     * Given a [SceneStrategyScope], calculate whether this [SceneStrategy] should take on the task
     * of rendering one or more of the entries in the scope.
     *
     * By returning a non-null [Scene], your [Scene] takes on the responsibility of rendering the
     * set of entries you declare in [Scene.entries]. If you return `null`, the next available
     * [SceneStrategy] will be called.
     *
     * @param entries The entries on the back stack that should be considered valid to render via a
     *   returned Scene.
     */
    public fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>?

    /**
     * Chains this [SceneStrategy] with another [sceneStrategy] to return a combined
     * [SceneStrategy].
     */
    public infix fun then(sceneStrategy: SceneStrategy<T>): SceneStrategy<T> =
        object : SceneStrategy<T> {
            override fun SceneStrategyScope<T>.calculateScene(
                entries: List<NavEntry<T>>
            ): Scene<T>? =
                calculateScene(entries) ?: with(sceneStrategy) { calculateScene(entries) }
        }
}
