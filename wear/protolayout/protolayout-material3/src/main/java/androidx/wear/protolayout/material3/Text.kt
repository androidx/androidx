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
import androidx.wear.protolayout.types.LayoutColor
import androidx.wear.protolayout.types.LayoutString

/**
 * ProtoLayout component that represents text object holding any information.
 *
 * There are pre-defined typography styles that can be obtained from Materials token system in
 * [Typography].
 *
 * @param text The text content for this component.
 * @param modifiers Modifiers to set to this element.
 * @param typography The typography from [Typography] to be applied to this text. This will have
 *   predefined default value specified by each components that uses this text, to achieve the
 *   recommended look.
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
@Suppress("DEPRECATION") // Intentionally using deprecated fallback for old renderer
public fun MaterialScope.text(
    text: LayoutString,
    modifiers: LayoutModifier = LayoutModifier,
    @TypographyToken typography: Int = defaultTextElementStyle.typography,
    color: LayoutColor = defaultTextElementStyle.color,
    italic: Boolean = defaultTextElementStyle.italic,
    underline: Boolean = defaultTextElementStyle.underline,
    scalable: Boolean =
        defaultTextElementStyle.scalable ?: TypographyFontSelection.getFontScalability(typography),
    maxLines: Int = defaultTextElementStyle.maxLines,
    @TextAlignment alignment: Int = defaultTextElementStyle.alignment,
    @TextOverflow overflow: Int = defaultTextElementStyle.overflow,
    @RequiresSchemaVersion(major = 1, minor = 400) settings: List<FontSetting> = emptyList()
): LayoutElement =
    basicText(
        text = text,
        fontStyle =
            createFontStyleBuilder(
                    typographyToken = typography,
                    deviceConfiguration = deviceConfiguration,
                    isScalable = scalable,
                    settings = settings
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
        modifier = modifiers
    )
