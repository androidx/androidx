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

package androidx.xr.scenecore.runtime

import androidx.annotation.RestrictTo
import androidx.xr.runtime.FieldOfView
import androidx.xr.runtime.math.Vector2
import androidx.xr.runtime.math.Vector3

/** Interface for a XR Runtime Panel entity */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface PanelEntity : Entity {
    /**
     * Sets the pixel (not Dp) dimensions of the view underlying this PanelEntity. Calling this
     * might cause the layout of the Panel contents to change. Updating this will not cause the
     * scale or pixel density to change.
     */
    public var sizeInPixels: PixelDimensions

    /** Sets a corner radius on all four corners of this PanelEntity. */
    public var cornerRadius: Float

    /**
     * Returns the spatial size of this Panel in meters, without considering any scaling applied to
     * this panel by itself or its parents.
     *
     * @return [Dimensions] size of this panel in meters. (Z will be 0)
     */
    public var size: Dimensions

    /**
     * Gets the perceived resolution of the entity in the camera view.
     *
     * This API is only intended for use in Full Space Mode and will return
     * [PerceivedResolutionResult.InvalidRenderViewpoint] in Home Space Mode.
     *
     * The entity's own rotation and the camera's viewing direction are disregarded; this value
     * represents the dimensions of the entity on the camera view if its largest surface was facing
     * the camera without changing the distance of the entity to the camera.
     *
     * @param renderViewScenePose The [ScenePose] that represents the camera pose.
     * @param renderViewFov The [FieldOfView] of the camera.
     * @return A [PerceivedResolutionResult] which encapsulates the outcome:
     *     - [PerceivedResolutionResult.Success] containing the [PixelDimensions] if the calculation
     *       is successful.
     *     - [PerceivedResolutionResult.EntityTooClose] if the entity is too close to the camera.
     *     - [PerceivedResolutionResult.InvalidRenderViewpoint] if the camera information required
     *       for the calculation is invalid or unavailable.
     *
     * @see PerceivedResolutionResult
     */
    public fun getPerceivedResolution(
        renderViewScenePose: ScenePose,
        renderViewFov: FieldOfView,
    ): PerceivedResolutionResult

    /**
     * Gets the 3D position of a 2D pixel coordinate within the Entity's local space.
     *
     * This method's inputs use a 2D pixel coordinate system where:
     * - The origin (0, 0) is at the **top-left** corner of the panel.
     * - The +X axis points towards the **right** edge of the panel content.
     * - The +Y axis points towards the **bottom** edge of the panel content.
     *
     * Input values are floats to allow for sub-pixel accuracy. Values outside the panel's pixel
     * dimensions (e.g., `x < 0` or `y > panelHeight`) are permitted and will result in a position
     * outside the panel's surface.
     *
     * Note that calling this method on [SceneRuntime.mainPanelEntity] during
     * [android.app.Activity.onCreate] can result in incorrect values.
     *
     * @param coordinates The pixel coordinate, relative to the top-left origin.
     * @return The 3D position in the Entity's local space corresponding to the 2D pixel coordinate.
     * @see ScenePose.transformPositionTo to transform the position to a different coordinate space.
     */
    public fun transformPixelCoordinatesToLocalPosition(coordinates: Vector2): Vector3

    /**
     * Gets the 3D position of a 2D normalized extent coordinate within the Entity's local space.
     *
     * This method's inputs use a 2D normalized coordinate system where:
     * - The origin (0.0, 0.0) is at the **center** of the panel.
     * - The +X axis points towards the **right** edge (mapped to 1.0) of the panel content.
     * - The +Y axis points towards the **top** edge (mapped to 1.0) of the panel content.
     *
     * Values outside the [-1.0, 1.0] range are permitted and will result in a position outside the
     * panel's surface.
     *
     * Note that calling this method on [SceneRuntime.mainPanelEntity] during
     * [android.app.Activity.onCreate] can result in incorrect values.
     *
     * @param coordinates The normalized coordinates, relative to the origin at the center of the
     *   panel.
     * @return The 3D position in the Entity's local space corresponding to the 2D normalized
     *   coordinate.
     * @see ScenePose.transformPositionTo to transform the position to a different coordinate space.
     */
    public fun transformNormalizedCoordinatesToLocalPosition(coordinates: Vector2): Vector3
}
