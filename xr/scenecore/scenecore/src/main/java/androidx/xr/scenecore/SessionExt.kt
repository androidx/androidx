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

@file:JvmName("SessionExt")

package androidx.xr.scenecore

import androidx.xr.runtime.Session
import androidx.xr.scenecore.runtime.RenderingRuntime
import androidx.xr.scenecore.runtime.SceneRuntime

/**
 * Gets the [Scene] associated with this Session.
 *
 * Accessing the scene in a destroyed activity can be dangerous.
 *
 * The `Scene` is the primary interface for creating and managing spatial content. There is a single
 * `Scene` instance for each `Session`.
 *
 * @see Scene
 */
public val Session.scene: Scene
    get() =
        // TODO: b/450009236 - This will return the scene even if the Session's Activity has been
        //  destroyed, which we may want to change in the future.
        this.sessionConnectors.filterIsInstance<Scene>().single()

internal val Session.sceneRuntime: SceneRuntime
    get() =
        runtimes.filterIsInstance<SceneRuntime>().firstOrNull()
            ?: throw IllegalStateException(
                "No scene runtime found. Did you create the Session with a non-Activity context?"
            )

internal val Session.renderingRuntime: RenderingRuntime
    get() =
        runtimes.filterIsInstance<RenderingRuntime>().firstOrNull()
            ?: throw IllegalStateException(
                "No rendering runtime found. Did you create the Session with a non-Activity context?"
            )
