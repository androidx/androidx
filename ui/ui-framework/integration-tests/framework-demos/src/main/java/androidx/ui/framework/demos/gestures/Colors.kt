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

val Red = Color(0xFFf44336.toInt())
val Pink = Color(0xFFe91e63.toInt())
val Purple = Color(0xFF9c27b0.toInt())
val DeepPurple = Color(0xFF673ab7.toInt())
val Indigo = Color(0xFF3f51b5.toInt())
val Blue = Color(0xFF2196f3.toInt())
val LightBlue = Color(0xFF03a9f4.toInt())
val Cyan = Color(0xFF00bcd4.toInt())
val Teal = Color(0xFF009688.toInt())
val Green = Color(0xFF4caf50.toInt())
val LightGreen = Color(0xFF8bc34a.toInt())
val Lime = Color(0xFFcddc39.toInt())
val Yellow = Color(0xFFffeb3b.toInt())
val Amber = Color(0xFFffc107.toInt())
val Orange = Color(0xFFff9800.toInt())
val DeepOrange = Color(0xFFff5722.toInt())
val Brown = Color(0xFF795548.toInt())
val Grey = Color(0xFF9e9e9e.toInt())
val BlueGrey = Color(0xFF607d8b.toInt())

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