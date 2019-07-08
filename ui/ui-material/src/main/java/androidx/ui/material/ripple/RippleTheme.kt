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

import androidx.ui.material.MaterialRippleTheme
import androidx.ui.graphics.Color
import androidx.compose.Ambient

/**
 * Defines the appearance and the behavior for [RippleEffect]s.
 * Used for customisation of [Ripple].
 *
 * To change some parameter and apply it to descendants modify
 * the [CurrentRippleTheme] ambient.
 *
 * To apply the default values based on the Material Design guidelines
 * use [MaterialRippleTheme].
 */
data class RippleTheme(
    /**
     * Defines the current [RippleEffect] implementation.
     */
    val factory: RippleEffectFactory,
    /**
     * The callback which returns the backgroundColor to be used by [RippleEffect].
     */
    val colorCallback: RippleColorCallback
)

// TODO(Andrey: We are having the background backgroundColor as a parameter as a temporary solution)
// By specification the ripple backgroundColor is taken from the text or iconography in the component.
// Opacity is 12% on light theme and 24% on dark theme.
// To be implemented in b/124500407
typealias RippleColorCallback = (background: Color?) -> (Color)

val CurrentRippleTheme = Ambient.of<RippleTheme> {
    error("No RippleTheme provided. Please add MaterialRippleTheme as an ancestor.")
}
