/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.compose.layout

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize

/**
 * Applies a container layout to a Composable.
 *
 * Sizes the Composable based on the provided [ContainerConfig]. The container size is calculated as
 * a percentage of the incoming constraints, clamped within the bounds specified in
 * [ContainerConfig.sizeBounds].
 *
 * It's recommended to use [com.android.compose.windowsizeclass.LocalWindowSizeClass] to evaluate if
 * container layout should be applied.
 *
 * Example usage:
 * ```
 * val isContainerized =
 *     LocalWindowSizeClass.current.isWidthAtLeastBreakpoint(
 *         WindowSizeClass.WIDTH_DP_EXTRA_LARGE_LOWER_BOUND
 *     )
 *
 * Modifier.then(
 *         if (isContainerized) {
 *             Modifier.containerize(containerConfig())
 *         } else {
 *             Modifier.fillMaxSize()
 *         }
 *     )
 * ```
 *
 * @param config The configuration for calculating the container size.
 */
fun Modifier.containerize(config: ContainerConfig) =
    this.layout { measurable, constraints ->
        val placeable = measurable.measure(config.calculateContainerSize(this, constraints))
        layout(placeable.width, placeable.height) { placeable.placeRelative(x = 0, y = 0) }
    }

/**
 * Configuration for the [containerize] modifier.
 *
 * @property sizePercentage The target size as a percentage (0.0 to 1.0) of the parent's
 *   constraints.
 * @property minSize The minimum container size.
 * @property maxSize The maximum container size.
 */
class ContainerConfig(val sizePercentage: Float, val minSize: DpSize, val maxSize: DpSize) {
    /**
     * Calculates fixed size [Constraints] for the container.
     *
     * The width and height are calculated as [sizePercentage] of the parent's maxWidth and
     * maxHeight, respectively. These values are then coerced to be between [minSize] and [maxSize].
     * The final size is also capped by the incoming [constraints]' maxWidth and maxHeight.
     *
     * Note that the final size will be smaller than [minSize] if incoming [constraints] are smaller
     * than [minSize].
     *
     * @param density Display [Density].
     * @param constraints The [Constraints] from the parent layout.
     * @return The calculated [Constraints] to be applied to the child.
     */
    fun calculateContainerSize(density: Density, constraints: Constraints): Constraints {
        with(density) {
            val width =
                minOf(
                    (constraints.maxWidth * sizePercentage)
                        .toInt()
                        .coerceIn(minSize.width.roundToPx(), maxSize.width.roundToPx()),
                    constraints.maxWidth,
                )
            val height =
                minOf(
                    (constraints.maxHeight * sizePercentage)
                        .toInt()
                        .coerceIn(minSize.height.roundToPx(), maxSize.height.roundToPx()),
                    constraints.maxHeight,
                )
            return Constraints.fixed(width, height)
        }
    }
}
