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

package androidx.compose.remote.player.compose.embedded

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.layout.managers.CoreText
import androidx.compose.remote.core.operations.layout.managers.TextLayout
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.remote.player.compose.embedded.state.rememberRemoteColorAsState
import androidx.compose.remote.player.compose.embedded.state.rememberRemoteStringAsState
import androidx.compose.remote.player.core.platform.TypefaceResolver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font as GoogleFontFactory
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

@Composable
internal fun RcPlayerText(layout: CoreText, modifier: Modifier) {
    val textId = layout.textId ?: return
    val text by rememberRemoteStringAsState(textId)
    val paintState = remember { ComposeLocalPaint() }.apply { reset() }
    updatePaintFromBundle(layout.mPaint, paintState, LocalRemoteContext.current)

    val data = layout.readDataReflection()

    val color =
        if (paintState.isColorSet) {
            Color(paintState.color)
        } else if (data.isDynamicColorEnabled) {
            rememberRemoteColorAsState(data.colorId).value
        } else {
            Color(data.colorValue)
        }
    val fontSize = if (paintState.isTextSizeSet) paintState.textSize else data.fontSizeValue
    val density = LocalDensity.current
    val fontSizeSp = with(density) { fontSize.toSp() }

    val remoteContext = LocalRemoteContext.current

    val fontWeight =
        if (paintState.isTypefaceSet) FontWeight(paintState.fontWeight)
        else FontWeight(data.fontWeightValue.toInt())
    val fontStyle =
        if (paintState.isTypefaceSet) paintState.fontStyle
        else {
            if (data.fontStyle == 1) FontStyle.Italic else FontStyle.Normal
        }

    val fontFamilyType = if (paintState.isTypefaceSet) paintState.fontFamily else data.type
    val customFontNameState = rememberCustomFontName(fontFamilyType, remoteContext)
    val typefaceResolver = LocalTypefaceResolver.current
    val fontCertsResId = (typefaceResolver as? HasFontCerts)?.fontCertsResId ?: 0
    val fontFamily =
        resolveFontFamily(
            fontFamilyType,
            customFontNameState.value,
            fontWeight,
            fontStyle,
            data.fontAxis,
            data.fontAxisValues,
            LocalRemoteContext.current,
            fontCertsResId,
            typefaceResolver,
        )

    val textDecoration =
        when {
            data.underline && data.strikethrough ->
                TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
            data.underline -> TextDecoration.Underline
            data.strikethrough -> TextDecoration.LineThrough
            else -> TextDecoration.None
        }

    val autoSize =
        if (data.autosize) {
            val min =
                if (data.minFontSize <= 0f) 4.sp else with(density) { data.minFontSize.toSp() }
            val max =
                if (data.maxFontSize <= 0f) 400.sp else with(density) { data.maxFontSize.toSp() }
            TextAutoSize.StepBased(minFontSize = min, maxFontSize = max, stepSize = 0.5.sp)
        } else {
            null
        }

    val textAlign = resolveTextAlign(data.textAlignValue, data.justificationMode)

    val lineBreak =
        when (data.lineBreakStrategy) {
            CoreText.BREAK_STRATEGY_HIGH_QUALITY -> LineBreak.Paragraph
            CoreText.BREAK_STRATEGY_BALANCED -> LineBreak.Heading
            else -> LineBreak.Unspecified
        }

    val hyphens =
        if (data.hyphenationFrequency != 0) {
            Hyphens.Auto
        } else {
            Hyphens.Unspecified
        }

    val overflow =
        when (data.overflow) {
            CoreText.OVERFLOW_CLIP -> TextOverflow.Clip
            CoreText.OVERFLOW_ELLIPSIS -> TextOverflow.Ellipsis
            CoreText.OVERFLOW_VISIBLE -> TextOverflow.Visible
            CoreText.OVERFLOW_START_ELLIPSIS -> TextOverflow.StartEllipsis
            CoreText.OVERFLOW_MIDDLE_ELLIPSIS -> TextOverflow.MiddleEllipsis
            else -> TextOverflow.Clip
        }

    BasicText(
        text = text,
        modifier = modifier,
        autoSize = autoSize,
        style =
            TextStyle(
                color = color,
                fontSize = fontSizeSp,
                fontWeight = fontWeight,
                fontFamily = fontFamily,
                fontStyle = fontStyle,
                textAlign = textAlign,
                lineBreak = lineBreak,
                hyphens = hyphens,
                letterSpacing = data.letterSpacing.em,
                lineHeight =
                    resolveCoreTextLineHeight(
                        fontSize = fontSize,
                        lineHeightMultiplier = data.lineHeightMultiplier,
                        lineHeightAdd = data.lineHeightAdd,
                        autosize = data.autosize,
                        density = density,
                    ),
                textDecoration = textDecoration,
            ),
        overflow = overflow,
        maxLines = data.maxLines,
    )
}

internal fun resolveCoreTextLineHeight(
    fontSize: Float,
    lineHeightMultiplier: Float,
    lineHeightAdd: Float,
    autosize: Boolean,
    density: Density,
): TextUnit =
    when {
        lineHeightMultiplier == 1f && lineHeightAdd == 0f -> TextUnit.Unspecified
        autosize && lineHeightAdd == 0f -> lineHeightMultiplier.em
        else -> with(density) { (fontSize * lineHeightMultiplier + lineHeightAdd).toSp() }
    }

@Composable
internal fun RcPlayerText(layout: TextLayout, modifier: Modifier) {
    val textId = layout.textId ?: return
    val text by rememberRemoteStringAsState(textId)
    val paintState = remember { ComposeLocalPaint() }.apply { reset() }
    updatePaintFromBundle(layout.mPaint, paintState, LocalRemoteContext.current)

    val data = layout.readDataReflection()

    val color =
        if (paintState.isColorSet) {
            Color(paintState.color)
        } else if (data.isDynamicColorEnabled) {
            rememberRemoteColorAsState(data.colorId).value
        } else {
            Color(data.colorValue)
        }
    val fontSize = if (paintState.isTextSizeSet) paintState.textSize else data.fontSizeValue
    val fontSizeSp = with(LocalDensity.current) { fontSize.toSp() }

    val fontWeight =
        if (paintState.isTypefaceSet) FontWeight(paintState.fontWeight)
        else FontWeight(data.fontWeight.toInt())
    val fontStyle = if (paintState.isTypefaceSet) paintState.fontStyle else FontStyle.Normal

    val fontFamilyType = if (paintState.isTypefaceSet) paintState.fontFamily else data.type
    val customFontNameState = rememberCustomFontName(fontFamilyType, LocalRemoteContext.current)
    val typefaceResolver = LocalTypefaceResolver.current
    val fontCertsResId = (typefaceResolver as? HasFontCerts)?.fontCertsResId ?: 0
    val fontFamily =
        resolveFontFamily(
            fontFamilyType,
            customFontNameState.value,
            fontWeight,
            fontStyle,
            null,
            null,
            LocalRemoteContext.current,
            fontCertsResId,
            typefaceResolver,
        )

    val overflow =
        when (data.overflow) {
            TextLayout.OVERFLOW_CLIP -> TextOverflow.Clip
            TextLayout.OVERFLOW_ELLIPSIS -> TextOverflow.Ellipsis
            TextLayout.OVERFLOW_VISIBLE -> TextOverflow.Visible
            TextLayout.OVERFLOW_START_ELLIPSIS -> TextOverflow.StartEllipsis
            TextLayout.OVERFLOW_MIDDLE_ELLIPSIS -> TextOverflow.MiddleEllipsis
            else -> TextOverflow.Clip
        }

    BasicText(
        text = text,
        modifier = modifier,
        style =
            TextStyle(
                color = color,
                fontSize = fontSizeSp,
                fontWeight = fontWeight,
                fontFamily = fontFamily,
                fontStyle = fontStyle,
                textAlign = resolveTextAlign(data.textAlignValue, 0),
            ),
        overflow = overflow,
        maxLines = data.maxLines,
    )
}

internal fun resolveTextAlign(textAlignValue: Int, justificationMode: Int = 0): TextAlign =
    if (justificationMode != CoreText.JUSTIFICATION_MODE_NONE) {
        TextAlign.Justify
    } else {
        when (textAlignValue) {
            CoreText.TEXT_ALIGN_LEFT -> TextAlign.Left
            CoreText.TEXT_ALIGN_RIGHT -> TextAlign.Right
            CoreText.TEXT_ALIGN_CENTER -> TextAlign.Center
            // The View player ignores TEXT_ALIGN_JUSTIFY in textAlign unless justificationMode
            // is explicitly set; map to Start to match View playback.
            CoreText.TEXT_ALIGN_JUSTIFY -> TextAlign.Start
            CoreText.TEXT_ALIGN_START -> TextAlign.Start
            CoreText.TEXT_ALIGN_END -> TextAlign.End
            else -> TextAlign.Start
        }
    }

@Composable
private fun rememberCustomFontName(fontFamilyType: Int, context: RemoteContext): State<String?> {
    return remember(fontFamilyType) {
        derivedStateOf {
            when (fontFamilyType) {
                0 -> "default"
                1 -> "sans-serif"
                2 -> "serif"
                3 -> "monospace"
                else -> context.getText(fontFamilyType)
            }
        }
    }
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
        if (name != null) {
            if (name == "wght") hasWght = true
            if (name == "ital") hasItal = true
            list.add(FontVariation.Setting(name, fontAxisValues[i]))
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

internal fun resolveFontFamily(
    fontFamilyType: Int,
    fontName: String?,
    fontWeight: FontWeight,
    fontStyle: FontStyle,
    fontAxis: IntArray?,
    fontAxisValues: FloatArray?,
    context: RemoteContext,
    fontCertsResId: Int = 0,
    typefaceResolver: TypefaceResolver? = null,
): FontFamily {
    // Built-in EmbeddedPlayerTypefaceResolver and GmsFontTypefaceResolver rely on Compose's native
    // FontFamily / GoogleFont.Provider resolution path below (using fontCertsResId and variation
    // settings). When a caller supplies a custom TypefaceResolver (e.g. host app or test harness
    // providing concrete android.graphics.Typeface instances), delegate to it directly.
    if (
        typefaceResolver != null &&
            typefaceResolver !is EmbeddedPlayerTypefaceResolver &&
            typefaceResolver !is GmsFontTypefaceResolver
    ) {
        val italic = fontStyle == FontStyle.Italic
        val fi =
            if (
                fontName != null &&
                    fontFamilyType !in
                        PaintBundle.FONT_TYPE_DEFAULT..PaintBundle.FONT_TYPE_MONOSPACE
            ) {
                typefaceResolver.resolve(fontName, fontWeight.weight, italic, null, 400, false)
            } else {
                typefaceResolver.resolve(
                    fontFamilyType,
                    fontWeight.weight,
                    italic,
                    null,
                    400,
                    false,
                )
            }
        return FontFamily(fi.getTypeface())
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
                if (fontCertsResId != 0 && actualName.isNotEmpty()) {
                    val googleFont = GoogleFont(actualName)
                    val provider =
                        GoogleFont.Provider(
                            providerAuthority = "com.google.android.gms.fonts",
                            providerPackage = "com.google.android.gms",
                            certificates = fontCertsResId,
                        )
                    return FontFamily(
                        GoogleFontFactory(
                            googleFont = googleFont,
                            fontProvider = provider,
                            weight = fontWeight,
                            style = fontStyle,
                        )
                    )
                } else if (actualName.isNotEmpty()) {
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
