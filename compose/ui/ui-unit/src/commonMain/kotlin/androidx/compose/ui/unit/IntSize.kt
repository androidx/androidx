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

package androidx.compose.ui.unit

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.packInts
import androidx.compose.ui.util.unpackInt1
import androidx.compose.ui.util.unpackInt2

/** Constructs an [IntSize] from width and height [Int] values. */
@Stable inline fun IntSize(width: Int, height: Int): IntSize = IntSize(packInts(width, height))

/** A two-dimensional size class used for measuring in [Int] pixels. */
@Immutable
@kotlin.jvm.JvmInline
value class IntSize
@PublishedApi
internal constructor(@PublishedApi internal val packedValue: Long) {
    /** The horizontal aspect of the size in [Int] pixels. */
    @Stable
    inline val width: Int
        get() = unpackInt1(packedValue)

    /** The vertical aspect of the size in [Int] pixels. */
    @Stable
    inline val height: Int
        get() = unpackInt2(packedValue)

    @Stable inline operator fun component1(): Int = width

    @Stable inline operator fun component2(): Int = height

    /** Returns an IntSize scaled by multiplying [width] and [height] by [other] */
    @Stable
    operator fun times(other: Int): IntSize =
        IntSize(packInts(unpackInt1(packedValue) * other, unpackInt2(packedValue) * other))

    /** Returns an IntSize scaled by dividing [width] and [height] by [other] */
    @Stable
    operator fun div(other: Int): IntSize =
        IntSize(packInts(unpackInt1(packedValue) / other, unpackInt2(packedValue) / other))

    @Stable override fun toString(): String = "$width x $height"

    companion object {
        /** IntSize with a zero (0) width and height. */
        val Zero = IntSize(0L)
    }
}

/** Returns an [IntSize] with [size]'s [IntSize.width] and [IntSize.height] multiplied by [this]. */
@Stable inline operator fun Int.times(size: IntSize) = size * this

/** Convert a [IntSize] to a [IntRect]. */
@Stable
fun IntSize.toIntRect(): IntRect {
    return IntRect(IntOffset.Zero, this)
}

/**
 * Returns the [IntOffset] of the center of the rect from the point of [0, 0] with this [IntSize].
 */
@Stable
val IntSize.center: IntOffset
    get() =
        IntOffset(
            // Divide X by 2 by moving it to the low bits, then place it back in the high bits
            (packedValue shr 33 shl 32) or
                // Move Y to the high bits so we can preserve the sign when dividing by 2, then
                // move Y back to the low bits and mask out the top 32 bits for X
                ((packedValue shl 32 shr 33) and 0xffffffffL)
        )

// temporary while PxSize is transitioned to Size
@Stable fun IntSize.toSize() = Size(width.toFloat(), height.toFloat())

/**
 * Convert a [Size] to an [IntSize]. This rounds the width and height values down to the nearest
 * integer.
 */
@Stable fun Size.toIntSize(): IntSize = IntSize(packInts(this.width.toInt(), this.height.toInt()))

/**
 * Convert a [Size] to an [IntSize]. This rounds [Size.width] and [Size.height] to the nearest
 * integer.
 */
@Stable
fun Size.roundToIntSize(): IntSize =
    IntSize(packInts(this.width.fastRoundToInt(), this.height.fastRoundToInt()))
