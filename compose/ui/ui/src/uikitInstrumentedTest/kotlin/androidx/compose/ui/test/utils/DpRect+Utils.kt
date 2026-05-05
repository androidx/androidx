/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.test.utils

import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.toDpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGPointMake
import platform.UIKit.UIView

@OptIn(ExperimentalForeignApi::class)
internal fun DpOffset.toCGPoint(): CValue<CGPoint> = CGPointMake(x.value.toDouble(), y.value.toDouble())

internal fun DpRect.center(): DpOffset = DpOffset((left + right) / 2, (top + bottom) / 2)

internal fun DpRectZero() = DpRect(0.dp, 0.dp, 0.dp, 0.dp)

internal fun DpRect.intersect(other: DpRect): DpRect {
    if (right < other.left || other.right < left) return DpRectZero()
    if (bottom < other.top || other.bottom < top) return DpRectZero()
    return DpRect(
        left = max(left, other.left),
        top = max(top, other.top),
        right = min(right, other.right),
        bottom = min(bottom, other.bottom)
    )
}

@OptIn(ExperimentalForeignApi::class)
internal fun UIView.dpRectInWindow() = convertRect(bounds, toView = null).toDpRect()
internal fun<T> List<T>.forEachWithPrevious(block: (T, T) -> Unit) {
    var previous: T? = null
    for (current in this) {
        previous?.let { block(it, current) }
        previous = current
    }
}
