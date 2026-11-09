/*
 * Copyright 2021 The Android Open Source Project
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

@file:JvmName("DynamicTonalPaletteKt")

package androidx.compose.material3

import android.content.Context
import android.os.Build
import androidx.annotation.ColorRes
import androidx.annotation.RequiresApi
import androidx.compose.material3.internal.colorUtil.Cam
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Creates a light dynamic color scheme.
 *
 * Use this function to create a color scheme based off the system wallpaper. If the developer
 * changes the wallpaper this color scheme will change accordingly. This dynamic scheme is a light
 * theme variant.
 *
 * @param context The context required to get system resource data.
 */
@RequiresApi(Build.VERSION_CODES.S)
public fun dynamicLightColorScheme(context: Context): ColorScheme {
    return if (Build.VERSION.SDK_INT >= 34) {
        // SDKs 34 and greater return appropriate Chroma6 values for neutral palette
        dynamicLightColorScheme34(context)
    } else {
        dynamicLightColorScheme31(context)
    }
}

/**
 * Creates a dark dynamic color scheme.
 *
 * Use this function to create a color scheme based off the system wallpaper. If the developer
 * changes the wallpaper this color scheme will change accordingly. This dynamic scheme is a dark
 * theme variant.
 *
 * @param context The context required to get system resource data.
 */
@RequiresApi(Build.VERSION_CODES.S)
public fun dynamicDarkColorScheme(context: Context): ColorScheme {
    return if (Build.VERSION.SDK_INT >= 34) {
        // SDKs 34 and greater return appropriate Chroma6 values for neutral palette
        dynamicDarkColorScheme34(context)
    } else {
        dynamicDarkColorScheme31(context)
    }
}

private object ColorResourceHelper {
    fun getColor(context: Context, @ColorRes id: Int): Color {
        return Color(context.resources.getColor(id, context.theme))
    }
}

/**
 * Returns the hue and chroma of the system neutral variant tone 40. SDKs 31-33 don't provide all
 * the neutral variant tones used by the color schemes, so the missing tones are generated from it.
 */
@RequiresApi(31)
private fun neutralVariantKeyColor(context: Context): Cam =
    Cam.fromInt(ColorResourceHelper.getColor(context, android.R.color.system_neutral2_600).toArgb())

/** Returns the color with this hue and chroma, and the given [tone] (L* in L*a*b*). */
private fun Cam.toColor(tone: Float): Color = Color(Cam.getInt(hue, chroma, tone))

/**
 * Creates a light dynamic color scheme from the system tonal palettes on SDKs 31-33.
 *
 * Only the tones used by the scheme are resolved. SDKs 31-33 return Chroma4 values for the neutral
 * palette, so the neutral variant palette, which provides Chroma8 values, is used instead for less
 * grey tones.
 */
@RequiresApi(31)
internal fun dynamicLightColorScheme31(context: Context): ColorScheme {
    val neutralVariantKeyColor = neutralVariantKeyColor(context)
    val primary100 = ColorResourceHelper.getColor(context, android.R.color.system_accent1_0)
    val primary90 = ColorResourceHelper.getColor(context, android.R.color.system_accent1_100)
    val primary80 = ColorResourceHelper.getColor(context, android.R.color.system_accent1_200)
    val primary40 = ColorResourceHelper.getColor(context, android.R.color.system_accent1_600)
    val primary30 = ColorResourceHelper.getColor(context, android.R.color.system_accent1_700)
    val primary10 = ColorResourceHelper.getColor(context, android.R.color.system_accent1_900)
    val secondary100 = ColorResourceHelper.getColor(context, android.R.color.system_accent2_0)
    val secondary90 = ColorResourceHelper.getColor(context, android.R.color.system_accent2_100)
    val secondary80 = ColorResourceHelper.getColor(context, android.R.color.system_accent2_200)
    val secondary40 = ColorResourceHelper.getColor(context, android.R.color.system_accent2_600)
    val secondary30 = ColorResourceHelper.getColor(context, android.R.color.system_accent2_700)
    val secondary10 = ColorResourceHelper.getColor(context, android.R.color.system_accent2_900)
    val tertiary100 = ColorResourceHelper.getColor(context, android.R.color.system_accent3_0)
    val tertiary90 = ColorResourceHelper.getColor(context, android.R.color.system_accent3_100)
    val tertiary80 = ColorResourceHelper.getColor(context, android.R.color.system_accent3_200)
    val tertiary40 = ColorResourceHelper.getColor(context, android.R.color.system_accent3_600)
    val tertiary30 = ColorResourceHelper.getColor(context, android.R.color.system_accent3_700)
    val tertiary10 = ColorResourceHelper.getColor(context, android.R.color.system_accent3_900)
    val neutralVariant100 = ColorResourceHelper.getColor(context, android.R.color.system_neutral2_0)
    val neutralVariant98 = neutralVariantKeyColor.toColor(tone = 98f)
    val neutralVariant96 = neutralVariantKeyColor.toColor(tone = 96f)
    val neutralVariant95 = ColorResourceHelper.getColor(context, android.R.color.system_neutral2_50)
    val neutralVariant94 = neutralVariantKeyColor.toColor(tone = 94f)
    val neutralVariant92 = neutralVariantKeyColor.toColor(tone = 92f)
    val neutralVariant90 =
        ColorResourceHelper.getColor(context, android.R.color.system_neutral2_100)
    val neutralVariant87 = neutralVariantKeyColor.toColor(tone = 87f)
    val neutralVariant80 =
        ColorResourceHelper.getColor(context, android.R.color.system_neutral2_200)
    val neutralVariant50 =
        ColorResourceHelper.getColor(context, android.R.color.system_neutral2_500)
    val neutralVariant30 =
        ColorResourceHelper.getColor(context, android.R.color.system_neutral2_700)
    val neutralVariant20 =
        ColorResourceHelper.getColor(context, android.R.color.system_neutral2_800)
    val neutralVariant10 =
        ColorResourceHelper.getColor(context, android.R.color.system_neutral2_900)
    val neutralVariant0 =
        ColorResourceHelper.getColor(context, android.R.color.system_neutral2_1000)
    return lightColorScheme(
        primary = primary40,
        onPrimary = primary100,
        primaryContainer = primary90,
        onPrimaryContainer = primary10,
        inversePrimary = primary80,
        secondary = secondary40,
        onSecondary = secondary100,
        secondaryContainer = secondary90,
        onSecondaryContainer = secondary10,
        tertiary = tertiary40,
        onTertiary = tertiary100,
        tertiaryContainer = tertiary90,
        onTertiaryContainer = tertiary10,
        background = neutralVariant98,
        onBackground = neutralVariant10,
        surface = neutralVariant98,
        onSurface = neutralVariant10,
        surfaceVariant = neutralVariant90,
        onSurfaceVariant = neutralVariant30,
        inverseSurface = neutralVariant20,
        inverseOnSurface = neutralVariant95,
        outline = neutralVariant50,
        outlineVariant = neutralVariant80,
        scrim = neutralVariant0,
        surfaceBright = neutralVariant98,
        surfaceDim = neutralVariant87,
        surfaceContainer = neutralVariant94,
        surfaceContainerHigh = neutralVariant92,
        surfaceContainerHighest = neutralVariant90,
        surfaceContainerLow = neutralVariant96,
        surfaceContainerLowest = neutralVariant100,
        surfaceTint = primary40,
        primaryFixed = primary90,
        primaryFixedDim = primary80,
        onPrimaryFixed = primary10,
        onPrimaryFixedVariant = primary30,
        secondaryFixed = secondary90,
        secondaryFixedDim = secondary80,
        onSecondaryFixed = secondary10,
        onSecondaryFixedVariant = secondary30,
        tertiaryFixed = tertiary90,
        tertiaryFixedDim = tertiary80,
        onTertiaryFixed = tertiary10,
        onTertiaryFixedVariant = tertiary30,
    )
}

@RequiresApi(34)
internal fun dynamicLightColorScheme34(context: Context) =
    lightColorScheme(
        primary = ColorResourceHelper.getColor(context, android.R.color.system_primary_light),
        onPrimary = ColorResourceHelper.getColor(context, android.R.color.system_on_primary_light),
        primaryContainer =
            ColorResourceHelper.getColor(context, android.R.color.system_primary_container_light),
        onPrimaryContainer =
            ColorResourceHelper.getColor(
                context,
                android.R.color.system_on_primary_container_light,
            ),
        inversePrimary = ColorResourceHelper.getColor(context, android.R.color.system_primary_dark),
        secondary = ColorResourceHelper.getColor(context, android.R.color.system_secondary_light),
        onSecondary =
            ColorResourceHelper.getColor(context, android.R.color.system_on_secondary_light),
        secondaryContainer =
            ColorResourceHelper.getColor(context, android.R.color.system_secondary_container_light),
        onSecondaryContainer =
            ColorResourceHelper.getColor(
                context,
                android.R.color.system_on_secondary_container_light,
            ),
        tertiary = ColorResourceHelper.getColor(context, android.R.color.system_tertiary_light),
        onTertiary =
            ColorResourceHelper.getColor(context, android.R.color.system_on_tertiary_light),
        tertiaryContainer =
            ColorResourceHelper.getColor(context, android.R.color.system_tertiary_container_light),
        onTertiaryContainer =
            ColorResourceHelper.getColor(
                context,
                android.R.color.system_on_tertiary_container_light,
            ),
        background = ColorResourceHelper.getColor(context, android.R.color.system_background_light),
        onBackground =
            ColorResourceHelper.getColor(context, android.R.color.system_on_background_light),
        surface = ColorResourceHelper.getColor(context, android.R.color.system_surface_light),
        onSurface = ColorResourceHelper.getColor(context, android.R.color.system_on_surface_light),
        surfaceVariant =
            ColorResourceHelper.getColor(context, android.R.color.system_surface_variant_light),
        onSurfaceVariant =
            ColorResourceHelper.getColor(context, android.R.color.system_on_surface_variant_light),
        inverseSurface = ColorResourceHelper.getColor(context, android.R.color.system_surface_dark),
        inverseOnSurface =
            ColorResourceHelper.getColor(context, android.R.color.system_on_surface_dark),
        outline = ColorResourceHelper.getColor(context, android.R.color.system_outline_light),
        outlineVariant =
            ColorResourceHelper.getColor(context, android.R.color.system_outline_variant_light),
        // scrim
        surfaceBright =
            ColorResourceHelper.getColor(context, android.R.color.system_surface_bright_light),
        surfaceDim =
            ColorResourceHelper.getColor(context, android.R.color.system_surface_dim_light),
        surfaceContainer =
            ColorResourceHelper.getColor(context, android.R.color.system_surface_container_light),
        surfaceContainerHigh =
            ColorResourceHelper.getColor(
                context,
                android.R.color.system_surface_container_high_light,
            ),
        surfaceContainerHighest =
            ColorResourceHelper.getColor(
                context,
                android.R.color.system_surface_container_highest_light,
            ),
        surfaceContainerLow =
            ColorResourceHelper.getColor(
                context,
                android.R.color.system_surface_container_low_light,
            ),
        surfaceContainerLowest =
            ColorResourceHelper.getColor(
                context,
                android.R.color.system_surface_container_lowest_light,
            ),
        surfaceTint = ColorResourceHelper.getColor(context, android.R.color.system_primary_light),
        primaryFixed = ColorResourceHelper.getColor(context, android.R.color.system_primary_fixed),
        primaryFixedDim =
            ColorResourceHelper.getColor(context, android.R.color.system_primary_fixed_dim),
        onPrimaryFixed =
            ColorResourceHelper.getColor(context, android.R.color.system_on_primary_fixed),
        onPrimaryFixedVariant =
            ColorResourceHelper.getColor(context, android.R.color.system_on_primary_fixed_variant),
        secondaryFixed =
            ColorResourceHelper.getColor(context, android.R.color.system_secondary_fixed),
        secondaryFixedDim =
            ColorResourceHelper.getColor(context, android.R.color.system_secondary_fixed_dim),
        onSecondaryFixed =
            ColorResourceHelper.getColor(context, android.R.color.system_on_secondary_fixed),
        onSecondaryFixedVariant =
            ColorResourceHelper.getColor(
                context,
                android.R.color.system_on_secondary_fixed_variant,
            ),
        tertiaryFixed =
            ColorResourceHelper.getColor(context, android.R.color.system_tertiary_fixed),
        tertiaryFixedDim =
            ColorResourceHelper.getColor(context, android.R.color.system_tertiary_fixed_dim),
        onTertiaryFixed =
            ColorResourceHelper.getColor(context, android.R.color.system_on_tertiary_fixed),
        onTertiaryFixedVariant =
            ColorResourceHelper.getColor(context, android.R.color.system_on_tertiary_fixed_variant),
    )

/**
 * Creates a dark dynamic color scheme from the system tonal palettes on SDKs 31-33.
 *
 * Only the tones used by the scheme are resolved. SDKs 31-33 return Chroma4 values for the neutral
 * palette, so the neutral variant palette, which provides Chroma8 values, is used instead for less
 * grey tones.
 */
@RequiresApi(31)
internal fun dynamicDarkColorScheme31(context: Context): ColorScheme {
    val neutralVariantKeyColor = neutralVariantKeyColor(context)
    val primary90 = ColorResourceHelper.getColor(context, android.R.color.system_accent1_100)
    val primary80 = ColorResourceHelper.getColor(context, android.R.color.system_accent1_200)
    val primary40 = ColorResourceHelper.getColor(context, android.R.color.system_accent1_600)
    val primary30 = ColorResourceHelper.getColor(context, android.R.color.system_accent1_700)
    val primary20 = ColorResourceHelper.getColor(context, android.R.color.system_accent1_800)
    val primary10 = ColorResourceHelper.getColor(context, android.R.color.system_accent1_900)
    val secondary90 = ColorResourceHelper.getColor(context, android.R.color.system_accent2_100)
    val secondary80 = ColorResourceHelper.getColor(context, android.R.color.system_accent2_200)
    val secondary30 = ColorResourceHelper.getColor(context, android.R.color.system_accent2_700)
    val secondary20 = ColorResourceHelper.getColor(context, android.R.color.system_accent2_800)
    val secondary10 = ColorResourceHelper.getColor(context, android.R.color.system_accent2_900)
    val tertiary90 = ColorResourceHelper.getColor(context, android.R.color.system_accent3_100)
    val tertiary80 = ColorResourceHelper.getColor(context, android.R.color.system_accent3_200)
    val tertiary30 = ColorResourceHelper.getColor(context, android.R.color.system_accent3_700)
    val tertiary20 = ColorResourceHelper.getColor(context, android.R.color.system_accent3_800)
    val tertiary10 = ColorResourceHelper.getColor(context, android.R.color.system_accent3_900)
    val neutralVariant90 =
        ColorResourceHelper.getColor(context, android.R.color.system_neutral2_100)
    val neutralVariant80 =
        ColorResourceHelper.getColor(context, android.R.color.system_neutral2_200)
    val neutralVariant60 =
        ColorResourceHelper.getColor(context, android.R.color.system_neutral2_400)
    val neutralVariant30 =
        ColorResourceHelper.getColor(context, android.R.color.system_neutral2_700)
    val neutralVariant24 = neutralVariantKeyColor.toColor(tone = 24f)
    val neutralVariant22 = neutralVariantKeyColor.toColor(tone = 22f)
    val neutralVariant20 =
        ColorResourceHelper.getColor(context, android.R.color.system_neutral2_800)
    val neutralVariant17 = neutralVariantKeyColor.toColor(tone = 17f)
    val neutralVariant12 = neutralVariantKeyColor.toColor(tone = 12f)
    val neutralVariant10 =
        ColorResourceHelper.getColor(context, android.R.color.system_neutral2_900)
    val neutralVariant6 = neutralVariantKeyColor.toColor(tone = 6f)
    val neutralVariant4 = neutralVariantKeyColor.toColor(tone = 4f)
    val neutralVariant0 =
        ColorResourceHelper.getColor(context, android.R.color.system_neutral2_1000)
    return darkColorScheme(
        primary = primary80,
        onPrimary = primary20,
        primaryContainer = primary30,
        onPrimaryContainer = primary90,
        inversePrimary = primary40,
        secondary = secondary80,
        onSecondary = secondary20,
        secondaryContainer = secondary30,
        onSecondaryContainer = secondary90,
        tertiary = tertiary80,
        onTertiary = tertiary20,
        tertiaryContainer = tertiary30,
        onTertiaryContainer = tertiary90,
        background = neutralVariant6,
        onBackground = neutralVariant90,
        surface = neutralVariant6,
        onSurface = neutralVariant90,
        surfaceVariant = neutralVariant30,
        onSurfaceVariant = neutralVariant80,
        inverseSurface = neutralVariant90,
        inverseOnSurface = neutralVariant20,
        outline = neutralVariant60,
        outlineVariant = neutralVariant30,
        scrim = neutralVariant0,
        surfaceBright = neutralVariant24,
        surfaceDim = neutralVariant6,
        surfaceContainer = neutralVariant12,
        surfaceContainerHigh = neutralVariant17,
        surfaceContainerHighest = neutralVariant22,
        surfaceContainerLow = neutralVariant10,
        surfaceContainerLowest = neutralVariant4,
        surfaceTint = primary80,
        primaryFixed = primary90,
        primaryFixedDim = primary80,
        onPrimaryFixed = primary10,
        onPrimaryFixedVariant = primary30,
        secondaryFixed = secondary90,
        secondaryFixedDim = secondary80,
        onSecondaryFixed = secondary10,
        onSecondaryFixedVariant = secondary30,
        tertiaryFixed = tertiary90,
        tertiaryFixedDim = tertiary80,
        onTertiaryFixed = tertiary10,
        onTertiaryFixedVariant = tertiary30,
    )
}

@RequiresApi(34)
internal fun dynamicDarkColorScheme34(context: Context) =
    darkColorScheme(
        primary = ColorResourceHelper.getColor(context, android.R.color.system_primary_dark),
        onPrimary = ColorResourceHelper.getColor(context, android.R.color.system_on_primary_dark),
        primaryContainer =
            ColorResourceHelper.getColor(context, android.R.color.system_primary_container_dark),
        onPrimaryContainer =
            ColorResourceHelper.getColor(context, android.R.color.system_on_primary_container_dark),
        inversePrimary =
            ColorResourceHelper.getColor(context, android.R.color.system_primary_light),
        secondary = ColorResourceHelper.getColor(context, android.R.color.system_secondary_dark),
        onSecondary =
            ColorResourceHelper.getColor(context, android.R.color.system_on_secondary_dark),
        secondaryContainer =
            ColorResourceHelper.getColor(context, android.R.color.system_secondary_container_dark),
        onSecondaryContainer =
            ColorResourceHelper.getColor(
                context,
                android.R.color.system_on_secondary_container_dark,
            ),
        tertiary = ColorResourceHelper.getColor(context, android.R.color.system_tertiary_dark),
        onTertiary = ColorResourceHelper.getColor(context, android.R.color.system_on_tertiary_dark),
        tertiaryContainer =
            ColorResourceHelper.getColor(context, android.R.color.system_tertiary_container_dark),
        onTertiaryContainer =
            ColorResourceHelper.getColor(
                context,
                android.R.color.system_on_tertiary_container_dark,
            ),
        background = ColorResourceHelper.getColor(context, android.R.color.system_background_dark),
        onBackground =
            ColorResourceHelper.getColor(context, android.R.color.system_on_background_dark),
        surface = ColorResourceHelper.getColor(context, android.R.color.system_surface_dark),
        onSurface = ColorResourceHelper.getColor(context, android.R.color.system_on_surface_dark),
        surfaceVariant =
            ColorResourceHelper.getColor(context, android.R.color.system_surface_variant_dark),
        onSurfaceVariant =
            ColorResourceHelper.getColor(context, android.R.color.system_on_surface_variant_dark),
        inverseSurface =
            ColorResourceHelper.getColor(context, android.R.color.system_surface_light),
        inverseOnSurface =
            ColorResourceHelper.getColor(context, android.R.color.system_on_surface_light),
        outline = ColorResourceHelper.getColor(context, android.R.color.system_outline_dark),
        outlineVariant =
            ColorResourceHelper.getColor(context, android.R.color.system_outline_variant_dark),
        // scrim
        surfaceBright =
            ColorResourceHelper.getColor(context, android.R.color.system_surface_bright_dark),
        surfaceDim = ColorResourceHelper.getColor(context, android.R.color.system_surface_dim_dark),
        surfaceContainer =
            ColorResourceHelper.getColor(context, android.R.color.system_surface_container_dark),
        surfaceContainerHigh =
            ColorResourceHelper.getColor(
                context,
                android.R.color.system_surface_container_high_dark,
            ),
        surfaceContainerHighest =
            ColorResourceHelper.getColor(
                context,
                android.R.color.system_surface_container_highest_dark,
            ),
        surfaceContainerLow =
            ColorResourceHelper.getColor(
                context,
                android.R.color.system_surface_container_low_dark,
            ),
        surfaceContainerLowest =
            ColorResourceHelper.getColor(
                context,
                android.R.color.system_surface_container_lowest_dark,
            ),
        surfaceTint = ColorResourceHelper.getColor(context, android.R.color.system_primary_dark),
        primaryFixed = ColorResourceHelper.getColor(context, android.R.color.system_primary_fixed),
        primaryFixedDim =
            ColorResourceHelper.getColor(context, android.R.color.system_primary_fixed_dim),
        onPrimaryFixed =
            ColorResourceHelper.getColor(context, android.R.color.system_on_primary_fixed),
        onPrimaryFixedVariant =
            ColorResourceHelper.getColor(context, android.R.color.system_on_primary_fixed_variant),
        secondaryFixed =
            ColorResourceHelper.getColor(context, android.R.color.system_secondary_fixed),
        secondaryFixedDim =
            ColorResourceHelper.getColor(context, android.R.color.system_secondary_fixed_dim),
        onSecondaryFixed =
            ColorResourceHelper.getColor(context, android.R.color.system_on_secondary_fixed),
        onSecondaryFixedVariant =
            ColorResourceHelper.getColor(
                context,
                android.R.color.system_on_secondary_fixed_variant,
            ),
        tertiaryFixed =
            ColorResourceHelper.getColor(context, android.R.color.system_tertiary_fixed),
        tertiaryFixedDim =
            ColorResourceHelper.getColor(context, android.R.color.system_tertiary_fixed_dim),
        onTertiaryFixed =
            ColorResourceHelper.getColor(context, android.R.color.system_on_tertiary_fixed),
        onTertiaryFixedVariant =
            ColorResourceHelper.getColor(context, android.R.color.system_on_tertiary_fixed_variant),
    )
