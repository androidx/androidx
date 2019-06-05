/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.baseui.shape.corner

import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.ui.core.DensityReceiver
import androidx.ui.core.Dp
import androidx.ui.core.Px
import androidx.ui.core.PxSize
import androidx.ui.core.minDimension
import androidx.ui.core.px

/**
 * Defines size of a corner. For example for rounded shape it can be a corner radius.
 */
typealias CornerSize = DensityReceiver.(PxSize) -> Px

/**
 * @size the corner size defined in [Dp].
 */
fun CornerSize(size: Dp): CornerSize = { size.toPx() }

/**
 * @size the corner size defined in [Px].
 */
fun CornerSize(size: Px): CornerSize = { size }

/**
 * @percent the corner size defined in float percents of the shape's smaller side.
 * Can't be negative or larger then 50 percents.
 */
fun CornerSize(@FloatRange(from = 0.0, to = 50.0) percent: Float): CornerSize {
    if (percent < 0 || percent > 50) {
        throw IllegalArgumentException()
    }
    return { size -> size.minDimension * (percent / 100f) }
}

/**
 * @percent the corner size defined in percents of the shape's smaller side.
 * Can't be negative or larger then 50 percents.
 */
fun /*inline*/ CornerSize(@IntRange(from = 0, to = 50) percent: Int) =
    CornerSize(percent.toFloat())

/**
 * [CornerSize] always equals to zero.
 */
val ZeroCornerSize: CornerSize = { 0.px }
