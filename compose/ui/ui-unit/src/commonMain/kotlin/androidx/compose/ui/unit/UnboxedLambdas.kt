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

package androidx.compose.ui.unit

/**
 * Alternative to `() -> Float` that's useful for avoiding boxing.
 *
 * Can be used as:
 *
 * fun nonBoxedArgs(a: FloatLambda?)
 */
fun interface FloatLambda {
    fun invoke(): Float
}

/**
 * Alternative to `() -> Double` that's useful for avoiding boxing.
 *
 * Can be used as:
 *
 * fun nonBoxedArgs(color: DoubleLambda?)
 */
fun interface DoubleLambda {
    fun invoke(): Double
}

/**
 * Alternative to `() -> Int` that's useful for avoiding boxing.
 *
 * Can be used as:
 *
 * fun nonBoxedArgs(a: IntLambda?)
 */
fun interface IntLambda {
    fun invoke(): Int
}

/**
 * Alternative to `() -> Long` that's useful for avoiding boxing.
 *
 * Can be used as:
 *
 * fun nonBoxedArgs(a: LongLambda?)
 */
fun interface LongLambda {
    fun invoke(): Long
}

/**
 * Alternative to `() -> Short` that's useful for avoiding boxing.
 *
 * Can be used as:
 *
 * fun nonBoxedArgs(a: ShortLambda?)
 */
fun interface ShortLambda {
    @Suppress("NoByteOrShort")
    fun invoke(): Short
}