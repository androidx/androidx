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

@file:Suppress("NOTHING_TO_INLINE")

package androidx.compose.ui.unit

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import kotlin.math.roundToInt

/**
 * Convert a [Offset] to a [DpOffset].
 */
@Stable
internal fun Offset.toDpOffset(density: Density): DpOffset = with(density) {
    if (isSpecified) {
        DpOffset(x.toDp(), y.toDp())
    } else {
        DpOffset.Unspecified
    }
}

/**
 * Convert a [DpOffset] to a [Offset].
 */
@Stable
internal fun DpOffset.toOffset(density: Density): Offset = with(density) {
    if (isSpecified) {
        Offset(x.toPx(), y.toPx())
    } else {
        Offset.Unspecified
    }
}

/**
 * Converts a [Rect] to a [DpRect].
 */
@Stable
internal inline fun Rect.toDpRect(density: Density): DpRect = with(density) {
    DpRect(
        origin = topLeft.toDpOffset(density),
        size = size.toDpSize()
    )
}

/** Convert a [DpRect] to a [Rect]. */
// Preventing more copies, keep for discoverability
@Stable
internal inline fun DpRect.toRect(density: Density): Rect = with(density) {
    toRect()
}

/** Convert a [Size] to a [DpSize]. */
// Preventing more copies, keep for discoverability
@Stable
internal inline fun Size.toDpSize(density: Density): DpSize = with(density) {
   toDpSize()
}

/** Convert a [DpSize] to a [Size]. */
// Preventing more copies, keep for discoverability
@Stable
internal inline fun DpSize.toSize(density: Density): Size = with(density) {
   toSize()
}

/**
 * Coerces this [DpSize] to at most the specified [size], on each axis.
 */
@Stable
internal inline fun DpSize.coerceAtMost(size: DpSize): DpSize =
    DpSize(
        width = width.coerceAtMost(size.width),
        height = height.coerceAtMost(size.height)
    )

/**
 * Converts a [IntSize] to a [Rect].
 */
@Stable
internal inline fun IntSize.toRect(): Rect =
    Rect(0f, 0f, width.toFloat(), height.toFloat())

@Stable
internal fun IntSize.toDpSize(density: Density): DpSize {
    with(density) {
        return DpSize(width.toDp(), height.toDp())
    }
}

@Stable
internal fun DpSize.roundToIntSize() = IntSize(
    width = width.value.roundToInt(),
    height = height.value.roundToInt()
)

@Stable
internal val DpRect.topLeft: DpOffset get() = DpOffset(left, top)

@Stable
internal operator fun DpRect.plus(offset: DpOffset): DpRect =
    DpRect(left + offset.x, top + offset.y, right + offset.x, bottom + offset.y)

@Stable
internal val Dp.isReal
    get() = isSpecified && isFinite

@Stable
internal fun Dp.requireReal(propertyName: String) =
    require(isReal) { "$propertyName must be specified and finite"}

@Stable
internal fun DpRect.requireReal(): DpRect {
    left.requireReal("left")
    top.requireReal("top")
    right.requireReal("right")
    bottom.requireReal("bottom")
    return this
}

@Stable
internal fun DpSize.requireReal(): DpSize {
    require(isSpecified) { "size must be specified" }
    width.requireReal("width")
    height.requireReal("height")
    return this
}

@Stable
internal fun DpOffset.requireReal(): DpOffset {
    require(isSpecified) { "offset must be specified" }
    x.requireReal("x")
    y.requireReal("y")
    return this
}
