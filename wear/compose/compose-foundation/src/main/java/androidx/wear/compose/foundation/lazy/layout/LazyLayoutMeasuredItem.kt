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

package androidx.wear.compose.foundation.lazy.layout

import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.wear.compose.foundation.lazy.MeasurementDirection

internal interface LazyLayoutMeasuredItem {
    val index: Int
    val key: Any
    val mainAxisSizeWithSpacings: Int
    val measuredHeight: Int
    val transformedHeight: Int
    val measurementDirection: MeasurementDirection
    val constraints: Constraints

    val mainAxisOffset: Int
    val crossAxisOffset: Int

    val parentData: Any?
}

internal fun LazyLayoutMeasuredItem.getOffset(): IntOffset =
    IntOffset(x = crossAxisOffset, y = mainAxisOffset)

internal fun LazyLayoutMeasuredItem.hasAnimations(): Boolean = parentData.specs != null

internal val Any?.specs
    get() = this as? LazyLayoutAnimationSpecsNode
