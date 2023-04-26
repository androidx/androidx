/*
 * Copyright 2023 The Android Open Source Project
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
package androidx.wear.compose.material3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.luminance

/**
 * CompositionLocal containing the preferred content alpha for a given position in the hierarchy.
 * This alpha is used for text and iconography ([Text] and Icon) to emphasize / de-emphasize
 * different parts of a component. See the Material guide on
 * [Text Legibility](https://material.io/design/color/text-legibility.html) for more information on
 * alpha levels used by text and iconography.
 *
 * See [ContentAlpha] for the default levels used by most Material components.
 *
 * [MaterialTheme] sets this to [ContentAlpha.high] by default, as this is the default alpha for
 * body text.
 *
 */
public val LocalContentAlpha: ProvidableCompositionLocal<Float> = compositionLocalOf { 1f }

/**
 * Default alpha levels used by Material components.
 *
 * See [LocalContentAlpha].
 */
public object ContentAlpha {
    /**
     * A high level of content alpha, used to represent high emphasis text.
     */
    public val high: Float
        @Composable
        get() = contentAlpha(
            highContrastAlpha = HighContrastContentAlpha.high,
            lowContrastAlpha = LowContrastContentAlpha.high
        )

    /**
     * A medium level of content alpha, used to represent medium emphasis text such as
     * placeholder text.
     */
    public val medium: Float
        @Composable
        get() = contentAlpha(
            highContrastAlpha = HighContrastContentAlpha.medium,
            lowContrastAlpha = LowContrastContentAlpha.medium
        )

    /**
     * A low level of content alpha used to represent disabled components, such as text in a
     * disabled Button.
     */
    public val disabled: Float
        @Composable
        get() = contentAlpha(
            highContrastAlpha = HighContrastContentAlpha.disabled,
            lowContrastAlpha = LowContrastContentAlpha.disabled
        )

    /**
     * This default implementation uses separate alpha levels depending on the luminance of the
     * incoming color, and whether the theme is light or dark. This is to ensure correct contrast
     * and accessibility on all surfaces.
     *
     * See [HighContrastContentAlpha] and [LowContrastContentAlpha] for what the levels are
     * used for, and under what circumstances.
     */
    @Composable
    private fun contentAlpha(
        /*@FloatRange(from = 0.0, to = 1.0)*/
        highContrastAlpha: Float,
        /*@FloatRange(from = 0.0, to = 1.0)*/
        lowContrastAlpha: Float
    ): Float {
        val contentColor = LocalContentColor.current
        return if (contentColor.luminance() < 0.5) highContrastAlpha else lowContrastAlpha
    }
}

/**
 * Alpha levels for high luminance content in light theme, or low luminance content in dark theme.
 *
 * This content will typically be placed on colored surfaces, so it is important that the
 * contrast here is higher to meet accessibility standards, and increase legibility.
 *
 * These levels are typically used for text / iconography in primary colored tabs /
 * bottom navigation / etc.
 */
private object HighContrastContentAlpha {
    const val high: Float = 1.00f
    const val medium: Float = 0.74f
    const val disabled: Float = 0.38f
}

/**
 * Alpha levels for low luminance content in light theme, or high luminance content in dark theme.
 *
 * This content will typically be placed on grayscale surfaces, so the contrast here can be lower
 * without sacrificing accessibility and legibility.
 *
 * These levels are typically used for body text on the main surface (white in light theme, grey
 * in dark theme) and text / iconography in surface colored tabs / bottom navigation / etc.
 */
private object LowContrastContentAlpha {
    const val high: Float = 0.87f
    const val medium: Float = 0.60f
    const val disabled: Float = 0.38f
}

internal const val DisabledBorderAndContainerAlpha = 0.12f
