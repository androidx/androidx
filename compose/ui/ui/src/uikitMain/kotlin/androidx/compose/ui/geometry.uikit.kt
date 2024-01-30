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

package androidx.compose.ui

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSize

internal fun Rect.toCGRect() =
    CGRectMake(left.toDouble(), top.toDouble(), size.width.toDouble(), size.height.toDouble())

internal fun Rect.toCGRect(density: Double) =
    CGRectMake(
        left / density,
        top / density,
        size.width / density,
        size.height / density
    )

internal operator fun IntRect.div(divider: Float) =
    Rect(left / divider, top / divider, right / divider, bottom / divider)

internal fun CGSize.toDpSize(): DpSize = DpSize(width.dp, height.dp)
internal fun CGPoint.toDpOffset(): DpOffset = DpOffset(x.dp, y.dp)
internal fun CGRect.toDpRect(): DpRect = DpRect(origin.toDpOffset(), size.toDpSize())
