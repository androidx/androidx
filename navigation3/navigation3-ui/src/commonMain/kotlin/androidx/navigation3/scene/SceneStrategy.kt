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

import androidx.compose.runtime.Immutable
import androidx.navigation3.runtime.NavEntry

/**
 * Scope used to create a [Scene] from a list of [NavEntry]s.
 *
 * This Scope should be provided to the [SceneStrategy.calculateScene] function to create Scenes.
 */
@Immutable
public open class SceneStrategyScope<T : Any>
internal constructor(
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
    public val onBack: () -> Unit
) {
    /**
     * Construct a [SceneStrategyScope] suitable for calling [SceneStrategy] functions in isolation.
     *
     * For more complicated cases, such as ones where you want to test if [onBack] is called
     * correctly, use [rememberSceneState], which will construct its own internal
     * [SceneStrategyScope] suitable for a Scene that closely mirror real scenarios and be passed to
     * [androidx.navigation3.ui.NavDisplay].
     */
    public constructor() : this(onBack = {})
}

/** A strategy that tries to calculate a [Scene] given a list of [NavEntry]s. */
@Immutable
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
     * [SceneStrategy]. For the returned [SceneStrategy], [calculateScene] will use the first
     * non-null result from the calculation.
     */
    public infix fun then(sceneStrategy: SceneStrategy<T>): SceneStrategy<T> {
        val firstStrategy = this
        return SceneStrategy { entries ->
            with(firstStrategy) {
                // with original scene strategy
                calculateScene(entries)
            }
                ?: with(sceneStrategy) {
                    // the chained scene strategy
                    calculateScene(entries)
                }
        }
    }
}
