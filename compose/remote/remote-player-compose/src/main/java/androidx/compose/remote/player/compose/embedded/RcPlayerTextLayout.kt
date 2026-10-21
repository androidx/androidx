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

import android.graphics.Typeface as AndroidTypeface
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.layout.managers.CoreText
import androidx.compose.remote.core.operations.layout.managers.TextLayout
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.remote.player.compose.embedded.state.rememberRemoteColorAsState
import androidx.compose.remote.player.compose.embedded.state.rememberRemoteFloatAsState
import androidx.compose.remote.player.compose.embedded.state.rememberRemoteStringAsState
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
import androidx.compose.ui.text.font.Typeface as ComposeTypeface
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
    updatePaintFromBundle(
        layout.mPaint,
        paintState,
        LocalGraphContext.current ?: LocalRemoteContext.current,
    )

    val color =
        resolveTextColor(
            paintState = paintState,
            isDynamicColorEnabled = layout.isDynamicColorEnabled,
            colorId = layout.colorId,
            colorValue = layout.colorValue,
        )
    val density = LocalDensity.current
    val fontSize = resolveTextFontSizePx(paintState, layout.fontSize)
    val fontSizeSp = with(density) { fontSize.toSp() }

    val remoteContext = LocalRemoteContext.current
    val readContext = LocalGraphContext.current ?: remoteContext

    val fontWeight =
        if (paintState.isTypefaceSet) FontWeight(paintState.fontWeight)
        else FontWeight(layout.fontWeightValue.toInt())
    val fontStyle =
        if (paintState.isTypefaceSet) paintState.fontStyle
        else {
            if (layout.fontStyle == 1) FontStyle.Italic else FontStyle.Normal
        }

    val fontFamilyType = if (paintState.isTypefaceSet) paintState.fontFamily else layout.type
    val customFontNameState = rememberCustomFontName(fontFamilyType, readContext)
    val typefaceResolver = LocalRcTypefaceResolver.current
    val effectiveFontAxis = paintState.fontAxis ?: layout.fontAxis
    val effectiveFontAxisValues = paintState.fontAxisValues ?: layout.fontAxisValues
    val fontFamily =
        resolveFontFamily(
            fontFamilyType,
            customFontNameState.value,
            fontWeight,
            fontStyle,
            effectiveFontAxis,
            effectiveFontAxisValues,
            readContext,
            typefaceResolver,
        )
    val fontFeatureSettings =
        buildFontFeatureSettings(effectiveFontAxis, effectiveFontAxisValues, readContext)

    val textDecoration =
        when {
            layout.underline && layout.strikethrough ->
                TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
            layout.underline -> TextDecoration.Underline
            layout.strikethrough -> TextDecoration.LineThrough
            else -> TextDecoration.None
        }

    val autoSize =
        if (layout.autosize) {
            val min =
                if (layout.minFontSize <= 0f) 4.sp else with(density) { layout.minFontSize.toSp() }
            val max =
                if (layout.maxFontSize <= 0f) 400.sp
                else with(density) { layout.maxFontSize.toSp() }
            TextAutoSize.StepBased(minFontSize = min, maxFontSize = max, stepSize = 0.5.sp)
        } else {
            null
        }

    val textAlign = resolveTextAlign(layout.textAlignValue, layout.justificationMode)

    val lineBreak =
        when (layout.lineBreakStrategy) {
            CoreText.BREAK_STRATEGY_HIGH_QUALITY -> LineBreak.Paragraph
            CoreText.BREAK_STRATEGY_BALANCED -> LineBreak.Heading
            else -> LineBreak.Unspecified
        }

    val hyphens =
        if (layout.hyphenationFrequency != 0) {
            Hyphens.Auto
        } else {
            Hyphens.Unspecified
        }

    val overflow =
        when (layout.overflow) {
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
                fontFeatureSettings = fontFeatureSettings,
                textAlign = textAlign,
                lineBreak = lineBreak,
                hyphens = hyphens,
                letterSpacing = layout.letterSpacing.em,
                lineHeight =
                    resolveCoreTextLineHeight(
                        fontSize = fontSize,
                        lineHeightMultiplier = layout.lineHeightMultiplier,
                        lineHeightAdd = layout.lineHeightAdd,
                        autosize = layout.autosize,
                        density = density,
                    ),
                textDecoration = textDecoration,
            ),
        overflow = overflow,
        maxLines = resolveEffectiveMaxLines(layout.maxLines, layout.overflow),
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
    updatePaintFromBundle(
        layout.mPaint,
        paintState,
        LocalGraphContext.current ?: LocalRemoteContext.current,
    )

    val color =
        resolveTextColor(
            paintState = paintState,
            isDynamicColorEnabled = layout.isDynamicColorEnabled,
            colorId = layout.color,
            colorValue = layout.colorValue,
        )
    val density = LocalDensity.current
    val fontSize = resolveTextFontSizePx(paintState, layout.fontSize)
    val fontSizeSp = with(density) { fontSize.toSp() }

    val fontWeight =
        if (paintState.isTypefaceSet) FontWeight(paintState.fontWeight)
        else FontWeight(layout.fontWeight.toInt())
    val fontStyle = if (paintState.isTypefaceSet) paintState.fontStyle else FontStyle.Normal

    val fontFamilyType = if (paintState.isTypefaceSet) paintState.fontFamily else layout.type
    val remoteContext = LocalRemoteContext.current
    val readContext = LocalGraphContext.current ?: remoteContext
    val customFontNameState = rememberCustomFontName(fontFamilyType, readContext)
    val typefaceResolver = LocalRcTypefaceResolver.current
    val fontFamily =
        resolveFontFamily(
            fontFamilyType,
            customFontNameState.value,
            fontWeight,
            fontStyle,
            paintState.fontAxis,
            paintState.fontAxisValues,
            readContext,
            typefaceResolver,
        )
    val fontFeatureSettings =
        buildFontFeatureSettings(paintState.fontAxis, paintState.fontAxisValues, readContext)

    val overflow =
        when (layout.overflow) {
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
                fontFeatureSettings = fontFeatureSettings,
                textAlign = resolveTextAlign(layout.textAlignValue, 0),
            ),
        overflow = overflow,
        maxLines = resolveEffectiveMaxLines(layout.maxLines, layout.overflow),
    )
}

@Composable
private fun resolveTextColor(
    paintState: ComposeLocalPaint,
    isDynamicColorEnabled: Boolean,
    colorId: Int,
    colorValue: Int,
): Color =
    if (paintState.isColorSet) {
        Color(paintState.color)
    } else if (isDynamicColorEnabled) {
        rememberRemoteColorAsState(colorId).value
    } else {
        Color(colorValue)
    }

@Composable
private fun resolveTextFontSizePx(paintState: ComposeLocalPaint, rawFontSize: Float): Float {
    val resolvedFontSize by rememberRemoteFloatAsState(rawFontSize)
    return when {
        paintState.isTextSizeSet -> paintState.textSize
        rawFontSize.isNaN() -> resolvedFontSize
        else -> rawDimensionPx(resolvedFontSize)
    }
}

/**
 * Mirrors `AndroidPaintContext.layoutComplexText`: `StaticLayout.Builder.setMaxLines` is only
 * applied when `maxLines == 1` or when `overflow` is an ellipsis mode (`OVERFLOW_ELLIPSIS`,
 * `OVERFLOW_START_ELLIPSIS`, `OVERFLOW_MIDDLE_ELLIPSIS`). For `OVERFLOW_CLIP` / `OVERFLOW_VISIBLE`
 * with `maxLines > 1`, the View player does not cap line count on the layout builder.
 */
internal fun resolveEffectiveMaxLines(maxLines: Int, overflow: Int): Int {
    if (maxLines <= 0) return Int.MAX_VALUE
    val hasEllipsis =
        overflow == CoreText.OVERFLOW_ELLIPSIS ||
            overflow == CoreText.OVERFLOW_START_ELLIPSIS ||
            overflow == CoreText.OVERFLOW_MIDDLE_ELLIPSIS
    return if (maxLines == 1 || hasEllipsis) maxLines else Int.MAX_VALUE
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
