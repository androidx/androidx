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

@file:Suppress("NOTHING_TO_INLINE")

package androidx.ui.unit

import androidx.compose.Immutable
import androidx.compose.Stable
import androidx.ui.util.packInts
import androidx.ui.util.unpackInt1
import androidx.ui.util.unpackInt2

/**
 * A two-dimensional size class used for measuring in [Int] pixels.
 */
@Suppress("EXPERIMENTAL_FEATURE_WARNING")
@Immutable
inline class IntSize(@PublishedApi internal val value: Long) {
    /**
     * The horizontal aspect of the size in [Int] pixels.
     */
    @Stable
    val width: Int
        get() = unpackInt1(value)

    /**
     * The vertical aspect of the size in [Int] pixels.
     */
    @Stable
    val height: Int
        get() = unpackInt2(value)

    /**
     * Returns an IntSize scaled by multiplying [width] and [height] by [other]
     */
    @Stable
    operator fun times(other: Int): IntSize =
        IntSize(width = width * other, height = height * other)

    /**
     * Returns an IntSize scaled by dividing [width] and [height] by [other]
     */
    @Stable
    operator fun div(other: Int): IntSize =
        IntSize(width = width / other, height = height / other)

    @Stable
    override fun toString(): String = "$width x $height"
}

/**
 * Returns an [IntSize] with [size]'s [IntSize.width] and [IntSize.height]
 * multiplied by [this].
 */
@Stable
operator fun Int.times(size: IntSize) = size * this

/**
 * Constructs an [IntPxSize] from width and height [IntPx] values.
 */
@Stable
fun IntSize(width: Int, height: Int): IntSize =
    IntSize(packInts(width, height))
