/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.window

import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import java.awt.Dimension
import java.awt.Point
import java.awt.Rectangle

internal val Dimension.rightBottom get() = Point(width, height)
internal operator fun Point.plus(other: Point) = Point(x + other.x, y + other.y)
internal operator fun Point.minus(other: Point) = Point(x - other.x, y - other.y)

internal val Rectangle.leftTop get() = Point(x, y)
internal val Rectangle.rightBottom get() = Point(x + width, y + height)

internal fun Dimension.asDpSize() = DpSize(width.dp, height.dp)
internal fun Point.asDpOffset() = DpOffset(x.dp, y.dp)
internal fun Rectangle.asDpRect() = DpRect(
    left = x.dp,
    top = y.dp,
    right = x.dp + width.dp,
    bottom = y.dp + height.dp
)
