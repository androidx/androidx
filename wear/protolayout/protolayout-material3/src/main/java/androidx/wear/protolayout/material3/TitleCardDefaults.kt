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
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.HorizontalAlignment
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.LayoutElementBuilders.Row
import androidx.wear.protolayout.ModifiersBuilders.Padding
import androidx.wear.protolayout.material3.Typography.TypographyToken
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

public object TitleCardDefaults {
    /**
     * Returns [LayoutElement] describing the inner content for the title card.
     *
     * This is a [Column] containing the following:
     * * header slot
     *     * title
     *     * spacing if time is present
     *     * time, if present
     * * spacing if content is present
     * * content, if present.
     */
    internal fun buildContentForTitleCard(
        title: LayoutElement,
        content: LayoutElement?,
        time: LayoutElement?,
        @HorizontalAlignment horizontalAlignment: Int,
        style: TitleCardStyle,
    ): LayoutElement {
        val verticalElementBuilder: Column.Builder =
            Column.Builder().setWidth(expand()).setHorizontalAlignment(horizontalAlignment)

        val headerSlot: Row.Builder =
            Row.Builder()
                .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_TOP)
                .setWidth(expand())

        val headerBuilder: ContainerWithSpacersBuilder<LayoutElement> =
            ContainerWithSpacersBuilder<LayoutElement>(
                    headerSlot::addContent,
                    Optional.ofNullable<LayoutElement>(title)
                        .map { element: LayoutElement? ->
                            Box.Builder()
                                .addContent(element!!)
                                .setHorizontalAlignment(horizontalAlignment)
                                .setWidth(expand())
                                .build()
                        }
                        .getOrNull(),
                )
                .addElement(time, verticalSpacer(style.titleToTimeSpaceDp))

        ContainerWithSpacersBuilder<LayoutElement>(
                { element: LayoutElement? -> verticalElementBuilder.addContent(element!!) },
                if (headerBuilder.isEmpty) null else headerSlot.build(),
            )
            .addElement(content, horizontalSpacer(style.titleToContentSpaceDp))

        return verticalElementBuilder.build()
    }
}

/** Provides style values for the title card component. */
public class TitleCardStyle
internal constructor(
    internal val innerPadding: Padding,
    @Dimension(unit = DP) internal val titleToTimeSpaceDp: Int,
    @Dimension(unit = DP) internal val titleToContentSpaceDp: Int,
    @TypographyToken internal val titleTypography: Int,
    @TypographyToken internal val contentTypography: Int,
    @TypographyToken internal val timeTypography: Int,
) {
    public companion object {
        /** The small spacer width or height that should be between different elements. */
        @Dimension(unit = DP) private const val SMALL_SPACE_DP: Int = 2
        /** The mid spacer width or height that should be between different elements. */
        @Dimension(unit = DP) private const val MID_SPACE_DP: Int = 3
        /** The default spacer width or height that should be between different elements. */
        @Dimension(unit = DP) private const val DEFAULT_SPACE_DP: Int = 4

        /**
         * Default style variation for the [titleCard] where all opinionated inner content is
         * displayed in a small size.
         */
        public fun smallTitleCardStyle(): TitleCardStyle =
            TitleCardStyle(
                innerPadding =
                    Padding.Builder()
                        .setTop(4.toDp())
                        .setBottom(4.toDp())
                        .setStart(14.toDp())
                        .setEnd(14.toDp())
                        .build(),
                titleToTimeSpaceDp = DEFAULT_SPACE_DP,
                titleToContentSpaceDp = SMALL_SPACE_DP,
                titleTypography = Typography.LABEL_SMALL,
                contentTypography = Typography.BODY_SMALL,
                timeTypography = Typography.BODY_SMALL,
            )

        /**
         * Default style variation for the [titleCard] where all opinionated inner content is
         * displayed in a medium size.
         */
        public fun defaultTitleCardStyle(): TitleCardStyle =
            TitleCardStyle(
                innerPadding =
                    Padding.Builder()
                        .setTop(10.toDp())
                        .setBottom(10.toDp())
                        .setStart(14.toDp())
                        .setEnd(14.toDp())
                        .build(),
                titleToTimeSpaceDp = DEFAULT_SPACE_DP,
                titleToContentSpaceDp = SMALL_SPACE_DP,
                titleTypography = Typography.LABEL_MEDIUM,
                contentTypography = Typography.LABEL_SMALL,
                timeTypography = Typography.BODY_MEDIUM,
            )

        /**
         * Default style variation for the [titleCard] where all opinionated inner content is
         * displayed in a large size.
         */
        public fun largeTitleCardStyle(): TitleCardStyle =
            TitleCardStyle(
                innerPadding =
                    Padding.Builder()
                        .setTop(10.toDp())
                        .setBottom(10.toDp())
                        .setStart(14.toDp())
                        .setEnd(14.toDp())
                        .build(),
                titleToTimeSpaceDp = DEFAULT_SPACE_DP,
                titleToContentSpaceDp = MID_SPACE_DP,
                titleTypography = Typography.TITLE_MEDIUM,
                contentTypography = Typography.LABEL_SMALL,
                timeTypography = Typography.BODY_MEDIUM,
            )

        /**
         * Default style variation for the [titleCard] where all opinionated inner content is
         * displayed in an extra large size.
         */
        public fun extraLargeTitleCardStyle(): TitleCardStyle =
            TitleCardStyle(
                innerPadding =
                    Padding.Builder()
                        .setTop(12.toDp())
                        .setBottom(12.toDp())
                        .setStart(14.toDp())
                        .setEnd(14.toDp())
                        .build(),
                titleToTimeSpaceDp = DEFAULT_SPACE_DP,
                titleToContentSpaceDp = DEFAULT_SPACE_DP,
                titleTypography = Typography.LABEL_LARGE,
                contentTypography = Typography.LABEL_SMALL,
                timeTypography = Typography.BODY_MEDIUM,
            )
    }
}
