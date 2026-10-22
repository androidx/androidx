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

@file:Suppress("RestrictedApiAndroidX")
@file:OptIn(ExperimentalRemotePlayerApi::class)

package androidx.compose.remote.player.compose.embedded.resources

import android.graphics.Typeface as AndroidTypeface
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.remote.player.compose.embedded.EmbeddedPlayerTypefaceResolver
import androidx.compose.remote.player.compose.embedded.RcTypefaceRequest
import androidx.compose.remote.player.compose.embedded.RcTypefaceResolver
import androidx.compose.remote.player.compose.embedded.draw.resolveFloat
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Typeface as ComposeTypeface

internal fun buildFontFeatureSettings(
    fontAxis: IntArray?,
    fontAxisValues: FloatArray?,
    context: RemoteContext,
): String? {
    if (fontAxis == null || fontAxisValues == null) return null
    val count = minOf(fontAxis.size, fontAxisValues.size)
    if (count == 0) return null
    var builder: StringBuilder? = null
    for (i in 0 until count) {
        val name = context.getText(fontAxis[i]) ?: continue
        if (isVariationAxisTag(name)) continue
        val resolvedValue = resolveFloat(fontAxisValues[i], fontAxisValues[i], context)
        val sb = builder ?: StringBuilder().also { builder = it }
        if (sb.isNotEmpty()) sb.append(", ")
        sb.append('\'').append(name).append("' ").append(resolvedValue.toInt())
    }
    return builder?.toString()
}

internal fun buildFontVariationSettings(
    fontAxis: IntArray?,
    fontAxisValues: FloatArray?,
    fontWeight: FontWeight,
    fontStyle: FontStyle,
    context: RemoteContext,
): FontVariation.Settings? {
    if (fontAxis == null || fontAxisValues == null) return null
    val list = ArrayList<FontVariation.Setting>()
    var hasWght = false
    var hasItal = false
    val count = minOf(fontAxis.size, fontAxisValues.size)
    for (i in 0 until count) {
        val name = context.getText(fontAxis[i])
        if (name != null && isVariationAxisTag(name)) {
            if (name == "wght") hasWght = true
            if (name == "ital") hasItal = true
            val rawVal = fontAxisValues[i]
            list.add(FontVariation.Setting(name, resolveFloat(rawVal, rawVal, context)))
        }
    }
    if (list.isEmpty()) return null
    if (!hasWght) {
        list.add(FontVariation.weight(fontWeight.weight))
    }
    if (!hasItal && fontStyle == FontStyle.Italic) {
        list.add(FontVariation.italic(1f))
    }
    return FontVariation.Settings(*list.toTypedArray())
}

internal fun buildFontVariationSettingsString(
    fontAxis: IntArray?,
    fontAxisValues: FloatArray?,
    context: RemoteContext,
): String? {
    if (fontAxis == null || fontAxisValues == null) return null
    val count = minOf(fontAxis.size, fontAxisValues.size)
    if (count == 0) return null
    var builder: StringBuilder? = null
    for (i in 0 until count) {
        val name = context.getText(fontAxis[i]) ?: continue
        if (!isVariationAxisTag(name)) continue
        val resolvedValue = resolveFloat(fontAxisValues[i], fontAxisValues[i], context)
        val sb = builder ?: StringBuilder().also { builder = it }
        if (sb.isNotEmpty()) sb.append(", ")
        sb.append('\'').append(name).append("' ").append(resolvedValue)
    }
    return builder?.toString()
}

internal fun buildTypefaceRequest(
    fontFamilyType: Int,
    fontName: String?,
    weight: Int,
    italic: Boolean,
    fontAxis: IntArray?,
    fontAxisValues: FloatArray?,
    context: RemoteContext,
): RcTypefaceRequest {
    val isNamedFont =
        fontName != null &&
            fontFamilyType !in PaintBundle.FONT_TYPE_DEFAULT..PaintBundle.FONT_TYPE_MONOSPACE
    val resolvedType =
        if (fontFamilyType in PaintBundle.FONT_TYPE_DEFAULT..PaintBundle.FONT_TYPE_MONOSPACE) {
            fontFamilyType
        } else {
            PaintBundle.FONT_TYPE_DEFAULT
        }
    val variationSettings = buildFontVariationSettingsString(fontAxis, fontAxisValues, context)
    return RcTypefaceRequest(
        fontFamily = if (isNamedFont) fontName else null,
        fontType = resolvedType,
        weight = weight,
        italic = italic,
        variationSettings = variationSettings,
    )
}

internal fun resolveTypeface(
    fontFamilyType: Int,
    fontName: String?,
    weight: Int,
    italic: Boolean,
    fontAxis: IntArray?,
    fontAxisValues: FloatArray?,
    context: RemoteContext,
    typefaceResolver: RcTypefaceResolver? = context.rcTypefaceResolver,
): AndroidTypeface {
    val request =
        buildTypefaceRequest(
            fontFamilyType = fontFamilyType,
            fontName = fontName,
            weight = weight,
            italic = italic,
            fontAxis = fontAxis,
            fontAxisValues = fontAxisValues,
            context = context,
        )
    if (typefaceResolver != null && typefaceResolver !is EmbeddedPlayerTypefaceResolver) {
        context.typefaceCache.resolve(request, typefaceResolver)?.let {
            return it
        }
    }
    return EmbeddedPlayerTypefaceResolver.resolveSync(request)
}

internal fun resolveFontFamily(
    fontFamilyType: Int,
    fontName: String?,
    fontWeight: FontWeight,
    fontStyle: FontStyle,
    fontAxis: IntArray?,
    fontAxisValues: FloatArray?,
    context: RemoteContext,
    typefaceResolver: RcTypefaceResolver? = context.rcTypefaceResolver,
): FontFamily {
    // A font embedded in the document (FontData) has an id > 3 in the object table and no text
    // entry (custom font family names are stored as strings via loadText, so fontName != null).
    if (fontName == null && fontFamilyType > 3) {
        documentFontTypeface(
                context,
                fontFamilyType,
                fontWeight.weight,
                fontStyle == FontStyle.Italic,
            )
            ?.let {
                return FontFamily(ComposeTypeface(it))
            }
    }
    // When a custom RcTypefaceResolver (such as GmsFontTypefaceResolver or a host/test resolver)
    // is configured, resolve the Typeface through the context's RcTypefaceCache. Built-in
    // EmbeddedPlayerTypefaceResolver falls through to Compose's native FontFamily resolution
    // below so Compose's FontFamily.SansSerif / DeviceFontFamilyName semantics are preserved.
    if (typefaceResolver != null && typefaceResolver !is EmbeddedPlayerTypefaceResolver) {
        val request =
            buildTypefaceRequest(
                fontFamilyType = fontFamilyType,
                fontName = fontName,
                weight = fontWeight.weight,
                italic = fontStyle == FontStyle.Italic,
                fontAxis = fontAxis,
                fontAxisValues = fontAxisValues,
                context = context,
            )
        context.typefaceCache.resolveFontFamily(request, typefaceResolver)?.let {
            return it
        }
    }
    if (fontName != null) {
        when {
            fontName.startsWith("device:") -> {
                val familyName = fontName.substring("device:".length)
                if (familyName.isNotEmpty()) {
                    return createDeviceFontFamily(
                        familyName,
                        fontWeight,
                        fontStyle,
                        fontAxis,
                        fontAxisValues,
                        context,
                    )
                }
            }
            fontName.startsWith("google:") -> {
                val actualName = fontName.substring("google:".length)
                if (actualName.isNotEmpty()) {
                    return createDeviceFontFamily(
                        actualName,
                        fontWeight,
                        fontStyle,
                        fontAxis,
                        fontAxisValues,
                        context,
                    )
                }
            }
        }
    }

    val standardName =
        fontName
            ?: when (fontFamilyType) {
                1 -> "sans-serif"
                2 -> "serif"
                3 -> "monospace"
                else -> "sans-serif"
            }

    val standardFontFamily =
        when (standardName) {
            "default" -> FontFamily.Default
            "sans-serif" -> FontFamily.SansSerif
            "serif" -> FontFamily.Serif
            "monospace" -> FontFamily.Monospace
            else ->
                if (standardName.isNotEmpty()) {
                    return createDeviceFontFamily(
                        standardName,
                        fontWeight,
                        fontStyle,
                        fontAxis,
                        fontAxisValues,
                        context,
                    )
                } else {
                    FontFamily.Default
                }
        }

    val settings =
        buildFontVariationSettings(fontAxis, fontAxisValues, fontWeight, fontStyle, context)
    if (settings != null) {
        val systemFamilyName = if (standardName == "default") "sans-serif" else standardName
        return FontFamily(
            Font(
                DeviceFontFamilyName(systemFamilyName),
                weight = fontWeight,
                style = fontStyle,
                variationSettings = settings,
            )
        )
    }

    return standardFontFamily
}

private fun createDeviceFontFamily(
    familyName: String,
    fontWeight: FontWeight,
    fontStyle: FontStyle,
    fontAxis: IntArray?,
    fontAxisValues: FloatArray?,
    context: RemoteContext,
): FontFamily {
    val settings =
        buildFontVariationSettings(fontAxis, fontAxisValues, fontWeight, fontStyle, context)
            ?: FontVariation.Settings()

    return FontFamily(
        Font(
            DeviceFontFamilyName(familyName),
            weight = fontWeight,
            style = fontStyle,
            variationSettings = settings,
        )
    )
}
