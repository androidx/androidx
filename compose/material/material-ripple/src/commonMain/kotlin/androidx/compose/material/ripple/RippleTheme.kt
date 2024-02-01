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

package androidx.compose.material.ripple

import androidx.compose.foundation.interaction.Interaction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Defines the appearance for Ripples. You can define a new theme and apply it using
 * [LocalRippleTheme]. See [defaultRippleColor] and [defaultRippleAlpha] for default values
 * that can be used when creating your own [RippleTheme].
 *
 * @see rememberRipple
 */
@Deprecated(RippleThemeDeprecationMessage, level = DeprecationLevel.ERROR)
public interface RippleTheme {
    /**
     * @return the default ripple color at the call site's position in the hierarchy.
     * This color will be used when a color is not explicitly set in the ripple itself.
     * @see defaultRippleColor
     */
    @Deprecated(RippleThemeDeprecationMessage, level = DeprecationLevel.ERROR)
    @Composable
    public fun defaultColor(): Color

    /**
     * @return the [RippleAlpha] used to calculate the alpha for the ripple depending on the
     * [Interaction] for a given component. This will be set as the alpha channel for
     * [defaultColor] or the color explicitly provided to the ripple.
     * @see defaultRippleAlpha
     */
    @Deprecated(RippleThemeDeprecationMessage, level = DeprecationLevel.ERROR)
    @Composable
    public fun rippleAlpha(): RippleAlpha

    public companion object {
        /**
         * Represents the default color that will be used for a ripple if a color has not been
         * explicitly set on the ripple instance.
         *
         * @param contentColor the color of content (text or iconography) in the component that
         * contains the ripple.
         * @param lightTheme whether the theme is light or not
         */
        @Deprecated(
            "The default ripple color varies between design system versions: this " +
                "function technically implements the default used by the material library, but " +
                "is not used by the material3 library. To remove confusion and link the " +
                "defaults more strongly to the design system library, these default values have " +
                "been moved to the material and material3 libraries. For material, use " +
                "MaterialRippleThemeDefaults#rippleColor. For material3, use content color " +
                "directly.",
            level = DeprecationLevel.WARNING
        )
        public fun defaultRippleColor(
            contentColor: Color,
            lightTheme: Boolean
        ): Color {
            val contentLuminance = contentColor.luminance()
            // If we are on a colored surface (typically indicated by low luminance content), the
            // ripple color should be white.
            return if (!lightTheme && contentLuminance < 0.5) {
                Color.White
                // Otherwise use contentColor
            } else {
                contentColor
            }
        }

        /**
         * Represents the default [RippleAlpha] that will be used for a ripple to indicate different
         * states.
         *
         * @param contentColor the color of content (text or iconography) in the component that
         * contains the ripple.
         * @param lightTheme whether the theme is light or not
         */
        @Deprecated(
            "The default ripple alpha varies between design system versions: this " +
                "function technically implements the default used by the material library, but " +
                "is not used by the material3 library. To remove confusion and link the " +
                "defaults more strongly to the design system library, these default values have " +
                "been moved to the material and material3 libraries. For material, use " +
                "MaterialRippleThemeDefaults#rippleAlpha. For material3, use " +
                "MaterialRippleThemeDefaults#RippleAlpha.",
            level = DeprecationLevel.WARNING
        )
        public fun defaultRippleAlpha(contentColor: Color, lightTheme: Boolean): RippleAlpha {
            return when {
                lightTheme -> {
                    if (contentColor.luminance() > 0.5) {
                        LightThemeHighContrastRippleAlpha
                    } else {
                        LightThemeLowContrastRippleAlpha
                    }
                }
                else -> {
                    DarkThemeRippleAlpha
                }
            }
        }
    }
}

/**
 * RippleAlpha defines the alpha of the ripple / state layer for different [Interaction]s.
 *
 * On Android, because the press ripple is drawn using the framework's RippleDrawable, there are
 * constraints / different behaviours for the actual press alpha used on different API versions.
 * Note that this *only* affects [pressedAlpha] - the other values are guaranteed to be consistent,
 * as they do not rely on framework code. Specifically:
 *
 * API 21-27: The actual ripple is split into two 'layers', with the alpha applied to both layers,
 * so there is no uniform 'alpha'.
 * API 28-32: The ripple is just one layer, but the alpha is clamped to a maximum of 0.5f - it is
 * not possible to have a fully opaque ripple.
 * API 33: There is a bug where the ripple is clamped to a *minimum* of 0.5, instead of a maximum
 * like before - this should be resolved in future versions.
 *
 * @property draggedAlpha the alpha used when the ripple is dragged
 * @property focusedAlpha the alpha used when the ripple is focused
 * @property hoveredAlpha the alpha used when the ripple is hovered
 * @property pressedAlpha the alpha used when the ripple is pressed
 */
@Immutable
public class RippleAlpha(
    public val draggedAlpha: Float,
    public val focusedAlpha: Float,
    public val hoveredAlpha: Float,
    public val pressedAlpha: Float
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RippleAlpha) return false

        if (draggedAlpha != other.draggedAlpha) return false
        if (focusedAlpha != other.focusedAlpha) return false
        if (hoveredAlpha != other.hoveredAlpha) return false
        if (pressedAlpha != other.pressedAlpha) return false

        return true
    }

    override fun hashCode(): Int {
        var result = draggedAlpha.hashCode()
        result = 31 * result + focusedAlpha.hashCode()
        result = 31 * result + hoveredAlpha.hashCode()
        result = 31 * result + pressedAlpha.hashCode()
        return result
    }

    override fun toString(): String {
        return "RippleAlpha(draggedAlpha=$draggedAlpha, focusedAlpha=$focusedAlpha, " +
            "hoveredAlpha=$hoveredAlpha, pressedAlpha=$pressedAlpha)"
    }
}

/**
 * CompositionLocal used for providing [RippleTheme] down the tree.
 *
 * See [RippleTheme.defaultRippleColor] and [RippleTheme.defaultRippleAlpha] functions for the
 * default implementations for color and alpha.
 */
@Suppress("DEPRECATION_ERROR")
@Deprecated(RippleThemeDeprecationMessage, level = DeprecationLevel.ERROR)
public val LocalRippleTheme: ProvidableCompositionLocal<RippleTheme> =
    staticCompositionLocalOf { DebugRippleTheme }

/**
 * Alpha values for high luminance content in a light theme.
 *
 * This content will typically be placed on colored surfaces, so it is important that the
 * contrast here is higher to meet accessibility standards, and increase legibility.
 *
 * These levels are typically used for text / iconography in primary colored tabs /
 * bottom navigation / etc.
 */
private val LightThemeHighContrastRippleAlpha = RippleAlpha(
    pressedAlpha = 0.24f,
    focusedAlpha = 0.24f,
    draggedAlpha = 0.16f,
    hoveredAlpha = 0.08f
)

/**
 * Alpha levels for low luminance content in a light theme.
 *
 * This content will typically be placed on grayscale surfaces, so the contrast here can be lower
 * without sacrificing accessibility and legibility.
 *
 * These levels are typically used for body text on the main surface (white in light theme, grey
 * in dark theme) and text / iconography in surface colored tabs / bottom navigation / etc.
 */
private val LightThemeLowContrastRippleAlpha = RippleAlpha(
    pressedAlpha = 0.12f,
    focusedAlpha = 0.12f,
    draggedAlpha = 0.08f,
    hoveredAlpha = 0.04f
)

/**
 * Alpha levels for all content in a dark theme.
 */
private val DarkThemeRippleAlpha = RippleAlpha(
    pressedAlpha = 0.10f,
    focusedAlpha = 0.12f,
    draggedAlpha = 0.08f,
    hoveredAlpha = 0.04f
)

/**
 * Simple debug indication that will assume black content color and light theme. You should
 * instead provide your own theme with meaningful values - this exists as an alternative to
 * crashing if no theme is provided.
 */
@Suppress("DEPRECATION_ERROR", "deprecation")
@Immutable
private object DebugRippleTheme : RippleTheme {
    @Deprecated("Super method is deprecated")
    @Composable
    override fun defaultColor() = RippleTheme.defaultRippleColor(Color.Black, lightTheme = true)

    @Deprecated("Super method is deprecated")
    @Composable
    override fun rippleAlpha(): RippleAlpha = RippleTheme.defaultRippleAlpha(
        Color.Black,
        lightTheme = true
    )
}

private const val RippleThemeDeprecationMessage = "RippleTheme and LocalRippleTheme have been " +
    "deprecated - they are not compatible with the new ripple implementation using the new " +
    "Indication APIs that provide notable performance improvements. For a migration guide and " +
    "background information, please visit developer.android.com"
