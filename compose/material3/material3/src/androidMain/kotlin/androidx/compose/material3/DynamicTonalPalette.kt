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

package androidx.compose.material3

import android.content.Context
import android.os.Build
import androidx.annotation.ColorRes
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.core.math.MathUtils
import kotlin.math.pow
import kotlin.math.roundToInt

/** Dynamic colors in Material. */
@RequiresApi(Build.VERSION_CODES.S)
internal fun dynamicTonalPalette(context: Context): TonalPalette = TonalPalette(
    // The neutral tonal range from the generated dynamic color palette.
    neutral100 = ColorResourceHelper.getColor(context, android.R.color.system_neutral1_0),
    neutral99 = ColorResourceHelper.getColor(context, android.R.color.system_neutral1_10),
    neutral98 = ColorResourceHelper.getColor(context, android.R.color.system_neutral1_600)
        .setLuminance(98f),
    neutral96 = ColorResourceHelper.getColor(context, android.R.color.system_neutral1_600)
        .setLuminance(96f),
    neutral95 = ColorResourceHelper.getColor(context, android.R.color.system_neutral1_50),
    neutral94 = ColorResourceHelper.getColor(context, android.R.color.system_neutral1_600)
        .setLuminance(94f),
    neutral92 = ColorResourceHelper.getColor(context, android.R.color.system_neutral1_600)
        .setLuminance(92f),
    neutral90 = ColorResourceHelper.getColor(context, android.R.color.system_neutral1_100),
    neutral87 = ColorResourceHelper.getColor(context, android.R.color.system_neutral1_600)
        .setLuminance(87f),
    neutral80 = ColorResourceHelper.getColor(context, android.R.color.system_neutral1_200),
    neutral70 = ColorResourceHelper.getColor(context, android.R.color.system_neutral1_300),
    neutral60 = ColorResourceHelper.getColor(context, android.R.color.system_neutral1_400),
    neutral50 = ColorResourceHelper.getColor(context, android.R.color.system_neutral1_500),
    neutral40 = ColorResourceHelper.getColor(context, android.R.color.system_neutral1_600),
    neutral30 = ColorResourceHelper.getColor(context, android.R.color.system_neutral1_700),
    neutral24 = ColorResourceHelper.getColor(context, android.R.color.system_neutral1_600)
        .setLuminance(24f),
    neutral22 = ColorResourceHelper.getColor(context, android.R.color.system_neutral1_600)
        .setLuminance(22f),
    neutral20 = ColorResourceHelper.getColor(context, android.R.color.system_neutral1_800),
    neutral17 = ColorResourceHelper.getColor(context, android.R.color.system_neutral1_600)
        .setLuminance(17f),
    neutral12 = ColorResourceHelper.getColor(context, android.R.color.system_neutral1_600)
        .setLuminance(12f),
    neutral10 = ColorResourceHelper.getColor(context, android.R.color.system_neutral1_900),
    neutral6 = ColorResourceHelper.getColor(context, android.R.color.system_neutral1_600)
        .setLuminance(6f),
    neutral4 = ColorResourceHelper.getColor(context, android.R.color.system_neutral1_600)
        .setLuminance(4f),
    neutral0 = ColorResourceHelper.getColor(context, android.R.color.system_neutral1_1000),

    // The neutral variant tonal range, sometimes called "neutral 2",  from the
    // generated dynamic color palette.
    neutralVariant100 = ColorResourceHelper.getColor(context, android.R.color.system_neutral2_0),
    neutralVariant99 = ColorResourceHelper.getColor(context, android.R.color.system_neutral2_10),
    neutralVariant95 = ColorResourceHelper.getColor(context, android.R.color.system_neutral2_50),
    neutralVariant90 = ColorResourceHelper.getColor(context, android.R.color.system_neutral2_100),
    neutralVariant80 = ColorResourceHelper.getColor(context, android.R.color.system_neutral2_200),
    neutralVariant70 = ColorResourceHelper.getColor(context, android.R.color.system_neutral2_300),
    neutralVariant60 = ColorResourceHelper.getColor(context, android.R.color.system_neutral2_400),
    neutralVariant50 = ColorResourceHelper.getColor(context, android.R.color.system_neutral2_500),
    neutralVariant40 = ColorResourceHelper.getColor(context, android.R.color.system_neutral2_600),
    neutralVariant30 = ColorResourceHelper.getColor(context, android.R.color.system_neutral2_700),
    neutralVariant20 = ColorResourceHelper.getColor(context, android.R.color.system_neutral2_800),
    neutralVariant10 = ColorResourceHelper.getColor(context, android.R.color.system_neutral2_900),
    neutralVariant0 = ColorResourceHelper.getColor(context, android.R.color.system_neutral2_1000),

    // The primary tonal range from the generated dynamic color palette.
    primary100 = ColorResourceHelper.getColor(context, android.R.color.system_accent1_0),
    primary99 = ColorResourceHelper.getColor(context, android.R.color.system_accent1_10),
    primary95 = ColorResourceHelper.getColor(context, android.R.color.system_accent1_50),
    primary90 = ColorResourceHelper.getColor(context, android.R.color.system_accent1_100),
    primary80 = ColorResourceHelper.getColor(context, android.R.color.system_accent1_200),
    primary70 = ColorResourceHelper.getColor(context, android.R.color.system_accent1_300),
    primary60 = ColorResourceHelper.getColor(context, android.R.color.system_accent1_400),
    primary50 = ColorResourceHelper.getColor(context, android.R.color.system_accent1_500),
    primary40 = ColorResourceHelper.getColor(context, android.R.color.system_accent1_600),
    primary30 = ColorResourceHelper.getColor(context, android.R.color.system_accent1_700),
    primary20 = ColorResourceHelper.getColor(context, android.R.color.system_accent1_800),
    primary10 = ColorResourceHelper.getColor(context, android.R.color.system_accent1_900),
    primary0 = ColorResourceHelper.getColor(context, android.R.color.system_accent1_1000),

    // The secondary tonal range from the generated dynamic color palette.
    secondary100 = ColorResourceHelper.getColor(context, android.R.color.system_accent2_0),
    secondary99 = ColorResourceHelper.getColor(context, android.R.color.system_accent2_10),
    secondary95 = ColorResourceHelper.getColor(context, android.R.color.system_accent2_50),
    secondary90 = ColorResourceHelper.getColor(context, android.R.color.system_accent2_100),
    secondary80 = ColorResourceHelper.getColor(context, android.R.color.system_accent2_200),
    secondary70 = ColorResourceHelper.getColor(context, android.R.color.system_accent2_300),
    secondary60 = ColorResourceHelper.getColor(context, android.R.color.system_accent2_400),
    secondary50 = ColorResourceHelper.getColor(context, android.R.color.system_accent2_500),
    secondary40 = ColorResourceHelper.getColor(context, android.R.color.system_accent2_600),
    secondary30 = ColorResourceHelper.getColor(context, android.R.color.system_accent2_700),
    secondary20 = ColorResourceHelper.getColor(context, android.R.color.system_accent2_800),
    secondary10 = ColorResourceHelper.getColor(context, android.R.color.system_accent2_900),
    secondary0 = ColorResourceHelper.getColor(context, android.R.color.system_accent2_1000),

    // The tertiary tonal range from the generated dynamic color palette.
    tertiary100 = ColorResourceHelper.getColor(context, android.R.color.system_accent3_0),
    tertiary99 = ColorResourceHelper.getColor(context, android.R.color.system_accent3_10),
    tertiary95 = ColorResourceHelper.getColor(context, android.R.color.system_accent3_50),
    tertiary90 = ColorResourceHelper.getColor(context, android.R.color.system_accent3_100),
    tertiary80 = ColorResourceHelper.getColor(context, android.R.color.system_accent3_200),
    tertiary70 = ColorResourceHelper.getColor(context, android.R.color.system_accent3_300),
    tertiary60 = ColorResourceHelper.getColor(context, android.R.color.system_accent3_400),
    tertiary50 = ColorResourceHelper.getColor(context, android.R.color.system_accent3_500),
    tertiary40 = ColorResourceHelper.getColor(context, android.R.color.system_accent3_600),
    tertiary30 = ColorResourceHelper.getColor(context, android.R.color.system_accent3_700),
    tertiary20 = ColorResourceHelper.getColor(context, android.R.color.system_accent3_800),
    tertiary10 = ColorResourceHelper.getColor(context, android.R.color.system_accent3_900),
    tertiary0 = ColorResourceHelper.getColor(context, android.R.color.system_accent3_1000),
)

/**
 * Creates a light dynamic color scheme.
 *
 * Use this function to create a color scheme based off the system wallpaper. If the developer
 * changes the wallpaper this color scheme will change accordingly. This dynamic scheme is a
 * light theme variant.
 *
 * @param context The context required to get system resource data.
 */
@RequiresApi(Build.VERSION_CODES.S)
fun dynamicLightColorScheme(context: Context): ColorScheme {
    val tonalPalette = dynamicTonalPalette(context)
    return lightColorScheme(
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
        background = tonalPalette.neutral99,
        onBackground = tonalPalette.neutral10,
        surface = tonalPalette.neutral99,
        onSurface = tonalPalette.neutral10,
        surfaceVariant = tonalPalette.neutralVariant90,
        onSurfaceVariant = tonalPalette.neutralVariant30,
        inverseSurface = tonalPalette.neutral20,
        inverseOnSurface = tonalPalette.neutral95,
        outline = tonalPalette.neutralVariant50,
        outlineVariant = tonalPalette.neutralVariant80,
        scrim = tonalPalette.neutral0,
        surfaceBright = tonalPalette.neutral98,
        surfaceDim = tonalPalette.neutral87,
        surfaceContainer = tonalPalette.neutral94,
        surfaceContainerHigh = tonalPalette.neutral92,
        surfaceContainerHighest = tonalPalette.neutral90,
        surfaceContainerLow = tonalPalette.neutral96,
        surfaceContainerLowest = tonalPalette.neutral100,
        surfaceTint = tonalPalette.primary40,
    )
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
fun dynamicDarkColorScheme(context: Context): ColorScheme {
    val tonalPalette = dynamicTonalPalette(context)
    return darkColorScheme(
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
        background = tonalPalette.neutral10,
        onBackground = tonalPalette.neutral90,
        surface = tonalPalette.neutral10,
        onSurface = tonalPalette.neutral90,
        surfaceVariant = tonalPalette.neutralVariant30,
        onSurfaceVariant = tonalPalette.neutralVariant80,
        inverseSurface = tonalPalette.neutral90,
        inverseOnSurface = tonalPalette.neutral20,
        outline = tonalPalette.neutralVariant60,
        outlineVariant = tonalPalette.neutral30,
        scrim = tonalPalette.neutral0,
        surfaceBright = tonalPalette.neutral24,
        surfaceDim = tonalPalette.neutral6,
        surfaceContainer = tonalPalette.neutral12,
        surfaceContainerHigh = tonalPalette.neutral17,
        surfaceContainerHighest = tonalPalette.neutral22,
        surfaceContainerLow = tonalPalette.neutral10,
        surfaceContainerLowest = tonalPalette.neutral4,
        surfaceTint = tonalPalette.primary80,
    )
}

@RequiresApi(23)
private object ColorResourceHelper {
    @DoNotInline
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
internal fun Color.setLuminance(
    /*@FloatRange(from = 0.0, to = 100.0)*/
    newLuminance: Float
): Color {
    if ((newLuminance < 0.0001) or (newLuminance > 99.9999)) {
        // aRGBFromLstar() from monet ColorUtil.java
        val y = 100 * labInvf((newLuminance + 16) / 116)
        println("y: $y")
        val component = delinearized(y)
        println("component: $component")

        return Color(
            /* red = */component,
            /* green = */component,
            /* blue = */component,
        )
    }

    val sLAB = this.convert(ColorSpaces.CieLab)
    return Color(
        /* luminance = */newLuminance,
        /* a = */sLAB.component2(),
        /* b = */sLAB.component3(),
        colorSpace = ColorSpaces.CieLab
    ).convert(ColorSpaces.Srgb)
}

/** Helper method from monet ColorUtils.java */
private fun labInvf(ft: Float): Float {
    val e = 216f / 24389f
    val kappa = 24389f / 27f
    val ft3 = ft * ft * ft
    return if (ft3 > e) {
        ft3
    } else {
        (116 * ft - 16) / kappa
    }
}

/**
 * Helper method from monet ColorUtils.java
 *
 * Delinearizes an RGB component.
 *
 * @param rgbComponent 0.0 <= rgb_component <= 100.0, represents linear R/G/B channel
 * @return 0 <= output <= 255, color channel converted to regular RGB space
 */
private fun delinearized(rgbComponent: Float): Int {
    val normalized = rgbComponent / 100
    val delinearized = if (normalized <= 0.0031308) {
        normalized * 12.92
    } else {
        1.055 * normalized.toDouble().pow(1.0 / 2.4) - 0.055
    }
    return MathUtils.clamp((delinearized * 255.0).roundToInt(), 0, 255)
}
