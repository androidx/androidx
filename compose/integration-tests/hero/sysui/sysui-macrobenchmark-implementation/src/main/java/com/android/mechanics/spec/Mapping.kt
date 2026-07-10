/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.mechanics.spec

import androidx.compose.ui.util.lerp

/**
 * Maps the `input` of a [MotionValue] to the desired output value.
 *
 * The mapping implementation can be arbitrary, but must not produce discontinuities.
 */
fun interface Mapping {
    /** Computes the [MotionValue]'s target output, given the input. */
    fun map(input: Float): Float

    /** `f(x) = x` */
    object Identity : Mapping {
        override fun map(input: Float): Float {
            return input
        }

        override fun toString(): String {
            return "Identity"
        }
    }

    /** `f(x) = value` */
    data class Fixed(val value: Float) : Mapping {
        init {
            require(value.isFinite())
        }

        override fun map(input: Float): Float {
            return value
        }
    }

    /** `f(x) = factor*x + offset` */
    data class Linear(val factor: Float, val offset: Float = 0f) : Mapping {
        init {
            require(factor.isFinite())
            require(offset.isFinite())
        }

        override fun map(input: Float): Float {
            return input * factor + offset
        }
    }

    companion object {
        val Zero = Fixed(0f)
        val One = Fixed(1f)
        val Two = Fixed(2f)

        /** Create a linear mapping defined as a line between {in0,out0} and {in1,out1}. */
        fun Linear(in0: Float, out0: Float, in1: Float, out1: Float): Linear {
            require(in0 != in1) {
                "Cannot define a linear function with both inputs being the same ($in0)."
            }

            val factor = (out1 - out0) / (in1 - in0)
            val offset = out0 - factor * in0
            return Linear(factor, offset)
        }
    }
}

/** Convenience helper to create a linear mappings */
object LinearMappings {

    /**
     * Creates a mapping defined as two line segments between {in0,out0} -> {in1,out1}, and
     * {in1,out1} -> {in2,out2}.
     *
     * The inputs must strictly be `in0 < in1 < in2`
     */
    fun linearMappingWithPivot(
        in0: Float,
        out0: Float,
        in1: Float,
        out1: Float,
        in2: Float,
        out2: Float,
    ): Mapping {
        require(in0 < in1 && in1 < in2)
        return Mapping { input ->
            if (input <= in1) {
                val t = (input - in0) / (in1 - in0)
                lerp(out0, out1, t)
            } else {
                val t = (input - in1) / (in2 - in1)
                lerp(out1, out2, t)
            }
        }
    }
}
