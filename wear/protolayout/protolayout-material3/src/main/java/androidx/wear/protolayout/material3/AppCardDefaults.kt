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
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_START
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.LayoutElementBuilders.Row
import androidx.wear.protolayout.ModifiersBuilders.Padding
import androidx.wear.protolayout.material3.Typography.TypographyToken
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

internal object AppCardDefaults {
    /**
     * Returns [LayoutElement] describing the inner content for the app card.
     *
     * This is a [Column] containing the following:
     * * header slot
     *     * avatar, if present
     *     * spacing if label is present
     *     * label, if present
     *     * spacing if time is present
     *     * time, if present
     * * spacing if title is present
     * * title, if present.
     * * spacing if content is present
     * * content, if present.
     */
    internal fun buildContentForAppCard(
        title: LayoutElement,
        content: LayoutElement?,
        time: LayoutElement?,
        label: LayoutElement?,
        avatar: LayoutElement?,
        style: AppCardStyle,
    ): LayoutElement {
        val verticalElementBuilder: Column.Builder =
            Column.Builder().setWidth(expand()).setHorizontalAlignment(HORIZONTAL_ALIGN_START)

        val headerSlot = Row.Builder().setWidth(expand())

        val headerBuilder: ContainerWithSpacersBuilder<LayoutElement> =
            ContainerWithSpacersBuilder(headerSlot::addContent, avatar)
                .addElement(
                    Optional.ofNullable<LayoutElement>(label)
                        .map { element: LayoutElement ->
                            Box.Builder()
                                .addContent(element)
                                .setHorizontalAlignment(HORIZONTAL_ALIGN_START)
                                .setWidth(expand())
                                .build()
                        }
                        .getOrNull(),
                    verticalSpacer(style.labelToAvatarSpaceDp),
                )
                .addElement(time, verticalSpacer(style.labelToTimeSpaceDp))

        ContainerWithSpacersBuilder<LayoutElement>(
                { element: LayoutElement? -> verticalElementBuilder.addContent(element!!) },
                if (headerBuilder.isEmpty) null else headerSlot.build(),
            )
            .addElement(title, horizontalSpacer(style.headerToTitleSpaceDp))
            .addElement(content)

        return verticalElementBuilder.build()
    }
}

/** Provides style values for the app card component. */
public class AppCardStyle
internal constructor(
    internal val innerPadding: Padding,
    @Dimension(unit = DP) internal val labelToTimeSpaceDp: Int,
    @Dimension(unit = DP) internal val labelToAvatarSpaceDp: Int,
    @Dimension(unit = DP) internal val headerToTitleSpaceDp: Int,
    @TypographyToken internal val titleTypography: Int,
    @TypographyToken internal val contentTypography: Int,
    @TypographyToken internal val labelTypography: Int,
    @TypographyToken internal val timeTypography: Int,
    @Dimension(unit = DP) internal val avatarSize: Int,
) {
    public companion object {
        /** The default spacer width or height that should be between different elements. */
        @Dimension(unit = DP) private const val DEFAULT_SPACE_DP: Int = 4

        /** The default smaller spacer width or height that should be between different elements. */
        @Dimension(unit = DP) private const val SMALL_SPACE_DP: Int = 2

        /**
         * Default style variation for the [appCard] where all opinionated inner content is
         * displayed in a small size.
         */
        public fun smallAppCardStyle(): AppCardStyle =
            AppCardStyle(
                innerPadding =
                    Padding.Builder()
                        .setTop(4.toDp())
                        .setBottom(4.toDp())
                        .setStart(14.toDp())
                        .setEnd(14.toDp())
                        .build(),
                labelToTimeSpaceDp = DEFAULT_SPACE_DP,
                labelToAvatarSpaceDp = DEFAULT_SPACE_DP,
                avatarSize = 16,
                headerToTitleSpaceDp = SMALL_SPACE_DP,
                titleTypography = Typography.LABEL_SMALL,
                contentTypography = Typography.BODY_SMALL,
                timeTypography = Typography.BODY_SMALL,
                labelTypography = Typography.LABEL_SMALL,
            )

        /**
         * Default style variation for the [appCard] where all opinionated inner content is
         * displayed in a medium size.
         */
        public fun defaultAppCardStyle(): AppCardStyle =
            AppCardStyle(
                innerPadding =
                    Padding.Builder()
                        .setTop(8.toDp())
                        .setBottom(8.toDp())
                        .setStart(14.toDp())
                        .setEnd(14.toDp())
                        .build(),
                labelToTimeSpaceDp = DEFAULT_SPACE_DP,
                labelToAvatarSpaceDp = DEFAULT_SPACE_DP,
                avatarSize = 16,
                headerToTitleSpaceDp = SMALL_SPACE_DP,
                titleTypography = Typography.LABEL_MEDIUM,
                contentTypography = Typography.LABEL_SMALL,
                timeTypography = Typography.BODY_MEDIUM,
                labelTypography = Typography.TITLE_SMALL,
            )

        /**
         * Default style variation for the [appCard] where all opinionated inner content is
         * displayed in a large size.
         */
        public fun largeAppCardStyle(): AppCardStyle =
            AppCardStyle(
                innerPadding =
                    Padding.Builder()
                        .setTop(12.toDp())
                        .setBottom(12.toDp())
                        .setStart(14.toDp())
                        .setEnd(14.toDp())
                        .build(),
                labelToTimeSpaceDp = DEFAULT_SPACE_DP,
                labelToAvatarSpaceDp = DEFAULT_SPACE_DP,
                avatarSize = 18,
                headerToTitleSpaceDp = DEFAULT_SPACE_DP,
                titleTypography = Typography.TITLE_MEDIUM,
                contentTypography = Typography.LABEL_SMALL,
                timeTypography = Typography.BODY_MEDIUM,
                labelTypography = Typography.TITLE_SMALL,
            )
    }
}
