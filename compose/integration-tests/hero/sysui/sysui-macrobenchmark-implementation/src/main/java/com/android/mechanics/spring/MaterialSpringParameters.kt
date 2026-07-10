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
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.android.mechanics.spring

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/** Converts a [SpringSpec] into its [SpringParameters] equivalent. */
fun SpringParameters(springSpec: SpringSpec<out Any>) =
    with(springSpec) { SpringParameters(stiffness, dampingRatio) }

/**
 * Converts a [FiniteAnimationSpec] from the [MotionScheme] into its [SpringParameters] equivalent.
 */
@ExperimentalMaterial3ExpressiveApi
fun SpringParameters(animationSpec: FiniteAnimationSpec<out Any>): SpringParameters {
    check(animationSpec is SpringSpec) {
        "animationSpec is expected to be a SpringSpec, but is $animationSpec"
    }
    return SpringParameters(animationSpec)
}

@Composable
fun defaultSpatialSpring(): SpringParameters {
    return SpringParameters(MaterialTheme.motionScheme.defaultSpatialSpec())
}

@Composable
fun defaultEffectSpring(): SpringParameters {
    return SpringParameters(MaterialTheme.motionScheme.defaultEffectsSpec())
}
