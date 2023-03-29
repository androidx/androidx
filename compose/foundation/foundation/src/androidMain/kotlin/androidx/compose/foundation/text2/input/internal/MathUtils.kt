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

package androidx.compose.foundation.text2.input.internal

/**
 * Adds [this] and [right], and if an overflow occurs returns result of [defaultValue].
 */
internal inline fun Int.addExactOrElse(right: Int, defaultValue: () -> Int): Int {
    val result = this + right
    // HD 2-12 Overflow iff both arguments have the opposite sign of the result
    return if (this xor result and (right xor result) < 0) defaultValue() else result
}

/**
 * Subtracts [right] from [this], and if an overflow occurs returns result of [defaultValue].
 */
internal fun Int.subtractExactOrElse(right: Int, defaultValue: () -> Int): Int {
    val result = this - right
    // HD 2-12 Overflow iff the arguments have different signs and
    // the sign of the result is different from the sign of x
    return if (this xor right and (this xor result) < 0) defaultValue() else result
}