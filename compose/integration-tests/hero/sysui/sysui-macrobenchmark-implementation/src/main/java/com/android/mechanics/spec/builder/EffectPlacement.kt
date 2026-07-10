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

package com.android.mechanics.spec.builder

import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2
import kotlin.jvm.JvmInline
import kotlin.math.max
import kotlin.math.min
import kotlin.math.nextDown
import kotlin.math.nextUp

/**
 * Describes the desired placement of an effect within the input domain of a [MotionSpec].
 *
 * [start] is always finite, and denotes a specific position in the input where the effects starts.
 *
 * [end] is either finite, describing a specific range in the input where the [Effect] applies.
 * Alternatively, the [end] can be either [Float.NEGATIVE_INFINITY] or [Float.POSITIVE_INFINITY],
 * indicating that the effect extends either
 * - for the effects intrinsic extent
 * - the boundaries of the next placed effect
 * - the specs' min/max limit
 *
 * Thus, [start] and [end] define an implicit direction of the effect. If not [isForward], the
 * [Effect] will be reversed when applied.
 */
@JvmInline
value class EffectPlacement internal constructor(val value: Long) {

    init {
        require(start.isFinite())
    }

    val start: Float
        get() = unpackFloat1(value)

    val end: Float
        get() = unpackFloat2(value)

    val type: EffectPlacemenType
        get() {
            return when {
                end.isNaN() -> EffectPlacemenType.At
                end == Float.NEGATIVE_INFINITY -> EffectPlacemenType.Before
                end == Float.POSITIVE_INFINITY -> EffectPlacemenType.After
                else -> EffectPlacemenType.Between
            }
        }

    val isForward: Boolean
        get() {
            return when (type) {
                EffectPlacemenType.At -> true
                EffectPlacemenType.Before -> false
                EffectPlacemenType.After -> true
                EffectPlacemenType.Between -> end >= start
            }
        }

    // TODO Convert this to a Comparable instead. Currently uses Double instead of Float, since
    //  nextUp / nextDown is only available on Doubles in Kotlin Multiplatform.
    internal val sortOrder: Double
        get() {
            return when (type) {
                EffectPlacemenType.At -> start.toDouble()
                EffectPlacemenType.Before -> start.toDouble().nextDown()
                EffectPlacemenType.After -> start.toDouble().nextUp()
                EffectPlacemenType.Between -> (start.toDouble() + end.toDouble()) / 2
            }
        }

    internal val min: Float
        get() = min(start, end)

    internal val max: Float
        get() = max(start, end)

    override fun toString(): String {
        return "EffectPlacement(start=$start, end=$end)"
    }

    companion object {
        fun at(position: Float) = EffectPlacement(packFloats(position, Float.NaN))

        fun after(position: Float) = EffectPlacement(packFloats(position, Float.POSITIVE_INFINITY))

        fun before(position: Float) = EffectPlacement(packFloats(position, Float.NEGATIVE_INFINITY))

        fun between(start: Float, end: Float) = EffectPlacement(packFloats(start, end))
    }
}

enum class EffectPlacemenType {
    At,
    Before,
    After,
    Between,
}
