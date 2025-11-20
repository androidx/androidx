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

package androidx.xr.compose.spatial

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.xr.compose.unit.Meter
import androidx.xr.compose.unit.Meter.Companion.meters
import androidx.xr.compose.unit.toMeter
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3

/** Calculate a [Pose] in 3D space based on the relative offset within the 2D space of a Panel. */
@Composable
internal fun rememberCalculatePose(
    contentOffset: Offset,
    parentViewSize: IntSize,
    contentSize: IntSize,
    zDepth: Dp = 0.dp,
): Pose {
    val density = LocalDensity.current
    return remember(contentOffset, parentViewSize, contentSize, zDepth) {
        calculatePose(contentOffset, parentViewSize, contentSize, density, zDepth)
    }
}

/**
 * Calculates the 3D pose of a composable within its parent layout.
 *
 * This function handles the conversion from Compose's 2D pixel-based coordinate system (Y-axis
 * points down) to the Spatial Scene Graphs's meter-based coordinate system (Y-axis points up).
 *
 * @param contentOffset The top-left (x, y) position of the content in pixels.
 * @param parentViewSize The width and height of the parent container in pixels.
 * @param contentSize The width and height of the content itself in pixels.
 * @param density The screen density, used for pixel-to-meter conversion.
 * @param zDepth The optional depth of the content on the Z-axis.
 * @return The calculated [Pose] for the 3D spatial scene graph.
 */
internal fun calculatePose(
    contentOffset: Offset,
    parentViewSize: IntSize,
    contentSize: IntSize,
    density: Density,
    zDepth: Dp = 0.dp,
): Pose {
    // Convert the 2D pixel offset to a 3D meter-based position, then add depth.
    val positionInMeters =
        contentOffset.toMeterPosition(parentViewSize, contentSize, density) +
            MeterPosition(z = zDepth.toMeter())

    return Pose(translation = positionInMeters.toVector3())
}

/** A 3D vector where each coordinate is [Meter]s. */
internal data class MeterPosition(
    val x: Meter = 0.meters,
    val y: Meter = 0.meters,
    val z: Meter = 0.meters,
) {
    /**
     * Adds this [MeterPosition] to the [other] one.
     *
     * @param other the other [MeterPosition] to add.
     * @return a new [MeterPosition] representing the sum of the two positions.
     */
    operator fun plus(other: MeterPosition) =
        MeterPosition(x = x + other.x, y = y + other.y, z = z + other.z)

    fun toVector3() = Vector3(x = x.toM(), y = y.toM(), z = z.toM())
}

/**
 * Converts a 2D pixel offset to a 3D meter-based position, correctly anchoring the content.
 *
 * It translates coordinates from a top-left anchor (used in 2D UI) to a center anchor (used in the
 * 3D scene) and inverts the Y-axis.
 */
private fun Offset.toMeterPosition(
    parentViewSize: IntSize,
    contentSize: IntSize,
    density: Density,
): MeterPosition {
    // Find the center of the content in the parent's coordinate space (in pixels).
    val centerXInPixels =
        x.fromTopLeftToCenterAnchor(
            contentDimension = contentSize.width,
            parentDimension = parentViewSize.width,
        )
    val centerYInPixels =
        y.fromTopLeftToCenterAnchor(
            contentDimension = contentSize.height,
            parentDimension = parentViewSize.height,
        )

    // The Y-axis is negated to convert from Compose's 2D pixel-based coordinate system (Y-down)
    // to the Spatial Scene Graphs's meter-based coordinate system (Y-up).
    return MeterPosition(
        x = Meter.fromPixel(centerXInPixels, density),
        y = Meter.fromPixel(-centerYInPixels, density),
    )
}

internal val View.size
    get() = IntSize(width, height)

/**
 * Translates a 2D coordinate from a top-left anchor system to a center-based anchor system.
 *
 * In Jetpack Compose, a composable's position (x, y) refers to its top-left corner. In the
 * underlying Spatial Scene graph, an entity's position refers to its center. This function
 * calculates the correct position for the entity's **center** so that its **top-left corner**
 * aligns with the desired 2D coordinate.
 *
 * Let `coord` be the desired top-left coordinate of the content (this function's receiver). In the
 * 3D system, the parent container is centered at origin `0`, so its edges are at
 * `-parentDimension/2` and `+parentDimension/2`. The desired position of the content's center in
 * the 2D system is `coord + contentDimension/2`. To convert this to the 3D system, we must shift it
 * relative to the parent's center: `(coord + contentDimension/2) - (parentDimension/2)`. This
 * simplifies to the formula used: `coord + (contentDimension - parentDimension) / 2`.
 *
 * @param contentDimension The size of the content (the child) along one axis.
 * @param parentDimension The size of the available space (the parent) along the same axis.
 * @return The adjusted coordinate for the content's center.
 */
private fun Float.fromTopLeftToCenterAnchor(contentDimension: Int, parentDimension: Int) =
    this + (contentDimension - parentDimension) / 2.0f
