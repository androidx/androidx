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
package androidx.ui

/**
 * Linearly interpolate between [a] and [b] with [t] fraction between them.
 */
fun lerp(a: Float, b: Float, t: Float): Float {
    return a + (b - a) * t
}

/**
 * Linearly interpolate between [a] and [b] with [t] fraction between them.
 */
fun lerp(a: Int, b: Int, t: Float): Float {
    return a + (b - a) * t
}

fun Float.toStringAsFixed(digits: Int) = String.format("%.${digits}f", this)

@UseExperimental(kotlin.ExperimentalUnsignedTypes::class)
fun Int.toHexString() = "0x${toUInt().toString(16).padStart(8, '0')}"