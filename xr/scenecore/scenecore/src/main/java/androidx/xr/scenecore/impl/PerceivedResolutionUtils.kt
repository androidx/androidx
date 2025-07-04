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

package androidx.xr.scenecore.impl

import androidx.annotation.VisibleForTesting
import androidx.xr.runtime.internal.CameraViewActivityPose
import androidx.xr.runtime.internal.Dimensions
import androidx.xr.runtime.internal.PerceivedResolutionResult
import androidx.xr.runtime.internal.PixelDimensions
import androidx.xr.runtime.math.Vector3
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.tan

/** A small constant value used for floating-point comparisons to account for precision errors. */
@VisibleForTesting internal const val PERCEIVED_RESOLUTION_EPSILON = 0.001f

/**
 * Retrieves the [CameraViewActivityPose] used for all Perceived Resolution calculations.
 *
 * It is currently set to specifically look for the left eye camera.
 *
 * @param entityManager The [EntityManager] instance that holds the system space activity poses,
 *   including camera views.
 * @return The [CameraViewActivityPose] for the left eye if found; otherwise, `null` if no such
 *   camera view is registered or available in the [EntityManager].
 */
internal fun getPerceivedResolutionCameraView(
    entityManager: EntityManager
): CameraViewActivityPose? {
    val cameraViews =
        entityManager.getSystemSpaceActivityPoseOfType(CameraViewActivityPose::class.java)
    for (cameraView in cameraViews) {
        if (cameraView.cameraType == CameraViewActivityPose.CameraType.CAMERA_TYPE_LEFT_EYE) {
            return cameraView
        }
    }

    return null
}

/**
 * Calculates the perceived pixel dimensions of a 3D box as it would appear on the display.
 *
 * This function first determines the dimensions of the largest face of the 3D box and calculates
 * the distance from the camera to this face. It assumes an orientation where this largest face is
 * directed towards the camera, maximizing its potential screen coverage.
 *
 * Subsequently, it treats this largest face as a 2D panel at the calculated distance and computes
 * its perceived resolution on the screen using [getPerceivedResolutionOfPanel].
 *
 * @param cameraView The current pose and Field of View (FOV) information of the camera.
 * @param boxDimensionsInActivitySpace The dimensions (width, height, depth) of the 3D box in the
 *   activity's coordinate space.
 * @param boxPositionInActivitySpace The position of the center of the 3D box in the activity's
 *   coordinate space.
 * @return If Success, the calculated [PixelDimensions] (width and height in pixels) that the
 *   largest face of the 3D box would occupy on the display, assuming it's oriented towards the
 *   camera. Returns [PerceivedResolutionResult.EntityTooClose] if the box's largest face is
 *   determined to be behind or too close to the camera.
 * @see getDimensionsAndDistanceOfLargest3dBoxSurface
 * @see getPerceivedResolutionOfPanel
 */
internal fun getPerceivedResolutionOf3DBox(
    cameraView: CameraViewActivityPose,
    boxDimensionsInActivitySpace: Dimensions,
    boxPositionInActivitySpace: Vector3,
): PerceivedResolutionResult {
    // Get dimensions of the largest box surface and its relative distance if it were closest.
    val surfaceDimensionsAndDistance =
        getDimensionsAndDistanceOfLargest3dBoxSurface(
            cameraView,
            boxDimensionsInActivitySpace,
            boxPositionInActivitySpace,
        )

    return getPerceivedResolutionOfPanel(
        cameraView,
        surfaceDimensionsAndDistance.width,
        surfaceDimensionsAndDistance.height,
        surfaceDimensionsAndDistance.depth,
    )
}

/**
 * Calculates the dimensions of the largest face of a 3D box and the distance from the camera to
 * this face.
 *
 * The function first identifies the largest face of the box and then assumes that the box is
 * oriented in such a way that the largest surface is facing the camera. This is the orientation in
 * which the 3D box could occupy the maximum amount of screen space without any translations.
 *
 * @param cameraView The current pose of the camera in the activity's coordinate space.
 * @param boxDimensionsInActivitySpace The dimensions (width, height, depth) of the 3D box in the
 *   activity's coordinate space.
 * @param boxPositionInActivitySpace The position of the center of the 3D box in the activity's
 *   coordinate space.
 * @return A [Dimensions] where:
 *     - `width`: The largest dimension of the box, considered the width of its largest surface.
 *     - `height`: The second largest dimension of the box, considered the height of its largest
 *       surface.
 *     - `depth`: The calculated distance from the camera to the largest face of the 3D box. This is
 *       derived by taking the distance from the camera to the box's center and subtracting half of
 *       the box's smallest dimension. All the values are in the units of the ActivitySpace.
 */
internal fun getDimensionsAndDistanceOfLargest3dBoxSurface(
    cameraView: CameraViewActivityPose,
    boxDimensionsInActivitySpace: Dimensions,
    boxPositionInActivitySpace: Vector3,
): Dimensions {
    // Get box surface with maximum size and rearrange the dimensions so that the face with the
    // largest area is facing the camera.
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
    // the camera to the largest face. Assume the largest face is always the closest to the camera.
    val cameraPositionInActivitySpace = cameraView.activitySpacePose.translation
    val distanceInActivitySpaceTo3DBox =
        Vector3.distance(cameraPositionInActivitySpace, boxPositionInActivitySpace)
    val distanceInActivitySpaceToLargestFace = distanceInActivitySpaceTo3DBox - depthTo3dBoxCenter

    return Dimensions(maxWidth, maxHeight, distanceInActivitySpaceToLargestFace)
}

/**
 * Computes the perceived pixel dimensions of a 2D rectangular panel in 3D space as it would appear
 * on the display.
 *
 * The calculation is based on the camera's field of view (FOV) and the distance to the panel. It
 * first determines the physical dimensions of the camera's view frustum at the panel's distance.
 * Then, it calculates what proportion of this view frustum the panel occupies and converts this
 * proportion into pixel dimensions based on the screen's total resolution.
 *
 * If the panel is determined to be too close to the camera (closer than
 * [PERCEIVED_RESOLUTION_EPSILON]), it returns [PerceivedResolutionResult.EntityTooClose] to avoid
 * issues like division by zero or infinitely large sizes.
 *
 * @param cameraView The pose and FOV information of the camera in the activity's coordinate space.
 * @param panelWidthInActivitySpace The physical width of the 2D panel in the units of the
 *   activity's coordinate space (e.g., meters).
 * @param panelHeightInActivitySpace The physical height of the 2D panel in the units of the
 *   activity's coordinate space (e.g., meters).
 * @param panelDistanceInActivitySpace The perpendicular distance from the camera to the plane of
 *   the 2D panel, in the activity's coordinate space.
 * @return If Success, The calculated [PixelDimensions] (width and height in pixels) that the panel
 *   would occupy on the display. Returns [PerceivedResolutionResult.EntityTooClose] if the panel is
 *   too close to the camera.
 */
internal fun getPerceivedResolutionOfPanel(
    cameraView: CameraViewActivityPose,
    panelWidthInActivitySpace: Float,
    panelHeightInActivitySpace: Float,
    panelDistanceInActivitySpace: Float,
): PerceivedResolutionResult {
    // If the panel is behind the camera or exactly at the camera's position, it's not visible
    // or would have infinite size on screen so return maximum resolution allowed.
    if (panelDistanceInActivitySpace <= PERCEIVED_RESOLUTION_EPSILON) {
        return PerceivedResolutionResult.EntityTooClose()
    }
    // If the distance to the camera isn't finite then there was an invalid camera state
    if (!panelDistanceInActivitySpace.isFinite()) {
        return PerceivedResolutionResult.InvalidCameraView()
    }

    // Calculate the activitySpace dimensions of the camera's view frustum at the panel's distance.
    // These are the dimensions of a rectangular plane perpendicular to the camera's forward vector
    // at 'distanceInActivitySpace' that precisely fits the camera's field of view.
    // `distance * tan(angle)` gives us the half-length from the optical axis to the edge of the
    // view in the direction of the angle. For example: adding the half-lengths of left and right
    // angles will give us the full width.
    val fov = cameraView.fov
    if (
        !(fov.angleLeft.isFinite() &&
            fov.angleRight.isFinite() &&
            fov.angleDown.isFinite() &&
            fov.angleUp.isFinite())
    ) {
        return PerceivedResolutionResult.InvalidCameraView()
    }
    val viewPlaneWidthActivitySpace =
        panelDistanceInActivitySpace * (abs(tan(fov.angleLeft)) + abs(tan(fov.angleRight)))
    val viewPlaneHeightActivitySpace =
        panelDistanceInActivitySpace * (abs(tan(fov.angleDown)) + abs(tan(fov.angleUp)))

    // Calculate the ratio of the panel dimensions within the view plane
    // Avoid division by zero if FOV or distance makes view plane dimensions near zero.
    val panelWidthRatioInViewPlane =
        if (viewPlaneWidthActivitySpace <= PERCEIVED_RESOLUTION_EPSILON) 0.0f
        else panelWidthInActivitySpace / viewPlaneWidthActivitySpace
    val panelHeightRatioInViewPlane =
        if (viewPlaneHeightActivitySpace <= PERCEIVED_RESOLUTION_EPSILON) 0.0f
        else panelHeightInActivitySpace / viewPlaneHeightActivitySpace

    // Calculate the width and height of the panel in pixels
    val viewPlaneInPixels = cameraView.displayResolutionInPixels
    if (viewPlaneInPixels.width == 0 || viewPlaneInPixels.height == 0) {
        return PerceivedResolutionResult.InvalidCameraView()
    }
    val pixelWidth = panelWidthRatioInViewPlane * viewPlaneInPixels.width
    val pixelHeight = panelHeightRatioInViewPlane * viewPlaneInPixels.height

    return PerceivedResolutionResult.Success(
        PixelDimensions(pixelWidth.roundToInt(), pixelHeight.roundToInt())
    )
}
