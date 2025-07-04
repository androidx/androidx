/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.text.font

import android.content.Context
import android.content.res.Configuration
import android.graphics.fonts.FontVariationAxis
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastJoinToString

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

@RequiresApi(Build.VERSION_CODES.O)
internal fun FontVariation.Settings.toAndroidArray(
    density: Density,
    weightAdjustment: Int,
): Array<FontVariationAxis> {
    if (weightAdjustment == 0) {
        return Array(settings.size) { i ->
            FontVariationAxis(settings[i].axisName, settings[i].toVariationValue(density))
        }
    }
    var wghtIncluded = false
    for (i in 0 until this.settings.size) {
        if (this.settings[i].axisName == "wght") {
            wghtIncluded = true
            break
        }
    }

    val arraySize = if (wghtIncluded) settings.size else settings.size + 1
    return Array(arraySize) { i ->
        if (i == settings.size) {
            // The wght axis is not included in the settings. So append it to the last with
            // adjusting from the regular weight.
            FontVariationAxis("wght", (400f + weightAdjustment).coerceInWeight())
        } else if (settings[i].axisName == "wght") {
            FontVariationAxis(
                "wght",
                (settings[i].toVariationValue(density) + weightAdjustment).coerceInWeight(),
            )
        } else {
            FontVariationAxis(settings[i].axisName, settings[i].toVariationValue(density))
        }
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

/**
 * Converts [FontVariation.Settings] to a CSS-like string suitable for use with Android APIs like
 * `Paint.setFontVariationSettings`.
 */
internal fun FontVariation.Settings.toAndroidString(context: Context): String =
    toAndroidString(Density(context), getFontWeightAdjustment(context))

private fun Float.coerceInWeight() =
    coerceIn(
        android.graphics.fonts.FontStyle.FONT_WEIGHT_MIN.toFloat(),
        android.graphics.fonts.FontStyle.FONT_WEIGHT_MAX.toFloat(),
    )
