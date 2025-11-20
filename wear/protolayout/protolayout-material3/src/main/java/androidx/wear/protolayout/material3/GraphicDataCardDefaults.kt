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
import androidx.annotation.FloatRange
import androidx.wear.protolayout.DimensionBuilders.ContainerDimension
import androidx.wear.protolayout.DimensionBuilders.DpProp
import androidx.wear.protolayout.DimensionBuilders.ExpandedDimensionProp
import androidx.wear.protolayout.DimensionBuilders.ProportionalDimensionProp
import androidx.wear.protolayout.DimensionBuilders.WrappedDimensionProp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.weight
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_END
import androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_START
import androidx.wear.protolayout.LayoutElementBuilders.HorizontalAlignment
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.LayoutElementBuilders.Row
import androidx.wear.protolayout.ModifiersBuilders.Padding
import androidx.wear.protolayout.material3.GraphicDataCardDefaults.GRAPHIC_SPACE_PERCENTAGE_DEFAULT
import androidx.wear.protolayout.material3.GraphicDataCardDefaults.GRAPHIC_SPACE_PERCENTAGE_LARGE
import androidx.wear.protolayout.material3.Typography.TypographyToken
import androidx.wear.protolayout.material3.Versions.hasExpandWithWeightSupport
import androidx.wear.protolayout.modifiers.padding
import androidx.wear.protolayout.types.dp

public object GraphicDataCardDefaults {
    @FloatRange(from = 0.0, to = 100.0)
    internal const val GRAPHIC_SPACE_PERCENTAGE_DEFAULT: Float = 32f
    @FloatRange(from = 0.0, to = 100.0)
    internal const val GRAPHIC_SPACE_PERCENTAGE_LARGE: Float = 40f
    /** The default ratio of the center icon size to the progress indicator size. */
    internal const val CENTER_ICON_SIZE_RATIO_IN_GRAPHIC = 0.4F

    /**
     * Returns [LayoutElement] describing the inner content for the graph data card.
     *
     * This is a Row containing the following:
     * * graph content, 40% of the horizontal space
     * * remaining is a Column with title and content
     */
    internal fun MaterialScope.buildContentForGraphicDataCard(
        title: LayoutElement,
        content: LayoutElement?,
        graphic: LayoutElement,
        style: GraphicDataCardStyle,
        height: ContainerDimension,
        @HorizontalAlignment horizontalAlignment: Int,
    ): LayoutElement {
        val verticalElementBuilder: Column.Builder =
            Column.Builder().setWidth(expand()).setHorizontalAlignment(HORIZONTAL_ALIGN_START)
        val horizontalElementBuilder: Row.Builder =
            Row.Builder().setWidth(expand()).setHeight(height)

        ContainerWithSpacersBuilder<LayoutElement>(
                { it: LayoutElement? -> verticalElementBuilder.addContent(it!!) },
                title,
            )
            .addElement(content)

        // Side padding - start
        horizontalElementBuilder.addContent(
            verticalSpacer(
                deviceConfiguration.weightForSpacer(
                    if (horizontalAlignment == HORIZONTAL_ALIGN_START) {
                        style.graphicSidePaddingWeight
                    } else {
                        style.contentSidePaddingWeight
                    }
                )
            )
        )

        // Wrap graphic in expandable box with weights
        val wrapGraphic =
            Box.Builder()
                .setWidth(deviceConfiguration.weightForContainer(style.graphicSpacePercentage))
                .setHeight(expand())
                .addContent(graphic)
                .build()

        if (horizontalAlignment == HORIZONTAL_ALIGN_START) {
            horizontalElementBuilder.addContent(wrapGraphic)
            horizontalElementBuilder.addContent(verticalSpacer(style.graphToTitleSpaceDp))
        }

        horizontalElementBuilder.addContent(
            Box.Builder()
                .setHorizontalAlignment(HORIZONTAL_ALIGN_START)
                .setWidth(
                    weightAsExpand(
                        100 -
                            style.graphicSidePaddingWeight -
                            style.contentSidePaddingWeight -
                            style.graphicSpacePercentage
                    )
                )
                .addContent(verticalElementBuilder.build())
                .build()
        )

        if (horizontalAlignment == HORIZONTAL_ALIGN_END) {
            horizontalElementBuilder.addContent(verticalSpacer(style.graphToTitleSpaceDp))
            horizontalElementBuilder.addContent(wrapGraphic)
        }

        // Side padding - end
        horizontalElementBuilder.addContent(
            verticalSpacer(
                deviceConfiguration.weightForSpacer(
                    if (horizontalAlignment == HORIZONTAL_ALIGN_END) {
                        style.graphicSidePaddingWeight
                    } else {
                        style.contentSidePaddingWeight
                    }
                )
            )
        )

        return horizontalElementBuilder.build()
    }

    /**
     * Helper to construct a graphic content with a [mainContent] and an [iconContent] in its
     * center, that can be passed into the [graphicDataCard].
     *
     * It is highly recommended for the [mainContent] to take a [circularProgressIndicator] or
     * [segmentedCircularProgressIndicator] and keep its default size as [expand] in order to fill
     * the available space for the best results across different screen sizes. To have contrasting
     * content colors, when using this constructed graphic inside a [graphicDataCard] with one of
     * the predefined colors in [CardDefaults], it is also highly recommended to leave its
     * [ProgressIndicatorColors] to default, which will automatically match to the card colors.
     *
     * @param mainContent The main content of the graphic slot. The main content is required to be
     *   wrapped in a [Box]. This helper is specially designed to supports well with main content as
     *   [circularProgressIndicator] or [segmentedCircularProgressIndicator] to be placed in the
     *   graphics slot.
     * @param iconContent The icon to be placed at the center of the main content. It is highly
     *   recommended to provide it by calling [icon] with only the resource ID, so that the returned
     *   layout would take advantage of the default styling functions provided by this helper, with
     *   equal width and height which are proportional to the [mainContent]'s width , and matching
     *   color to the card.
     * @param iconSizeRatio The ratio of the icon size to the [mainContent]'s width.
     * @throws IllegalArgumentException When the mainContent has size of [WrappedDimensionProp].
     */
    public fun MaterialScope.constructGraphic(
        mainContent: (MaterialScope.() -> Box),
        iconContent: (MaterialScope.() -> LayoutElement),
        iconSizeRatio: Float = CENTER_ICON_SIZE_RATIO_IN_GRAPHIC,
    ): LayoutElement {
        val contentMain = mainContent()
        val size: ContainerDimension =
            when (val width = contentMain.width) {
                is DpProp -> width.value.dp
                is ExpandedDimensionProp ->
                    width.layoutWeight?.let { weightAsExpand(it.value) } ?: expand()
                is WrappedDimensionProp -> {
                    throw IllegalArgumentException("Main content with wrap size is not supported.")
                }
                else -> {
                    throw IllegalArgumentException("Unknown dimension type of ContainerDimension.")
                }
            }

        val centeredIcon: LayoutElement =
            if (
                size is ExpandedDimensionProp &&
                    deviceConfiguration.rendererSchemaVersion.hasExpandWithWeightSupport()
            ) {
                // The total weight of the row is 1.
                val spacer = verticalSpacer(weight(0.5f - iconSizeRatio / 2f))
                Row.Builder()
                    .setWidth(expand())
                    .setHeight(expand())
                    .addContent(spacer)
                    .addContent(
                        Box.Builder()
                            .setWidth(weight(iconSizeRatio))
                            .setHeight(expand())
                            .addContent(
                                withStyle(
                                    defaultIconStyle =
                                        IconStyle(
                                            width = expand(),
                                            height =
                                                ProportionalDimensionProp.Builder()
                                                    .setAspectRatioHeight(1)
                                                    .setAspectRatioWidth(1)
                                                    .build(),
                                            tintColor = defaultIconStyle.tintColor,
                                        )
                                ) {
                                    iconContent()
                                }
                            )
                            .build()
                    )
                    .addContent(spacer)
                    .build()
            } else {
                val iconSize =
                    (iconSizeRatio *
                            (if (size is DpProp) {
                                size.value
                            } else {
                                // Use approximate weight as this is a fallback so it wouldn't be
                                // 100% accurate.
                                deviceConfiguration.screenWidthDp * GRAPHIC_SPACE_PERCENTAGE_DEFAULT
                            }))
                        .dp
                withStyle(
                    defaultIconStyle =
                        IconStyle(
                            width = iconSize,
                            height = iconSize,
                            tintColor = defaultIconStyle.tintColor,
                        )
                ) {
                    iconContent()
                }
            }

        return Box.Builder()
            .setWidth(size)
            .setHeight(size)
            .addContent(contentMain)
            .addContent(centeredIcon)
            .build()
    }
}

/** Provides style values for the graphic data card component. */
public class GraphicDataCardStyle
internal constructor(
    internal val innerPadding: Padding,
    @TypographyToken internal val titleTypography: Int,
    @TypographyToken internal val contentTypography: Int,
    @Dimension(unit = DP) internal val graphToTitleSpaceDp: Int,
    internal val graphicSidePaddingWeight: Float,
    internal val contentSidePaddingWeight: Float,
    // This doesn't correspond 1 to 1 to the spec, because Figma spec doesn't have way of specifying
    // the weight instead of percentage of the screen size. The way we calculate the weight is
    // based on the spec, taking what is the width of the graph and dividing it by the total width
    // of the card in a case of default margins to get the weight number. Same applies to the
    // paddings.
    @FloatRange(from = 0.0, to = 100.0) internal val graphicSpacePercentage: Float,
) {
    public companion object {
        private const val DEFAULT_VERTICAL_PADDING_DP = 8f

        /**
         * Default style variation for the [graphicDataCard] where all opinionated inner content is
         * displayed in a medium size.
         */
        public fun defaultGraphicDataCardStyle(): GraphicDataCardStyle =
            GraphicDataCardStyle(
                innerPadding =
                    padding(
                        top = DEFAULT_VERTICAL_PADDING_DP,
                        bottom = DEFAULT_VERTICAL_PADDING_DP,
                    ),
                titleTypography = Typography.DISPLAY_SMALL,
                contentTypography = Typography.LABEL_SMALL,
                graphToTitleSpaceDp = 12,
                graphicSidePaddingWeight = 6.67f,
                contentSidePaddingWeight = 10f,
                graphicSpacePercentage = GRAPHIC_SPACE_PERCENTAGE_DEFAULT,
            )

        /**
         * Default style variation for the [graphicDataCard] where all opinionated inner content is
         * displayed in a large size.
         */
        public fun largeGraphicDataCardStyle(): GraphicDataCardStyle =
            GraphicDataCardStyle(
                innerPadding =
                    padding(
                        top = DEFAULT_VERTICAL_PADDING_DP,
                        bottom = DEFAULT_VERTICAL_PADDING_DP,
                    ),
                titleTypography = Typography.DISPLAY_MEDIUM,
                contentTypography = Typography.LABEL_MEDIUM,
                graphToTitleSpaceDp = 12,
                graphicSidePaddingWeight = 6.67f,
                contentSidePaddingWeight = 10f,
                graphicSpacePercentage = GRAPHIC_SPACE_PERCENTAGE_LARGE,
            )
    }
}
