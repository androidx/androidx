/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.protolayout.layout

import android.annotation.SuppressLint
import androidx.annotation.Dimension
import androidx.annotation.Dimension.Companion.SP
import androidx.annotation.OptIn
import androidx.wear.protolayout.LayoutElementBuilders.FONT_WEIGHT_UNDEFINED
import androidx.wear.protolayout.LayoutElementBuilders.FontSetting
import androidx.wear.protolayout.LayoutElementBuilders.FontStyle
import androidx.wear.protolayout.LayoutElementBuilders.FontWeight
import androidx.wear.protolayout.LayoutElementBuilders.TEXT_ALIGN_UNDEFINED
import androidx.wear.protolayout.LayoutElementBuilders.TEXT_OVERFLOW_UNDEFINED
import androidx.wear.protolayout.LayoutElementBuilders.Text
import androidx.wear.protolayout.LayoutElementBuilders.TextAlignment
import androidx.wear.protolayout.LayoutElementBuilders.TextOverflow
import androidx.wear.protolayout.expression.ProtoLayoutExperimental
import androidx.wear.protolayout.expression.RequiresSchemaVersion
import androidx.wear.protolayout.modifiers.LayoutModifier
import androidx.wear.protolayout.modifiers.toProtoLayoutModifiers
import androidx.wear.protolayout.types.LayoutColor
import androidx.wear.protolayout.types.LayoutString
import androidx.wear.protolayout.types.em
import androidx.wear.protolayout.types.sp
import java.util.stream.Collectors.toList
import java.util.stream.Stream
import kotlin.collections.emptyList

/**
 * Builds a text string.
 *
 * @param text The text to render.
 * @param fontStyle The style of font to use (size, bold etc). If not specified, defaults to the
 *   platform's default body font.
 * @param modifier Modifiers to set to this element..
 * @param maxLines The maximum number of lines that can be represented by the [Text] element. If not
 *   defined, the [Text] element will be treated as a single-line element.
 * @param alignment Alignment of the text within its bounds. Note that a [Text] element will size
 *   itself to wrap its contents, so this option is meaningless for single-line text (for that, use
 *   alignment of the outer container), unless this text overflows. For multi-line text, however,
 *   this will set the alignment of lines relative to the [Text] element bounds. If not defined,
 *   defaults to TEXT_ALIGN_CENTER.
 * @param overflow How to handle text which overflows the bound of the [Text] element. A [Text]
 *   element will grow as large as possible inside its parent container (while still respecting
 *   max_lines); if it cannot grow large enough to render all of its text, the text which cannot fit
 *   inside its container will be truncated. If not defined, defaults to TEXT_OVERFLOW_TRUNCATE.
 * @param lineHeight The explicit height between lines of text. This is equivalent to the vertical
 *   distance between subsequent baselines. If not specified, defaults the font's recommended
 *   interline spacing.
 */
@SuppressLint("ProtoLayoutMinSchema")
@Suppress("MissingJvmstatic") // Kotlin-friendly version of already available Java Apis
fun basicText(
    text: LayoutString,
    fontStyle: FontStyle? = null,
    modifier: LayoutModifier? = null,
    maxLines: Int = 0,
    @TextAlignment alignment: Int = TEXT_ALIGN_UNDEFINED,
    @TextOverflow overflow: Int = TEXT_OVERFLOW_UNDEFINED,
    @Dimension(SP) lineHeight: Float = Float.NaN,
) =
    Text.Builder()
        .setText(text.prop)
        .apply {
            text.layoutConstraint?.let { setLayoutConstraintsForDynamicText(it) }
            fontStyle?.let { setFontStyle(it) }
            modifier?.let { setModifiers(it.toProtoLayoutModifiers()) }
            if (maxLines != 0) {
                setMaxLines(maxLines)
            }
            if (alignment != TEXT_ALIGN_UNDEFINED) {
                setMultilineAlignment(alignment)
            }
            if (overflow != TEXT_OVERFLOW_UNDEFINED) {
                setOverflow(overflow)
            }
            if (!lineHeight.isNaN()) {
                setLineHeight(lineHeight.sp)
            }
        }
        .build()

/**
 * Builds the styling of a font (e.g. font size, and metrics).
 *
 * @param size The size of the font, in scaled pixels (sp). If not specified, defaults to the size
 *   of the system's "body" font.
 * @param italic Whether the text should be rendered in a italic typeface.
 * @param underline Whether the text should be rendered with an underline.
 * @param color The text color. If not defined, defaults to white.
 * @param weight The weight of the font. If the provided value is not supported on a platform, the
 *   nearest supported value will be used. If not defined, or when set to an invalid value, defaults
 *   to "normal".
 * @param letterSpacingEm The text letter-spacing. Positive numbers increase the space between
 *   letters while negative numbers tighten the space. If not specified, defaults to 0.
 * @param additionalSizesSp when this [FontStyle] is applied to a [Text] element with static text,
 *   the text size will be automatically picked from the provided sizes to try to perfectly fit
 *   within its parent bounds. In other words, the largest size from the specified preset sizes that
 *   can fit the most text within the parent bounds will be used.
 * @param settings The collection of font settings to be applied. If more than one Setting with the
 *   same axis tag is specified, the first one will be used. Supported settings depend on the font
 *   used and renderer version.
 * @param preferredFontFamilies is the ordered list of font families to pick from for this
 *   [FontStyle]. If the given font family is not available on a device, the fallback values will be
 *   attempted to use, in order in which they are given. Note that support for font family
 *   customization is dependent on the target platform.
 */
@OptIn(ProtoLayoutExperimental::class)
@SuppressLint("ProtoLayoutMinSchema")
@Suppress("MissingJvmstatic") // Kotlin-friendly version of already available Java Apis
fun fontStyle(
    @Dimension(SP) size: Float = 0f,
    italic: Boolean = false,
    underline: Boolean = false,
    color: LayoutColor? = null,
    @FontWeight weight: Int = FONT_WEIGHT_UNDEFINED,
    letterSpacingEm: Float = Float.NaN,
    @RequiresSchemaVersion(major = 1, minor = 300) additionalSizesSp: List<Float> = emptyList(),
    @RequiresSchemaVersion(major = 1, minor = 400) settings: List<FontSetting> = emptyList(),
    @RequiresSchemaVersion(major = 1, minor = 400) preferredFontFamilies: List<String> = emptyList()
): FontStyle =
    FontStyle.Builder()
        .apply {
            if (size != 0f) {
                setSize(size.sp)
            }
            setItalic(italic)
            setUnderline(underline)
            color?.let { setColor(it.prop) }
            if (weight != FONT_WEIGHT_UNDEFINED) {
                setWeight(weight)
            }
            if (settings.isNotEmpty()) {
                setSettings(*settings.toTypedArray())
            }
            if (!letterSpacingEm.isNaN()) {
                setLetterSpacing(letterSpacingEm.em)
            }
            if (preferredFontFamilies.isNotEmpty()) {
                setPreferredFontFamilies(
                    preferredFontFamilies.first(),
                    *preferredFontFamilies.subList(1, preferredFontFamilies.size).toTypedArray()
                )
            }
            if (additionalSizesSp.isNotEmpty()) {
                setSizes(
                    *Stream.concat(
                            if (size != 0f) Stream.of(size.toInt()) else Stream.empty(),
                            additionalSizesSp.stream().map { it.toInt() }
                        )
                        .collect(toList())
                        .toIntArray()
                )
            }
        }
        .build()
