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

import androidx.annotation.Dimension
import androidx.annotation.Dimension.Companion.DP
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.LayoutElementBuilders.VERTICAL_ALIGN_CENTER
import androidx.wear.protolayout.LayoutElementBuilders.VerticalAlignment
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.ModifiersBuilders.Padding
import androidx.wear.protolayout.ModifiersBuilders.SEMANTICS_ROLE_BUTTON
import androidx.wear.protolayout.material3.ButtonDefaults.filledButtonColors
import androidx.wear.protolayout.material3.EdgeButtonDefaults.BOTTOM_MARGIN_DP
import androidx.wear.protolayout.material3.EdgeButtonDefaults.CONTAINER_HEIGHT_DP
import androidx.wear.protolayout.material3.EdgeButtonDefaults.EDGE_BUTTON_HEIGHT_DP
import androidx.wear.protolayout.material3.EdgeButtonDefaults.HORIZONTAL_MARGIN_PERCENT_LARGE
import androidx.wear.protolayout.material3.EdgeButtonDefaults.HORIZONTAL_MARGIN_PERCENT_SMALL
import androidx.wear.protolayout.material3.EdgeButtonDefaults.ICON_SIZE_DP
import androidx.wear.protolayout.material3.EdgeButtonDefaults.METADATA_TAG
import androidx.wear.protolayout.material3.EdgeButtonDefaults.TEXT_SIDE_PADDING_DP
import androidx.wear.protolayout.material3.EdgeButtonDefaults.TEXT_TOP_PADDING_DP
import androidx.wear.protolayout.material3.EdgeButtonDefaults.TOP_CORNER_RADIUS
import androidx.wear.protolayout.material3.EdgeButtonFallbackDefaults.BOTTOM_MARGIN_FALLBACK_DP
import androidx.wear.protolayout.material3.EdgeButtonFallbackDefaults.CORNER_RADIUS_FALLBACK_DP
import androidx.wear.protolayout.material3.EdgeButtonFallbackDefaults.EDGE_BUTTON_HEIGHT_FALLBACK_DP
import androidx.wear.protolayout.material3.EdgeButtonFallbackDefaults.ICON_SIDE_PADDING_FALLBACK_DP
import androidx.wear.protolayout.material3.EdgeButtonFallbackDefaults.ICON_SIZE_FALLBACK_DP
import androidx.wear.protolayout.material3.EdgeButtonFallbackDefaults.TEXT_SIDE_PADDING_FALLBACK_DP
import androidx.wear.protolayout.material3.EdgeButtonStyle.Companion.ICON
import androidx.wear.protolayout.material3.EdgeButtonStyle.Companion.ICON_FALLBACK
import androidx.wear.protolayout.material3.EdgeButtonStyle.Companion.TEXT
import androidx.wear.protolayout.material3.EdgeButtonStyle.Companion.TEXT_FALLBACK
import androidx.wear.protolayout.material3.Versions.hasAsymmetricalCornersSupport
import androidx.wear.protolayout.modifiers.LayoutModifier
import androidx.wear.protolayout.modifiers.background
import androidx.wear.protolayout.modifiers.clickable
import androidx.wear.protolayout.modifiers.clip
import androidx.wear.protolayout.modifiers.clipBottomLeft
import androidx.wear.protolayout.modifiers.clipBottomRight
import androidx.wear.protolayout.modifiers.contentDescription
import androidx.wear.protolayout.modifiers.padding
import androidx.wear.protolayout.modifiers.semanticsRole
import androidx.wear.protolayout.modifiers.tag
import androidx.wear.protolayout.modifiers.toProtoLayoutModifiers
import androidx.wear.protolayout.types.dp

/**
 * ProtoLayout Material3 component edge button that offers a single slot to take an icon or similar
 * round, small content.
 *
 * The edge button is intended to be used at the bottom of a round screen. It has a special shape
 * with its bottom almost follows the screen's curvature. It has fixed height, and takes 1 line of
 * icon. This button represents the most important action on the screen, and it must occupy the
 * whole horizontal space in its position as well as being anchored to the screen bottom.
 *
 * This component is not intended to be used with an image background.
 *
 * @param onClick Associated [Clickable] for click events. When the button is clicked it will fire
 *   the associated action.
 * @param modifier Modifiers to set to this element. It's highly recommended to set a content
 *   description using [contentDescription].
 * @param colors The colors used for this button. If not set, [ButtonDefaults.filledButtonColors]
 *   will be used as high emphasis button. Other recommended colors are
 *   [ButtonDefaults.filledTonalButtonColors] and [ButtonDefaults.filledVariantButtonColors]. If
 *   using custom colors, it is important to choose a color pair from same role to ensure
 *   accessibility with sufficient color contrast.
 * @param iconContent The icon slot for content displayed in this button. It is recommended to use
 *   default styling that is automatically provided by only calling [icon] with the resource ID.
 * @sample androidx.wear.protolayout.material3.samples.edgeButtonSampleIcon
 */
// TODO: b/346958146 - link EdgeButton visuals in DAC
public fun MaterialScope.iconEdgeButton(
    onClick: Clickable,
    modifier: LayoutModifier = LayoutModifier,
    colors: ButtonColors = filledButtonColors(),
    iconContent: (MaterialScope.() -> LayoutElement)
): LayoutElement {
    val style =
        if (deviceConfiguration.rendererSchemaVersion.hasAsymmetricalCornersSupport()) {
            ICON
        } else {
            ICON_FALLBACK
        }

    return edgeButton(onClick = onClick, modifier = modifier, colors = colors, style = style) {
        withStyle(
                defaultIconStyle =
                    IconStyle(
                        width = style.iconSizeDp.dp,
                        height = style.iconSizeDp.dp,
                        tintColor = colors.iconColor
                    )
            )
            .iconContent()
    }
}

/**
 * ProtoLayout Material3 component edge button that offers a single slot to take a text or similar
 * long and wide content.
 *
 * The edge button is intended to be used at the bottom of a round screen. It has a special shape
 * with its bottom almost follows the screen's curvature. It has fixed height, and takes 1 line of
 * text. This button represents the most important action on the screen, and it must occupy the
 * whole horizontal space in its position as well as being anchored to the screen bottom.
 *
 * This component is not intended to be used with an image background.
 *
 * @param onClick Associated [Clickable] for click events. When the button is clicked it will fire
 *   the associated action.
 * @param modifier Modifiers to set to this element. It's highly recommended to set a content
 *   description using [contentDescription].
 * @param colors The colors used for this button. If not set, [ButtonDefaults.filledButtonColors]
 *   will be used as high emphasis button. Other recommended colors are
 *   [ButtonDefaults.filledTonalButtonColors] and [ButtonDefaults.filledVariantButtonColors]. If
 *   using custom colors, it is important to choose a color pair from same role to ensure
 *   accessibility with sufficient color contrast.
 * @param labelContent The label slot for content displayed in this button. It is recommended to use
 *   default styling that is automatically provided by only calling [text] with the content.
 * @sample androidx.wear.protolayout.material3.samples.edgeButtonSampleText
 */
// TODO(b/346958146): link EdgeButton visuals in DAC
public fun MaterialScope.textEdgeButton(
    onClick: Clickable,
    modifier: LayoutModifier = LayoutModifier,
    colors: ButtonColors = filledButtonColors(),
    labelContent: (MaterialScope.() -> LayoutElement)
): LayoutElement =
    edgeButton(
        onClick = onClick,
        modifier = modifier,
        colors = colors,
        style =
            if (deviceConfiguration.rendererSchemaVersion.hasAsymmetricalCornersSupport()) {
                TEXT
            } else {
                TEXT_FALLBACK
            }
    ) {
        withStyle(
                defaultTextElementStyle =
                    TextElementStyle(
                        typography = Typography.LABEL_MEDIUM,
                        color = colors.iconColor,
                        scalable = false
                    )
            )
            .labelContent()
    }

/**
 * ProtoLayout Material3 component edge button that offers a single slot to take any content.
 *
 * The edge button is intended to be used at the bottom of a round screen. It has a special shape
 * with its bottom almost follows the screen's curvature. It has fixed height, and takes 1 line of
 * text or a single icon. This button represents the most important action on the screen, and it
 * must occupy the whole horizontal space in its position as well as being anchored to the screen
 * bottom.
 *
 * This component is not intended to be used with an image background.
 *
 * @param onClick Associated [Clickable] for click events. When the button is clicked it will fire
 *   the associated action.
 * @param colors The colors used for this button. If not set, [ButtonDefaults.filledButtonColors]
 *   will be used as high emphasis button. Other recommended colors are
 *   [ButtonDefaults.filledTonalButtonColors] and [ButtonDefaults.filledVariantButtonColors]. If
 *   using custom colors, it is important to choose a color pair from same role to ensure
 *   accessibility with sufficient color contrast.
 * @param modifier Modifiers to set to this element. It's highly recommended to set a content
 *   description using [contentDescription].
 * @param style The style used for the inner content, specifying how the content should be aligned.
 *   It is recommended to use [EdgeButtonStyle.TEXT] for long, wide content. If not set, defaults to
 *   [EdgeButtonStyle.ICON] which center-aligns the content.
 * @param content The inner content to be put inside of this edge button.
 * @sample androidx.wear.protolayout.material3.samples.edgeButtonSampleIcon
 */
// TODO(b/346958146): link EdgeButton visuals in DAC
private fun MaterialScope.edgeButton(
    onClick: Clickable,
    colors: ButtonColors,
    modifier: LayoutModifier = LayoutModifier,
    style: EdgeButtonStyle = ICON,
    content: MaterialScope.() -> LayoutElement
): LayoutElement {
    val containerWidth = deviceConfiguration.screenWidthDp.toDp()
    val horizontalMarginPercent: Float =
        if (deviceConfiguration.screenWidthDp.isBreakpoint()) {
            HORIZONTAL_MARGIN_PERCENT_LARGE
        } else {
            HORIZONTAL_MARGIN_PERCENT_SMALL
        }
    val edgeButtonWidth: Float =
        (100f - 2f * horizontalMarginPercent) * deviceConfiguration.screenWidthDp / 100f

    var mod =
        (LayoutModifier.semanticsRole(SEMANTICS_ROLE_BUTTON) then modifier)
            .clickable(onClick)
            .background(colors.containerColor)
            .clip(style.topCornerRadiusDp)

    if (deviceConfiguration.rendererSchemaVersion.hasAsymmetricalCornersSupport()) {
        val bottomCornerRadiusX = edgeButtonWidth / 2f
        val bottomCornerRadiusY = style.buttonHeightDp - style.topCornerRadiusDp
        mod =
            mod.clipBottomLeft(bottomCornerRadiusX, bottomCornerRadiusY)
                .clipBottomRight(bottomCornerRadiusX, bottomCornerRadiusY)
    }

    style.padding?.let { mod = mod.padding(it) }

    val button =
        Box.Builder()
            .setHeight(style.buttonHeightDp.dp)
            .setWidth(dp(edgeButtonWidth))
            .setVerticalAlignment(style.verticalAlignment)
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .addContent(content())
            .setModifiers(mod.toProtoLayoutModifiers())
            .build()

    return Box.Builder()
        .setHeight(CONTAINER_HEIGHT_DP.dp)
        .setWidth(containerWidth)
        .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_BOTTOM)
        .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
        .addContent(button)
        .setModifiers(
            LayoutModifier.tag(METADATA_TAG)
                .padding(padding(bottom = style.bottomMarginDp))
                .toProtoLayoutModifiers()
        )
        .build()
}

/**
 * Provides style values for edge button component.
 *
 * An [edgeButton] has a wrapper container with the screen width, and fixed height of
 * [CONTAINER_HEIGHT_DP].
 *
 * The visible button box has height of [buttonHeightDp], it is centered horizontally in side the
 * wrapper container with [HORIZONTAL_MARGIN_PERCENT_SMALL] or [HORIZONTAL_MARGIN_PERCENT_LARGE]
 * depending on the screen size. It is then horizontally aligned to the bottom with bottom margin of
 * [bottomMarginDp].
 *
 * The visible button box has its top two corners clipped with the [topCornerRadiusDp], while its
 * bottom two corners will be clipped fully both horizontally and vertically to achieving the edge
 * hugging shape. In the fallback implementation, without asymmetrical corners support, the
 * [topCornerRadiusDp] is applied to all four corners.
 *
 * The content (icon or text) of the button is located inside the visible button box with [padding],
 * horizontally centered and vertically aligned with the given [verticalAlignment].
 */
internal class EdgeButtonStyle
internal constructor(
    @VerticalAlignment internal val verticalAlignment: Int = VERTICAL_ALIGN_CENTER,
    internal val padding: Padding? = null,
    @Dimension(DP) internal val buttonHeightDp: Float = EDGE_BUTTON_HEIGHT_DP,
    @Dimension(DP) internal val bottomMarginDp: Float = BOTTOM_MARGIN_DP,
    @Dimension(DP) internal val iconSizeDp: Float = ICON_SIZE_DP,
    @Dimension(DP) internal val topCornerRadiusDp: Float = TOP_CORNER_RADIUS
) {
    internal companion object {
        /**
         * Style variation for having text content with edge hugging shape.
         *
         * The text is vertically aligned to top with [TEXT_TOP_PADDING_DP] to to accommodate for
         * more horizontal space.
         */
        internal val TEXT: EdgeButtonStyle =
            EdgeButtonStyle(
                verticalAlignment = LayoutElementBuilders.VERTICAL_ALIGN_TOP,
                padding =
                    padding(
                        start = TEXT_SIDE_PADDING_DP,
                        top = TEXT_TOP_PADDING_DP,
                        end = TEXT_SIDE_PADDING_DP
                    )
            )

        /**
         * Style variation for having icon content with edge hugging shape.
         *
         * The icon is centered in the visible button box with the size of [ICON_SIZE_DP].
         */
        internal val ICON: EdgeButtonStyle = EdgeButtonStyle()

        /**
         * Style variation for fallback implementation with text content, when there is no
         * asymmetrical corners support.
         *
         * Without the edge hugging shape, the [topCornerRadius] value is a full cornered value and
         * is applied to all four corners. To avoid being clipped by the screen edge, the visible
         * button box is pushed upwards with a bigger bottom margin of [BOTTOM_MARGIN_FALLBACK_DP].
         * Also the box height shrinks to [EDGE_BUTTON_HEIGHT_FALLBACK_DP].
         *
         * Its text content is center placed with a increased horizontal padding of
         * [TEXT_SIDE_PADDING_FALLBACK_DP].
         */
        internal val TEXT_FALLBACK: EdgeButtonStyle =
            EdgeButtonStyle(
                verticalAlignment = VERTICAL_ALIGN_CENTER,
                padding =
                    padding(
                        start = TEXT_SIDE_PADDING_FALLBACK_DP,
                        end = TEXT_SIDE_PADDING_FALLBACK_DP,
                    ),
                buttonHeightDp = EDGE_BUTTON_HEIGHT_FALLBACK_DP,
                topCornerRadiusDp = CORNER_RADIUS_FALLBACK_DP,
                bottomMarginDp = BOTTOM_MARGIN_FALLBACK_DP
            )

        /**
         * Style variation for fallback implementation with icon content, when there is no
         * asymmetrical corners support.
         *
         * Without the edge hugging shape, the [topCornerRadius] value is a full cornered value and
         * is applied to all four corners. To avoid being clipped by the screen, the visible button
         * box is pushed upwards with a bigger bottom margin of [BOTTOM_MARGIN_FALLBACK_DP]. Also
         * the box height shrinks to [EDGE_BUTTON_HEIGHT_FALLBACK_DP]
         *
         * Its icon content center placed with increased horizontal padding
         * [ICON_SIDE_PADDING_FALLBACK_DP]. Also, the icon size is also increased to
         * [ICON_SIZE_FALLBACK_DP].
         */
        internal val ICON_FALLBACK: EdgeButtonStyle =
            EdgeButtonStyle(
                verticalAlignment = VERTICAL_ALIGN_CENTER,
                padding =
                    padding(
                        start = ICON_SIDE_PADDING_FALLBACK_DP,
                        end = ICON_SIDE_PADDING_FALLBACK_DP,
                    ),
                buttonHeightDp = EDGE_BUTTON_HEIGHT_FALLBACK_DP,
                topCornerRadiusDp = CORNER_RADIUS_FALLBACK_DP,
                bottomMarginDp = BOTTOM_MARGIN_FALLBACK_DP,
                iconSizeDp = ICON_SIZE_FALLBACK_DP
            )
    }
}

internal object EdgeButtonDefaults {
    @Dimension(DP) internal const val TOP_CORNER_RADIUS = 17f
    /** The horizontal margin used for width of the EdgeButton, below the 225dp breakpoint. */
    internal const val HORIZONTAL_MARGIN_PERCENT_SMALL = 24f
    /** The horizontal margin used for width of the EdgeButton, above the 225dp breakpoint. */
    internal const val HORIZONTAL_MARGIN_PERCENT_LARGE = 26f
    @Dimension(DP) internal const val BOTTOM_MARGIN_DP = 3f
    @Dimension(DP) internal const val EDGE_BUTTON_HEIGHT_DP = 46f
    @Dimension(DP) internal const val CONTAINER_HEIGHT_DP = EDGE_BUTTON_HEIGHT_DP + BOTTOM_MARGIN_DP
    internal const val METADATA_TAG = "EB"
    @Dimension(DP) internal const val ICON_SIZE_DP = 24f
    @Dimension(DP) internal const val TEXT_TOP_PADDING_DP = 12f
    @Dimension(DP) internal const val TEXT_SIDE_PADDING_DP = 8f
}

/**
 * This object provides constants and styles of the fallback layout for [iconEdgeButton] and
 * [textEdgeButton] when the renderer version is lower than 1.3.3 where asymmetrical corners support
 * is not available.
 */
internal object EdgeButtonFallbackDefaults {
    @Dimension(DP) internal const val ICON_SIZE_FALLBACK_DP = 26f
    @Dimension(DP) internal const val EDGE_BUTTON_HEIGHT_FALLBACK_DP = 40f
    @Dimension(DP) internal const val BOTTOM_MARGIN_FALLBACK_DP = 7f
    @Dimension(DP)
    internal const val CORNER_RADIUS_FALLBACK_DP = EDGE_BUTTON_HEIGHT_FALLBACK_DP / 2f
    @Dimension(DP) internal const val TEXT_SIDE_PADDING_FALLBACK_DP = 14f
    @Dimension(DP) internal const val ICON_SIDE_PADDING_FALLBACK_DP = 20f
}

internal fun LayoutElement.isSlotEdgeButton(): Boolean =
    this is Box && METADATA_TAG == this.modifiers?.metadata?.toTagName()
