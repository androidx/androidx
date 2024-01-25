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

package androidx.compose.ui.test

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.constrain
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.max
import kotlin.math.min

/**
 * Render [content] that is forced to have the given [size] without clipping.
 *
 * This is only suitable for tests, since this will override [LocalDensity] to ensure that the
 * [size] is met (as opposed to `Modifier.requiredSize` which will result in clipping).
 */
@Composable
internal fun DensityForcedSize(
    size: DpSize,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    SubcomposeLayout(
        modifier = modifier
    ) { constraints ->
        val measurables = subcompose(Unit) {
            val maxWidth = constraints.maxWidth.toDp()
            val maxHeight = constraints.maxHeight.toDp()
            val requiredWidth = if (size.isSpecified) {
                max(maxWidth, size.width)
            } else {
                maxWidth
            }
            val requiredHeight = if (size.isSpecified) {
                max(maxHeight, size.height)
            } else {
                maxHeight
            }
            // Compute the minimum density required so that both the requested width and height both
            // fit
            val density = LocalDensity.current.density * min(
                maxWidth / requiredWidth,
                maxHeight / requiredHeight,
            )

            CompositionLocalProvider(
                LocalDensity provides Density(
                    // Override the density with the factor needed to meet both the minimum width and
                    // height requirements, and the platform density requirements.
                    density = coerceDensity(density),
                    // Pass through the font scale
                    fontScale = LocalDensity.current.fontScale
                )
            ) {
                Layout(
                    content = content,
                    // This size will now be guaranteed to be able to match the constraints
                    modifier = Modifier
                        .then(
                            if (size.isSpecified) {
                                Modifier.size(size)
                            } else {
                                Modifier
                            }
                        )
                ) { measurables, constraints ->
                    val placeables = measurables.map { it.measure(constraints) }
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeables.forEach {
                            it.placeRelative(0, 0)
                        }
                    }
                }
            }
        }

        val placeables = measurables.map { it.measure(constraints) }

        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEach {
                it.placeRelative(0, 0)
            }
        }
    }
}

/**
 * Applies platform specific coercion to the given [density].
 *
 * On some platforms, not every possible density is possible. This should coerce the density to a
 * possible density, rounding down to the nearest possible density to ensure that there is enough
 * space to display the content.
 */
internal expect fun coerceDensity(density: Float): Float

/**
 * A very simplified size [Modifier].
 */
@Stable
private fun Modifier.size(size: DpSize) = layout { measurable, constraints ->
    val placeable = measurable.measure(
        constraints.constrain(
            Constraints.fixed(
                size.width.roundToPx(),
                size.height.roundToPx()
            )
        )
    )

    layout(placeable.width, placeable.height) {
        placeable.placeRelative(0, 0)
    }
}
