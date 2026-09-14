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

import android.app.Activity
import androidx.annotation.RestrictTo
import androidx.xr.runtime.math.FieldOfView
import androidx.xr.runtime.math.Vector2
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.Dimensions
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.PanelEntity
import androidx.xr.scenecore.runtime.PerceivedResolutionResult
import androidx.xr.scenecore.runtime.PixelDimensions
import androidx.xr.scenecore.runtime.ScenePose
import java.util.concurrent.ScheduledExecutorService

/** Implementation of SceneCore's main [PanelEntity] for OpenXR. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class OpenXrMainPanelEntity
internal constructor(
    activity: Activity?,
    entityHandle: Long,
    nativeWrapper: SceneCoreOpenXrNative,
    sceneNodeRegistry: OpenXrSceneNodeRegistry,
    executor: ScheduledExecutorService,
    parent: Entity? = null,
) : OpenXrEntity(activity, entityHandle, nativeWrapper, sceneNodeRegistry, executor), PanelEntity {

    init {
        this.parent = parent
    }

    override var sizeInPixels: PixelDimensions
        get() = TODO()
        set(_) = TODO()

    override var cornerRadius: Float
        get() = TODO()
        set(_) = TODO()

    override var size: Dimensions
        get() = TODO()
        set(_) = TODO()

    override fun transformPixelCoordinatesToLocalPosition(coordinates: Vector2): Vector3 = TODO()

    override fun transformNormalizedCoordinatesToLocalPosition(coordinates: Vector2): Vector3 =
        TODO()

    override fun getPerceivedResolution(
        renderViewScenePose: ScenePose,
        renderViewFov: FieldOfView,
    ): PerceivedResolutionResult = TODO()
}
