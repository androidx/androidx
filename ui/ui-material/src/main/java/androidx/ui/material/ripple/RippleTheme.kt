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

import androidx.compose.Composable
import androidx.compose.staticAmbientOf
import androidx.ui.foundation.contentColor
import androidx.ui.graphics.Color
import androidx.ui.material.MaterialTheme

/**
 * Defines the appearance and the behavior for [ripple]s.
 *
 * You can define new theme and apply it via [RippleThemeAmbient].
 */
data class RippleTheme(
    /**
     * Defines the current [RippleEffect] implementation.
     */
    val factory: RippleEffectFactory,
    /**
     * The effect that will be used to calculate the [ripple] color when it is not explicitly
     * set in a [ripple].
     */
    val defaultColor: @Composable () -> Color,
    /**
     * The effect that will be used to calculate the opacity applied to the [ripple] color.
     * For example, it can be different in dark and light modes.
     */
    val opacity: @Composable () -> Float
)

/**
 * Ambient used for providing [RippleTheme] down the tree.
 */
val RippleThemeAmbient = staticAmbientOf { DefaultRippleTheme }

private val DefaultRippleTheme = RippleTheme(
    factory = DefaultRippleEffectFactory,
    defaultColor = { contentColor() },
    opacity = {
        if (MaterialTheme.colors.isLight) LightRippleOpacity else DarkRippleOpacity
    }
)

private const val LightRippleOpacity = 0.12f
private const val DarkRippleOpacity = 0.24f
