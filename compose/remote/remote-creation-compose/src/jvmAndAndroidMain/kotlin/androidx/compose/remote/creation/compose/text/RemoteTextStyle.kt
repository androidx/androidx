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

import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteTextUnit
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Immutable

/**
 * A remote-aware text style that mirrors `androidx.compose.ui.text.TextStyle` but uses remote types
 * where applicable.
 *
 * @param color The color of the text.
 * @param fontSize The size of glyphs to use when painting the text in [RemoteTextUnit].
 * @param fontWeight The typeface thickness to use when painting the text (e.g.,
 *   [RemoteFontWeight.Bold]).
 * @param fontStyle The font style to use when painting the text (e.g., [RemoteFontStyle.Italic]).
 * @param fontFamily The font family to be used when rendering the text.
 * @param letterSpacing The amount of space to add between each letter in [RemoteTextUnit].
 * @param background The background color for the text.
 * @param textAlign The alignment of the text within the lines of the paragraph.
 * @param lineHeight Line height for the text in [RemoteTextUnit] unit, e.g. SP or EM.
 * @param textDecoration The decorations to paint on the text (e.g.,
 *   [RemoteTextDecoration.Underline]).
 * @param lineBreak The configuration of line break.
 * @param hyphens The configuration of hyphenation.
 * @param fontFeatureSettings The advanced typography settings provided by font in CSS format (e.g.
 *   "smcp" or "tnum").
 * @param fontVariationSettings The font variation settings to be applied to the text.
 */
@Immutable
public class RemoteTextStyle(
    public val color: RemoteColor? = null,
    public val fontSize: RemoteTextUnit? = null,
    public val fontWeight: RemoteFontWeight? = null,
    public val fontStyle: RemoteFontStyle? = null,
    public val fontFamily: RemoteFontFamily? = null,
    public val letterSpacing: RemoteTextUnit? = null,
    public val background: RemoteColor? = null,
    public val textAlign: RemoteTextAlign = RemoteTextAlign.Unspecified,
    public val lineHeight: RemoteTextUnit? = null,
    public val textDecoration: RemoteTextDecoration? = null,
    public val lineBreak: RemoteLineBreak = RemoteLineBreak.Unspecified,
    public val hyphens: RemoteHyphens = RemoteHyphens.Unspecified,
    public val fontFeatureSettings: String? = null,
    public val fontVariationSettings: RemoteFontVariation.Settings? = null,
) {

    /**
     * Returns a new [RemoteTextStyle] that is a combination of this style and the given [other]
     * style.
     *
     * If [other] is null, this style is returned. If [other] has any unspecified properties, the
     * values from this style are used for those properties.
     *
     * @param other The style to merge into this style.
     * @return A new [RemoteTextStyle] with properties from [other] taking precedence.
     */
    public fun merge(other: RemoteTextStyle?): RemoteTextStyle {
        if (other == null) return this
        return RemoteTextStyle(
            color = other.color ?: this.color,
            fontSize = other.fontSize ?: this.fontSize,
            fontWeight = other.fontWeight ?: this.fontWeight,
            fontStyle = other.fontStyle ?: this.fontStyle,
            fontFamily = other.fontFamily ?: this.fontFamily,
            letterSpacing = other.letterSpacing ?: this.letterSpacing,
            background = other.background ?: this.background,
            textAlign =
                if (other.textAlign != RemoteTextAlign.Unspecified) other.textAlign
                else this.textAlign,
            lineHeight = other.lineHeight ?: this.lineHeight,
            textDecoration = other.textDecoration ?: this.textDecoration,
            lineBreak =
                if (other.lineBreak != RemoteLineBreak.Unspecified) other.lineBreak
                else this.lineBreak,
            hyphens =
                if (other.hyphens != RemoteHyphens.Unspecified) other.hyphens else this.hyphens,
            fontFeatureSettings = other.fontFeatureSettings ?: this.fontFeatureSettings,
            fontVariationSettings = other.fontVariationSettings ?: this.fontVariationSettings,
        )
    }

    /**
     * Returns a new [RemoteTextStyle] that is a combination of this style and the given attributes,
     * keeping this style's values for any attributes left unspecified (`null` or `Unspecified`).
     *
     * @param color The color of the text.
     * @param fontSize The size of glyphs to use when painting the text in [RemoteTextUnit].
     * @param fontWeight The typeface thickness to use when painting the text (e.g.,
     *   [RemoteFontWeight.Bold]).
     * @param fontStyle The font style to use when painting the text (e.g.,
     *   [RemoteFontStyle.Italic]).
     * @param fontFamily The font family to be used when rendering the text.
     * @param letterSpacing The amount of space to add between each letter in [RemoteTextUnit].
     * @param background The background color for the text.
     * @param textAlign The alignment of the text within the lines of the paragraph.
     * @param lineHeight Line height for the text in [RemoteTextUnit] unit, e.g. SP or EM.
     * @param textDecoration The decorations to paint on the text (e.g.,
     *   [RemoteTextDecoration.Underline]).
     * @param lineBreak The configuration of line break.
     * @param hyphens The configuration of hyphenation.
     * @param fontFeatureSettings The advanced typography settings provided by font in CSS format
     *   (e.g. "smcp" or "tnum").
     * @param fontVariationSettings The font variation settings to be applied to the text.
     * @return a new [RemoteTextStyle] with the specified attributes merged into this style
     */
    public fun merge(
        color: RemoteColor? = null,
        fontSize: RemoteTextUnit? = null,
        fontWeight: RemoteFontWeight? = null,
        fontStyle: RemoteFontStyle? = null,
        fontFamily: RemoteFontFamily? = null,
        letterSpacing: RemoteTextUnit? = null,
        background: RemoteColor? = null,
        textAlign: RemoteTextAlign = RemoteTextAlign.Unspecified,
        lineHeight: RemoteTextUnit? = null,
        textDecoration: RemoteTextDecoration? = null,
        lineBreak: RemoteLineBreak = RemoteLineBreak.Unspecified,
        hyphens: RemoteHyphens = RemoteHyphens.Unspecified,
        fontFeatureSettings: String? = null,
        fontVariationSettings: RemoteFontVariation.Settings? = null,
    ): RemoteTextStyle {
        return RemoteTextStyle(
            color = color ?: this.color,
            fontSize = fontSize ?: this.fontSize,
            fontWeight = fontWeight ?: this.fontWeight,
            fontStyle = fontStyle ?: this.fontStyle,
            fontFamily = fontFamily ?: this.fontFamily,
            letterSpacing = letterSpacing ?: this.letterSpacing,
            background = background ?: this.background,
            textAlign = if (textAlign != RemoteTextAlign.Unspecified) textAlign else this.textAlign,
            lineHeight = lineHeight ?: this.lineHeight,
            textDecoration = textDecoration ?: this.textDecoration,
            lineBreak = if (lineBreak != RemoteLineBreak.Unspecified) lineBreak else this.lineBreak,
            hyphens = if (hyphens != RemoteHyphens.Unspecified) hyphens else this.hyphens,
            fontFeatureSettings = fontFeatureSettings ?: this.fontFeatureSettings,
            fontVariationSettings = fontVariationSettings ?: this.fontVariationSettings,
        )
    }

    /**
     * Creates a copy of this [RemoteTextStyle] with the ability to override individual attributes.
     *
     * @param color The color of the text.
     * @param fontSize The size of glyphs to use when painting the text in [RemoteTextUnit].
     * @param fontWeight The typeface thickness to use when painting the text (e.g.,
     *   [RemoteFontWeight.Bold]).
     * @param fontStyle The font style to use when painting the text (e.g.,
     *   [RemoteFontStyle.Italic]).
     * @param fontFamily The font family to be used when rendering the text.
     * @param letterSpacing The amount of space to add between each letter in [RemoteTextUnit].
     * @param background The background color for the text.
     * @param textAlign The alignment of the text within the lines of the paragraph.
     * @param lineHeight Line height for the text in [RemoteTextUnit] unit, e.g. SP or EM.
     * @param textDecoration The decorations to paint on the text (e.g.,
     *   [RemoteTextDecoration.Underline]).
     * @param lineBreak The configuration of line break.
     * @param hyphens The configuration of hyphenation.
     * @param fontFeatureSettings The advanced typography settings provided by font in CSS format
     *   (e.g. "smcp" or "tnum").
     * @param fontVariationSettings The font variation settings to be applied to the text.
     * @return a new [RemoteTextStyle] with the specified attributes overridden
     */
    public fun copy(
        color: RemoteColor? = this.color,
        fontSize: RemoteTextUnit? = this.fontSize,
        fontWeight: RemoteFontWeight? = this.fontWeight,
        fontStyle: RemoteFontStyle? = this.fontStyle,
        fontFamily: RemoteFontFamily? = this.fontFamily,
        letterSpacing: RemoteTextUnit? = this.letterSpacing,
        background: RemoteColor? = this.background,
        textAlign: RemoteTextAlign = this.textAlign,
        lineHeight: RemoteTextUnit? = this.lineHeight,
        textDecoration: RemoteTextDecoration? = this.textDecoration,
        lineBreak: RemoteLineBreak = this.lineBreak,
        hyphens: RemoteHyphens = this.hyphens,
        fontFeatureSettings: String? = this.fontFeatureSettings,
        fontVariationSettings: RemoteFontVariation.Settings? = this.fontVariationSettings,
    ): RemoteTextStyle {
        return RemoteTextStyle(
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            fontFamily = fontFamily,
            letterSpacing = letterSpacing,
            background = background,
            textAlign = textAlign,
            lineHeight = lineHeight,
            textDecoration = textDecoration,
            lineBreak = lineBreak,
            hyphens = hyphens,
            fontFeatureSettings = fontFeatureSettings,
            fontVariationSettings = fontVariationSettings,
        )
    }

    public companion object {
        /** Constant for default [RemoteTextStyle] with no overridden properties. */
        public val Default: RemoteTextStyle = RemoteTextStyle()
    }
}

/**
 * Parses a CSS-style fontFeatureSettings string (e.g., "tnum", "tnum, zero", "'tnum' 1, 'smcp' 1")
 * into a list of [RemoteFontVariation.Setting].
 */
internal fun parseFontFeatureSettings(
    fontFeatureSettings: String?
): List<RemoteFontVariation.Setting> {
    if (fontFeatureSettings.isNullOrBlank()) return emptyList()
    val result = mutableListOf<RemoteFontVariation.Setting>()
    val parts = fontFeatureSettings.split(',')
    for (i in parts.indices) {
        val part = parts[i]
        val trimmed = part.trim()
        if (trimmed.isEmpty()) continue
        val tokens = trimmed.split(Regex("\\s+"))
        val rawTag = tokens[0].replace("'", "").replace("\"", "").trim()
        if (rawTag.isEmpty()) continue
        val value =
            if (tokens.size > 1) {
                tokens[1].toFloatOrNull() ?: 1f
            } else {
                1f
            }
        result.add(RemoteFontVariation.Setting(rawTag, value.rf))
    }
    return result
}

/**
 * Combines [fontFeatureSettings] string and [fontVariationSettings] into a single
 * [RemoteFontVariation.Settings]. Used when passing font settings to remote-creation APIs.
 */
internal fun combineFontSettings(
    fontFeatureSettings: String?,
    fontVariationSettings: RemoteFontVariation.Settings?,
): RemoteFontVariation.Settings? {
    val featureList = parseFontFeatureSettings(fontFeatureSettings)
    if (featureList.isEmpty()) return fontVariationSettings
    if (fontVariationSettings == null) {
        return RemoteFontVariation.Settings(*featureList.toTypedArray())
    }
    val combinedMap = mutableMapOf<String, RemoteFontVariation.Setting>()
    val settings = fontVariationSettings.settings
    for (i in settings.indices) {
        val setting = settings[i]
        combinedMap[setting.axisName] = setting
    }
    for (i in featureList.indices) {
        val setting = featureList[i]
        combinedMap[setting.axisName] = setting
    }
    return RemoteFontVariation.Settings(*combinedMap.values.toTypedArray())
}
