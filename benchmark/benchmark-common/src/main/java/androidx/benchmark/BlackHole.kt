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

package androidx.benchmark

import dalvik.annotation.optimization.CriticalNative
import dalvik.annotation.optimization.FastNative

/**
 * Function calls which can be used to prevent optimization of results.
 *
 * Both the Kotlin compiler and R8 can remove code you intend to benchmark. To prevent this, pass
 * the result to [BlackHole.consume].
 */
@ExperimentalBlackHoleApi
object BlackHole {
    init {
        System.loadLibrary("benchmarkNative")
    }

    /** Prevent dead code elimination of [value] and its computation. */
    @JvmStatic @CriticalNative external fun consume(value: Byte)

    /** Prevent dead code elimination of [value] and its computation. */
    @JvmStatic @CriticalNative external fun consume(value: Short)

    /** Prevent dead code elimination of [value] and its computation. */
    @JvmStatic @CriticalNative external fun consume(value: Int)

    /** Prevent dead code elimination of [value] and its computation. */
    @JvmStatic @CriticalNative external fun consume(value: Long)

    /** Prevent dead code elimination of [value] and its computation. */
    @JvmStatic @CriticalNative external fun consume(value: Float)

    /** Prevent dead code elimination of [value] and its computation. */
    @JvmStatic @CriticalNative external fun consume(value: Double)

    /** Prevent dead code elimination of [value] and its computation. */
    @JvmStatic @CriticalNative external fun consume(value: Boolean)

    /** Prevent dead code elimination of [value] and its computation. */
    @JvmStatic @CriticalNative external fun consume(value: Char)

    /** Prevent dead code elimination of [value] and its computation. */
    @JvmStatic @FastNative external fun consume(value: Any)
}
