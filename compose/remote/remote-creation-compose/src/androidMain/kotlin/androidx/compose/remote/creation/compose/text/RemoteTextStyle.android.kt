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

@file:JvmName("RemoteTextStyleKt")
@file:JvmMultifileClass

package androidx.compose.remote.creation.compose.text

import androidx.compose.remote.creation.compose.layout.toRemoteFontFamily
import androidx.compose.remote.creation.compose.layout.toRemoteFontStyle
import androidx.compose.remote.creation.compose.layout.toRemoteFontWeight
import androidx.compose.remote.creation.compose.layout.toRemoteHyphens
import androidx.compose.remote.creation.compose.layout.toRemoteLineBreak
import androidx.compose.remote.creation.compose.layout.toRemoteTextAlign
import androidx.compose.remote.creation.compose.layout.toRemoteTextDecoration
import androidx.compose.remote.creation.compose.state.asRemoteTextUnit
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.util.fastFilter
import androidx.compose.remote.creation.compose.util.fastFirstOrNull
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.AndroidFont
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.ResourceFont
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit

/**
 * Creates a [RemoteTextStyle] from a [TextStyle].
 *
 * [TextStyle.fontFeatureSettings] is carried over as [RemoteTextStyle.fontFeatureSettings] and
 * never affects [RemoteTextStyle.fontVariationSettings]. The variation settings are taken from the
 * [Font] in the [FontListFontFamily] that best matches the style's [FontWeight] and [FontStyle],
 * using the same CSS font matching rules as Compose, among the fonts that declare variation
 * settings.
 *
 * @param style The Compose [TextStyle] to convert.
 * @return a [RemoteTextStyle] converted from [style]
 */
public fun RemoteTextStyle.Companion.fromTextStyle(style: TextStyle): RemoteTextStyle {
    // Maps unspecified color into null as it's not supported in remote compose.
    val color = if (style.color == Color.Unspecified) null else style.color.rc
    val background = if (style.background == Color.Unspecified) null else style.background.rc
    val fontSize =
        if (style.fontSize == TextUnit.Unspecified) null else style.fontSize.asRemoteTextUnit()
    val letterSpacing =
        if (style.letterSpacing == TextUnit.Unspecified) null
        else style.letterSpacing.asRemoteTextUnit()
    val lineHeight =
        if (style.lineHeight == TextUnit.Unspecified) null else style.lineHeight.asRemoteTextUnit()
    val fontVariationSettings =
        (style.fontFamily as? FontListFontFamily)?.matchFontVariationSettings(
            fontWeight = style.fontWeight ?: FontWeight.Normal,
            fontStyle = style.fontStyle ?: FontStyle.Normal,
        )
    return RemoteTextStyle(
        color = color,
        fontSize = fontSize,
        fontWeight = style.fontWeight?.toRemoteFontWeight(),
        fontStyle = style.fontStyle?.toRemoteFontStyle(),
        fontFamily = style.fontFamily?.toRemoteFontFamily(),
        letterSpacing = letterSpacing,
        background = background,
        textAlign =
            if (style.textAlign == TextAlign.Unspecified) RemoteTextAlign.Unspecified
            else style.textAlign.toRemoteTextAlign(),
        lineHeight = lineHeight,
        textDecoration = style.textDecoration?.toRemoteTextDecoration(),
        lineBreak =
            if (style.lineBreak == LineBreak.Unspecified) RemoteLineBreak.Unspecified
            else style.lineBreak.toRemoteLineBreak(),
        hyphens =
            if (style.hyphens == Hyphens.Unspecified) RemoteHyphens.Unspecified
            else style.hyphens.toRemoteHyphens(),
        fontFeatureSettings = style.fontFeatureSettings,
        fontVariationSettings = fontVariationSettings?.toRemoteFontVariationSettings(),
    )
}

@OptIn(ExperimentalTextApi::class)
private fun Font.extractVariationSettings(): FontVariation.Settings? =
    when (this) {
        is AndroidFont -> variationSettings
        is ResourceFont -> variationSettings
        else -> null
    }

/**
 * Returns the [FontVariation.Settings] of the [Font] that Compose would select for [fontWeight] and
 * [fontStyle], considering only fonts that declare non-empty variation settings. Returns null if no
 * font declares variation settings.
 *
 * Remote documents carry a single set of font axes rather than the whole font list, so fonts
 * without variation settings are skipped instead of resolving to no axes.
 */
internal fun FontListFontFamily.matchFontVariationSettings(
    fontWeight: FontWeight,
    fontStyle: FontStyle,
): FontVariation.Settings? {
    val candidates = fonts.fastFilter {
        it.extractVariationSettings()?.settings?.isNotEmpty() == true
    }
    if (candidates.isEmpty()) return null
    return matchFont(candidates, fontWeight, fontStyle).extractVariationSettings()
}

/**
 * Selects the best [Font] for [fontWeight] and [fontStyle] using the
 * [CSS 4 font matching](https://www.w3.org/TR/css-fonts-4/#font-style-matching) rules, mirroring
 * Compose's internal `FontMatcher`.
 */
private fun matchFont(fonts: List<Font>, fontWeight: FontWeight, fontStyle: FontStyle): Font {
    fonts
        .fastFirstOrNull { it.weight == fontWeight && it.style == fontStyle }
        ?.let {
            return it
        }
    val fontsToSearch = fonts.fastFilter { it.style == fontStyle }.ifEmpty { fonts }
    val bestWeight =
        when {
            fontWeight < FontWeight.W400 ->
                fontsToSearch.closestWeight(fontWeight, preferBelow = true)
            fontWeight > FontWeight.W500 ->
                fontsToSearch.closestWeight(fontWeight, preferBelow = false)
            else ->
                fontsToSearch.closestWeight(
                    fontWeight,
                    preferBelow = false,
                    maxSearchRange = FontWeight.W500,
                )
                    ?: fontsToSearch.closestWeight(
                        fontWeight,
                        preferBelow = false,
                        minSearchRange = FontWeight.W500,
                    )
        }
    return fontsToSearch.fastFirstOrNull { it.weight == bestWeight } ?: fontsToSearch.first()
}

private fun List<Font>.closestWeight(
    fontWeight: FontWeight,
    preferBelow: Boolean,
    minSearchRange: FontWeight? = null,
    maxSearchRange: FontWeight? = null,
): FontWeight? {
    var bestWeightAbove: FontWeight? = null
    var bestWeightBelow: FontWeight? = null
    for (index in indices) {
        val weight = get(index).weight
        if (minSearchRange != null && weight < minSearchRange) continue
        if (maxSearchRange != null && weight > maxSearchRange) continue
        if (weight < fontWeight) {
            if (bestWeightBelow == null || weight > bestWeightBelow) bestWeightBelow = weight
        } else if (weight > fontWeight) {
            if (bestWeightAbove == null || weight < bestWeightAbove) bestWeightAbove = weight
        } else {
            return weight
        }
    }
    return if (preferBelow) bestWeightBelow ?: bestWeightAbove
    else bestWeightAbove ?: bestWeightBelow
}
