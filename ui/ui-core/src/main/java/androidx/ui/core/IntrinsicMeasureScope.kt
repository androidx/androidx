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

package androidx.ui.core

import androidx.ui.unit.Density
import androidx.ui.unit.IntPx

/**
 * The receiver scope of a layout's intrinsic measurements lambdas.
 */
abstract class IntrinsicMeasureScope : Density {
    /**
     * The [LayoutDirection] of the `Layout` or `LayoutModifier2` using the measure scope
     * to measure their children.
     */
    // TODO(popam): Try to make this protected after the modules structure is updated.
    abstract val layoutDirection: LayoutDirection

    /**
     * Calculates the minimum width that the layout can be such that
     * the content of the layout will be painted correctly.
     */
    fun IntrinsicMeasurable.minIntrinsicWidth(height: IntPx) =
        minIntrinsicWidth(height, layoutDirection)

    /**
     * Calculates the smallest width beyond which increasing the width never
     * decreases the height.
     */
    fun IntrinsicMeasurable.maxIntrinsicWidth(height: IntPx) =
        maxIntrinsicWidth(height, layoutDirection)

    /**
     * Calculates the minimum height that the layout can be such that
     * the content of the layout will be painted correctly.
     */
    fun IntrinsicMeasurable.minIntrinsicHeight(width: IntPx) =
        minIntrinsicHeight(width, layoutDirection)

    /**
     * Calculates the smallest height beyond which increasing the height never
     * decreases the width.
     */
    fun IntrinsicMeasurable.maxIntrinsicHeight(width: IntPx) =
        maxIntrinsicHeight(width, layoutDirection)
}