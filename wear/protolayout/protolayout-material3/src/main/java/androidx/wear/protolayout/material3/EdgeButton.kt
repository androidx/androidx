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

import androidx.wear.protolayout.ColorBuilders.ColorProp
import androidx.wear.protolayout.DimensionBuilders.DpProp
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.LayoutElementBuilders.VERTICAL_ALIGN_CENTER
import androidx.wear.protolayout.LayoutElementBuilders.VerticalAlignment
import androidx.wear.protolayout.ModifiersBuilders.Background
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.ModifiersBuilders.Corner
import androidx.wear.protolayout.ModifiersBuilders.Modifiers
import androidx.wear.protolayout.ModifiersBuilders.Padding
import androidx.wear.protolayout.TypeBuilders.StringProp
import androidx.wear.protolayout.material3.EdgeButtonDefaults.BOTTOM_MARGIN_DP
import androidx.wear.protolayout.material3.EdgeButtonDefaults.EDGE_BUTTON_HEIGHT_DP
import androidx.wear.protolayout.material3.EdgeButtonDefaults.HORIZONTAL_MARGIN_PERCENT
import androidx.wear.protolayout.material3.EdgeButtonDefaults.ICON_SIZE_DP
import androidx.wear.protolayout.material3.EdgeButtonDefaults.METADATA_TAG
import androidx.wear.protolayout.material3.EdgeButtonDefaults.TEXT_SIDE_PADDING_DP
import androidx.wear.protolayout.material3.EdgeButtonDefaults.TEXT_TOP_PADDING_DP
import androidx.wear.protolayout.material3.EdgeButtonDefaults.TOP_CORNER_RADIUS
import androidx.wear.protolayout.material3.EdgeButtonDefaults.filled
import androidx.wear.protolayout.material3.EdgeButtonStyle.Companion.DEFAULT
import androidx.wear.protolayout.material3.EdgeButtonStyle.Companion.TOP_ALIGN

/**
 * ProtoLayout Material3 component edge button that offers a single slot to take an icon or similar
 * round, small content.
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
 * @param contentDescription The content description to be read by Talkback.
 * @param colors The colors used for this button. If not set, [EdgeButtonDefaults.filled] will be
 *   used as high emphasis button. Other recommended colors are [EdgeButtonDefaults.filledTonal] and
 *   [EdgeButtonDefaults.filledVariant]. If using custom colors, it is important to choose a color
 *   pair from same role to ensure accessibility with sufficient color contrast.
 * @param iconContent The icon slot for content displayed in this button. It is recommended to use
 *   default styling that is automatically provided by only calling [icon] with the resource ID.
 * @sample androidx.wear.protolayout.material3.samples.edgeButtonSampleIcon
 */
// TODO(b/346958146): link EdgeButton visuals in DAC
public fun MaterialScope.iconEdgeButton(
    onClick: Clickable,
    contentDescription: StringProp,
    colors: EdgeButtonColors = filled(),
    iconContent: (MaterialScope.() -> LayoutElement)
): LayoutElement =
    edgeButton(
        onClick = onClick,
        contentDescription = contentDescription,
        colors = colors,
        style = DEFAULT
    ) {
        withStyle(
                defaultIconStyle = IconStyle(size = ICON_SIZE_DP.toDp(), tintColor = colors.content)
            )
            .iconContent()
    }

/**
 * ProtoLayout Material3 component edge button that offers a single slot to take a text or similar
 * long and wide content.
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
 * @param contentDescription The content description to be read by Talkback.
 * @param colors The colors used for this button. If not set, [EdgeButtonDefaults.filled] will be
 *   used as high emphasis button. Other recommended colors are [EdgeButtonDefaults.filledTonal] and
 *   [EdgeButtonDefaults.filledVariant]. If using custom colors, it is important to choose a color
 *   pair from same role to ensure accessibility with sufficient color contrast.
 * @param labelContent The label slot for content displayed in this button. It is recommended to use
 *   default styling that is automatically provided by only calling [text] with the content.
 * @sample androidx.wear.protolayout.material3.samples.edgeButtonSampleText
 */
// TODO(b/346958146): link EdgeButton visuals in DAC
public fun MaterialScope.textEdgeButton(
    onClick: Clickable,
    contentDescription: StringProp,
    colors: EdgeButtonColors = filled(),
    labelContent: (MaterialScope.() -> LayoutElement)
): LayoutElement =
    edgeButton(
        onClick = onClick,
        contentDescription = contentDescription,
        colors = colors,
        style = TOP_ALIGN
    ) {
        withStyle(
                defaultTextElementStyle =
                    TextElementStyle(
                        typography = Typography.LABEL_MEDIUM,
                        color = colors.content,
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
 * @param contentDescription The content description to be read by Talkback.
 * @param colors The colors used for this button. If not set, [EdgeButtonDefaults.filled] will be
 *   used as high emphasis button. Other recommended colors are [EdgeButtonDefaults.filledTonal] and
 *   [EdgeButtonDefaults.filledVariant]. If using custom colors, it is important to choose a color
 *   pair from same role to ensure accessibility with sufficient color contrast.
 * @param style The style used for the inner content, specifying how the content should be aligned.
 *   It is recommended to use [EdgeButtonStyle.TOP_ALIGN] for long, wide content. If not set,
 *   defaults to [EdgeButtonStyle.DEFAULT] which center-aligns the content.
 * @param content The inner content to be put inside of this edge button.
 * @sample androidx.wear.protolayout.material3.samples.edgeButtonSampleIcon
 */
// TODO(b/346958146): link EdgeButton visuals in DAC
internal fun MaterialScope.edgeButton(
    onClick: Clickable,
    contentDescription: StringProp,
    colors: EdgeButtonColors,
    style: EdgeButtonStyle = DEFAULT,
    content: MaterialScope.() -> LayoutElement
): LayoutElement {
    val containerWidth = deviceConfiguration.screenWidthDp.toDp()
    val edgeButtonWidth: Float =
        (100f - 2f * HORIZONTAL_MARGIN_PERCENT) * deviceConfiguration.screenWidthDp / 100f
    val bottomCornerRadiusX = dp(edgeButtonWidth / 2f)
    val bottomCornerRadiusY = dp(EDGE_BUTTON_HEIGHT_DP - TOP_CORNER_RADIUS.value)

    val modifiers: Modifiers.Builder =
        Modifiers.Builder()
            .setClickable(onClick)
            .setSemantics(contentDescription.buttonRoleSemantics())
            .setBackground(
                Background.Builder()
                    .setColor(colors.container)
                    .setCorner(
                        Corner.Builder()
                            .setRadius(TOP_CORNER_RADIUS)
                            .setBottomLeftRadius(bottomCornerRadiusX, bottomCornerRadiusY)
                            .setBottomRightRadius(bottomCornerRadiusX, bottomCornerRadiusY)
                            .build()
                    )
                    .build()
            )

    val button = Box.Builder().setHeight(EDGE_BUTTON_HEIGHT_DP.toDp()).setWidth(dp(edgeButtonWidth))
    button
        .setVerticalAlignment(style.verticalAlignment)
        .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
        .addContent(content())

    style.padding?.let { modifiers.setPadding(it) }

    return Box.Builder()
        .setHeight((EDGE_BUTTON_HEIGHT_DP + BOTTOM_MARGIN_DP).toDp())
        .setWidth(containerWidth)
        .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_TOP)
        .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
        .addContent(button.setModifiers(modifiers.build()).build())
        .setModifiers(Modifiers.Builder().setMetadata(METADATA_TAG.toElementMetadata()).build())
        .build()
}

/** Provides style values for edge button component. */
public class EdgeButtonStyle
private constructor(
    @VerticalAlignment internal val verticalAlignment: Int = VERTICAL_ALIGN_CENTER,
    internal val padding: Padding? = null
) {
    public companion object {
        /**
         * Style variation for having content of the edge button anchored to the top.
         *
         * This should be used for text-like content, or the content that is wide, to accommodate
         * for more space.
         */
        @JvmField
        public val TOP_ALIGN: EdgeButtonStyle =
            EdgeButtonStyle(
                verticalAlignment = LayoutElementBuilders.VERTICAL_ALIGN_TOP,
                padding =
                    Padding.Builder()
                        .setTop(TEXT_TOP_PADDING_DP.toDp())
                        .setStart(TEXT_SIDE_PADDING_DP.toDp())
                        .setEnd(TEXT_SIDE_PADDING_DP.toDp())
                        .build()
            )

        /**
         * Default style variation for having content of the edge button center aligned.
         *
         * This should be used for icon-like or small, round content that doesn't occupy a lot of
         * space.
         */
        @JvmField public val DEFAULT: EdgeButtonStyle = EdgeButtonStyle()
    }
}

public object EdgeButtonDefaults {
    /**
     * [EdgeButtonColors] for the high-emphasis button representing the primary, most important or
     * most common action on a screen.
     *
     * These colors are using [ColorTokens.PRIMARY] for background color and
     * [ColorTokens.ON_PRIMARY] for content color.
     */
    public fun MaterialScope.filled(): EdgeButtonColors =
        EdgeButtonColors(getColorProp(ColorTokens.PRIMARY), getColorProp(ColorTokens.ON_PRIMARY))

    /**
     * [EdgeButtonColors] for the medium-emphasis button.
     *
     * These colors are using [ColorTokens.SURFACE_CONTAINER] for background color and
     * [ColorTokens.ON_SURFACE] for content color.
     */
    public fun MaterialScope.filledTonal(): EdgeButtonColors =
        EdgeButtonColors(
            getColorProp(ColorTokens.SURFACE_CONTAINER),
            getColorProp(ColorTokens.ON_SURFACE)
        )

    /**
     * Alternative [EdgeButtonColors] for the high-emphasis button.
     *
     * These colors are using [ColorTokens.PRIMARY_CONTAINER] for background color and
     * [ColorTokens.ON_PRIMARY_CONTAINER] for content color.
     */
    public fun MaterialScope.filledVariant(): EdgeButtonColors =
        EdgeButtonColors(
            getColorProp(ColorTokens.PRIMARY_CONTAINER),
            getColorProp(ColorTokens.ON_PRIMARY_CONTAINER)
        )

    @JvmField internal val TOP_CORNER_RADIUS: DpProp = dp(17f)
    internal const val HORIZONTAL_MARGIN_PERCENT: Float = 24f
    internal const val BOTTOM_MARGIN_DP: Int = 3
    internal const val EDGE_BUTTON_HEIGHT_DP: Int = 46
    internal const val METADATA_TAG: String = "EB"
    internal const val ICON_SIZE_DP = 24
    internal const val TEXT_TOP_PADDING_DP = 12
    internal const val TEXT_SIDE_PADDING_DP = 8
}

/** Represents the container and content colors used in [textEdgeButton] or [iconEdgeButton]. */
public class EdgeButtonColors(
    /** The container color to be used for a button. */
    public val container: ColorProp,
    /** The color or icon tint color to be used for all content within a button. */
    public val content: ColorProp
)
