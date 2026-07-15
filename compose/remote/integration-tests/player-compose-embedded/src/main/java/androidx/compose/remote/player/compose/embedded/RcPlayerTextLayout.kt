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

package androidx.compose.remote.player.compose.embedded

import androidx.compose.material3.Text
import androidx.compose.remote.core.operations.layout.managers.CoreText
import androidx.compose.remote.core.operations.layout.managers.TextLayout
import androidx.compose.remote.player.compose.embedded.state.rememberRemoteStringAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em

@Composable
internal fun RcPlayerText(layout: CoreText, modifier: Modifier) {
    val textId = layout.textId ?: return
    val text by rememberRemoteStringAsState(textId)
    val paintState = ComposeLocalPaint()
    updatePaintFromBundle(layout.mPaint, paintState, LocalRemoteContext.current)

    val data = layout.readDataReflection()

    val color = if (paintState.isColorSet) Color(paintState.color) else Color(data.colorValue)
    val fontSize = if (paintState.isTextSizeSet) paintState.textSize else data.fontSizeValue
    val fontSizeSp = with(LocalDensity.current) { fontSize.toSp() }

    val remoteContext = LocalRemoteContext.current
    val fontVariationSettings =
        if (data.fontAxis != null && data.fontAxisValues != null) {
            val settings =
                data.fontAxis.asList().mapIndexedNotNull { index, id ->
                    val name = remoteContext.getText(id)
                    if (name != null) {
                        FontVariation.Setting(name, data.fontAxisValues[index])
                    } else {
                        null
                    }
                }
            if (settings.isNotEmpty()) {
                FontVariation.Settings(*settings.toTypedArray())
            } else {
                null
            }
        } else {
            null
        }

    val fontWeight =
        if (paintState.isTypefaceSet) FontWeight(paintState.fontWeight)
        else FontWeight(data.fontWeightValue.toInt())
    val fontStyle =
        if (paintState.isTypefaceSet) paintState.fontStyle
        else {
            if (data.fontStyle == 1) FontStyle.Italic else FontStyle.Normal
        }

    val fontFamilyType = if (paintState.isTypefaceSet) paintState.fontFamily else data.type
    val fontFamily =
        if (fontVariationSettings != null) {
            val familyNameStr =
                when (fontFamilyType) {
                    1 -> "sans-serif"
                    2 -> "serif"
                    3 -> "monospace"
                    else -> "sans-serif"
                }
            FontFamily(
                Font(
                    DeviceFontFamilyName(familyNameStr),
                    weight = fontWeight,
                    style = fontStyle,
                    variationSettings = fontVariationSettings,
                )
            )
        } else {
            when (fontFamilyType) {
                1 -> FontFamily.SansSerif
                2 -> FontFamily.Serif
                3 -> FontFamily.Monospace
                else -> FontFamily.Default
            }
        }

    val textDecoration =
        when {
            data.underline && data.strikethrough ->
                TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
            data.underline -> TextDecoration.Underline
            data.strikethrough -> TextDecoration.LineThrough
            else -> TextDecoration.None
        }

    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSizeSp,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        fontStyle = fontStyle,
        textAlign =
            when (data.textAlignValue) {
                CoreText.TEXT_ALIGN_LEFT -> TextAlign.Left
                CoreText.TEXT_ALIGN_RIGHT -> TextAlign.Right
                CoreText.TEXT_ALIGN_CENTER -> TextAlign.Center
                CoreText.TEXT_ALIGN_JUSTIFY -> TextAlign.Justify
                CoreText.TEXT_ALIGN_START -> TextAlign.Start
                CoreText.TEXT_ALIGN_END -> TextAlign.End
                else -> TextAlign.Start
            },
        overflow =
            when (data.overflow) {
                CoreText.OVERFLOW_CLIP -> TextOverflow.Clip
                CoreText.OVERFLOW_ELLIPSIS -> TextOverflow.Ellipsis
                CoreText.OVERFLOW_VISIBLE -> TextOverflow.Visible
                else -> TextOverflow.Clip
            },
        maxLines = data.maxLines,
        letterSpacing = data.letterSpacing.em,
        lineHeight =
            if (data.lineHeightMultiplier != 1f || data.lineHeightAdd != 0f) {
                with(LocalDensity.current) {
                    (data.fontSizeValue * data.lineHeightMultiplier + data.lineHeightAdd).toSp()
                }
            } else {
                TextUnit.Unspecified
            },
        textDecoration = textDecoration,
    )
}

@Composable
internal fun RcPlayerText(layout: TextLayout, modifier: Modifier) {
    val textId = layout.textId ?: return
    val text by rememberRemoteStringAsState(textId)
    val paintState = ComposeLocalPaint()
    updatePaintFromBundle(layout.mPaint, paintState, LocalRemoteContext.current)

    val data = layout.readDataReflection()

    val color = if (paintState.isColorSet) Color(paintState.color) else Color(data.colorValue)
    val fontSize = if (paintState.isTextSizeSet) paintState.textSize else data.fontSizeValue
    val fontSizeSp = with(LocalDensity.current) { fontSize.toSp() }

    val fontFamilyType = if (paintState.isTypefaceSet) paintState.fontFamily else data.type
    val fontFamily =
        when (fontFamilyType) {
            1 -> FontFamily.SansSerif
            2 -> FontFamily.Serif
            3 -> FontFamily.Monospace
            else -> FontFamily.Default
        }
    val fontWeight =
        if (paintState.isTypefaceSet) FontWeight(paintState.fontWeight)
        else FontWeight(data.fontWeight.toInt())
    val fontStyle = if (paintState.isTypefaceSet) paintState.fontStyle else FontStyle.Normal

    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSizeSp,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        fontStyle = fontStyle,
        textAlign =
            when (data.textAlignValue) {
                TextLayout.TEXT_ALIGN_LEFT -> TextAlign.Left
                TextLayout.TEXT_ALIGN_RIGHT -> TextAlign.Right
                TextLayout.TEXT_ALIGN_CENTER -> TextAlign.Center
                TextLayout.TEXT_ALIGN_JUSTIFY -> TextAlign.Justify
                TextLayout.TEXT_ALIGN_START -> TextAlign.Start
                TextLayout.TEXT_ALIGN_END -> TextAlign.End
                else -> TextAlign.Start
            },
        overflow =
            when (data.overflow) {
                TextLayout.OVERFLOW_CLIP -> TextOverflow.Clip
                TextLayout.OVERFLOW_ELLIPSIS -> TextOverflow.Ellipsis
                TextLayout.OVERFLOW_VISIBLE -> TextOverflow.Visible
                else -> TextOverflow.Clip
            },
        maxLines = data.maxLines,
    )
}
