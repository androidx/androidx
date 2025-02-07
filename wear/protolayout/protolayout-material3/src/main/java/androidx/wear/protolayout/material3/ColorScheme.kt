/*
 * Copyright 2024 The Android Open Source Project
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
package androidx.wear.protolayout.material3

import androidx.wear.protolayout.material3.tokens.ColorTokens
import androidx.wear.protolayout.types.LayoutColor
import androidx.wear.protolayout.types.argb

/**
 * A [ColorScheme] holds all the named color parameters for a [MaterialTheme].
 *
 * Color schemes are designed to be harmonious, ensure accessible text, and distinguish UI elements
 * and surfaces from one another.
 *
 * The Material color system and custom schemes provide default values for color as a starting point
 * for customization.
 *
 * To learn more about color schemes, see
 * [Material Design Color System](https://m3.material.io/styles/color/the-color-system/color-roles).
 *
 * @property primary is the color displayed most frequently across your app’s screens and components
 * @property primaryDim is less prominent than [primary] for component backgrounds
 * @property primaryContainer is a standout container color for key components
 * @property onPrimary color is used for text and icons displayed on top of the primary color
 * @property onPrimaryContainer color (and state variants) that should be used for content on top of
 *   [primaryContainer]
 * @property secondary color provides more ways to accent and distinguish your product
 * @property secondaryDim is less prominent than [secondary] for component backgrounds
 * @property secondaryContainer is a tonal color to be used in containers
 * @property onSecondary color is used for text and icons displayed on top of the secondary color
 * @property onSecondaryContainer color (and state variants) should be used for content on top of
 *   [secondaryContainer]
 * @property tertiary color that can be used to balance primary and secondary colors, or bring
 *   heightened attention to an element
 * @property tertiaryDim is a less prominent tertiary color that can be used to balance primary and
 *   secondary colors, or bring heightened attention to an element
 * @property tertiaryContainer is a tonal color to be used in containers
 * @property onTertiary color is used for text and icons displayed on top of the tertiary color
 * @property onTertiaryContainer color (and state variants) that should be used for content on top
 *   of [tertiaryContainer]
 * @property surfaceContainerLow is a surface color used for large containment components such as
 *   Card and Button with low prominence
 * @property surfaceContainer is the main surface color that affect surfaces of components with
 *   large containment areas, such as Card and Button
 * @property surfaceContainerHigh is a surface color used for large containment components such Card
 *   and Button with high prominence
 * @property onSurface color is used for text and icons displayed on top of the surface color
 * @property onSurfaceVariant is the color for secondary text and icons on top of [surfaceContainer]
 * @property outline is the main color for primary outline components. The outline color role adds
 *   contrast for accessibility purposes.
 * @property outlineVariant is the secondary color for secondary outline components
 * @property background color that appears behind other content
 * @property onBackground color is used for text and icons displayed on top of the background color
 * @property error color that indicates remove, delete, close or dismiss actions. Added as a
 *   slightly less alarming and urgent alternative to errorContainer than the errorDim color
 * @property errorDim color that indicates high priority errors or emergency actions, such as safety
 *   alerts
 * @property errorContainer is color that indicates errors or emergency actions, such as safety
 *   alerts. This color is for use-cases that are more alarming and urgent than the error color.
 * @property onError color is used for text and icons displayed on top of the error color
 * @property onErrorContainer is color used for text and icons on the errorContainer color
 */
public class ColorScheme(
    public val primary: LayoutColor = ColorTokens.PRIMARY.argb,
    public val primaryDim: LayoutColor = ColorTokens.PRIMARY_DIM.argb,
    public val primaryContainer: LayoutColor = ColorTokens.PRIMARY_CONTAINER.argb,
    public val onPrimary: LayoutColor = ColorTokens.ON_PRIMARY.argb,
    public val onPrimaryContainer: LayoutColor = ColorTokens.ON_PRIMARY_CONTAINER.argb,
    public val secondary: LayoutColor = ColorTokens.SECONDARY.argb,
    public val secondaryDim: LayoutColor = ColorTokens.SECONDARY_DIM.argb,
    public val secondaryContainer: LayoutColor = ColorTokens.SECONDARY_CONTAINER.argb,
    public val onSecondary: LayoutColor = ColorTokens.ON_SECONDARY.argb,
    public val onSecondaryContainer: LayoutColor = ColorTokens.ON_SECONDARY_CONTAINER.argb,
    public val tertiary: LayoutColor = ColorTokens.TERTIARY.argb,
    public val tertiaryDim: LayoutColor = ColorTokens.TERTIARY_DIM.argb,
    public val tertiaryContainer: LayoutColor = ColorTokens.TERTIARY_CONTAINER.argb,
    public val onTertiary: LayoutColor = ColorTokens.ON_TERTIARY.argb,
    public val onTertiaryContainer: LayoutColor = ColorTokens.ON_TERTIARY_CONTAINER.argb,
    public val surfaceContainerLow: LayoutColor = ColorTokens.SURFACE_CONTAINER_LOW.argb,
    public val surfaceContainer: LayoutColor = ColorTokens.SURFACE_CONTAINER.argb,
    public val surfaceContainerHigh: LayoutColor = ColorTokens.SURFACE_CONTAINER_HIGH.argb,
    public val onSurface: LayoutColor = ColorTokens.ON_SURFACE.argb,
    public val onSurfaceVariant: LayoutColor = ColorTokens.ON_SURFACE_VARIANT.argb,
    public val outline: LayoutColor = ColorTokens.OUTLINE.argb,
    public val outlineVariant: LayoutColor = ColorTokens.OUTLINE_VARIANT.argb,
    public val background: LayoutColor = ColorTokens.BACKGROUND.argb,
    public val onBackground: LayoutColor = ColorTokens.ON_BACKGROUND.argb,
    public val error: LayoutColor = ColorTokens.ERROR.argb,
    public val errorDim: LayoutColor = ColorTokens.ERROR_DIM.argb,
    public val errorContainer: LayoutColor = ColorTokens.ERROR_CONTAINER.argb,
    public val onError: LayoutColor = ColorTokens.ON_ERROR.argb,
    public val onErrorContainer: LayoutColor = ColorTokens.ON_ERROR_CONTAINER.argb,
)
