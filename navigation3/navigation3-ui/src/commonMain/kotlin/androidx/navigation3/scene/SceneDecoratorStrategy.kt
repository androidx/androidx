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

package androidx.navigation3.scene

import androidx.compose.runtime.Immutable

/**
 * Scope used to create a [Scene] from another [Scene].
 *
 * This Scope should be provided to the [SceneDecoratorStrategy.decorateScene] function to create
 * Scenes.
 */
@Immutable
public class SceneDecoratorStrategyScope<T : Any>
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
    onBack: () -> Unit
) : SceneStrategyScope<T>(onBack) {
    /**
     * Construct a [SceneDecoratorStrategyScope] suitable for calling [SceneDecoratorStrategy]
     * functions in isolation.
     *
     * For more complicated cases, such as ones where you want to test if [onBack] is called
     * correctly, use [rememberSceneState], which will construct its own internal
     * [SceneStrategyScope] suitable for a Scene that closely mirror real scenarios and be passed to
     * [androidx.navigation3.ui.NavDisplay].
     */
    public constructor() : this(onBack = {})
}

/** A strategy that tries to decorate a [Scene] given another [Scene]. */
public fun interface SceneDecoratorStrategy<T : Any> {
    /**
     * Decorates the given [Scene].
     *
     * The newly returned [Scene] may or may not include the content of the given [scene].
     *
     * @param scene The scene to be decorated
     */
    public fun SceneDecoratorStrategyScope<T>.decorateScene(scene: Scene<T>): Scene<T>
}
