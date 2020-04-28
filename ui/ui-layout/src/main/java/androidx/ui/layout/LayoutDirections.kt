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

import androidx.ui.core.Constraints
import androidx.ui.core.IntrinsicMeasurable
import androidx.ui.core.IntrinsicMeasureScope
import androidx.ui.core.LayoutDirection
import androidx.ui.core.LayoutModifier
import androidx.ui.core.Measurable
import androidx.ui.core.MeasureScope
import androidx.ui.core.Modifier
import androidx.ui.unit.IntPx
import androidx.ui.unit.ipx

/**
 * [Modifier] that changes the [LayoutDirection] of the wrapped layout to [LayoutDirection.Ltr].
 */
val Modifier.ltr: Modifier get() = this + LtrModifier

/**
 * [Modifier] that changes the [LayoutDirection] of the wrapped layout to [LayoutDirection.Rtl].
 */
val Modifier.rtl: Modifier get() = this + RtlModifier

private val LtrModifier = LayoutDirectionModifier(LayoutDirection.Ltr)

private val RtlModifier = LayoutDirectionModifier(LayoutDirection.Rtl)

private data class LayoutDirectionModifier(
    val prescribedLayoutDirection: LayoutDirection
) : LayoutModifier {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
        layoutDirection: LayoutDirection
    ): MeasureScope.MeasureResult {
        val placeable = measurable.measure(constraints, prescribedLayoutDirection)
        return layout(placeable.width, placeable.height) {
            placeable.place(0.ipx, 0.ipx)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: IntPx,
        layoutDirection: LayoutDirection
    ) = measurable.minIntrinsicWidth(height, prescribedLayoutDirection)

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: IntPx,
        layoutDirection: LayoutDirection
    ) = measurable.minIntrinsicHeight(width, prescribedLayoutDirection)

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: IntPx,
        layoutDirection: LayoutDirection
    ) = measurable.maxIntrinsicWidth(height, prescribedLayoutDirection)

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: IntPx,
        layoutDirection: LayoutDirection
    ) = measurable.maxIntrinsicHeight(width, prescribedLayoutDirection)
}
