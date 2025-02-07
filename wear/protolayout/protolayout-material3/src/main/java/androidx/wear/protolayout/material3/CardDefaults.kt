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

import androidx.wear.protolayout.material3.CircularProgressIndicatorDefaults.filledProgressIndicatorColors
import androidx.wear.protolayout.material3.CircularProgressIndicatorDefaults.filledTonalProgressIndicatorColors
import androidx.wear.protolayout.material3.CircularProgressIndicatorDefaults.filledVariantProgressIndicatorColors
import androidx.wear.protolayout.types.LayoutColor

/**
 * Represents colors used in card components, such as [titleCard] or [appCard].
 *
 * @param backgroundColor [LayoutColor] which is used to as the background color for the card.
 * @param titleColor the color used for title for the card.
 * @param contentColor the content color for the card.
 * @param timeColor the color used for time for the card.
 * @param labelColor the color used for label for the card.
 * @param secondaryIconColor the color used for icon in the data card type.
 * @param secondaryTextColor the color used for secondary label for the data card type.
 * @param graphicProgressIndicatorColors the color used for the progress indicator set as graphic of
 *   the card. If null, uses the default color defined in [circularProgressIndicator] and
 *   [segmentedCircularProgressIndicator], which is [filledProgressIndicatorColors].
 * @param graphicIconColor the color used for the icon to be put at the center of the progress
 *   indicator to compose the graphic. If null, uses the default icon color, which is
 *   [ColorScheme.primary].
 */
public class CardColors(
    public val backgroundColor: LayoutColor,
    public val titleColor: LayoutColor,
    public val contentColor: LayoutColor,
    public val timeColor: LayoutColor = contentColor,
    public val labelColor: LayoutColor = titleColor,
    public val secondaryIconColor: LayoutColor = titleColor,
    public val secondaryTextColor: LayoutColor = timeColor,
    public val graphicProgressIndicatorColors: ProgressIndicatorColors? = null,
    public val graphicIconColor: LayoutColor? = null
)

public object CardDefaults {
    /**
     * [CardColors] for the high-emphasis card representing the primary, most important or most
     * common action on a screen.
     *
     * These colors are using [ColorScheme.primary] for background color and [ColorScheme.onPrimary]
     * for content colors.
     */
    public fun MaterialScope.filledCardColors(): CardColors =
        CardColors(
            backgroundColor = theme.colorScheme.primary,
            titleColor = theme.colorScheme.onPrimary,
            contentColor = theme.colorScheme.onPrimary.withOpacity(0.8f),
            graphicProgressIndicatorColors = filledProgressIndicatorColors(),
            graphicIconColor = theme.colorScheme.primaryContainer
        )

    /**
     * [CardColors] for the medium-emphasis card.
     *
     * These colors are using [ColorScheme.surfaceContainer] for background color and
     * [ColorScheme.onSurface] and [ColorScheme.onSurfaceVariant] for content colors.
     */
    public fun MaterialScope.filledTonalCardColors(): CardColors =
        CardColors(
            backgroundColor = theme.colorScheme.surfaceContainer,
            titleColor = theme.colorScheme.onSurface,
            contentColor = theme.colorScheme.onSurfaceVariant,
            secondaryIconColor = theme.colorScheme.primary,
            graphicProgressIndicatorColors = filledTonalProgressIndicatorColors(),
            graphicIconColor = theme.colorScheme.primaryDim
        )

    /**
     * Alternative [CardColors] for the high-emphasis card.
     *
     * These colors are using [ColorScheme.primaryContainer] for background color and
     * [ColorScheme.primaryContainer] for content colors.
     */
    public fun MaterialScope.filledVariantCardColors(): CardColors =
        CardColors(
            backgroundColor = theme.colorScheme.primaryContainer,
            titleColor = theme.colorScheme.onPrimaryContainer,
            contentColor = theme.colorScheme.onPrimaryContainer.withOpacity(0.9f),
            graphicProgressIndicatorColors = filledVariantProgressIndicatorColors(),
            graphicIconColor = theme.colorScheme.primaryDim
        )

    /**
     * Alternative [CardColors] for the card with [backgroundImage] as a background.
     *
     * These colors are using [ColorScheme.onBackground] for content colors.
     */
    public fun MaterialScope.imageBackgroundCardColors(): CardColors =
        CardColors(
            backgroundColor = theme.colorScheme.background,
            titleColor = theme.colorScheme.onBackground,
            contentColor = theme.colorScheme.onBackground
        )

    internal const val METADATA_TAG: String = "CR"
    internal const val DEFAULT_CONTENT_PADDING = 4f
}
