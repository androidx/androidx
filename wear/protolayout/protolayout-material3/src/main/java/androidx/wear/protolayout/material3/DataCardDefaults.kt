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
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.ModifiersBuilders.Padding
import androidx.wear.protolayout.material3.TitleContentPlacementInDataCard.Companion.Bottom
import androidx.wear.protolayout.material3.TitleContentPlacementInDataCard.Companion.Top
import androidx.wear.protolayout.material3.Typography.TypographyToken
import androidx.wear.protolayout.modifiers.padding

internal object DataCardDefaults {
    /**
     * Returns [LayoutElement] describing the inner content for the data card.
     *
     * This is a [Column] containing the following:
     * * icon or secondary label, if top aligned and if present
     * * title
     * * content
     * * icon or secondary label, if bottom aligned and if present
     */
    internal fun buildContentForDataCard(
        title: LayoutElement,
        content: LayoutElement?,
        style: DataCardStyle,
        secondaryText: LayoutElement? = null,
        secondaryIcon: LayoutElement? = null,
        // Bottom, because when there's no secondaryIcon, label should be on top
        titleContentPlacement: TitleContentPlacementInDataCard = Bottom
    ): LayoutElement {
        val verticalElementBuilder: Column.Builder = Column.Builder()

        // If it's labels only, placement doesn't matter, and icon and secondaryLabel won't be
        // added.
        when (titleContentPlacement) {
            Top ->
                ContainerWithSpacersBuilder<LayoutElement>(
                        { it: LayoutElement? -> verticalElementBuilder.addContent(it!!) },
                        title
                    )
                    .addElement(content, horizontalSpacer(style.titleToContentSpaceDp))
                    .addElement(
                        secondaryIcon ?: secondaryText,
                        horizontalSpacer(
                            if (secondaryIcon != null) {
                                style.iconToTextSpaceDp
                            } else {
                                style.secondaryLabelToTextSpaceDp
                            }
                        )
                    )
            Bottom ->
                ContainerWithSpacersBuilder<LayoutElement>(
                        { it: LayoutElement? -> verticalElementBuilder.addContent(it!!) },
                        secondaryIcon ?: secondaryText,
                    )
                    .addElement(
                        title,
                        horizontalSpacer(
                            if (secondaryIcon != null) {
                                style.iconToTextSpaceDp
                            } else {
                                style.secondaryLabelToTextSpaceDp
                            }
                        )
                    )
                    .addElement(content, horizontalSpacer(style.titleToContentSpaceDp))
        }
        return verticalElementBuilder.build()
    }
}

/**
 * Defines the placement of the `title` and `content` slots in [iconDataCard], relative to other
 * optional slots in that type of card.
 */
@JvmInline
public value class TitleContentPlacementInDataCard private constructor(internal val value: Int) {
    public companion object {
        /**
         * Slots for `title` and `content` in [iconDataCard] will be placed first, and followed by
         * `icon` or `secondaryLabel` if present.
         */
        public val Top: TitleContentPlacementInDataCard = TitleContentPlacementInDataCard(0)

        /**
         * Slots for `title` and `content` in [iconDataCard] will be placed last, with `icon` or
         * `secondaryLabel` above it, sif present.
         */
        public val Bottom: TitleContentPlacementInDataCard = TitleContentPlacementInDataCard(1)
    }
}

/** Provides style values for the data card component. */
public class DataCardStyle
internal constructor(
    internal val innerPadding: Padding,
    @Dimension(unit = DP) internal val titleToContentSpaceDp: Int,
    @TypographyToken internal val titleTypography: Int,
    @TypographyToken internal val contentTypography: Int,
    @TypographyToken internal val secondaryLabelTypography: Int,
    @Dimension(unit = DP) internal val iconSize: Int,
    @Dimension(unit = DP) internal val iconToTextSpaceDp: Int = 6,
    @Dimension(unit = DP) internal val secondaryLabelToTextSpaceDp: Int = 8
) {
    public companion object {
        /** The default spacer width or height that should be between different elements. */
        @Dimension(unit = DP) private const val DEFAULT_SPACE_DP: Int = 4

        /** The default smaller spacer width or height that should be between different elements. */
        @Dimension(unit = DP) private const val SMALL_SPACE_DP: Int = 2

        /** The default no spacing width or height that should be between different elements. */
        @Dimension(unit = DP) private const val EMPTY_SPACE_DP: Int = 2

        @Dimension(unit = DP) private const val ICON_SIZE_SMALL_DP: Int = 26

        @Dimension(unit = DP) private const val ICON_SIZE_LARGE_DP: Int = 32

        @Dimension(unit = DP) private const val PADDING_SMALL_DP = 8f

        @Dimension(unit = DP) private const val PADDING_DEFAULT_DP = 10f

        @Dimension(unit = DP) private const val PADDING_LARGE_DP = 14f

        @Dimension(unit = DP) private const val PADDING_EXTRA_LARGE_DP = 16f

        /**
         * Default style variation for the [iconDataCard] or [textDataCard] where all opinionated
         * inner content is displayed in a small size.
         */
        public fun smallDataCardStyle(): DataCardStyle =
            DataCardStyle(
                innerPadding = padding(PADDING_SMALL_DP),
                titleToContentSpaceDp = SMALL_SPACE_DP,
                titleTypography = Typography.LABEL_MEDIUM,
                contentTypography = Typography.BODY_SMALL,
                secondaryLabelTypography = Typography.BODY_MEDIUM,
                iconSize = ICON_SIZE_SMALL_DP
            )

        /**
         * Default style variation for the [iconDataCard] or [textDataCard] where all opinionated
         * inner content is displayed in a medium size.
         */
        public fun defaultDataCardStyle(): DataCardStyle =
            DataCardStyle(
                innerPadding = padding(PADDING_DEFAULT_DP),
                titleToContentSpaceDp = SMALL_SPACE_DP,
                titleTypography = Typography.LABEL_LARGE,
                contentTypography = Typography.BODY_SMALL,
                secondaryLabelTypography = Typography.BODY_MEDIUM,
                iconSize = ICON_SIZE_LARGE_DP
            )

        /**
         * Default style variation for the [iconDataCard] or [textDataCard] where all opinionated
         * inner content is displayed in a large size.
         */
        public fun largeDataCardStyle(): DataCardStyle =
            DataCardStyle(
                innerPadding = padding(PADDING_DEFAULT_DP),
                titleToContentSpaceDp = EMPTY_SPACE_DP,
                titleTypography = Typography.DISPLAY_SMALL,
                contentTypography = Typography.BODY_SMALL,
                secondaryLabelTypography = Typography.BODY_MEDIUM,
                iconSize = ICON_SIZE_LARGE_DP
            )

        /**
         * Default style variation for the [iconDataCard] or [textDataCard] where all opinionated
         * inner content is displayed in an extra large size.
         */
        public fun extraLargeDataCardStyle(): DataCardStyle =
            DataCardStyle(
                innerPadding =
                    padding(horizontal = PADDING_DEFAULT_DP, vertical = PADDING_EXTRA_LARGE_DP),
                titleToContentSpaceDp = EMPTY_SPACE_DP,
                titleTypography = Typography.DISPLAY_MEDIUM,
                contentTypography = Typography.BODY_SMALL,
                secondaryLabelTypography = Typography.BODY_MEDIUM,
                iconSize = ICON_SIZE_LARGE_DP
            )

        /**
         * Default style variation for the [iconDataCard] or [textDataCard] where all opinionated
         * inner content is displayed in a small size. This should be used when [iconDataCard] or
         * [textDataCard] only has `title` and `content`.
         */
        public fun smallCompactDataCardStyle(): DataCardStyle =
            DataCardStyle(
                innerPadding = padding(horizontal = PADDING_LARGE_DP, vertical = PADDING_SMALL_DP),
                titleToContentSpaceDp = DEFAULT_SPACE_DP,
                titleTypography = Typography.NUMERAL_MEDIUM,
                contentTypography = Typography.LABEL_MEDIUM,
                secondaryLabelTypography = Typography.BODY_MEDIUM,
                iconSize = EMPTY_SPACE_DP
            )

        /**
         * Default style variation for the [iconDataCard] or [textDataCard] where all opinionated
         * inner content is displayed in a medium size. This should be used when [iconDataCard] or
         * [textDataCard] only has `title` and `content`.
         */
        public fun defaultCompactDataCardStyle(): DataCardStyle =
            DataCardStyle(
                innerPadding = padding(horizontal = PADDING_LARGE_DP, vertical = PADDING_SMALL_DP),
                titleToContentSpaceDp = EMPTY_SPACE_DP,
                titleTypography = Typography.NUMERAL_LARGE,
                contentTypography = Typography.LABEL_LARGE,
                secondaryLabelTypography = Typography.BODY_MEDIUM,
                iconSize = EMPTY_SPACE_DP
            )

        /**
         * Default style variation for the [iconDataCard] or [textDataCard] where all opinionated
         * inner content is displayed in a large size. This should be used when [iconDataCard] or
         * [textDataCard] only has `title` and `content`.
         */
        public fun largeCompactDataCardStyle(): DataCardStyle =
            DataCardStyle(
                innerPadding = padding(horizontal = PADDING_LARGE_DP, vertical = PADDING_SMALL_DP),
                titleToContentSpaceDp = EMPTY_SPACE_DP,
                titleTypography = Typography.NUMERAL_EXTRA_LARGE,
                contentTypography = Typography.LABEL_LARGE,
                secondaryLabelTypography = Typography.BODY_MEDIUM,
                iconSize = EMPTY_SPACE_DP
            )
    }
}
