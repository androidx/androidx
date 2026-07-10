/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.text.googlefonts

import android.content.Context
import android.content.res.Configuration
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastJoinToString

/** Apply font variation settings to a typeface on supported API levels (26+) */
@ExperimentalTextApi
internal fun Typeface?.setFontVariationSettings(
    variationSettings: FontVariation.Settings,
    context: Context,
): Typeface? {
    return if (Build.VERSION.SDK_INT >= 26) {
        TypefaceCompatApi26.setFontVariationSettings(this, variationSettings, context)
    } else {
        this
    }
}

@RequiresApi(26)
private object TypefaceCompatApi26 {
    private var threadLocalPaint: ThreadLocal<Paint> = ThreadLocal()

    @ExperimentalTextApi
    fun setFontVariationSettings(
        typeface: Typeface?,
        variationSettings: FontVariation.Settings,
        context: Context,
    ): Typeface? {
        if (typeface == null) return null
        if (variationSettings.settings.isEmpty()) {
            return typeface
        }
        var localPaint = threadLocalPaint.get()
        if (localPaint == null) {
            localPaint = Paint()
            this.threadLocalPaint.set(localPaint)
        }
        localPaint.fontVariationSettings = null /* don't let paint cache b/353609778 */
        localPaint.typeface = typeface
        localPaint.fontVariationSettings = variationSettings.toAndroidString(context)
        return localPaint.typeface
    }
}

/**
 * Converts [FontVariation.Settings] to a CSS-like string suitable for use with Android APIs like
 * `Paint.setFontVariationSettings`.
 */
internal fun FontVariation.Settings.toAndroidString(context: Context): String =
    toAndroidString(Density(context), getFontWeightAdjustment(context))

@VisibleForTesting
internal fun FontVariation.Settings.toAndroidString(
    density: Density,
    weightAdjustment: Int,
): String {
    if (weightAdjustment == 0) {
        return settings.fastJoinToString { setting ->
            "'${setting.axisName}' ${setting.toVariationValue(density)}"
        }
    } else {
        var out = ""
        var wghtApplied = false
        settings.fastForEachIndexed { i, setting ->
            val styleValue =
                if (setting.axisName == "wght") {
                    wghtApplied = true
                    (setting.toVariationValue(density) + weightAdjustment).coerceInWeight()
                } else {
                    setting.toVariationValue(density)
                }
            if (i != 0) {
                out += ","
            }
            out += "'${setting.axisName}' $styleValue"
        }
        if (!wghtApplied) {
            val styleValue = (400f + weightAdjustment).coerceInWeight()
            if (settings.isNotEmpty()) {
                out += ","
            }
            out += "'wght' $styleValue"
        }
        return out
    }
}

/**
 * Returns the font weight adjustment value from the current configuration.
 *
 * Returns 0 if the context is null, the Android version is less than 31, or if the font weight
 * adjustment is undefined.
 */
internal fun getFontWeightAdjustment(context: Context?) =
    if (context != null && Build.VERSION.SDK_INT >= 31) {
        val rawWeightAdjustment = context.resources.configuration.fontWeightAdjustment
        if (rawWeightAdjustment == Configuration.FONT_WEIGHT_ADJUSTMENT_UNDEFINED) {
            0
        } else {
            context.resources.configuration.fontWeightAdjustment
        }
    } else {
        0
    }

private fun Float.coerceInWeight() =
    coerceIn(
        android.graphics.fonts.FontStyle.FONT_WEIGHT_MIN.toFloat(),
        android.graphics.fonts.FontStyle.FONT_WEIGHT_MAX.toFloat(),
    )
