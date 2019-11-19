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

package androidx.ui.framework.demos.gestures

import androidx.ui.graphics.Color
import kotlin.random.Random

val DefaultBackgroundColor = Color(0xffffffff)
val PressedColor = Color(0x1f000000)
val BorderColor = Color(0x1f000000)

val Red = Color(0xFFf44336)
val Pink = Color(0xFFe91e63)
val Purple = Color(0xFF9c27b0)
val DeepPurple = Color(0xFF673ab7)
val Indigo = Color(0xFF3f51b5)
val Blue = Color(0xFF2196f3)
val LightBlue = Color(0xFF03a9f4)
val Cyan = Color(0xFF00bcd4)
val Teal = Color(0xFF009688)
val Green = Color(0xFF4caf50)
val LightGreen = Color(0xFF8bc34a)
val Lime = Color(0xFFcddc39)
val Yellow = Color(0xFFffeb3b)
val Amber = Color(0xFFffc107)
val Orange = Color(0xFFff9800)
val DeepOrange = Color(0xFFff5722)
val Brown = Color(0xFF795548)
val Grey = Color(0xFF9e9e9e)
val BlueGrey = Color(0xFF607d8b)

val Colors = listOf(
    Red,
    Pink,
    Purple,
    DeepPurple,
    Indigo,
    Blue,
    LightBlue,
    Cyan,
    Teal,
    Green,
    LightGreen,
    Lime,
    Yellow,
    Amber,
    Orange,
    DeepOrange,
    Brown,
    Grey,
    BlueGrey
)

fun Color.anotherRandomColor() = Colors.random(this)

fun Color.next() = Colors.inOrder(this, true)
fun Color.prev() = Colors.inOrder(this, false)

fun List<Color>.random(exclude: Color?): Color {
    val excludeIndex = indexOf(exclude)

    val max = size - if (excludeIndex >= 0) 1 else 0

    val random = Random.nextInt(max).run {
        if (excludeIndex >= 0 && this >= excludeIndex) {
            this + 1
        } else {
            this
        }
    }

    return this[random]
}

fun List<Color>.inOrder(current: Color?, forward: Boolean): Color {
    val currentIndex = indexOf(current)

    val next =
        if (forward) {
            if (currentIndex == -1) {
                0
            } else {
                (currentIndex + 1) % size
            }
        } else {
            if (currentIndex == -1) {
                size - 1
            } else {
                (currentIndex - 1 + size) % size
            }
        }

    return this[next]
}

@Suppress("NOTHING_TO_INLINE")
private inline fun mergeColor(c1: Float, a1: Float, c2: Float, a2: Float): Float {
    return (c1 * a1 + (c2 * a2) * (1f - a1)) / (a1 + a2 * (1f - a1))
}

fun Color.over(that: Color): Color {
    val a1 = alpha
    val a2 = that.alpha
    val r = mergeColor(red, a1, that.red, a2)
    val g = mergeColor(green, a1, that.green, a2)
    val b = mergeColor(blue, a1, that.blue, a2)
    val a = a1 + (a2 * (1 - a1))

    return Color(red = r, green = g, blue = b, alpha = a)
}