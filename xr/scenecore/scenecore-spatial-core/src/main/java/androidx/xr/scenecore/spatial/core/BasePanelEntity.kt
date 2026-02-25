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
package androidx.xr.scenecore.spatial.core

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import androidx.core.util.TypedValueCompat
import androidx.xr.runtime.FieldOfView
import androidx.xr.runtime.SpatialApiVersionHelper.spatialApiVersion
import androidx.xr.runtime.math.Vector2
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.math.Vector3.Companion.distance
import androidx.xr.scenecore.runtime.Dimensions
import androidx.xr.scenecore.runtime.PanelEntity
import androidx.xr.scenecore.runtime.PerceivedResolutionResult
import androidx.xr.scenecore.runtime.PixelDimensions
import androidx.xr.scenecore.runtime.ScenePose
import androidx.xr.scenecore.runtime.Space
import com.android.extensions.xr.XrExtensions
import com.android.extensions.xr.node.Node
import java.util.concurrent.ScheduledExecutorService
import kotlin.math.min

/** BasePanelEntity provides implementations of capabilities common to PanelEntities. */
internal abstract class BasePanelEntity(
    context: Context,
    node: Node,
    extensions: XrExtensions,
    entityManager: EntityManager,
    executor: ScheduledExecutorService,
) : AndroidXrEntity(context, node, extensions, entityManager, executor), PanelEntity {
    protected val defaultPixelDensity: Float
        get() {
            // Spatial api versions 1 and 2+, have different density behaviors. In 2+, pixels per
            // meter should remain a constant value even when system density changes.
            return if (spatialApiVersion >= 2) {
                mExtensions.underlyingObject.config.defaultPixelsPerMeter()
            } else {
                mExtensions.config.defaultPixelsPerMeter(
                    Resources.getSystem().displayMetrics.density
                )
            }
        }

    protected val defaultCornerRadiusInMeters: Float
        get() {
            // Get the width and height of the panel in DP.
            val widthDp =
                TypedValueCompat.deriveDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    sizeInPixels.width.toFloat(),
                    Resources.getSystem().displayMetrics,
                )
            val heightDp =
                TypedValueCompat.deriveDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    sizeInPixels.height.toFloat(),
                    Resources.getSystem().displayMetrics,
                )
            var radiusDp: Float = DEFAULT_CORNER_RADIUS_DP

            // If the pixel dimensions are smaller than the default corner radius, use the smaller
            // of the two dimensions as the corner radius.
            if (widthDp < DEFAULT_CORNER_RADIUS_DP * 2 || heightDp < DEFAULT_CORNER_RADIUS_DP * 2) {
                radiusDp = min(widthDp / 2, heightDp / 2)
            }

            // Convert the updated corner radius to pixels.
            val radiusPixels =
                TypedValueCompat.dpToPx(radiusDp, Resources.getSystem().displayMetrics)

            // Convert the pixel radius to meters.
            return radiusPixels / this.defaultPixelDensity
        }

    override var size: Dimensions
        get() {
            return Dimensions(
                sizeInPixels.width / defaultPixelDensity,
                sizeInPixels.height / defaultPixelDensity,
                0f,
            )
        }
        set(dimensions) {
            sizeInPixels =
                PixelDimensions(
                    (dimensions.width * defaultPixelDensity).toInt(),
                    (dimensions.height * defaultPixelDensity).toInt(),
                )
        }

    override var sizeInPixels: PixelDimensions = PixelDimensions(0, 0)

    override fun getPerceivedResolution(
        renderViewScenePose: ScenePose,
        renderViewFov: FieldOfView,
    ): PerceivedResolutionResult {
        // Compute the width, height, and distance to camera, of the panel in activity space units
        val panelWidthInActivitySpace = size.width * getScale(Space.ACTIVITY).x
        val panelHeightInActivitySpace = size.height * getScale(Space.ACTIVITY).y
        val cameraPositionInActivitySpace = renderViewScenePose.activitySpacePose.translation
        val panelDistanceToCameraInActivitySpace =
            distance(cameraPositionInActivitySpace, getPose(Space.ACTIVITY).translation)

        return getPerceivedResolutionOfPanel(
            renderViewFov,
            getDisplayResolutionInPixels(context!!),
            panelWidthInActivitySpace,
            panelHeightInActivitySpace,
            panelDistanceToCameraInActivitySpace,
        )
    }

    protected var cornerRadiusValue = defaultCornerRadiusInMeters

    override var cornerRadius: Float
        get() = cornerRadiusValue
        set(value) {
            require(!(value < 0.0f)) { "Corner radius can't be negative: $value" }
            cornerRadiusValue = value
            mExtensions.createNodeTransaction().use { transaction ->
                transaction.setCornerRadius(mNode, value).apply()
            }
        }

    override fun transformPixelCoordinatesToLocalPosition(coordinates: Vector2): Vector3 {
        // Convert Pixel units to a normalized [0, 1] (x) and [1, 0] (y) range
        val normalizedPixelWidth = coordinates.x / sizeInPixels.width
        val normalizedPixelHeight = coordinates.y / sizeInPixels.height

        // Subtract the vertical range from one to turn [1,0] into [0,1] since the vertical axis for
        // pixel coordinates is flipped with respect to the extents coordinate space.
        val normalizedPixelHeightFlipped = 1 - normalizedPixelHeight

        // Multiply by 2 to get [0,2] and subtract one to get [-1,1] to match the extents range
        return transformNormalizedCoordinatesToLocalPosition(
            Vector2(normalizedPixelWidth * 2 - 1, normalizedPixelHeightFlipped * 2 - 1)
        )
    }

    override fun transformNormalizedCoordinatesToLocalPosition(coordinates: Vector2): Vector3 {
        // One input unit covers the extent from the center to the edge so we have to multiply by
        // the half-width or half-height to get the appropriate position in 3D space.
        val size = size
        val xInLocal3DSpace = coordinates.x * (size.width / 2f)
        val yInLocal3DSpace = coordinates.y * (size.height / 2f)
        return Vector3(xInLocal3DSpace, yInLocal3DSpace, 0f)
    }

    companion object {
        private const val DEFAULT_CORNER_RADIUS_DP = 32.0f
    }
}
