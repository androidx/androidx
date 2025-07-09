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

package androidx.xr.runtime.testing

import androidx.annotation.RestrictTo
import androidx.xr.runtime.internal.Dimensions
import androidx.xr.runtime.internal.PanelEntity
import androidx.xr.runtime.internal.PerceivedResolutionResult
import androidx.xr.runtime.internal.PixelDimensions

/** Test-only implementation of [PanelEntity] */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public open class FakePanelEntity() : FakeEntity(), PanelEntity {
    /**
     * Sets the pixel (not Dp) dimensions of the view underlying this PanelEntity. Calling this
     * might cause the layout of the Panel contents to change. Updating this will not cause the
     * scale or pixel density to change.
     */
    override var sizeInPixels: PixelDimensions = PixelDimensions(640, 480)

    /**
     * The corner radius of the panel in meters.
     *
     * Only non-negative values are allowed.
     */
    override var cornerRadius: Float = 32.0f
        set(value) {
            if (value >= 0f) {
                field = value
            }
        }

    /**
     * Returns the spatial size of this Panel in meters, without considering any scaling applied to
     * this panel by itself or its parents.
     *
     * @return [Dimensions] size of this panel in meters. (Z will be 0)
     */
    override var size: Dimensions = Dimensions(1.0f, 1.0f, 0.0f)
        set(value) {
            if (value.width >= 0f && value.height >= 0f && value.depth >= 0f) {
                field = Dimensions(value.width, value.height, 0.0f)
            }
        }
        get() {
            if (field.depth > 0f) {
                field = Dimensions(field.width, field.height, 0.0f)
            }
            return field
        }

    private var perceivedResolutionResult: PerceivedResolutionResult =
        PerceivedResolutionResult.InvalidCameraView()

    /**
     * For test purposes only.
     *
     * Sets the [PerceivedResolutionResult] that will be returned by [getPerceivedResolution].
     */
    public fun setPerceivedResolution(perceivedResolution: PerceivedResolutionResult) {
        this.perceivedResolutionResult = perceivedResolution
    }

    /**
     * Gets the perceived resolution of the entity in the camera view.
     *
     * This API is only intended for use in Full Space Mode and will return
     * [PerceivedResolutionResult.InvalidCameraView] in Home Space Mode.
     *
     * The entity's own rotation and the camera's viewing direction are disregarded; this value
     * represents the dimensions of the entity on the camera view if its largest surface was facing
     * the camera without changing the distance of the entity to the camera.
     *
     * @return A [PerceivedResolutionResult] which encapsulates the outcome:
     *     - [PerceivedResolutionResult.Success] containing the [PixelDimensions] if the calculation
     *       is successful.
     *     - [PerceivedResolutionResult.EntityTooClose] if the entity is too close to the camera.
     *     - [PerceivedResolutionResult.InvalidCameraView] if the camera information required for
     *       the calculation is invalid or unavailable.
     *
     * @see PerceivedResolutionResult
     */
    override fun getPerceivedResolution(): PerceivedResolutionResult {
        return perceivedResolutionResult
    }
}
