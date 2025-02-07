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

import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.DimensionBuilders.ContainerDimension
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.weight
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER
import androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_END
import androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_START
import androidx.wear.protolayout.LayoutElementBuilders.HorizontalAlignment
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.ModifiersBuilders.Corner
import androidx.wear.protolayout.ModifiersBuilders.Padding
import androidx.wear.protolayout.material3.AvatarButtonStyle.Companion.defaultAvatarButtonStyle
import androidx.wear.protolayout.material3.ButtonDefaults.DEFAULT_CONTENT_PADDING
import androidx.wear.protolayout.material3.ButtonDefaults.IMAGE_BUTTON_DEFAULT_SIZE_DP
import androidx.wear.protolayout.material3.ButtonDefaults.METADATA_TAG_BUTTON
import androidx.wear.protolayout.material3.ButtonDefaults.buildContentForAvatarButton
import androidx.wear.protolayout.material3.ButtonDefaults.buildContentForCompactButton
import androidx.wear.protolayout.material3.ButtonDefaults.buildContentForPillShapeButton
import androidx.wear.protolayout.material3.ButtonDefaults.filledButtonColors
import androidx.wear.protolayout.material3.ButtonStyle.Companion.defaultButtonStyle
import androidx.wear.protolayout.material3.CompactButtonStyle.COMPACT_BUTTON_DEFAULT_CONTENT_PADDING_DP
import androidx.wear.protolayout.material3.CompactButtonStyle.COMPACT_BUTTON_HEIGHT_DP
import androidx.wear.protolayout.material3.CompactButtonStyle.COMPACT_BUTTON_ICON_SIZE_LARGE_DP
import androidx.wear.protolayout.material3.CompactButtonStyle.COMPACT_BUTTON_ICON_SIZE_SMALL_DP
import androidx.wear.protolayout.material3.CompactButtonStyle.COMPACT_BUTTON_LABEL_TYPOGRAPHY
import androidx.wear.protolayout.material3.IconButtonStyle.Companion.defaultIconButtonStyle
import androidx.wear.protolayout.material3.PredefinedPrimaryLayoutMargins.maxPrimaryLayoutMargins
import androidx.wear.protolayout.material3.PredefinedPrimaryLayoutMargins.minPrimaryLayoutMargins
import androidx.wear.protolayout.material3.TextButtonStyle.Companion.defaultTextButtonStyle
import androidx.wear.protolayout.modifiers.LayoutModifier
import androidx.wear.protolayout.modifiers.background
import androidx.wear.protolayout.modifiers.clip
import androidx.wear.protolayout.modifiers.contentDescription
import androidx.wear.protolayout.modifiers.tag
import androidx.wear.protolayout.modifiers.toProtoLayoutModifiers

/**
 * Opinionated ProtoLayout Material3 icon button that offers a single slot to take content
 * representing icon, for example [icon].
 *
 * The button is usually either a circular shape with the same [width] and [height], or highly
 * recommended stadium shape occupying available space with [width] and [height] set to [expand] or
 * [weight], usually used in the [buttonGroup] to arrange them
 *
 * @param onClick Associated [Clickable] for click events. When the button is clicked it will fire
 *   the associated action.
 * @param modifier Modifiers to set to this element. It's highly recommended to set a content
 *   description using [contentDescription].
 * @param shape Defines the button's shape, in other words the corner radius for this button. If
 *   changing these to radius smaller than [Shapes.medium], it is important to adjusts the margins
 *   of [primaryLayout] used to accommodate for more space, for example by using
 *   [maxPrimaryLayoutMargins]. Or, if the [shape] is set to [Shapes.full], using
 *   [minPrimaryLayoutMargins] can be considered.
 * @param width The width of this button. It's highly recommended to set this to [expand] or
 *   [weight]
 * @param height The height of this button. It's highly recommended to set this to [expand] or
 *   [weight]
 * @param colors The colors used for this button. If not set, [ButtonDefaults.filledButtonColors]
 *   will be used as high emphasis button. Other recommended colors are
 *   [ButtonDefaults.filledTonalButtonColors] and [ButtonDefaults.filledVariantButtonColors]. If
 *   using custom colors, it is important to choose a color pair from same role to ensure
 *   accessibility with sufficient color contrast.
 * @param style The style which provides the attribute values required for constructing this icon
 *   button and its inner content. It also provides default style for the inner content, that can be
 *   overridden by each content slot.
 * @param contentPadding The inner padding used to prevent inner content from being too close to the
 *   button's edge. It's highly recommended to keep the default.
 * @param iconContent The icon slot for content displayed in this button. It is recommended to use
 *   default styling that is automatically provided by only calling [icon] with the resource ID.
 * @sample androidx.wear.protolayout.material3.samples.oneSlotButtonsSample
 */
// TODO: b/346958146 - Link Button visuals in DAC
public fun MaterialScope.iconButton(
    onClick: Clickable,
    iconContent: (MaterialScope.() -> LayoutElement),
    modifier: LayoutModifier = LayoutModifier,
    width: ContainerDimension = wrapWithMinTapTargetDimension(),
    height: ContainerDimension = wrapWithMinTapTargetDimension(),
    shape: Corner = shapes.full,
    colors: ButtonColors = filledButtonColors(),
    style: IconButtonStyle = defaultIconButtonStyle(),
    contentPadding: Padding = style.innerPadding
): LayoutElement =
    buttonContainer(
        onClick = onClick,
        modifier = modifier.background(color = colors.containerColor, corner = shape),
        width = width,
        height = height,
        contentPadding = contentPadding,
        content = {
            withStyle(
                    defaultIconStyle =
                        IconStyle(
                            width = dp(style.iconSize),
                            height = dp(style.iconSize),
                            tintColor = colors.iconColor
                        )
                )
                .iconContent()
        }
    )

/**
 * Opinionated ProtoLayout Material3 text button that offers a single slot to take content
 * representing short text, for example [text].
 *
 * The button is usually either a circular shape with the same [width] and [height], or highly
 * recommended stadium shape occupying available space with [width] and [height] set to [expand] or
 * [weight], usually used in the [buttonGroup] to arrange them.
 *
 * @param onClick Associated [Clickable] for click events. When the button is clicked it will fire
 *   the associated action.
 * @param modifier Modifiers to set to this element. It's highly recommended to set a content
 *   description using [contentDescription].
 * @param shape Defines the button's shape, in other words the corner radius for this button. If
 *   changing these to radius smaller than [Shapes.medium], it is important to adjusts the margins
 *   of [primaryLayout] used to accommodate for more space, for example by using
 *   [maxPrimaryLayoutMargins]. Or, if the [shape] is set to [Shapes.full], using
 *   [minPrimaryLayoutMargins] can be considered.
 * @param width The width of this button. It's highly recommended to set this to [expand] or
 *   [weight]
 * @param height The height of this button. It's highly recommended to set this to [expand] or
 *   [weight]
 * @param colors The colors used for this button. If not set, [ButtonDefaults.filledButtonColors]
 *   will be used as high emphasis button. Other recommended colors are
 *   [ButtonDefaults.filledTonalButtonColors] and [ButtonDefaults.filledVariantButtonColors]. If
 *   using custom colors, it is important to choose a color pair from same role to ensure
 *   accessibility with sufficient color contrast.
 * @param style The style which provides the attribute values required for constructing this text
 *   button and its inner content. It also provides default style for the inner content, that can be
 *   overridden by each content slot.
 * @param contentPadding The inner padding used to prevent inner content from being too close to the
 *   button's edge. It's highly recommended to keep the default.
 * @param labelContent The text slot for content displayed in this button. It is recommended to use
 *   default styling that is automatically provided by only calling [text]. This should be small
 *   text, usually up to 3 characters text.
 * @sample androidx.wear.protolayout.material3.samples.oneSlotButtonsSample
 */
// TODO: b/346958146 - Link Button visuals in DAC
public fun MaterialScope.textButton(
    onClick: Clickable,
    labelContent: (MaterialScope.() -> LayoutElement),
    modifier: LayoutModifier = LayoutModifier,
    width: ContainerDimension = wrapWithMinTapTargetDimension(),
    height: ContainerDimension = wrapWithMinTapTargetDimension(),
    shape: Corner = shapes.full,
    colors: ButtonColors = filledButtonColors(),
    style: TextButtonStyle = defaultTextButtonStyle(),
    contentPadding: Padding = style.innerPadding
): LayoutElement =
    buttonContainer(
        onClick = onClick,
        modifier = modifier.background(color = colors.containerColor, corner = shape),
        width = width,
        height = height,
        contentPadding = contentPadding,
        content = {
            withStyle(
                    defaultTextElementStyle =
                        TextElementStyle(
                            typography = style.labelTypography,
                            color = colors.labelColor
                        )
                )
                .labelContent()
        }
    )

/**
 * Opinionated ProtoLayout Material3 pill shape button that offers up to three slots to take content
 * representing vertically stacked label and secondary label, and an icon next to it.
 *
 * @param onClick Associated [Clickable] for click events. When the button is clicked it will fire
 *   the associated action.
 * @param labelContent The text slot for content displayed in this button. It is recommended to use
 *   default styling that is automatically provided by only calling [text].
 * @param modifier Modifiers to set to this element. It's highly recommended to set a content
 *   description using [contentDescription].
 * @param secondaryLabelContent The text slot for content displayed in this button. It is
 *   recommended to use default styling that is automatically provided by only calling [text].
 * @param iconContent The icon slot for content displayed in this button. It is recommended to use
 *   default styling that is automatically provided by only calling [icon] with the resource ID.
 * @param shape Defines the button's shape, in other words the corner radius for this button. If
 *   changing these to radius smaller than [Shapes.medium], it is important to adjusts the margins
 *   of [primaryLayout] used to accommodate for more space, for example by using
 *   [maxPrimaryLayoutMargins]. Or, if the [shape] is set to [Shapes.full], using
 *   [minPrimaryLayoutMargins] can be considered.
 * @param width The width of this button. It's highly recommended to set this to [expand] or
 *   [weight]
 * @param height The height of this button. It's highly recommended to set this to [expand] or
 *   [weight]
 * @param colors The colors used for this button. If not set, [ButtonDefaults.filledButtonColors]
 *   will be used as high emphasis button. Other recommended colors are
 *   [ButtonDefaults.filledTonalButtonColors] and [ButtonDefaults.filledVariantButtonColors]. If
 *   using custom colors, it is important to choose a color pair from same role to ensure
 *   accessibility with sufficient color contrast.
 * @param backgroundContent The background object to be used behind the content in the button. It is
 *   recommended to use the default styling that is automatically provided by only calling
 *   [backgroundImage] with the content. It can be combined with the specified
 *   [ButtonColors.containerColor] behind it.
 * @param style The style which provides the attribute values required for constructing this pill
 *   shape button and its inner content. It also provides default style for the inner content, that
 *   can be overridden by each content slot.
 * @param horizontalAlignment The horizontal placement of the [labelContent] and
 *   [secondaryLabelContent] content. If [iconContent] is present, this should be
 *   [HORIZONTAL_ALIGN_START]. Defaults to [HORIZONTAL_ALIGN_CENTER] if only [labelContent] is
 *   present, otherwise it default to [HORIZONTAL_ALIGN_START].
 * @param contentPadding The inner padding used to prevent inner content from being too close to the
 *   button's edge. It's highly recommended to keep the default.
 * @sample androidx.wear.protolayout.material3.samples.pillShapeButtonsSample
 * @sample androidx.wear.protolayout.material3.samples.customButtonSample
 */
// TODO: b/346958146 - Link Button visuals in DAC
public fun MaterialScope.button(
    onClick: Clickable,
    labelContent: (MaterialScope.() -> LayoutElement),
    modifier: LayoutModifier = LayoutModifier,
    secondaryLabelContent: (MaterialScope.() -> LayoutElement)? = null,
    iconContent: (MaterialScope.() -> LayoutElement)? = null,
    width: ContainerDimension = wrapWithMinTapTargetDimension(),
    height: ContainerDimension = wrapWithMinTapTargetDimension(),
    shape: Corner = shapes.full,
    colors: ButtonColors = filledButtonColors(),
    backgroundContent: (MaterialScope.() -> LayoutElement)? = null,
    style: ButtonStyle = defaultButtonStyle(),
    @HorizontalAlignment
    horizontalAlignment: Int =
        if (iconContent == null && secondaryLabelContent == null) {
            HORIZONTAL_ALIGN_CENTER
        } else {
            HORIZONTAL_ALIGN_START
        },
    contentPadding: Padding = style.innerPadding
): LayoutElement =
    buttonContainer(
        onClick = onClick,
        modifier = modifier.background(color = colors.containerColor, corner = shape),
        width = width,
        height = height,
        backgroundContent = backgroundContent,
        contentPadding = contentPadding,
        content = {
            buildContentForPillShapeButton(
                label =
                    withStyle(
                            defaultTextElementStyle =
                                TextElementStyle(
                                    typography = style.labelTypography,
                                    color = colors.labelColor,
                                    alignment = horizontalAlignment.horizontalAlignToTextAlign()
                                )
                        )
                        .labelContent(),
                secondaryLabel =
                    secondaryLabelContent?.let {
                        withStyle(
                                defaultTextElementStyle =
                                    TextElementStyle(
                                        typography = style.secondaryLabelTypography,
                                        color = colors.secondaryLabelColor,
                                        alignment = horizontalAlignment.horizontalAlignToTextAlign()
                                    )
                            )
                            .secondaryLabelContent()
                    },
                icon =
                    iconContent?.let {
                        withStyle(
                                defaultIconStyle =
                                    IconStyle(
                                        width = dp(style.iconSize),
                                        height = dp(style.iconSize),
                                        tintColor = colors.iconColor
                                    )
                            )
                            .iconContent()
                    },
                horizontalAlignment = horizontalAlignment,
                style = style
            )
        }
    )

/**
 * Opinionated ProtoLayout Material3 pill shape avatar button that offers up to three slots to take
 * content representing vertically stacked label and secondary label, and an image (avatar) next to
 * it.
 *
 * Difference from the [button] is that this one takes an image instead of an icon and spaces the
 * content proportionally, so that edge of the button nicely hugs the avatar image.
 *
 * @param onClick Associated [Clickable] for click events. When the button is clicked it will fire
 *   the associated action.
 * @param labelContent The text slot for content displayed in this button. It is recommended to use
 *   default styling that is automatically provided by only calling [text].
 * @param modifier Modifiers to set to this element. It's highly recommended to set a content
 *   description using [contentDescription].
 * @param secondaryLabelContent The text slot for content displayed in this button. It is
 *   recommended to use default styling that is automatically provided by only calling [text].
 * @param avatarContent The avatar slot for content displayed in this button. It is recommended to
 *   use default styling that is automatically provided by only calling [avatarImage] with the
 *   resource ID. Width and height of this element should be set to [expand].
 * @param shape Defines the button's shape, in other words the corner radius for this button. If
 *   changing these to radius smaller than [Shapes.medium], it is important to adjusts the margins
 *   of [primaryLayout] used to accommodate for more space, for example by using
 *   [maxPrimaryLayoutMargins]. Or, if the [shape] is set to [Shapes.full], using
 *   [minPrimaryLayoutMargins] can be considered.
 * @param height The height of this button. It's highly recommended to set this to [expand] or
 *   [weight]
 * @param colors The colors used for this button. If not set, [ButtonDefaults.filledButtonColors]
 *   will be used as high emphasis button. Other recommended colors are
 *   [ButtonDefaults.filledTonalButtonColors] and [ButtonDefaults.filledVariantButtonColors]. If
 *   using custom colors, it is important to choose a color pair from same role to ensure
 *   accessibility with sufficient color contrast.
 * @param style The style which provides the attribute values required for constructing this pill
 *   shape button and its inner content. It also provides default style for the inner content, that
 *   can be overridden by each content slot.
 * @param horizontalAlignment The horizontal placement of the [avatarContent]. This should be
 *   [HORIZONTAL_ALIGN_START] to place the [avatarContent] on the start side of the button, or
 *   [HORIZONTAL_ALIGN_END] to place in on the end side. [HORIZONTAL_ALIGN_CENTER] will be ignored
 *   and replaced with [HORIZONTAL_ALIGN_START].
 * @param contentPadding The inner padding used to prevent inner content from being too close to the
 *   button's edge. It's highly recommended to keep the default. Only vertical values would be used,
 *   as horizontally elements are spaced out proportionally to the buttons width.
 * @sample androidx.wear.protolayout.material3.samples.avatarButtonSample
 */
// TODO: b/346958146 - Link Button visuals in DAC
public fun MaterialScope.avatarButton(
    onClick: Clickable,
    labelContent: (MaterialScope.() -> LayoutElement),
    avatarContent: (MaterialScope.() -> LayoutElement),
    modifier: LayoutModifier = LayoutModifier,
    secondaryLabelContent: (MaterialScope.() -> LayoutElement)? = null,
    height: ContainerDimension = wrapWithMinTapTargetDimension(),
    shape: Corner = shapes.full,
    colors: ButtonColors = filledButtonColors(),
    style: AvatarButtonStyle = defaultAvatarButtonStyle(),
    @HorizontalAlignment horizontalAlignment: Int = HORIZONTAL_ALIGN_START,
    contentPadding: Padding = style.innerVerticalPadding
): LayoutElement =
    buttonContainer(
        onClick = onClick,
        modifier = modifier.background(color = colors.containerColor, corner = shape),
        width = expand(),
        height = height,
        contentPadding = contentPadding,
        content = {
            buildContentForAvatarButton(
                label =
                    withStyle(
                            defaultTextElementStyle =
                                TextElementStyle(
                                    typography = style.labelTypography,
                                    color = colors.labelColor,
                                    alignment = HORIZONTAL_ALIGN_START.horizontalAlignToTextAlign()
                                )
                        )
                        .labelContent(),
                secondaryLabel =
                    secondaryLabelContent?.let {
                        withStyle(
                                defaultTextElementStyle =
                                    TextElementStyle(
                                        typography = style.secondaryLabelTypography,
                                        color = colors.secondaryLabelColor,
                                        alignment =
                                            HORIZONTAL_ALIGN_START.horizontalAlignToTextAlign()
                                    )
                            )
                            .secondaryLabelContent()
                    },
                avatar =
                    withStyle(
                            defaultAvatarImageStyle =
                                AvatarImageStyle(
                                    width = expand(),
                                    // We want height to be same as the calculated width
                                    height =
                                        DimensionBuilders.ProportionalDimensionProp.Builder()
                                            .setAspectRatioWidth(1)
                                            .setAspectRatioHeight(1)
                                            .build(),
                                    contentScaleMode = LayoutElementBuilders.CONTENT_SCALE_MODE_FIT
                                )
                        )
                        .avatarContent(),
                horizontalAlignment =
                    if (horizontalAlignment == HORIZONTAL_ALIGN_CENTER) {
                        HORIZONTAL_ALIGN_START
                    } else {
                        horizontalAlignment
                    },
                style = style,
                height = height
            )
        }
    )

/**
 * ProtoLayout Material3 clickable image button that doesn't offer additional slots, only image (for
 * example [backgroundImage] as a background.
 *
 * The button is usually stadium or circle shaped with fully rounded corners by default. It is
 * highly recommended to set its width and height to fill the available space, by [expand] or
 * [weight] for optimal experience across different screen sizes, and use [buttonGroup] to arrange
 * them.
 *
 * @param onClick Associated [Clickable] for click events. When the button is clicked it will fire
 *   the associated action.
 * @param modifier Modifiers to set to this element. It's highly recommended to set a content
 *   description using [contentDescription]. If [LayoutModifier.background] modifier is used and the
 *   the background image is also specified, the image will be laid out on top of this color. In
 *   case of the fully opaque background image, then the background color will not be shown.
 * @param backgroundContent The background object to be used behind the content in the button. It is
 *   recommended to use the default styling that is automatically provided by only calling
 *   [backgroundImage] with the content. It can be combined with the specified
 *   [LayoutModifier.background] behind it.
 * @param width The width of this button. It's highly recommended to set this to [expand] or
 *   [weight]
 * @param height The height of this button. It's highly recommended to set this to [expand] or
 *   [weight]
 * @sample androidx.wear.protolayout.material3.samples.imageButtonSample
 */
public fun MaterialScope.imageButton(
    onClick: Clickable,
    backgroundContent: (MaterialScope.() -> LayoutElement),
    modifier: LayoutModifier = LayoutModifier,
    width: ContainerDimension = IMAGE_BUTTON_DEFAULT_SIZE_DP.toDp(),
    height: ContainerDimension = IMAGE_BUTTON_DEFAULT_SIZE_DP.toDp()
): LayoutElement =
    buttonContainer(
        onClick = onClick,
        modifier = modifier,
        width = width,
        height = height,
        backgroundContent = backgroundContent
    )

/**
 * Opinionated ProtoLayout Material3 compact button that offers up to two slots to take horizontally
 * stacked content representing an icon and text next to it.
 *
 * @param onClick Associated [Clickable] for click events. When the button is clicked it will fire
 *   the associated action.
 * @param modifier Modifiers to set to this element. It's highly recommended to set a content
 *   description using [contentDescription].
 * @param labelContent The text slot for content displayed in this button. It is recommended to use
 *   default styling that is automatically provided by calling [text] with only the text data
 *   parameter.
 * @param iconContent The icon slot for content displayed in this button. It is recommended to use
 *   default styling that is automatically provided by only calling [icon] with the resource ID.
 * @param shape Defines the button's shape, in other words the corner radius for this button. If
 *   changing these to radius smaller than [Shapes.medium], it is important to adjusts the margins
 *   of [primaryLayout] used to accommodate for more space, for example by using
 *   [maxPrimaryLayoutMargins]. Or, if the [shape] is set to [Shapes.full], using
 *   [minPrimaryLayoutMargins] can be considered.
 * @param width The width of this button. It's highly recommended to set this to [expand] or
 *   [weight]
 * @param colors The colors used for this button. If not set, [ButtonDefaults.filledButtonColors]
 *   will be used as high emphasis button. Other recommended colors are
 *   [ButtonDefaults.filledTonalButtonColors] and [ButtonDefaults.filledVariantButtonColors]. If
 *   using custom colors, it is important to choose colors from the same role to ensure
 *   accessibility with sufficient color contrast.
 * @param horizontalAlignment The horizontal placement of the [labelContent] and [iconContent]
 *   content. If both are present, this should be [HORIZONTAL_ALIGN_START] or [HORIZONTAL_ALIGN_END]
 *   (in which case [iconContent] would be on the start or end side, respectively). Defaults to
 *   [HORIZONTAL_ALIGN_CENTER] if only [labelContent] or [iconContent] is present, otherwise it
 *   default to [HORIZONTAL_ALIGN_START].
 * @param contentPadding The inner padding used to prevent inner content from being too close to the
 *   button's edge. It's highly recommended to keep the default.
 * @sample androidx.wear.protolayout.material3.samples.compactButtonsSample
 */
// TODO: b/346958146 - Link Button visuals in DAC
public fun MaterialScope.compactButton(
    onClick: Clickable,
    modifier: LayoutModifier = LayoutModifier,
    labelContent: (MaterialScope.() -> LayoutElement)? = null,
    iconContent: (MaterialScope.() -> LayoutElement)? = null,
    width: ContainerDimension = wrapWithMinTapTargetDimension(),
    shape: Corner = shapes.full,
    colors: ButtonColors = filledButtonColors(),
    @HorizontalAlignment
    horizontalAlignment: Int =
        if (iconContent != null && labelContent != null) {
            HORIZONTAL_ALIGN_START
        } else {
            HORIZONTAL_ALIGN_CENTER
        },
    contentPadding: Padding =
        Padding.Builder()
            .setStart(COMPACT_BUTTON_DEFAULT_CONTENT_PADDING_DP.toDp())
            .setEnd(COMPACT_BUTTON_DEFAULT_CONTENT_PADDING_DP.toDp())
            .build()
): LayoutElement =
    // Compact button has a fixed height of 32 dp, we need to wrap it in a box to add 8dp margin on
    // its top and bottom for accessibility.
    Box.Builder()
        .addContent(
            // The actual visible part of compact button
            componentContainer(
                onClick = onClick,
                modifier = modifier.background(color = colors.containerColor, corner = shape),
                width = width,
                height = dp(COMPACT_BUTTON_HEIGHT_DP),
                contentPadding = contentPadding,
                backgroundContent = null,
                metadataTag = null,
                content = {
                    buildContentForCompactButton(
                        label =
                            labelContent?.let {
                                withStyle(
                                        defaultTextElementStyle =
                                            TextElementStyle(
                                                typography = COMPACT_BUTTON_LABEL_TYPOGRAPHY,
                                                color = colors.labelColor,
                                                alignment =
                                                    horizontalAlignment.horizontalAlignToTextAlign()
                                            )
                                    )
                                    .labelContent()
                            },
                        icon =
                            iconContent?.let {
                                val iconSize =
                                    dp(
                                        if (labelContent == null) {
                                            COMPACT_BUTTON_ICON_SIZE_LARGE_DP
                                        } else {
                                            COMPACT_BUTTON_ICON_SIZE_SMALL_DP
                                        }
                                    )
                                withStyle(
                                        defaultIconStyle =
                                            IconStyle(
                                                width = iconSize,
                                                height = iconSize,
                                                tintColor = colors.iconColor
                                            )
                                    )
                                    .iconContent()
                            },
                        horizontalAlignment = horizontalAlignment,
                        width = width
                    )
                }
            )
        )
        .setModifiers(LayoutModifier.tag(METADATA_TAG_BUTTON).toProtoLayoutModifiers())
        .setHeight(MINIMUM_TAP_TARGET_SIZE)
        .setWidth(width)
        .build()

/**
 * ProtoLayout Material3 clickable component button that offers a single slot to take any content.
 *
 * The button is usually stadium or circle shaped with fully rounded corners by default. It is
 * highly recommended to set its width and height to fill the available space, by [expand] or
 * [weight] for optimal experience across different screen sizes, and use [buttonGroup] to arrange
 * them.
 *
 * It can be used for displaying any clickable container with additional data, text or images.
 *
 * @param onClick Associated [Clickable] for click events. When the button is clicked it will fire
 *   the associated action.
 * @param modifier Modifiers to set to this element. It's highly recommended to set a content
 *   description using [contentDescription]. If [LayoutModifier.background] modifier is used and the
 *   the background image is also specified, the image will be laid out on top of this color. In
 *   case of the fully opaque background image, then the background color will not be shown.
 * @param backgroundContent The background object to be used behind the content in the button. It is
 *   recommended to use the default styling that is automatically provided by only calling
 *   [backgroundImage] with the content. It can be combined with the specified
 *   [LayoutModifier.background] behind it.
 * @param width The width of this button. It's highly recommended to set this to [expand] or
 *   [weight]
 * @param height The height of this button. It's highly recommended to set this to [expand] or
 *   [weight]
 * @param contentPadding The inner padding used to prevent inner content from being too close to the
 *   button's edge. It's highly recommended to keep the default.
 * @param content The inner content to be put inside of this button.
 */
internal fun MaterialScope.buttonContainer(
    onClick: Clickable,
    modifier: LayoutModifier = LayoutModifier,
    content: (MaterialScope.() -> LayoutElement)? = null,
    width: ContainerDimension =
        if (content == null) {
            IMAGE_BUTTON_DEFAULT_SIZE_DP.toDp()
        } else {
            wrapWithMinTapTargetDimension()
        },
    height: ContainerDimension =
        if (content == null) {
            IMAGE_BUTTON_DEFAULT_SIZE_DP.toDp()
        } else {
            wrapWithMinTapTargetDimension()
        },
    backgroundContent: (MaterialScope.() -> LayoutElement)? = null,
    contentPadding: Padding = DEFAULT_CONTENT_PADDING
): LayoutElement =
    componentContainer(
        onClick = onClick,
        modifier = LayoutModifier.clip(shapes.full) then modifier,
        width = width,
        height = height,
        backgroundContent = backgroundContent,
        contentPadding = contentPadding,
        metadataTag = METADATA_TAG_BUTTON,
        content = content
    )
