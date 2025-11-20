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

package androidx.wear.protolayout.material3

import androidx.wear.protolayout.LayoutElementBuilders.FontSetting
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.LayoutElementBuilders.TEXT_OVERFLOW_ELLIPSIZE
import androidx.wear.protolayout.LayoutElementBuilders.TEXT_OVERFLOW_ELLIPSIZE_END
import androidx.wear.protolayout.LayoutElementBuilders.TextAlignment
import androidx.wear.protolayout.LayoutElementBuilders.TextOverflow
import androidx.wear.protolayout.expression.RequiresSchemaVersion
import androidx.wear.protolayout.layout.basicText
import androidx.wear.protolayout.material3.Typography.TypographyToken
import androidx.wear.protolayout.material3.Versions.hasTextOverflowEllipsizeSupport
import androidx.wear.protolayout.modifiers.LayoutModifier
import androidx.wear.protolayout.modifiers.contentDescription
import androidx.wear.protolayout.modifiers.semanticsHeading
import androidx.wear.protolayout.types.LayoutColor
import androidx.wear.protolayout.types.LayoutString

/**
 * ProtoLayout component that represents text object holding any information.
 *
 * There are pre-defined typography styles that can be obtained from Materials token system in
 * [Typography].
 *
 * This text, when added to the `titleSlot`, `bottomSlot` and `labelForBottomSlot` in
 * [primaryLayout], is considered as important for accessibility automatically and gets read out by
 * the screen reader even without content description provided in the [modifier]. This behavior can
 * be removed with [LayoutModifier.clearSemantics] if not desired. In all other places, this text is
 * considered as not important for accessibility by default unless content description is
 * specifically provided in the [modifier].
 *
 * @param text The text content for this component.
 * @param modifier Modifiers to set to this element.
 * @param typography The typography from [Typography] to be applied to this text. This will have
 *   predefined default value specified by each components that uses this text, to achieve the
 *   recommended look. If using some of `Typography.NUMERAL_` styles and the provided text is
 *   animating, the [settings] should include [FontSetting.tabularNum] font feature setting for the
 *   best results and to avoid text changing its width based on the number present.
 * @param color The color to be applied to this text. It is recommended to use predefined default
 *   styles created by each component or `getColorProp(token)`.
 * @param italic Whether text should be displayed as italic.
 * @param underline Whether text should be displayed as underlined.
 * @param scalable Whether text should scale with the user font size.
 * @param maxLines The maximum number of lines that text can occupy.
 * @param alignment The horizontal alignment of the multiple lines of text or one line of text when
 *   text overflows.
 * @param overflow The overflow strategy when text doesn't have enough space to be shown.
 * @param settings The collection of font settings to be applied. If more than one Setting with the
 *   same axis tag is specified, the first one will be used. Supported settings depend on the font
 *   used and renderer version. Each default typography will apply appropriate default setting axes
 *   for it ([FontSetting.weight], [FontSetting.width] and [FontSetting.roundness].
 * @param incrementsForTypographySize The collection increments for the given [typography] size to
 *   allow text to grow or shrink for that amount to make text auto size in the space it has. The
 *   largest increment that fits the most text will be used. Increments can be positive for text to
 *   grow more than the specified [typography] or negative, for text to be smaller than the
 *   specified [typography]. All other parameters of the [Typography] stay as defined by the
 *   [typography] parameter. For the best results when using auto size,
 *   [incrementsForTypographySize] should be negative, to allow text to shrink while still
 *   maintaining space between multiple lines. If using positive increments in
 *   [incrementsForTypographySize] to make the text bigger, pay attention to the line height as that
 *   one won't be increased and if increment is too big, text in multiple lines can start
 *   overlapping. Positive increments on a single line text can work correctly. Note that this text
 *   element should be used in a constraint parent, not purely the one with `wrap` dimension for the
 *   best results.
 * @sample androidx.wear.protolayout.material3.samples.helloWorldTextDefault
 * @sample androidx.wear.protolayout.material3.samples.helloWorldTextDynamicCustom
 * @sample androidx.wear.protolayout.material3.samples.helloWorldTextAutosize
 * @sample androidx.wear.protolayout.material3.samples.primaryLayoutWithTextNotImportantForAccessibility
 */
@Suppress("DEPRECATION") // Intentionally using deprecated fallback for old renderer
public fun MaterialScope.text(
    text: LayoutString,
    modifier: LayoutModifier = LayoutModifier,
    @TypographyToken typography: Int = defaultTextElementStyle.typography,
    color: LayoutColor = defaultTextElementStyle.color,
    italic: Boolean = defaultTextElementStyle.italic,
    underline: Boolean = defaultTextElementStyle.underline,
    scalable: Boolean =
        defaultTextElementStyle.scalable ?: TypographyFontSelection.getFontScalability(typography),
    maxLines: Int = defaultTextElementStyle.maxLines,
    @TextAlignment alignment: Int = defaultTextElementStyle.alignment,
    @TextOverflow overflow: Int = defaultTextElementStyle.overflow,
    @RequiresSchemaVersion(major = 1, minor = 400) settings: List<FontSetting> = emptyList(),
    incrementsForTypographySize: List<Float> = emptyList(),
): LayoutElement =
    basicText(
        text = text,
        fontStyle =
            createFontStyleBuilder(
                    typographyToken = typography,
                    deviceConfiguration = deviceConfiguration,
                    isScalable = scalable,
                    settings = settings,
                    incrementsForTypographySize = incrementsForTypographySize,
                )
                .setColor(color.prop)
                .setItalic(italic)
                .setUnderline(underline)
                .build(),
        maxLines = maxLines,
        alignment = alignment,
        overflow =
            if (overflow == TEXT_OVERFLOW_ELLIPSIZE) {
                if (deviceConfiguration.rendererSchemaVersion.hasTextOverflowEllipsizeSupport()) {
                    overflow
                } else {
                    TEXT_OVERFLOW_ELLIPSIZE_END
                }
            } else {
                overflow
            },
        modifier =
            LayoutModifier.let {
                    if (defaultTextElementStyle.isAccessibilityHeading) {
                        it.semanticsHeading(true)
                    } else {
                        it
                    }
                }
                .let {
                    // Text by default is not important for accessibility.
                    // In M3 spec, text in primaryLayout tileSlot, bottomSlot and labelForBottomSlot
                    // should be important for accessibility by default.
                    if (defaultTextElementStyle.importantForAccessibility) {
                        it.contentDescription(text.staticValue, text.dynamicValue)
                    } else {
                        it
                    }
                } then modifier,
        lineHeight = theme.getLineHeight(typography).value,
    )

/**
 * ProtoLayout component that represents text object holding any information.
 *
 * There are pre-defined typography styles that can be obtained from Materials token system in
 * [Typography].
 *
 * This text, when added to the `titleSlot`, `bottomSlot` and `labelForBottomSlot` in
 * [primaryLayout], is considered as important for accessibility automatically and gets read out by
 * the screen reader even without content description provided in the [modifier]. This behavior can
 * be removed with [LayoutModifier.clearSemantics] if not desired. In all other places, this text is
 * considered as not important for accessibility by default unless content description is
 * specifically provided in the [modifier].
 *
 * @param text The text content for this component.
 * @param modifier Modifiers to set to this element.
 * @param typography The typography from [Typography] to be applied to this text. This will have
 *   predefined default value specified by each components that uses this text, to achieve the
 *   recommended look. If using some of `Typography.NUMERAL_` styles and the provided text is
 *   animating, the [settings] should include [FontSetting.tabularNum] font feature setting for the
 *   best results and to avoid text changing its width based on the number present.
 * @param color The color to be applied to this text. It is recommended to use predefined default
 *   styles created by each component or `getColorProp(token)`.
 * @param italic Whether text should be displayed as italic.
 * @param underline Whether text should be displayed as underlined.
 * @param scalable Whether text should scale with the user font size.
 * @param maxLines The maximum number of lines that text can occupy.
 * @param alignment The horizontal alignment of the multiple lines of text or one line of text when
 *   text overflows.
 * @param overflow The overflow strategy when text doesn't have enough space to be shown.
 * @param settings The collection of font settings to be applied. If more than one Setting with the
 *   same axis tag is specified, the first one will be used. Supported settings depend on the font
 *   used and renderer version. Each default typography will apply appropriate default setting axes
 *   for it ([FontSetting.weight], [FontSetting.width] and [FontSetting.roundness].
 * @sample androidx.wear.protolayout.material3.samples.helloWorldTextDefault
 * @sample androidx.wear.protolayout.material3.samples.helloWorldTextDynamicCustom
 */
@Deprecated("Maintained for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun MaterialScope.text(
    text: LayoutString,
    modifier: LayoutModifier = LayoutModifier,
    @TypographyToken typography: Int = defaultTextElementStyle.typography,
    @RequiresSchemaVersion(major = 1, minor = 300)
    color: LayoutColor = defaultTextElementStyle.color,
    italic: Boolean = defaultTextElementStyle.italic,
    underline: Boolean = defaultTextElementStyle.underline,
    scalable: Boolean =
        defaultTextElementStyle.scalable ?: TypographyFontSelection.getFontScalability(typography),
    maxLines: Int = defaultTextElementStyle.maxLines,
    @TextAlignment alignment: Int = defaultTextElementStyle.alignment,
    @TextOverflow overflow: Int = defaultTextElementStyle.overflow,
    @RequiresSchemaVersion(major = 1, minor = 400) settings: List<FontSetting> = emptyList(),
): LayoutElement =
    text(
        text = text,
        modifier = modifier,
        typography = typography,
        color = color,
        italic = italic,
        underline = underline,
        scalable = scalable,
        maxLines = maxLines,
        alignment = alignment,
        overflow = overflow,
        settings = settings,
        incrementsForTypographySize = emptyList(),
    )
