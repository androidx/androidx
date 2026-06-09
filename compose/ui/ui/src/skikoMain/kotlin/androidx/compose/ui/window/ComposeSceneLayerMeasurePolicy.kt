/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.window

import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.PlatformInsets
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.constrain
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxOfOrDefault
import kotlin.math.min

internal fun ComposeSceneLayerMeasurePolicy(
    platformInsets: PlatformInsets,
    usePlatformDefaultWidth: Boolean,
    calculatePosition: MeasureScope.(contentSize: IntSize) -> IntOffset,
) = MeasurePolicy { measurables, constraints ->
    val platformConstraints = applyPlatformConstraints(
        constraints, platformInsets, usePlatformDefaultWidth
    )
    val placeables = measurables.fastMap { it.measure(platformConstraints) }
    val contentSize = IntSize(
        width = placeables.fastMaxOfOrDefault(constraints.minWidth) { it.width },
        height = placeables.fastMaxOfOrDefault(constraints.minHeight) { it.height }
    )

    // When unconstrained, use content size as layout dimensions to allow the content's
    // preferred size to be measured
    val width = if (constraints.hasBoundedWidth) constraints.maxWidth else contentSize.width
    val height = if (constraints.hasBoundedHeight) constraints.maxHeight else contentSize.height
    layout(width, height) {
        val position = calculatePosition(contentSize)
        placeables.fastForEach {
            it.place(position.x, position.y)
        }
    }
}

private fun Density.applyPlatformConstraints(
    constraints: Constraints,
    platformInsets: PlatformInsets,
    usePlatformDefaultWidth: Boolean
): Constraints {
    val horizontal = platformInsets.left + platformInsets.right
    val vertical = platformInsets.top + platformInsets.bottom
    val platformConstraints = constraints.offset(-horizontal, -vertical)
    return if (usePlatformDefaultWidth) {
        platformConstraints.constrain(
            platformDefaultConstrains(constraints)
        )
    } else {
        platformConstraints
    }
}

@Suppress("UnusedReceiverParameter")
internal fun MeasureScope.positionWithInsets(
    insets: PlatformInsets,
    size: IntSize,
    calculatePosition: (sizeWithoutInsets: IntSize) -> IntOffset,
): IntOffset {
    val horizontal = insets.left + insets.right
    val vertical = insets.top + insets.bottom
    val sizeWithoutInsets = IntSize(
        width = size.width - horizontal,
        height = size.height - vertical
    )
    val position = calculatePosition(sizeWithoutInsets)
    val offset = IntOffset(
        x = insets.left,
        y = insets.top
    )
    return position + offset
}

private fun Density.platformDefaultConstrains(
    constraints: Constraints
): Constraints = constraints.copy(
    maxWidth = min(preferredDialogWidth(constraints), constraints.maxWidth)
)

// Ported from Android. See https://cs.android.com/search?q=abc_config_prefDialogWidth
private fun Density.preferredDialogWidth(constraints: Constraints): Int {
    val smallestWidth = min(constraints.maxWidth, constraints.maxHeight).toDp()
    return when {
        smallestWidth >= 600.dp -> 580.dp
        smallestWidth >= 480.dp -> 440.dp
        else -> 320.dp
    }.roundToPx()
}
