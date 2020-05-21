/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.layout

import androidx.compose.Stable
import androidx.compose.State
import androidx.compose.mutableStateOf
import androidx.ui.core.Constraints
import androidx.ui.core.LayoutDirection
import androidx.ui.core.LayoutModifier
import androidx.ui.core.Measurable
import androidx.ui.core.MeasureScope
import androidx.ui.core.Modifier
import androidx.ui.unit.Dp
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
import kotlin.math.roundToInt

/**
 * Offset the content by ([x] dp, [y] dp). The offsets can be positive as well as non positive.
 *
 * Example usage:
 * @sample androidx.ui.layout.samples.LayoutOffsetModifier
 */
@Stable
fun Modifier.offset(x: Dp = 0.dp, y: Dp = 0.dp) = this + OffsetModifier(x, y)

/**
 * Offset the content by ([x] px, [y]px). The offsets can be positive as well as non positive.
 * This modifier is designed to be used for offsets that change, possibly due to user interactions.
 *
 * Example usage:
 * @sample androidx.ui.layout.samples.LayoutOffsetPxModifier
 */
fun Modifier.offsetPx(
    x: State<Float> = mutableStateOf(0f),
    y: State<Float> = mutableStateOf(0f)
) = this + OffsetPxModifier(x, y)

private data class OffsetModifier(val x: Dp, val y: Dp) : LayoutModifier {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
        layoutDirection: LayoutDirection
    ): MeasureScope.MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.place(x.toIntPx(), y.toIntPx())
        }
    }
}

private data class OffsetPxModifier(val x: State<Float>, val y: State<Float>) : LayoutModifier {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
        layoutDirection: LayoutDirection
    ): MeasureScope.MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.place(x.value.roundToInt().ipx, y.value.roundToInt().ipx)
        }
    }
}
