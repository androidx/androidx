/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.material.ripple

import androidx.compose.Ambient
import androidx.compose.Effect
import androidx.compose.ambient
import androidx.compose.effectOf
import androidx.ui.graphics.Color
import androidx.ui.graphics.luminance
import androidx.ui.material.MaterialTheme
import androidx.ui.material.surface.CurrentBackground
import androidx.ui.material.textColorForBackground

/**
 * Defines the appearance and the behavior for [Ripple]s.
 *
 * To change some parameter and apply it to descendants modify the [CurrentRippleTheme] ambient.
 *
 * To apply the default values based on the Material Design guidelines use [MaterialTheme].
 */
data class RippleTheme(
    /**
     * Defines the current [RippleEffect] implementation.
     */
    val factory: RippleEffectFactory,
    /**
     * The effect that will be used to calculate the [Ripple] color when it is not explicitly
     * set in a [Ripple].
     */
    val defaultColor: Effect<Color>,
    /**
     * The effect that will be used to calculate the opacity applied to the [Ripple] color.
     * For example, it can be different in dark and light modes.
     */
    val opacity: Effect<Float>
)

val CurrentRippleTheme = Ambient.of { DefaultRippleTheme }

private val DefaultRippleTheme = RippleTheme(
        factory = DefaultRippleEffectFactory,
        defaultColor = effectOf {
            val background = +ambient(CurrentBackground)
            val textColor = +textColorForBackground(background)
            when {
                textColor != null -> textColor
                background.alpha == 0f || background.luminance() >= 0.5 -> Color.Black
                else -> Color.White
            }
        },
        opacity = effectOf {
            val isDarkTheme = (+MaterialTheme.colors()).surface.luminance() < 0.5f
            if (isDarkTheme) 0.24f else 0.12f
        }
    )