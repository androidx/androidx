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

package com.android.mechanics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.android.mechanics.haptics.LocalHapticPlayer
import com.android.mechanics.spec.MotionSpec
import com.android.mechanics.spec.builder.MotionBuilderContext
import com.android.mechanics.spec.builder.rememberMotionBuilderContext

@Composable
fun rememberMotionValue(
    input: () -> Float,
    gestureContext: GestureContext,
    spec: () -> MotionSpec,
    label: String? = null,
    stableThreshold: Float = 0.01f,
): MotionValue {
    val specGetter by rememberUpdatedState(spec)
    val hapticPlayer = LocalHapticPlayer.current

    val motionValue =
        remember(input, hapticPlayer) {
            MotionValue(
                input = input,
                gestureContext = gestureContext,
                spec = { specGetter() },
                label = label,
                stableThreshold = stableThreshold,
                hapticPlayer = hapticPlayer,
            )
        }

    LaunchedEffect(motionValue) { motionValue.keepRunning() }
    return motionValue
}

@Composable
fun rememberMotionValue(
    input: () -> Float,
    gestureContext: GestureContext,
    spec: State<MotionSpec>,
    label: String? = null,
    stableThreshold: Float = 0.01f,
): MotionValue {
    return rememberMotionValue(
        input = input,
        gestureContext = gestureContext,
        spec = spec::value,
        label = label,
        stableThreshold = stableThreshold,
    )
}

@Composable
fun rememberDerivedMotionValue(
    input: MotionValue,
    specProvider: () -> MotionSpec,
    stableThreshold: Float = 0.01f,
    label: String? = null,
): MotionValue {
    val motionValue =
        remember(input, specProvider) {
            MotionValue.createDerived(
                source = input,
                spec = specProvider,
                label = label,
                stableThreshold = stableThreshold,
            )
        }

    LaunchedEffect(motionValue) { motionValue.keepRunning() }
    return motionValue
}

/**
 * Efficiently creates and remembers a [MotionSpec], providing it via a stable lambda.
 *
 * This function memoizes the [MotionSpec] to avoid expensive recalculations. The spec is
 * re-computed only when a state dependency within the `spec` lambda changes, not on every
 * recomposition or each time the output is read.
 *
 * @param calculation A lambda with a [MotionBuilderContext] receiver that defines the [MotionSpec].
 * @return A stable provider `() -> MotionSpec`. Invoking this function is cheap as it returns the
 *   latest cached value.
 */
@Composable
fun rememberMotionSpecAsState(
    calculation: MotionBuilderContext.() -> MotionSpec
): State<MotionSpec> {
    val updatedSpec = rememberUpdatedState(calculation)
    val context = rememberMotionBuilderContext()
    return remember(context) { derivedStateOf { updatedSpec.value(context) } }
}
