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
import android.content.res.TypedArray
import android.os.Build
import androidx.annotation.ColorRes
import androidx.annotation.FloatRange
import androidx.annotation.RequiresApi
import androidx.annotation.StyleableRes
import androidx.compose.material3.internal.colorUtil.Cam
import androidx.compose.material3.internal.colorUtil.CamUtils
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/** Dynamic colors in Material. */
@RequiresApi(31)
internal fun dynamicTonalPalette(context: Context): TonalPalette {
    val typedArray = context.obtainStyledAttributes(R.style.mc3_palette, R.styleable.mc3_palette)

    try {
        return TonalPalette(
            // The primary tonal range from the generated dynamic color palette
            primary100 = typedArray.getColor(R.styleable.mc3_palette_mc3_primary_100),
            primary99 = typedArray.getColor(R.styleable.mc3_palette_mc3_primary_99),
            primary95 = typedArray.getColor(R.styleable.mc3_palette_mc3_primary_95),
            primary90 = typedArray.getColor(R.styleable.mc3_palette_mc3_primary_90),
            primary80 = typedArray.getColor(R.styleable.mc3_palette_mc3_primary_80),
            primary70 = typedArray.getColor(R.styleable.mc3_palette_mc3_primary_70),
            primary60 = typedArray.getColor(R.styleable.mc3_palette_mc3_primary_60),
            primary50 = typedArray.getColor(R.styleable.mc3_palette_mc3_primary_50),
            primary40 = typedArray.getColor(R.styleable.mc3_palette_mc3_primary_40),
            primary30 = typedArray.getColor(R.styleable.mc3_palette_mc3_primary_30),
            primary20 = typedArray.getColor(R.styleable.mc3_palette_mc3_primary_20),
            primary10 = typedArray.getColor(R.styleable.mc3_palette_mc3_primary_10),
            primary0 = typedArray.getColor(R.styleable.mc3_palette_mc3_primary_0),

            // The secondary tonal range from the generated dynamic color palette
            secondary100 = typedArray.getColor(R.styleable.mc3_palette_mc3_secondary_100),
            secondary99 = typedArray.getColor(R.styleable.mc3_palette_mc3_secondary_99),
            secondary95 = typedArray.getColor(R.styleable.mc3_palette_mc3_secondary_95),
            secondary90 = typedArray.getColor(R.styleable.mc3_palette_mc3_secondary_90),
            secondary80 = typedArray.getColor(R.styleable.mc3_palette_mc3_secondary_80),
            secondary70 = typedArray.getColor(R.styleable.mc3_palette_mc3_secondary_70),
            secondary60 = typedArray.getColor(R.styleable.mc3_palette_mc3_secondary_60),
            secondary50 = typedArray.getColor(R.styleable.mc3_palette_mc3_secondary_50),
            secondary40 = typedArray.getColor(R.styleable.mc3_palette_mc3_secondary_40),
            secondary30 = typedArray.getColor(R.styleable.mc3_palette_mc3_secondary_30),
            secondary20 = typedArray.getColor(R.styleable.mc3_palette_mc3_secondary_20),
            secondary10 = typedArray.getColor(R.styleable.mc3_palette_mc3_secondary_10),
            secondary0 = typedArray.getColor(R.styleable.mc3_palette_mc3_secondary_0),

            // The tertiary tonal range from the generated dynamic color palette
            tertiary100 = typedArray.getColor(R.styleable.mc3_palette_mc3_tertiary_100),
            tertiary99 = typedArray.getColor(R.styleable.mc3_palette_mc3_tertiary_99),
            tertiary95 = typedArray.getColor(R.styleable.mc3_palette_mc3_tertiary_95),
            tertiary90 = typedArray.getColor(R.styleable.mc3_palette_mc3_tertiary_90),
            tertiary80 = typedArray.getColor(R.styleable.mc3_palette_mc3_tertiary_80),
            tertiary70 = typedArray.getColor(R.styleable.mc3_palette_mc3_tertiary_70),
            tertiary60 = typedArray.getColor(R.styleable.mc3_palette_mc3_tertiary_60),
            tertiary50 = typedArray.getColor(R.styleable.mc3_palette_mc3_tertiary_50),
            tertiary40 = typedArray.getColor(R.styleable.mc3_palette_mc3_tertiary_40),
            tertiary30 = typedArray.getColor(R.styleable.mc3_palette_mc3_tertiary_30),
            tertiary20 = typedArray.getColor(R.styleable.mc3_palette_mc3_tertiary_20),
            tertiary10 = typedArray.getColor(R.styleable.mc3_palette_mc3_tertiary_10),
            tertiary0 = typedArray.getColor(R.styleable.mc3_palette_mc3_tertiary_0),

            // The neutral tonal range from the generated dynamic color palette
            neutral100 = typedArray.getColor(R.styleable.mc3_palette_mc3_neutral_100),
            neutral99 = typedArray.getColor(R.styleable.mc3_palette_mc3_neutral_99),
            neutral98 =
                typedArray.getColor(R.styleable.mc3_palette_mc3_neutral_40).setLuminance(98f),
            neutral96 =
                typedArray.getColor(R.styleable.mc3_palette_mc3_neutral_40).setLuminance(96f),
            neutral95 = typedArray.getColor(R.styleable.mc3_palette_mc3_neutral_95),
            neutral94 =
                typedArray.getColor(R.styleable.mc3_palette_mc3_neutral_40).setLuminance(94f),
            neutral92 =
                typedArray.getColor(R.styleable.mc3_palette_mc3_neutral_40).setLuminance(92f),
            neutral90 = typedArray.getColor(R.styleable.mc3_palette_mc3_neutral_90),
            neutral87 =
                typedArray.getColor(R.styleable.mc3_palette_mc3_neutral_40).setLuminance(87f),
            neutral80 = typedArray.getColor(R.styleable.mc3_palette_mc3_neutral_80),
            neutral70 = typedArray.getColor(R.styleable.mc3_palette_mc3_neutral_70),
            neutral60 = typedArray.getColor(R.styleable.mc3_palette_mc3_neutral_60),
            neutral50 = typedArray.getColor(R.styleable.mc3_palette_mc3_neutral_50),
            neutral40 = typedArray.getColor(R.styleable.mc3_palette_mc3_neutral_40),
            neutral30 = typedArray.getColor(R.styleable.mc3_palette_mc3_neutral_30),
            neutral24 =
                typedArray.getColor(R.styleable.mc3_palette_mc3_neutral_40).setLuminance(24f),
            neutral22 =
                typedArray.getColor(R.styleable.mc3_palette_mc3_neutral_40).setLuminance(22f),
            neutral20 = typedArray.getColor(R.styleable.mc3_palette_mc3_neutral_20),
            neutral17 =
                typedArray.getColor(R.styleable.mc3_palette_mc3_neutral_40).setLuminance(17f),
            neutral12 =
                typedArray.getColor(R.styleable.mc3_palette_mc3_neutral_40).setLuminance(12f),
            neutral10 = typedArray.getColor(R.styleable.mc3_palette_mc3_neutral_10),
            neutral6 = typedArray.getColor(R.styleable.mc3_palette_mc3_neutral_40).setLuminance(6f),
            neutral4 = typedArray.getColor(R.styleable.mc3_palette_mc3_neutral_40).setLuminance(4f),
            neutral0 = typedArray.getColor(R.styleable.mc3_palette_mc3_neutral_0),

            // The neutral variant tonal range
            neutralVariant100 =
                typedArray.getColor(R.styleable.mc3_palette_mc3_neutral_variant_100),
            neutralVariant99 = typedArray.getColor(R.styleable.mc3_palette_mc3_neutral_variant_99),
            neutralVariant98 =
                typedArray
                    .getColor(R.styleable.mc3_palette_mc3_neutral_variant_40)
                    .setLuminance(98f),
            neutralVariant96 =
                typedArray
                    .getColor(R.styleable.mc3_palette_mc3_neutral_variant_40)
                    .setLuminance(96f),
            neutralVariant95 = typedArray.getColor(R.styleable.mc3_palette_mc3_neutral_variant_95),
            neutralVariant94 =
                typedArray
                    .getColor(R.styleable.mc3_palette_mc3_neutral_variant_40)
                    .setLuminance(94f),
            neutralVariant92 =
                typedArray
                    .getColor(R.styleable.mc3_palette_mc3_neutral_variant_40)
                    .setLuminance(92f),
            neutralVariant90 = typedArray.getColor(R.styleable.mc3_palette_mc3_neutral_variant_90),
            neutralVariant87 =
                typedArray
                    .getColor(R.styleable.mc3_palette_mc3_neutral_variant_40)
                    .setLuminance(87f),
            neutralVariant80 = typedArray.getColor(R.styleable.mc3_palette_mc3_neutral_variant_80),
            neutralVariant70 = typedArray.getColor(R.styleable.mc3_palette_mc3_neutral_variant_70),
            neutralVariant60 = typedArray.getColor(R.styleable.mc3_palette_mc3_neutral_variant_60),
            neutralVariant50 = typedArray.getColor(R.styleable.mc3_palette_mc3_neutral_variant_50),
            neutralVariant40 = typedArray.getColor(R.styleable.mc3_palette_mc3_neutral_variant_40),
            neutralVariant30 = typedArray.getColor(R.styleable.mc3_palette_mc3_neutral_variant_30),
            neutralVariant24 =
                typedArray
                    .getColor(R.styleable.mc3_palette_mc3_neutral_variant_40)
                    .setLuminance(24f),
            neutralVariant22 =
                typedArray
                    .getColor(R.styleable.mc3_palette_mc3_neutral_variant_40)
                    .setLuminance(22f),
            neutralVariant20 = typedArray.getColor(R.styleable.mc3_palette_mc3_neutral_variant_20),
            neutralVariant17 =
                typedArray
                    .getColor(R.styleable.mc3_palette_mc3_neutral_variant_40)
                    .setLuminance(17f),
            neutralVariant12 =
                typedArray
                    .getColor(R.styleable.mc3_palette_mc3_neutral_variant_40)
                    .setLuminance(12f),
            neutralVariant10 = typedArray.getColor(R.styleable.mc3_palette_mc3_neutral_variant_10),
            neutralVariant6 =
                typedArray
                    .getColor(R.styleable.mc3_palette_mc3_neutral_variant_40)
                    .setLuminance(6f),
            neutralVariant4 =
                typedArray
                    .getColor(R.styleable.mc3_palette_mc3_neutral_variant_40)
                    .setLuminance(4f),
            neutralVariant0 = typedArray.getColor(R.styleable.mc3_palette_mc3_neutral_variant_0),
        )
    } finally {
        typedArray.recycle()
    }
}

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
        // SDKs 31-33 return Chroma4 values for neutral palette, we instead leverage neutral
        // variant which provides chroma8 for less grey tones.
        val tonalPalette = dynamicTonalPalette(context)
        dynamicLightColorScheme31(tonalPalette)
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
        // SDKs 31-33 return Chroma4 values for neutral palette, we instead leverage neutral
        // variant which provides chroma8 for less grey tones.
        val tonalPalette = dynamicTonalPalette(context)
        dynamicDarkColorScheme31(tonalPalette)
    }
}

@RequiresApi(23)
private object ColorResourceHelper {
    fun getColor(context: Context, @ColorRes id: Int): Color {
        return Color(context.resources.getColor(id, context.theme))
    }
}

/**
 * Set the luminance(tone) of this color. Chroma may decrease because chroma has a different maximum
 * for any given hue and luminance.
 *
 * @param newLuminance 0 <= newLuminance <= 100; invalid values are corrected.
 */
internal fun Color.setLuminance(@FloatRange(from = 0.0, to = 100.0) newLuminance: Float): Color {
    if ((newLuminance < 0.0001) or (newLuminance > 99.9999)) {
        return Color(CamUtils.argbFromLstar(newLuminance.toDouble()))
    }

    val baseCam: Cam = Cam.fromInt(this.toArgb())
    val baseColor = Cam.getInt(baseCam.hue, baseCam.chroma, newLuminance)

    return Color(baseColor)
}

@RequiresApi(31)
internal fun dynamicLightColorScheme31(tonalPalette: TonalPalette) =
    lightColorScheme(
        primary = tonalPalette.primary40,
        onPrimary = tonalPalette.primary100,
        primaryContainer = tonalPalette.primary90,
        onPrimaryContainer = tonalPalette.primary10,
        inversePrimary = tonalPalette.primary80,
        secondary = tonalPalette.secondary40,
        onSecondary = tonalPalette.secondary100,
        secondaryContainer = tonalPalette.secondary90,
        onSecondaryContainer = tonalPalette.secondary10,
        tertiary = tonalPalette.tertiary40,
        onTertiary = tonalPalette.tertiary100,
        tertiaryContainer = tonalPalette.tertiary90,
        onTertiaryContainer = tonalPalette.tertiary10,
        background = tonalPalette.neutralVariant98,
        onBackground = tonalPalette.neutralVariant10,
        surface = tonalPalette.neutralVariant98,
        onSurface = tonalPalette.neutralVariant10,
        surfaceVariant = tonalPalette.neutralVariant90,
        onSurfaceVariant = tonalPalette.neutralVariant30,
        inverseSurface = tonalPalette.neutralVariant20,
        inverseOnSurface = tonalPalette.neutralVariant95,
        outline = tonalPalette.neutralVariant50,
        outlineVariant = tonalPalette.neutralVariant80,
        scrim = tonalPalette.neutralVariant0,
        surfaceBright = tonalPalette.neutralVariant98,
        surfaceDim = tonalPalette.neutralVariant87,
        surfaceContainer = tonalPalette.neutralVariant94,
        surfaceContainerHigh = tonalPalette.neutralVariant92,
        surfaceContainerHighest = tonalPalette.neutralVariant90,
        surfaceContainerLow = tonalPalette.neutralVariant96,
        surfaceContainerLowest = tonalPalette.neutralVariant100,
        surfaceTint = tonalPalette.primary40,
        primaryFixed = tonalPalette.primary90,
        primaryFixedDim = tonalPalette.primary80,
        onPrimaryFixed = tonalPalette.primary10,
        onPrimaryFixedVariant = tonalPalette.primary30,
        secondaryFixed = tonalPalette.secondary90,
        secondaryFixedDim = tonalPalette.secondary80,
        onSecondaryFixed = tonalPalette.secondary10,
        onSecondaryFixedVariant = tonalPalette.secondary30,
        tertiaryFixed = tonalPalette.tertiary90,
        tertiaryFixedDim = tonalPalette.tertiary80,
        onTertiaryFixed = tonalPalette.tertiary10,
        onTertiaryFixedVariant = tonalPalette.tertiary30,
    )

@RequiresApi(34)
internal fun dynamicLightColorScheme34(context: Context): ColorScheme {
    val typedArray =
        context.obtainStyledAttributes(R.style.mc3_light_scheme, R.styleable.mc3_scheme)
    try {
        return lightColorScheme(
            primary = typedArray.getColor(R.styleable.mc3_scheme_mc3_primary),
            onPrimary = typedArray.getColor(R.styleable.mc3_scheme_mc3_on_primary),
            primaryContainer = typedArray.getColor(R.styleable.mc3_scheme_mc3_primary_container),
            onPrimaryContainer =
                typedArray.getColor(R.styleable.mc3_scheme_mc3_on_primary_container),
            inversePrimary = typedArray.getColor(R.styleable.mc3_scheme_mc3_inverse_primary),
            secondary = typedArray.getColor(R.styleable.mc3_scheme_mc3_secondary),
            onSecondary = typedArray.getColor(R.styleable.mc3_scheme_mc3_on_secondary),
            secondaryContainer =
                typedArray.getColor(R.styleable.mc3_scheme_mc3_secondary_container),
            onSecondaryContainer =
                typedArray.getColor(R.styleable.mc3_scheme_mc3_on_secondary_container),
            tertiary = typedArray.getColor(R.styleable.mc3_scheme_mc3_tertiary),
            onTertiary = typedArray.getColor(R.styleable.mc3_scheme_mc3_on_tertiary),
            tertiaryContainer = typedArray.getColor(R.styleable.mc3_scheme_mc3_tertiary_container),
            onTertiaryContainer =
                typedArray.getColor(R.styleable.mc3_scheme_mc3_on_tertiary_container),
            background = typedArray.getColor(R.styleable.mc3_scheme_mc3_background),
            onBackground = typedArray.getColor(R.styleable.mc3_scheme_mc3_on_background),
            surface = typedArray.getColor(R.styleable.mc3_scheme_mc3_surface),
            onSurface = typedArray.getColor(R.styleable.mc3_scheme_mc3_on_surface),
            surfaceVariant = typedArray.getColor(R.styleable.mc3_scheme_mc3_surface_variant),
            onSurfaceVariant = typedArray.getColor(R.styleable.mc3_scheme_mc3_on_surface_variant),
            inverseSurface = typedArray.getColor(R.styleable.mc3_scheme_mc3_inverse_surface),
            inverseOnSurface = typedArray.getColor(R.styleable.mc3_scheme_mc3_inverse_on_surface),
            outline = typedArray.getColor(R.styleable.mc3_scheme_mc3_outline),
            outlineVariant = typedArray.getColor(R.styleable.mc3_scheme_mc3_outline_variant),
            surfaceBright = typedArray.getColor(R.styleable.mc3_scheme_mc3_surface_bright),
            surfaceDim = typedArray.getColor(R.styleable.mc3_scheme_mc3_surface_dim),
            surfaceContainer = typedArray.getColor(R.styleable.mc3_scheme_mc3_surface_container),
            surfaceContainerHigh =
                typedArray.getColor(R.styleable.mc3_scheme_mc3_surface_container_high),
            surfaceContainerHighest =
                typedArray.getColor(R.styleable.mc3_scheme_mc3_surface_container_highest),
            surfaceContainerLow =
                typedArray.getColor(R.styleable.mc3_scheme_mc3_surface_container_low),
            surfaceContainerLowest =
                typedArray.getColor(R.styleable.mc3_scheme_mc3_surface_container_lowest),
            surfaceTint = typedArray.getColor(R.styleable.mc3_scheme_mc3_surface_tint),
            primaryFixed = typedArray.getColor(R.styleable.mc3_scheme_mc3_primary_fixed),
            primaryFixedDim = typedArray.getColor(R.styleable.mc3_scheme_mc3_primary_fixed_dim),
            onPrimaryFixed = typedArray.getColor(R.styleable.mc3_scheme_mc3_on_primary_fixed),
            onPrimaryFixedVariant =
                typedArray.getColor(R.styleable.mc3_scheme_mc3_on_primary_fixed_variant),
            secondaryFixed = typedArray.getColor(R.styleable.mc3_scheme_mc3_secondary_fixed),
            secondaryFixedDim = typedArray.getColor(R.styleable.mc3_scheme_mc3_secondary_fixed_dim),
            onSecondaryFixed = typedArray.getColor(R.styleable.mc3_scheme_mc3_on_secondary_fixed),
            onSecondaryFixedVariant =
                typedArray.getColor(R.styleable.mc3_scheme_mc3_on_secondary_fixed_variant),
            tertiaryFixed = typedArray.getColor(R.styleable.mc3_scheme_mc3_tertiary_fixed),
            tertiaryFixedDim = typedArray.getColor(R.styleable.mc3_scheme_mc3_tertiary_fixed_dim),
            onTertiaryFixed = typedArray.getColor(R.styleable.mc3_scheme_mc3_on_tertiary_fixed),
            onTertiaryFixedVariant =
                typedArray.getColor(R.styleable.mc3_scheme_mc3_on_tertiary_fixed_variant),
        )
    } finally {
        typedArray.recycle()
    }
}

@RequiresApi(31)
internal fun dynamicDarkColorScheme31(tonalPalette: TonalPalette) =
    darkColorScheme(
        primary = tonalPalette.primary80,
        onPrimary = tonalPalette.primary20,
        primaryContainer = tonalPalette.primary30,
        onPrimaryContainer = tonalPalette.primary90,
        inversePrimary = tonalPalette.primary40,
        secondary = tonalPalette.secondary80,
        onSecondary = tonalPalette.secondary20,
        secondaryContainer = tonalPalette.secondary30,
        onSecondaryContainer = tonalPalette.secondary90,
        tertiary = tonalPalette.tertiary80,
        onTertiary = tonalPalette.tertiary20,
        tertiaryContainer = tonalPalette.tertiary30,
        onTertiaryContainer = tonalPalette.tertiary90,
        background = tonalPalette.neutralVariant6,
        onBackground = tonalPalette.neutralVariant90,
        surface = tonalPalette.neutralVariant6,
        onSurface = tonalPalette.neutralVariant90,
        surfaceVariant = tonalPalette.neutralVariant30,
        onSurfaceVariant = tonalPalette.neutralVariant80,
        inverseSurface = tonalPalette.neutralVariant90,
        inverseOnSurface = tonalPalette.neutralVariant20,
        outline = tonalPalette.neutralVariant60,
        outlineVariant = tonalPalette.neutralVariant30,
        scrim = tonalPalette.neutralVariant0,
        surfaceBright = tonalPalette.neutralVariant24,
        surfaceDim = tonalPalette.neutralVariant6,
        surfaceContainer = tonalPalette.neutralVariant12,
        surfaceContainerHigh = tonalPalette.neutralVariant17,
        surfaceContainerHighest = tonalPalette.neutralVariant22,
        surfaceContainerLow = tonalPalette.neutralVariant10,
        surfaceContainerLowest = tonalPalette.neutralVariant4,
        surfaceTint = tonalPalette.primary80,
        primaryFixed = tonalPalette.primary90,
        primaryFixedDim = tonalPalette.primary80,
        onPrimaryFixed = tonalPalette.primary10,
        onPrimaryFixedVariant = tonalPalette.primary30,
        secondaryFixed = tonalPalette.secondary90,
        secondaryFixedDim = tonalPalette.secondary80,
        onSecondaryFixed = tonalPalette.secondary10,
        onSecondaryFixedVariant = tonalPalette.secondary30,
        tertiaryFixed = tonalPalette.tertiary90,
        tertiaryFixedDim = tonalPalette.tertiary80,
        onTertiaryFixed = tonalPalette.tertiary10,
        onTertiaryFixedVariant = tonalPalette.tertiary30,
    )

@RequiresApi(34)
internal fun dynamicDarkColorScheme34(context: Context): ColorScheme {
    val typedArray = context.obtainStyledAttributes(R.style.mc3_dark_scheme, R.styleable.mc3_scheme)
    try {
        return darkColorScheme(
            primary = typedArray.getColor(R.styleable.mc3_scheme_mc3_primary),
            onPrimary = typedArray.getColor(R.styleable.mc3_scheme_mc3_on_primary),
            primaryContainer = typedArray.getColor(R.styleable.mc3_scheme_mc3_primary_container),
            onPrimaryContainer =
                typedArray.getColor(R.styleable.mc3_scheme_mc3_on_primary_container),
            inversePrimary = typedArray.getColor(R.styleable.mc3_scheme_mc3_inverse_primary),
            secondary = typedArray.getColor(R.styleable.mc3_scheme_mc3_secondary),
            onSecondary = typedArray.getColor(R.styleable.mc3_scheme_mc3_on_secondary),
            secondaryContainer =
                typedArray.getColor(R.styleable.mc3_scheme_mc3_secondary_container),
            onSecondaryContainer =
                typedArray.getColor(R.styleable.mc3_scheme_mc3_on_secondary_container),
            tertiary = typedArray.getColor(R.styleable.mc3_scheme_mc3_tertiary),
            onTertiary = typedArray.getColor(R.styleable.mc3_scheme_mc3_on_tertiary),
            tertiaryContainer = typedArray.getColor(R.styleable.mc3_scheme_mc3_tertiary_container),
            onTertiaryContainer =
                typedArray.getColor(R.styleable.mc3_scheme_mc3_on_tertiary_container),
            background = typedArray.getColor(R.styleable.mc3_scheme_mc3_background),
            onBackground = typedArray.getColor(R.styleable.mc3_scheme_mc3_on_background),
            surface = typedArray.getColor(R.styleable.mc3_scheme_mc3_surface),
            onSurface = typedArray.getColor(R.styleable.mc3_scheme_mc3_on_surface),
            surfaceVariant = typedArray.getColor(R.styleable.mc3_scheme_mc3_surface_variant),
            onSurfaceVariant = typedArray.getColor(R.styleable.mc3_scheme_mc3_on_surface_variant),
            inverseSurface = typedArray.getColor(R.styleable.mc3_scheme_mc3_inverse_surface),
            inverseOnSurface = typedArray.getColor(R.styleable.mc3_scheme_mc3_inverse_on_surface),
            outline = typedArray.getColor(R.styleable.mc3_scheme_mc3_outline),
            outlineVariant = typedArray.getColor(R.styleable.mc3_scheme_mc3_outline_variant),
            surfaceBright = typedArray.getColor(R.styleable.mc3_scheme_mc3_surface_bright),
            surfaceDim = typedArray.getColor(R.styleable.mc3_scheme_mc3_surface_dim),
            surfaceContainer = typedArray.getColor(R.styleable.mc3_scheme_mc3_surface_container),
            surfaceContainerHigh =
                typedArray.getColor(R.styleable.mc3_scheme_mc3_surface_container_high),
            surfaceContainerHighest =
                typedArray.getColor(R.styleable.mc3_scheme_mc3_surface_container_highest),
            surfaceContainerLow =
                typedArray.getColor(R.styleable.mc3_scheme_mc3_surface_container_low),
            surfaceContainerLowest =
                typedArray.getColor(R.styleable.mc3_scheme_mc3_surface_container_lowest),
            surfaceTint = typedArray.getColor(R.styleable.mc3_scheme_mc3_surface_tint),
            primaryFixed = typedArray.getColor(R.styleable.mc3_scheme_mc3_primary_fixed),
            primaryFixedDim = typedArray.getColor(R.styleable.mc3_scheme_mc3_primary_fixed_dim),
            onPrimaryFixed = typedArray.getColor(R.styleable.mc3_scheme_mc3_on_primary_fixed),
            onPrimaryFixedVariant =
                typedArray.getColor(R.styleable.mc3_scheme_mc3_on_primary_fixed_variant),
            secondaryFixed = typedArray.getColor(R.styleable.mc3_scheme_mc3_secondary_fixed),
            secondaryFixedDim = typedArray.getColor(R.styleable.mc3_scheme_mc3_secondary_fixed_dim),
            onSecondaryFixed = typedArray.getColor(R.styleable.mc3_scheme_mc3_on_secondary_fixed),
            onSecondaryFixedVariant =
                typedArray.getColor(R.styleable.mc3_scheme_mc3_on_secondary_fixed_variant),
            tertiaryFixed = typedArray.getColor(R.styleable.mc3_scheme_mc3_tertiary_fixed),
            tertiaryFixedDim = typedArray.getColor(R.styleable.mc3_scheme_mc3_tertiary_fixed_dim),
            onTertiaryFixed = typedArray.getColor(R.styleable.mc3_scheme_mc3_on_tertiary_fixed),
            onTertiaryFixedVariant =
                typedArray.getColor(R.styleable.mc3_scheme_mc3_on_tertiary_fixed_variant),
        )
    } finally {
        typedArray.recycle()
    }
}

private fun TypedArray.getColor(@StyleableRes index: Int): Color {
    return Color(getColor(index, 0))
}
