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

package com.android.mechanics.impl

import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2
import com.android.mechanics.spec.Breakpoint
import com.android.mechanics.spec.Guarantee
import com.android.mechanics.spec.InputDirection
import com.android.mechanics.spring.SpringParameters
import kotlin.jvm.JvmInline
import kotlin.math.max

/**
 * Captures the origin of a guarantee, and the maximal distance the input has been away from the
 * origin at most.
 */
@JvmInline
internal value class GuaranteeState(val packedValue: Long) {
    private val start: Float
        get() = unpackFloat1(packedValue)

    private val maxDelta: Float
        get() = unpackFloat2(packedValue)

    private val isInactive: Boolean
        get() = this == Inactive

    fun withCurrentValue(value: Float, direction: InputDirection): GuaranteeState {
        if (isInactive) return Inactive

        val delta = ((value - start) * direction.sign).fastCoerceAtLeast(0f)
        return GuaranteeState(start, max(delta, maxDelta))
    }

    fun updatedSpringParameters(breakpoint: Breakpoint): SpringParameters {
        if (isInactive) return breakpoint.spring

        val denominator =
            when (val guarantee = breakpoint.guarantee) {
                is Guarantee.None -> return breakpoint.spring
                is Guarantee.InputDelta -> guarantee.delta
                is Guarantee.GestureDragDelta -> guarantee.delta
            }

        val springTighteningFraction = maxDelta / denominator
        return com.android.mechanics.spring.lerp(
            breakpoint.spring,
            SpringParameters.Snap,
            springTighteningFraction,
        )
    }

    companion object {
        val Inactive = GuaranteeState(packFloats(Float.NaN, Float.NaN))

        fun withStartValue(start: Float) = GuaranteeState(packFloats(start, 0f))
    }
}

internal fun GuaranteeState(start: Float, maxDelta: Float) =
    GuaranteeState(packFloats(start, maxDelta))
