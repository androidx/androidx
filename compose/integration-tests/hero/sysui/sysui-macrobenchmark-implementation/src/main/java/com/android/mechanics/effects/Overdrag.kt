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

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.mechanics.spec.BreakpointKey
import com.android.mechanics.spec.Mapping
import com.android.mechanics.spec.SemanticKey
import com.android.mechanics.spec.builder.Effect
import com.android.mechanics.spec.builder.EffectApplyScope
import com.android.mechanics.spec.builder.EffectPlacement
import com.android.mechanics.spec.builder.MotionBuilderContext
import com.android.mechanics.spec.with

/** Gesture effect to soft-limit. */
class Overdrag(
    private val overdragLimit: SemanticKey<Float?> = Defaults.OverdragLimit,
    private val maxOverdrag: Dp = Defaults.MaxOverdrag,
    private val tilt: Float = Defaults.tilt,
) : Effect.PlaceableBefore, Effect.PlaceableAfter {

    override fun MotionBuilderContext.intrinsicSize() = Float.POSITIVE_INFINITY

    override fun EffectApplyScope.createSpec(
        minLimit: Float,
        minLimitKey: BreakpointKey,
        maxLimit: Float,
        maxLimitKey: BreakpointKey,
        placement: EffectPlacement,
    ) {

        val maxOverdragPx = maxOverdrag.toPx()

        val limitValue = baseValue(placement.start)
        val mapping = Mapping { input ->
            val baseMapped = baseMapping.map(input)

            maxOverdragPx * kotlin.math.tanh((baseMapped - limitValue) / (maxOverdragPx * tilt)) +
                limitValue
        }

        unidirectional(mapping, listOf(overdragLimit with limitValue)) {
            if (!placement.isForward) {
                after(semantics = listOf(overdragLimit with null))
            }
        }
    }

    object Defaults {
        val OverdragLimit = SemanticKey<Float?>()
        val MaxOverdrag = 30.dp
        val tilt = 3f
    }
}
