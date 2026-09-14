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

package androidx.xr.scenecore.openxr

import androidx.annotation.RestrictTo
import androidx.xr.scenecore.runtime.SpatialEnvironment
import java.util.concurrent.Executor
import java.util.function.Consumer

/** Implementation of SceneCore's [SpatialEnvironment] for OpenXR. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class OpenXrSpatialEnvironment
internal constructor(private val nativeWrapper: SceneCoreOpenXrNative) : SpatialEnvironment {

    override val currentPassthroughOpacity: Float
        get() = TODO()

    override var preferredPassthroughOpacity: Float
        get() = TODO()
        set(_) = TODO()

    override var preferredSpatialEnvironment: SpatialEnvironment.SpatialEnvironmentPreference?
        get() = TODO()
        set(_) = TODO()

    override val isPreferredSpatialEnvironmentActive: Boolean
        get() = TODO()

    override fun addOnSpatialEnvironmentChangedListener(
        executor: Executor,
        listener: Consumer<Boolean>,
    ): Unit = TODO()

    override fun removeOnSpatialEnvironmentChangedListener(listener: Consumer<Boolean>): Unit =
        TODO()

    override fun addOnPassthroughOpacityChangedListener(
        executor: Executor,
        listener: Consumer<Float>,
    ): Unit = TODO()

    override fun removeOnPassthroughOpacityChangedListener(listener: Consumer<Float>): Unit = TODO()
}
