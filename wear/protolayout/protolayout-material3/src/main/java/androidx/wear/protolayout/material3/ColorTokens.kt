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

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.ColorBuilders.ColorProp
import androidx.wear.protolayout.material3.tokens.ColorTokens

/** Class holding color scheme as defined by the Wear Material3 color specification. */
public object ColorTokens {
    /** Returns the [ColorProp] from the color tokens for the given token name. */
    internal fun fromToken(@ColorToken colorToken: Int): ColorProp {
        return when (colorToken) {
            BACKGROUND -> ColorBuilders.argb(ColorTokens.BACKGROUND)
            ERROR -> ColorBuilders.argb(ColorTokens.ERROR)
            ERROR_CONTAINER -> ColorBuilders.argb(ColorTokens.ERROR_CONTAINER)
            ON_BACKGROUND -> ColorBuilders.argb(ColorTokens.ON_BACKGROUND)
            ON_ERROR -> ColorBuilders.argb(ColorTokens.ON_ERROR)
            ON_ERROR_CONTAINER -> ColorBuilders.argb(ColorTokens.ON_ERROR_CONTAINER)
            ON_PRIMARY -> ColorBuilders.argb(ColorTokens.ON_PRIMARY)
            ON_PRIMARY_CONTAINER -> ColorBuilders.argb(ColorTokens.ON_PRIMARY_CONTAINER)
            ON_SECONDARY -> ColorBuilders.argb(ColorTokens.ON_SECONDARY)
            ON_SECONDARY_CONTAINER -> ColorBuilders.argb(ColorTokens.ON_SECONDARY_CONTAINER)
            ON_SURFACE -> ColorBuilders.argb(ColorTokens.ON_SURFACE)
            ON_SURFACE_VARIANT -> ColorBuilders.argb(ColorTokens.ON_SURFACE_VARIANT)
            ON_TERTIARY -> ColorBuilders.argb(ColorTokens.ON_TERTIARY)
            ON_TERTIARY_CONTAINER -> ColorBuilders.argb(ColorTokens.ON_TERTIARY_CONTAINER)
            OUTLINE -> ColorBuilders.argb(ColorTokens.OUTLINE)
            OUTLINE_VARIANT -> ColorBuilders.argb(ColorTokens.OUTLINE_VARIANT)
            PRIMARY -> ColorBuilders.argb(ColorTokens.PRIMARY)
            PRIMARY_CONTAINER -> ColorBuilders.argb(ColorTokens.PRIMARY_CONTAINER)
            PRIMARY_DIM -> ColorBuilders.argb(ColorTokens.PRIMARY_DIM)
            SECONDARY -> ColorBuilders.argb(ColorTokens.SECONDARY)
            SECONDARY_CONTAINER -> ColorBuilders.argb(ColorTokens.SECONDARY_CONTAINER)
            SECONDARY_DIM -> ColorBuilders.argb(ColorTokens.SECONDARY_DIM)
            SURFACE_CONTAINER -> ColorBuilders.argb(ColorTokens.SURFACE_CONTAINER)
            SURFACE_CONTAINER_HIGH -> ColorBuilders.argb(ColorTokens.SURFACE_CONTAINER_HIGH)
            SURFACE_CONTAINER_LOW -> ColorBuilders.argb(ColorTokens.SURFACE_CONTAINER_LOW)
            TERTIARY -> ColorBuilders.argb(ColorTokens.TERTIARY)
            TERTIARY_CONTAINER -> ColorBuilders.argb(ColorTokens.TERTIARY_CONTAINER)
            TERTIARY_DIM -> ColorBuilders.argb(ColorTokens.TERTIARY_DIM)
            else -> throw IllegalArgumentException("Color $colorToken does not exits.")
        }
    }

    /**
     * Static color used behind scrollable content like text, cards and buttons, not to be confused
     * with surface (which can change through color and elevation). For watch this is best for
     * battery life and contrast of other colors.
     */
    public const val BACKGROUND: Int = 0

    /**
     * Indicates remove, delete, close or dismiss actions, such as Swipe to Reveal. Added as a
     * container alternative that is slightly less alarming and urgent than the default error color.
     */
    public const val ERROR: Int = 1

    /**
     * Indicates errors or emergency actions, such as safety alerts. This color is for use-cases
     * that are more alarming and urgent than the default errorContainer color.
     */
    public const val ERROR_CONTAINER: Int = 2

    /** Used for text, cards, buttons and icons shown against the background color. */
    public const val ON_BACKGROUND: Int = 3

    /** Used for text and icons on the error color. */
    public const val ON_ERROR: Int = 4

    /** Used for text and icons on the error color. */
    public const val ON_ERROR_CONTAINER: Int = 5

    /** Text and icons shown against the primary color */
    public const val ON_PRIMARY: Int = 6

    /** Contrast-passing colour shown against the primary container */
    public const val ON_PRIMARY_CONTAINER: Int = 7

    /** Text and icons shown against the secondary color */
    public const val ON_SECONDARY: Int = 8

    /** Contrast-passing color shown against the secondary container */
    public const val ON_SECONDARY_CONTAINER: Int = 9

    /** Text and icons shown against the surface color */
    public const val ON_SURFACE: Int = 10

    /**
     * On-surface variant for text and icons shown against the surface color where 3:1 contrast
     * ratio isn’t required, a disabled state, or non-interactive icon.
     */
    public const val ON_SURFACE_VARIANT: Int = 11

    /** Text and icons shown against the tertiary color */
    public const val ON_TERTIARY: Int = 12

    /** Contrast-passing color shown against the tertiary container */
    public const val ON_TERTIARY_CONTAINER: Int = 13

    /** Subtle color used for borders like outline buttons */
    public const val OUTLINE: Int = 14

    /**
     * Outline-variant is used to define the border of a component where 3:1 contrast ratio isn’t
     * required, a container, or a divider.
     */
    public const val OUTLINE_VARIANT: Int = 15

    /** Main color used across screens and components */
    public const val PRIMARY: Int = 16

    /** Standout container color for key components */
    public const val PRIMARY_CONTAINER: Int = 17

    /** Less prominent color, for components and screens */
    public const val PRIMARY_DIM: Int = 18

    /** Accent color used across screens and components */
    public const val SECONDARY: Int = 19

    /** Less prominent container color, for components like tonal buttons */
    public const val SECONDARY_CONTAINER: Int = 20

    /** Less prominent accent color, for components and screens */
    public const val SECONDARY_DIM: Int = 21

    /** Container colour for tonal buttons, cards, or any component sitting above the background. */
    public const val SURFACE_CONTAINER: Int = 22

    /**
     * Container colour for tonal buttons, cards, or any component sitting above the background.
     * Slightly higher contrast to the background than surface. Surface variant for surface colour
     * containing text and icons where 3:1 contrast ratio isn’t required, a disabled state, or
     * non-interactive icon.
     */
    public const val SURFACE_CONTAINER_HIGH: Int = 23

    /**
     * Container colour for tonal buttons, cards, or any component sitting above the background.
     * Slightly lower contrast to the background than surface.
     */
    public const val SURFACE_CONTAINER_LOW: Int = 24

    /** Accent color used across screens and components */
    public const val TERTIARY: Int = 25

    /** Contrasting container color, for components like selection toggles and buttons */
    public const val TERTIARY_CONTAINER: Int = 26

    /** Less prominent accent color, for components and screens */
    public const val TERTIARY_DIM: Int = 27

    internal const val TOKEN_COUNT = 28

    /** The referencing token names for a range of contrasting colors in Material3. */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        BACKGROUND,
        ERROR,
        ERROR_CONTAINER,
        ON_BACKGROUND,
        ON_ERROR,
        ON_ERROR_CONTAINER,
        ON_PRIMARY,
        ON_PRIMARY_CONTAINER,
        ON_SECONDARY,
        ON_SECONDARY_CONTAINER,
        ON_SURFACE,
        ON_SURFACE_VARIANT,
        ON_TERTIARY,
        ON_TERTIARY_CONTAINER,
        OUTLINE,
        OUTLINE_VARIANT,
        PRIMARY,
        PRIMARY_CONTAINER,
        PRIMARY_DIM,
        SECONDARY,
        SECONDARY_CONTAINER,
        SECONDARY_DIM,
        SURFACE_CONTAINER,
        SURFACE_CONTAINER_HIGH,
        SURFACE_CONTAINER_LOW,
        TERTIARY,
        TERTIARY_CONTAINER,
        TERTIARY_DIM
    )
    public annotation class ColorToken
}
