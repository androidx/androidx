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

package androidx.compose.ui.util

// See explanation in InlineClassHelper.kt
actual inline fun floatFromBits(bits: Int): Float = java.lang.Float.intBitsToFloat(bits)

actual inline fun doubleFromBits(bits: Long): Double = java.lang.Double.longBitsToDouble(bits)

actual inline fun Float.fastRoundToInt(): Int = Math.round(this)

actual inline fun Double.fastRoundToInt(): Int = Math.round(this).toInt()
