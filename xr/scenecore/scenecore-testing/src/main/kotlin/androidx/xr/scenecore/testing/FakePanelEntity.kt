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

package androidx.xr.scenecore.testing

import android.content.Context
import android.view.View
import android.view.WindowManager
import androidx.annotation.RestrictTo
import androidx.xr.runtime.FieldOfView
import androidx.xr.runtime.math.Vector2
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.Dimensions
import androidx.xr.scenecore.runtime.PanelEntity
import androidx.xr.scenecore.runtime.PerceivedResolutionResult
import androidx.xr.scenecore.runtime.PixelDimensions
import androidx.xr.scenecore.runtime.ScenePose
import kotlin.math.roundToInt

/** Test-only implementation of [androidx.xr.scenecore.runtime.PanelEntity] */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public open class FakePanelEntity(public val view: View? = null, name: String = "") :
    FakeEntity(name), PanelEntity {

    private val context = view?.context
    private val windowManager = context?.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    internal var dpPerMeter: Float = FakeSceneRuntime.DEFAULT_DP_PER_METER
    private val density
        get() = context?.resources?.displayMetrics?.density ?: 1f

    init {
        windowManager?.addView(
            view,
            WindowManager.LayoutParams(WindowManager.LayoutParams.TYPE_APPLICATION_PANEL).apply {
                width = WindowManager.LayoutParams.WRAP_CONTENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
            },
        )
    }

    override fun dispose() {
        windowManager?.removeView(view)
        super.dispose()
    }

    /**
     * Sets the pixel (not Dp) dimensions of the view underlying this PanelEntity. Calling this
     * might cause the layout of the Panel contents to change. Updating this will not cause the
     * scale or pixel density to change.
     */
    override var sizeInPixels: PixelDimensions = Dimensions(1.0f, 1.0f, 0f).toPixelDimensions()
        set(value) {
            if (field != value && value.width >= 0 && value.height >= 0) {
                field = value
                windowManager?.updateViewLayout(
                    view,
                    WindowManager.LayoutParams(WindowManager.LayoutParams.TYPE_APPLICATION_PANEL)
                        .apply {
                            width = value.width
                            height = value.height
                        },
                )
            }
        }

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
     * @return [androidx.xr.scenecore.runtime.Dimensions] size of this panel in meters. (Z will
     *   be 0)
     */
    override var size: Dimensions
        get() = sizeInPixels.toMeterDimensions()
        set(value) {
            if (value.width >= 0 && value.height >= 0 && value.depth >= 0) {
                sizeInPixels = value.toPixelDimensions()
            }
        }

    private var perceivedResolutionResult: PerceivedResolutionResult =
        PerceivedResolutionResult.InvalidRenderViewpoint()

    /**
     * For test purposes only.
     *
     * Sets the [androidx.xr.scenecore.runtime.PerceivedResolutionResult] that will be returned by
     * [getPerceivedResolution].
     */
    public fun setPerceivedResolution(perceivedResolution: PerceivedResolutionResult) {
        this.perceivedResolutionResult = perceivedResolution
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
        return perceivedResolutionResult
    }

    private fun Dimensions.toPixelDimensions(): PixelDimensions {
        val pixelsPerMeter = dpPerMeter * density
        return PixelDimensions(
            (this.width * pixelsPerMeter).roundToInt(),
            (this.height * pixelsPerMeter).roundToInt(),
        )
    }

    private fun PixelDimensions.toMeterDimensions(): Dimensions {
        val pixelsPerMeter = dpPerMeter * density
        return Dimensions(this.width / pixelsPerMeter, this.height / pixelsPerMeter, 0f)
    }

    override fun transformPixelCoordinatesToLocalPosition(coordinates: Vector2): Vector3 {
        val u = coordinates.x / sizeInPixels.width
        val v = coordinates.y / sizeInPixels.height
        return transformNormalizedCoordinatesToLocalPosition(Vector2(u * 2 - 1, (1 - v) * 2 - 1))
    }

    override fun transformNormalizedCoordinatesToLocalPosition(coordinates: Vector2): Vector3 {
        val xInLocal3DSpace = coordinates.x * size.width / 2f
        val yInLocal3DSpace = coordinates.y * size.height / 2f
        return Vector3(xInLocal3DSpace, yInLocal3DSpace, 0f)
    }
}
