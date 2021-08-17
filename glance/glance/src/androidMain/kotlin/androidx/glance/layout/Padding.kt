@file:OptIn(GlanceInternalApi::class)
/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.glance.layout

import androidx.glance.unit.Dp
import androidx.glance.unit.dp
import androidx.glance.GlanceInternalApi
import androidx.glance.Modifier

/**
 * Apply additional space along each edge of the content in [Dp]: [start], [top], [end] and
 * [bottom]. The start and end edges will be determined by layout direction of the current locale.
 * Padding is applied before content measurement and takes precedence; content may only be as large
 * as the remaining space.
 */
fun Modifier.padding(
    start: Dp = 0.dp,
    top: Dp = 0.dp,
    end: Dp = 0.dp,
    bottom: Dp = 0.dp
) = this.then(
    PaddingModifier(
        start = start,
        top = top,
        end = end,
        bottom = bottom,
        rtlAware = true
    )
)

/**
 * Apply [horizontal] dp space along the left and right edges of the content, and [vertical] dp
 * space along the top and bottom edges.
 */
fun Modifier.padding(
    horizontal: Dp = 0.dp,
    vertical: Dp = 0.dp
) = this.then(
    PaddingModifier(
        start = horizontal,
        top = vertical,
        end = horizontal,
        bottom = vertical,
        rtlAware = true
    )
)

/**
 * Apply [all] dp of additional space along each edge of the content, left, top, right and bottom.
 */
fun Modifier.padding(all: Dp) = this.then(
    PaddingModifier(
        start = all,
        top = all,
        end = all,
        bottom = all,
        rtlAware = true
    )
)

/**
 *  Apply additional space along each edge of the content in [Dp]: [left], [top], [right] and
 * [bottom], ignoring the current locale's layout direction.
 */
fun Modifier.absolutePadding(
    left: Dp = 0.dp,
    top: Dp = 0.dp,
    right: Dp = 0.dp,
    bottom: Dp = 0.dp
) = this.then(
    PaddingModifier(
        start = left,
        top = top,
        end = right,
        bottom = bottom,
        rtlAware = false
    )
)

@GlanceInternalApi
class PaddingModifier(
    val start: Dp = 0.dp,
    val top: Dp = 0.dp,
    val end: Dp = 0.dp,
    val bottom: Dp = 0.dp,
    val rtlAware: Boolean
) : Modifier.Element {
    override fun toString(): String {
        return "PaddingModifier(start=$start, top=$top, end=$end, bottom=$bottom, " +
            "rtlAware=$rtlAware)"
    }
}
