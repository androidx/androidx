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

@file:Suppress("DEPRECATION")

package androidx.xr.scenecore.testing

import android.view.View
import androidx.annotation.RestrictTo
import androidx.xr.runtime.math.FieldOfView
import androidx.xr.runtime.math.Vector2
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.Dimensions
import androidx.xr.scenecore.runtime.PanelEntity
import androidx.xr.scenecore.runtime.PerceivedResolutionResult
import androidx.xr.scenecore.runtime.PixelDimensions
import androidx.xr.scenecore.runtime.ScenePose
import androidx.xr.scenecore.testing.internal.FakePanelEntity as InternalFakePanelEntity

/** Test-only implementation of [androidx.xr.scenecore.runtime.PanelEntity] */
@Deprecated("Use SceneCoreTestRule instead.")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public open class FakePanelEntity
internal constructor(
    public val view: View? = null,
    name: String = "",
    fakeInternal: InternalFakePanelEntity,
) : FakeEntity(name, fakeInternal), PanelEntity {

    public constructor(
        view: View? = null,
        name: String = "",
    ) : this(view, name, InternalFakePanelEntity(view, name))

    private val internalPanelEntity: InternalFakePanelEntity = fakeInternal
    internal var dpPerMeter: Float
        get() = internalPanelEntity.dpPerMeter
        set(value) {
            internalPanelEntity.dpPerMeter = value
        }

    override fun dispose() {
        internalPanelEntity.dispose()
    }

    /**
     * Sets the pixel (not Dp) dimensions of the view underlying this PanelEntity. Calling this
     * might cause the layout of the Panel contents to change. Updating this will not cause the
     * scale or pixel density to change.
     */
    override var sizeInPixels: PixelDimensions
        get() = internalPanelEntity.sizeInPixels
        set(value) {
            internalPanelEntity.sizeInPixels = value
        }

    /**
     * The corner radius of the panel in meters.
     *
     * Only non-negative values are allowed.
     */
    override var cornerRadius: Float
        get() = internalPanelEntity.cornerRadius
        set(value) {
            internalPanelEntity.cornerRadius = value
        }

    /**
     * Returns the spatial size of this Panel in meters, without considering any scaling applied to
     * this panel by itself or its parents.
     *
     * @return [androidx.xr.scenecore.runtime.Dimensions] size of this panel in meters. (Z will
     *   be 0)
     */
    override var size: Dimensions
        get() = internalPanelEntity.size
        set(value) {
            internalPanelEntity.size = value
        }

    /**
     * For test purposes only.
     *
     * Sets the [androidx.xr.scenecore.runtime.PerceivedResolutionResult] that will be returned by
     * [getPerceivedResolution].
     */
    public fun setPerceivedResolution(perceivedResolution: PerceivedResolutionResult) {
        internalPanelEntity.setPerceivedResolution(perceivedResolution)
    }

    /**
     * Gets the perceived resolution of the entity in the camera view.
     *
     * This API is only intended for use in Full Space Mode and will return
     * [androidx.xr.scenecore.runtime.PerceivedResolutionResult.InvalidRenderViewpoint] in Home
     * Space Mode.
     *
     * The entity's own rotation and the camera's viewing direction are disregarded; this value
     * represents the dimensions of the entity on the camera view if its largest surface was facing
     * the camera without changing the distance of the entity to the camera.
     *
     * @param renderViewScenePose The [ScenePose] that represents the camera pose.
     * @param renderViewFov The [FieldOfView] of the camera.
     * @return A [androidx.xr.scenecore.runtime.PerceivedResolutionResult] which encapsulates the
     *   outcome:
     *     - [PerceivedResolutionResult.Success] containing the [PixelDimensions] if the calculation
     *       is successful.
     *     - [PerceivedResolutionResult.EntityTooClose] if the entity is too close to the camera.
     *     - [PerceivedResolutionResult.InvalidRenderViewpoint] if the camera information required
     *       for the calculation is invalid or unavailable.
     *
     * @see androidx.xr.scenecore.runtime.PerceivedResolutionResult
     */
    override fun getPerceivedResolution(
        renderViewScenePose: ScenePose,
        renderViewFov: FieldOfView,
    ): PerceivedResolutionResult {
        return internalPanelEntity.getPerceivedResolution(renderViewScenePose, renderViewFov)
    }

    override fun transformPixelCoordinatesToLocalPosition(coordinates: Vector2): Vector3 {
        return internalPanelEntity.transformPixelCoordinatesToLocalPosition(coordinates)
    }

    override fun transformNormalizedCoordinatesToLocalPosition(coordinates: Vector2): Vector3 {
        return internalPanelEntity.transformNormalizedCoordinatesToLocalPosition(coordinates)
    }
}
