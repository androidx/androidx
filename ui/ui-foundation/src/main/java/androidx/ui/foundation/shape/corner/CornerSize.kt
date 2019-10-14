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

package androidx.ui.foundation.shape.corner

import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.ui.core.Density
import androidx.ui.core.Dp
import androidx.ui.core.Px
import androidx.ui.core.PxSize
import androidx.ui.core.minDimension
import androidx.ui.core.px
import androidx.ui.core.withDensity

/**
 * Defines size of a corner in [Px]. For example for rounded shape it can be a corner radius.
 */
interface CornerSize {
    /**
     * @param shapeSize the size of the shape
     * @param density the current density of the screen.
     *
     * @return resolved size of the corner in [Px]
     */
    fun toPx(shapeSize: PxSize, density: Density): Px
}

/**
 * @size the corner size defined in [Dp].
 */
fun CornerSize(size: Dp): CornerSize = DpCornerSize(size)

private data class DpCornerSize(private val size: Dp) : CornerSize {
    override fun toPx(shapeSize: PxSize, density: Density) =
        withDensity(density) { size.toPx() }
}

/**
 * @size the corner size defined in [Px].
 */
fun CornerSize(size: Px): CornerSize = PxCornerSize(size)

private data class PxCornerSize(private val size: Px) : CornerSize {
    override fun toPx(shapeSize: PxSize, density: Density) = size
}

/**
 * @percent the corner size defined in percents of the shape's smaller side.
 * Can't be negative or larger then 50 percents.
 */
fun /*inline*/ CornerSize(@IntRange(from = 0, to = 50) percent: Int) =
    CornerSize(percent.toFloat())

/**
 * @percent the corner size defined in float percents of the shape's smaller side.
 * Can't be negative or larger then 50 percents.
 */
fun CornerSize(@FloatRange(from = 0.0, to = 50.0) percent: Float): CornerSize =
    PercentCornerSize(percent)

private data class PercentCornerSize(private val percent: Float) : CornerSize {
    init {
        if (percent < 0 || percent > 50) {
            throw IllegalArgumentException("The percent should be in the range of [0, 50]")
        }
    }

    override fun toPx(shapeSize: PxSize, density: Density) =
        shapeSize.minDimension * (percent / 100f)
}

/**
 * [CornerSize] always equals to zero.
 */
val ZeroCornerSize: CornerSize = object : CornerSize {
    override fun toPx(shapeSize: PxSize, density: Density) = 0.px
}
