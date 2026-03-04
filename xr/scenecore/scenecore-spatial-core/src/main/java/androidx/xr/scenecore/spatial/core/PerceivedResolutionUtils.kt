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

@file:JvmName("PerceivedResolutionUtils")

package androidx.xr.scenecore.spatial.core

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.annotation.VisibleForTesting
import androidx.xr.runtime.FieldOfView
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.Dimensions
import androidx.xr.scenecore.runtime.PerceivedResolutionResult
import androidx.xr.scenecore.runtime.PixelDimensions
import androidx.xr.scenecore.runtime.ScenePose
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.tan

/** A small constant value used for floating-point comparisons to account for precision errors. */
@VisibleForTesting internal const val PERCEIVED_RESOLUTION_EPSILON = 0.001f

/**
 * Calculates the perceived pixel dimensions of a 3D box as it would appear on the display.
 *
 * This function first determines the dimensions of the largest face of the 3D box and calculates
 * the distance from the render viewpoint to this face. It assumes an orientation where this largest
 * face is directed towards the render viewpoint, maximizing its potential screen coverage.
 *
 * Subsequently, it treats this largest face as a 2D panel at the calculated distance and computes
 * its perceived resolution on the screen using [getPerceivedResolutionOfPanel].
 *
 * @param renderView The [ScenePose] reflecting the pose of the render viewpoint in the activity's
 *   coordinate system.
 * @param fieldOfView The field of view of the render viewpoint.
 * @param viewPlaneInPixels The size of the panel in pixels.
 * @param boxDimensionsInActivitySpace The dimensions (width, height, depth) of the 3D box in the
 *   activity's coordinate space.
 * @param boxPositionInActivitySpace The position of the center of the 3D box in the activity's
 *   coordinate space.
 * @return If Success, the calculated [androidx.xr.scenecore.runtime.PixelDimensions] (width and
 *   height in pixels) that the largest face of the 3D box would occupy on the display, assuming
 *   it's oriented towards the render viewpoint. Returns
 *   [androidx.xr.scenecore.runtime.PerceivedResolutionResult.EntityTooClose] if the box's largest
 *   face is determined to be behind or too close to the render viewpoint.
 * @see getDimensionsAndDistanceOfLargest3dBoxSurface
 * @see getPerceivedResolutionOfPanel
 */
internal fun getPerceivedResolutionOf3DBox(
    renderView: ScenePose,
    fieldOfView: FieldOfView,
    viewPlaneInPixels: PixelDimensions,
    boxDimensionsInActivitySpace: Dimensions,
    boxPositionInActivitySpace: Vector3,
): PerceivedResolutionResult {
    // Get dimensions of the largest box surface and its relative distance if it were closest.
    val surfaceDimensionsAndDistance =
        getDimensionsAndDistanceOfLargest3dBoxSurface(
            renderView,
            boxDimensionsInActivitySpace,
            boxPositionInActivitySpace,
        )

    return getPerceivedResolutionOfPanel(
        fieldOfView,
        viewPlaneInPixels,
        surfaceDimensionsAndDistance.width,
        surfaceDimensionsAndDistance.height,
        surfaceDimensionsAndDistance.depth,
    )
}

/**
 * Calculates the dimensions of the largest face of a 3D box and the distance from the render
 * viewpoint to this face.
 *
 * The function first identifies the largest face of the box and then assumes that the box is
 * oriented in such a way that the largest surface is facing the render viewpoint. This is the
 * orientation in which the 3D box could occupy the maximum amount of screen space without any
 * translations.
 *
 * @param renderView The [ScenePose] reflecting the pose of the render viewpoint in the activity's
 *   coordinate system.
 * @param boxDimensionsInActivitySpace The dimensions (width, height, depth) of the 3D box in the
 *   activity's coordinate space.
 * @param boxPositionInActivitySpace The position of the center of the 3D box in the activity's
 *   coordinate space.
 * @return A [androidx.xr.scenecore.runtime.Dimensions] where:
 *     - `width`: The largest dimension of the box, considered the width of its largest surface.
 *     - `height`: The second largest dimension of the box, considered the height of its largest
 *       surface.
 *     - `depth`: The calculated distance from the render viewpoint to the largest face of the 3D
 *       box. This is by taking the distance from the render viewpoint to the box's center and
 *       subtracting half of the box's smallest dimension. All the values are in the units of the
 *       ActivitySpace.
 */
internal fun getDimensionsAndDistanceOfLargest3dBoxSurface(
    renderView: ScenePose,
    boxDimensionsInActivitySpace: Dimensions,
    boxPositionInActivitySpace: Vector3,
): FloatSize3d {
    // Get box surface with maximum size and rearrange the dimensions so that the face with the
    // largest area is facing the render viewpoint.
    val sortedDimensions =
        listOf(
                boxDimensionsInActivitySpace.width,
                boxDimensionsInActivitySpace.height,
                boxDimensionsInActivitySpace.depth,
            )
            .sortedDescending()
    val maxWidth = sortedDimensions[0]
    val maxHeight = sortedDimensions[1]
    val depthTo3dBoxCenter = sortedDimensions[2] / 2.0f

    // Substract 1/2 of the depth of the 3D box from the total distance to get the distance from
    // the render viewpoint to the largest face. Assume the largest face is always the closest to
    // the render viewpoint.
    val renderViewpointPositionInActivitySpace = renderView.activitySpacePose.translation
    val distanceInActivitySpaceTo3DBox =
        Vector3.distance(renderViewpointPositionInActivitySpace, boxPositionInActivitySpace)
    val distanceInActivitySpaceToLargestFace = distanceInActivitySpaceTo3DBox - depthTo3dBoxCenter

    return FloatSize3d(maxWidth, maxHeight, distanceInActivitySpaceToLargestFace)
}

/**
 * Computes the perceived pixel dimensions of a 2D rectangular panel in 3D space as it would appear
 * on the display.
 *
 * The calculation is based on the render viewpoints's field of view (FOV) and the distance to the
 * panel. It first determines the physical dimensions of the render viewpoints's view frustum at the
 * panel's distance. Then, it calculates what proportion of this view frustum the panel occupies and
 * converts this proportion into pixel dimensions based on the screen's total resolution.
 *
 * If the panel is determined to be too close to the render viewpoint (closer than
 * [PERCEIVED_RESOLUTION_EPSILON]), it returns
 * [androidx.xr.scenecore.runtime.PerceivedResolutionResult.EntityTooClose] to avoid issues like
 * division by zero or infinitely large sizes.
 *
 * @param fieldOfView The field of view of the render viewpoint.
 * @param viewPlaneInPixels The size of the panel in pixels.
 * @param panelWidthInActivitySpace The physical width of the 2D panel in the units of the
 *   activity's coordinate space (e.g., meters).
 * @param panelHeightInActivitySpace The physical height of the 2D panel in the units of the
 *   activity's coordinate space (e.g., meters).
 * @param panelDistanceInActivitySpace The perpendicular distance from the render viewpoint to the
 *   plane of the 2D panel, in the activity's coordinate space.
 * @return If Success, The calculated [androidx.xr.scenecore.runtime.PixelDimensions] (width and
 *   height in pixels) that the panel would occupy on the display. Returns
 *   [androidx.xr.scenecore.runtime.PerceivedResolutionResult.EntityTooClose] if the panel is too
 *   close to the render viewpoint.
 */
internal fun getPerceivedResolutionOfPanel(
    fieldOfView: FieldOfView,
    viewPlaneInPixels: PixelDimensions,
    panelWidthInActivitySpace: Float,
    panelHeightInActivitySpace: Float,
    panelDistanceInActivitySpace: Float,
): PerceivedResolutionResult {
    // If the panel is behind the render viewpoint or exactly at the render viewpoints's position,
    // it's not visible or would have infinite size on screen so return maximum resolution allowed.
    if (panelDistanceInActivitySpace <= PERCEIVED_RESOLUTION_EPSILON) {
        return PerceivedResolutionResult.EntityTooClose()
    }
    // If the distance to the render viewpoint isn't finite then there was an invalid render
    // viewpoint state
    if (!panelDistanceInActivitySpace.isFinite()) {
        return PerceivedResolutionResult.InvalidRenderViewpoint()
    }

    // Calculate the activitySpace dimensions of the render viewpoints's view frustum at the panel's
    // distance. These are the dimensions of a rectangular plane perpendicular to the render
    // viewpoints's forward vector at 'distanceInActivitySpace' that precisely fits the render
    // viewpoints's field of view. `distance * tan(angle)` gives us the half-length from the optical
    // axis to the edge of the view in the direction of the angle. For example: adding the
    // half-lengths of left and right angles will give us the full width.
    if (
        !(fieldOfView.angleLeft.isFinite() &&
            fieldOfView.angleRight.isFinite() &&
            fieldOfView.angleDown.isFinite() &&
            fieldOfView.angleUp.isFinite())
    ) {
        return PerceivedResolutionResult.InvalidRenderViewpoint()
    }
    val viewPlaneWidthActivitySpace =
        panelDistanceInActivitySpace *
            (abs(tan(fieldOfView.angleLeft)) + abs(tan(fieldOfView.angleRight)))
    val viewPlaneHeightActivitySpace =
        panelDistanceInActivitySpace *
            (abs(tan(fieldOfView.angleDown)) + abs(tan(fieldOfView.angleUp)))

    // Calculate the ratio of the panel dimensions within the view plane
    // Avoid division by zero if FOV or distance makes view plane dimensions near zero.
    val panelWidthRatioInViewPlane =
        if (viewPlaneWidthActivitySpace <= PERCEIVED_RESOLUTION_EPSILON) 0.0f
        else panelWidthInActivitySpace / viewPlaneWidthActivitySpace
    val panelHeightRatioInViewPlane =
        if (viewPlaneHeightActivitySpace <= PERCEIVED_RESOLUTION_EPSILON) 0.0f
        else panelHeightInActivitySpace / viewPlaneHeightActivitySpace

    // Calculate the width and height of the panel in pixels
    if (viewPlaneInPixels.width == 0 || viewPlaneInPixels.height == 0) {
        return PerceivedResolutionResult.InvalidRenderViewpoint()
    }
    val pixelWidth = panelWidthRatioInViewPlane * viewPlaneInPixels.width
    val pixelHeight = panelHeightRatioInViewPlane * viewPlaneInPixels.height

    return PerceivedResolutionResult.Success(
        PixelDimensions(pixelWidth.roundToInt(), pixelHeight.roundToInt())
    )
}

/**
 * Returns the resolution of the display in pixels for each eye.
 *
 * @param context The [Context] that provides the default [android.view.Display] for determining the
 *   display resolution.
 * @return The [PixelDimensions]s of the resolution for a single eye.
 */
// Suppress warnings: windowManager's getDefaultDisplay and getRealMetrics.
@Suppress("DEPRECATION")
internal fun getDisplayResolutionInPixels(context: Context): PixelDimensions {
    val windowManager =
        context.getSystemService(WindowManager::class.java)
            // WindowManager not available, cannot get display resolution. Returning (0, 0).
            ?: return PixelDimensions(0, 0)

    val display =
        windowManager.defaultDisplay
            // Default display not available, cannot get display resolution. Returning
            // (0,0).
            ?: return PixelDimensions(0, 0)

    val displayMetrics = DisplayMetrics()
    display.getRealMetrics(displayMetrics)

    // Divide the width by 2 because we want single eye resolution, not full display
    // resolution
    return PixelDimensions(displayMetrics.widthPixels / 2, displayMetrics.heightPixels)
}
