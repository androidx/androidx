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

package com.android.mechanics.effects

import com.android.mechanics.spec.BreakpointKey
import com.android.mechanics.spec.Mapping
import com.android.mechanics.spec.builder.Effect
import com.android.mechanics.spec.builder.EffectApplyScope
import com.android.mechanics.spec.builder.EffectPlacement
import com.android.mechanics.spec.builder.MotionBuilderContext
import com.android.mechanics.spec.builder.MotionSpecBuilderScope

/** Creates a [FixedValue] effect with the given [value]. */
fun MotionSpecBuilderScope.fixed(value: Float) = FixedValue(value)

val MotionSpecBuilderScope.zero: FixedValue
    get() = FixedValue.Zero
val MotionSpecBuilderScope.one: FixedValue
    get() = FixedValue.One

/** Produces a fixed [value]. */
class FixedValue(val value: Float) :
    Effect.PlaceableAfter, Effect.PlaceableBefore, Effect.PlaceableBetween {

    override fun MotionBuilderContext.intrinsicSize(): Float = Float.NaN

    override fun EffectApplyScope.createSpec(
        minLimit: Float,
        minLimitKey: BreakpointKey,
        maxLimit: Float,
        maxLimitKey: BreakpointKey,
        placement: EffectPlacement,
    ) {
        return unidirectional(Mapping.Fixed(value))
    }

    companion object {
        val Zero = FixedValue(0f)
        val One = FixedValue(1f)
    }
}
