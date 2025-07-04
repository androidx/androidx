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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.xr.compose.unit.Meter
import androidx.xr.compose.unit.toMeter
import androidx.xr.runtime.math.Pose

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

internal fun calculatePose(
    contentOffset: Offset,
    parentViewSize: IntSize,
    contentSize: IntSize,
    density: Density,
    zDepth: Dp = 0.dp,
): Pose {
    val meterPosition =
        contentOffset.toMeterPosition(parentViewSize, contentSize, density) +
            MeterPosition(z = zDepth.toMeter())
    return Pose(translation = meterPosition.toVector3())
}

/**
 * Resolves the coordinate systems between 2D app pixel space and 3D meter space. In 2d space, views
 * and composables are anchored at the top left corner; however, in 3D space they are anchored at
 * the center. This fixes that by adjusting for the space size and the content's size so they are
 * anchored in the top left corner in 3D space.
 *
 * This conversion requires that [density] be specified.
 */
private fun Offset.toMeterPosition(
    parentViewSize: IntSize,
    contentSize: IntSize,
    density: Density,
) =
    MeterPosition(
        Meter.fromPixel(x.scale(contentSize.width, parentViewSize.width), density),
        Meter.fromPixel(-y.scale(contentSize.height, parentViewSize.height), density),
    )

private fun Float.scale(size: Int, space: Int) = this + (size - space) / 2.0f
