/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.vectormath64

const val PI = 3.1415926536
const val HALF_PI = PI * 0.5
const val TWO_PI = PI * 2.0
const val FOUR_PI = PI * 4.0
const val INV_PI = 1.0 / PI
const val INV_TWO_PI = INV_PI * 0.5
const val INV_FOUR_PI = INV_PI * 0.25

inline fun mix(a: Double, b: Double, x: Double) = a * (1.0 - x) + b * x

inline fun degrees(v: Double) = v * (180.0 * INV_PI)

inline fun radians(v: Double) = v * (PI / 180.0)

inline fun fract(v: Double) = v % 1

inline fun sqr(v: Double) = v * v

inline fun pow(x: Double, y: Double) = StrictMath.pow(x, y)
