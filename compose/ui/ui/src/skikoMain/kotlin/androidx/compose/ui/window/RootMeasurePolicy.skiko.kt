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
import androidx.compose.ui.util.fastMaxBy
import kotlin.math.min

internal fun RootMeasurePolicy(
    platformInsets: PlatformInsets,
    usePlatformDefaultWidth: Boolean,
    calculatePosition: MeasureScope.(contentSize: IntSize) -> IntOffset,
) = MeasurePolicy { measurables, constraints ->
    val platformConstraints = applyPlatformConstrains(
        constraints, platformInsets, usePlatformDefaultWidth
    )
    val placeables = measurables.fastMap { it.measure(platformConstraints) }
    val contentSize = IntSize(
        width = placeables.fastMaxBy { it.width }?.width ?: constraints.minWidth,
        height = placeables.fastMaxBy { it.height }?.height ?: constraints.minHeight
    )
    val position = calculatePosition(contentSize)
    layout(constraints.maxWidth, constraints.maxHeight) {
        placeables.fastForEach {
            it.place(position.x, position.y)
        }
    }
}

private fun Density.applyPlatformConstrains(
    constraints: Constraints,
    platformInsets: PlatformInsets,
    usePlatformDefaultWidth: Boolean
): Constraints {
    val horizontal = platformInsets.left.roundToPx() + platformInsets.right.roundToPx()
    val vertical = platformInsets.top.roundToPx() + platformInsets.bottom.roundToPx()
    val platformConstraints = constraints.offset(-horizontal, -vertical)
    return if (usePlatformDefaultWidth) {
        platformConstraints.constrain(
            platformDefaultConstrains(constraints)
        )
    } else {
        platformConstraints
    }
}

internal fun MeasureScope.positionWithInsets(
    insets: PlatformInsets,
    size: IntSize,
    calculatePosition: (sizeWithoutInsets: IntSize) -> IntOffset,
): IntOffset {
    val horizontal = insets.left.roundToPx() + insets.right.roundToPx()
    val vertical = insets.top.roundToPx() + insets.bottom.roundToPx()
    val sizeWithoutInsets = IntSize(
        width = size.width - horizontal,
        height = size.height - vertical
    )
    val position = calculatePosition(sizeWithoutInsets)
    val offset = IntOffset(
        x = insets.left.roundToPx(),
        y = insets.top.roundToPx()
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
