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

package androidx.xr.compose.subspace.layout

import androidx.annotation.IntRange
import androidx.compose.ui.unit.Dp

/** Base class for feathering effects. */
public abstract class SpatialFeatheringEffect internal constructor()

/**
 * A feathering effect that applies an alpha gradient to the edges of the canvas to give a fade out
 * effect.
 */
internal class SpatialSmoothFeatheringEffect(internal val size: SpatialSmoothFeatheringSize) :
    SpatialFeatheringEffect()

/**
 * Defines [SpatialSmoothFeatheringEffect] based on the percent width and height of the layout.
 *
 * @param percentHorizontal Value to feather horizontal edges. A value of 5 represents 5% of the
 *   width of the visible canvas. Accepted value range is 0 - 50 percent.
 * @param percentVertical Value to feather vertical edges. A value of 5 represents 5% of the height
 *   the visible canvas. Accepted value range is 0 - 50 percent.
 */
public fun SpatialSmoothFeatheringEffect(
    @IntRange(from = 0, to = 50) percentHorizontal: Int,
    @IntRange(from = 0, to = 50) percentVertical: Int,
): SpatialFeatheringEffect =
    SpatialSmoothFeatheringEffect(spatialSmoothFeatheringSize(percentHorizontal, percentVertical))

/**
 * Defines a [SpatialSmoothFeatheringEffect] using [Dp].
 *
 * @param horizontal Non-negative [Dp] value to feather horizontal edges. Value will be capped at
 *   50% of canvas width if it is too large.
 * @param vertical Non-negative [Dp] value to feather vertical edges. Value will be capped at 50% of
 *   canvas height if it is too large.
 */
public fun SpatialSmoothFeatheringEffect(horizontal: Dp, vertical: Dp): SpatialFeatheringEffect =
    SpatialSmoothFeatheringEffect(spatialSmoothFeatheringSize(horizontal, vertical))

/**
 * Defines a [SpatialSmoothFeatheringEffect] using pixels.
 *
 * @param horizontal Non-negative pixels value to feather horizontal edges. Value will be capped at
 *   50% of canvas width if it is too large.
 * @param vertical Non-negative pixels value to feather vertical edges. Value will be capped at 50%
 *   of canvas height if it is too large.
 */
public fun SpatialSmoothFeatheringEffect(
    horizontal: Float,
    vertical: Float,
): SpatialFeatheringEffect =
    SpatialSmoothFeatheringEffect(spatialSmoothFeatheringSize(horizontal, vertical))

/** An effect representing no feathering. */
public val ZeroFeatheringEffect: SpatialFeatheringEffect = SpatialSmoothFeatheringEffect(0, 0)
