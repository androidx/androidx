/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.material3

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.tokens.ListTokens
import androidx.compose.material3.tokens.TypographyKeyTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import kotlin.math.max

/**
 * <a href="https://m3.material.io/components/lists/overview" class="external" target="_blank">Material Design list item.</a>
 *
 * Lists are continuous, vertical indexes of text or images.
 *
 * ![Lists image](https://developer.android.com/images/reference/androidx/compose/material3/lists.png)
 *
 * This component can be used to achieve the list item templates existing in the spec. One-line list
 * items have a singular line of headline content. Two-line list items additionally have either
 * supporting or overline content. Three-line list items have either both supporting and overline
 * content, or extended (two-line) supporting text. For example:
 * - one-line item
 * @sample androidx.compose.material3.samples.OneLineListItem
 * - two-line item
 * @sample androidx.compose.material3.samples.TwoLineListItem
 * - three-line item with both overline and supporting content
 * @sample androidx.compose.material3.samples.ThreeLineListItemWithOverlineAndSupporting
 * - three-line item with extended supporting content
 * @sample androidx.compose.material3.samples.ThreeLineListItemWithExtendedSupporting
 *
 * @param headlineContent the headline content of the list item
 * @param modifier [Modifier] to be applied to the list item
 * @param overlineContent the content displayed above the headline content
 * @param supportingContent the supporting content of the list item
 * @param leadingContent the leading content of the list item
 * @param trailingContent the trailing meta text, icon, switch or checkbox
 * @param colors [ListItemColors] that will be used to resolve the background and content color for
 * this list item in different states. See [ListItemDefaults.colors]
 * @param tonalElevation the tonal elevation of this list item
 * @param shadowElevation the shadow elevation of this list item
 */
@Composable
fun ListItem(
    headlineContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    colors: ListItemColors = ListItemDefaults.colors(),
    tonalElevation: Dp = ListItemDefaults.Elevation,
    shadowElevation: Dp = ListItemDefaults.Elevation,
) {
    val decoratedHeadlineContent: @Composable () -> Unit = {
        ProvideTextStyleFromToken(
            colors.headlineColor(enabled = true).value,
            ListTokens.ListItemLabelTextFont,
            headlineContent
        )
    }
    val decoratedSupportingContent: @Composable (() -> Unit)? = supportingContent?.let {
        {
            ProvideTextStyleFromToken(
                colors.supportingColor().value,
                ListTokens.ListItemSupportingTextFont,
                it
            )
        }
    }
    val decoratedOverlineContent: @Composable (() -> Unit)? = overlineContent?.let {
        {
            ProvideTextStyleFromToken(
                colors.overlineColor().value,
                ListTokens.ListItemOverlineFont,
                it
            )
        }
    }
    val decoratedLeadingContent: @Composable (() -> Unit)? = leadingContent?.let {
        {
            Box(Modifier.padding(end = LeadingContentEndPadding)) {
                CompositionLocalProvider(
                    LocalContentColor provides colors.leadingIconColor(enabled = true).value,
                    content = it
                )
            }
        }
    }
    val decoratedTrailingContent: @Composable (() -> Unit)? = trailingContent?.let {
        {
            Box(Modifier.padding(start = TrailingContentStartPadding)) {
                ProvideTextStyleFromToken(
                    colors.trailingIconColor(enabled = true).value,
                    ListTokens.ListItemTrailingSupportingTextFont,
                    content = it
                )
            }
        }
    }

    Surface(
        modifier = Modifier.semantics(mergeDescendants = true) {}.then(modifier),
        shape = ListItemDefaults.shape,
        color = colors.containerColor().value,
        contentColor = colors.headlineColor(enabled = true).value,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
    ) {
        ListItemLayout(
            headline = decoratedHeadlineContent,
            overline = decoratedOverlineContent,
            supporting = decoratedSupportingContent,
            leading = decoratedLeadingContent,
            trailing = decoratedTrailingContent,
        )
    }
}

@Composable
private fun ListItemLayout(
    leading: @Composable (() -> Unit)?,
    trailing: @Composable (() -> Unit)?,
    headline: @Composable () -> Unit,
    overline: @Composable (() -> Unit)?,
    supporting: @Composable (() -> Unit)?,
) {
    val layoutDirection = LocalLayoutDirection.current
    Layout(
        contents = listOf(
            headline,
            overline ?: {},
            supporting ?: {},
            leading ?: {},
            trailing ?: {},
        )
    ) { measurables, constraints ->
        val (headlineMeasurable, overlineMeasurable, supportingMeasurable, leadingMeasurable,
            trailingMeasurable) = measurables
        var currentTotalWidth = 0

        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
            .offset(
                horizontal = -(ListItemStartPadding + ListItemEndPadding).roundToPx(),
                vertical = -(ListItemVerticalPadding * 2).roundToPx()
            )

        val leadingPlaceable = leadingMeasurable.firstOrNull()?.measure(looseConstraints)
        currentTotalWidth += widthOrZero(leadingPlaceable)

        val trailingPlaceable = trailingMeasurable.firstOrNull()?.measure(
            looseConstraints.offset(
                horizontal = -currentTotalWidth
            ))
        currentTotalWidth += widthOrZero(trailingPlaceable)

        var currentTotalHeight = 0

        val headlinePlaceable = headlineMeasurable.first().measure(
            looseConstraints.offset(
                horizontal = -currentTotalWidth
            ))
        currentTotalHeight += headlinePlaceable.height

        val supportingPlaceable = supportingMeasurable.firstOrNull()?.measure(
            looseConstraints.offset(
                horizontal = -currentTotalWidth,
                vertical = -currentTotalHeight
            ))
        currentTotalHeight += heightOrZero(supportingPlaceable)
        val isSupportingMultiline = supportingPlaceable != null &&
            (supportingPlaceable[FirstBaseline] != supportingPlaceable[LastBaseline])

        val overlinePlaceable = overlineMeasurable.firstOrNull()?.measure(
            looseConstraints.offset(
                horizontal = -currentTotalWidth,
                vertical = -currentTotalHeight
            ))

        val listItemType = ListItemType.getListItemType(
            hasOverline = overlinePlaceable != null,
            hasSupporting = supportingPlaceable != null,
            isSupportingMultiline = isSupportingMultiline
        )
        val isThreeLine = listItemType == ListItemType.ThreeLine

        val paddingValues = PaddingValues(
            start = ListItemStartPadding,
            end = ListItemEndPadding,
            top = if (isThreeLine) ListItemThreeLineVerticalPadding else ListItemVerticalPadding,
            bottom = if (isThreeLine) ListItemThreeLineVerticalPadding else ListItemVerticalPadding,
        )

        val width = calculateWidth(
            leadingPlaceable = leadingPlaceable,
            trailingPlaceable = trailingPlaceable,
            headlinePlaceable = headlinePlaceable,
            overlinePlaceable = overlinePlaceable,
            supportingPlaceable = supportingPlaceable,
            paddingValues = paddingValues,
            layoutDirection = layoutDirection,
            constraints = constraints,
        )
        val height = calculateHeight(
            leadingPlaceable = leadingPlaceable,
            trailingPlaceable = trailingPlaceable,
            headlinePlaceable = headlinePlaceable,
            overlinePlaceable = overlinePlaceable,
            supportingPlaceable = supportingPlaceable,
            listItemType = listItemType,
            paddingValues = paddingValues,
            constraints = constraints,
        )

        place(
            width = width,
            height = height,
            leadingPlaceable = leadingPlaceable,
            trailingPlaceable = trailingPlaceable,
            headlinePlaceable = headlinePlaceable,
            overlinePlaceable = overlinePlaceable,
            supportingPlaceable = supportingPlaceable,
            isThreeLine = isThreeLine,
            layoutDirection = layoutDirection,
            paddingValues = paddingValues,
        )
    }
}

private fun MeasureScope.calculateWidth(
    leadingPlaceable: Placeable?,
    trailingPlaceable: Placeable?,
    headlinePlaceable: Placeable,
    overlinePlaceable: Placeable?,
    supportingPlaceable: Placeable?,
    layoutDirection: LayoutDirection,
    paddingValues: PaddingValues,
    constraints: Constraints,
): Int {
    if (constraints.hasBoundedWidth) {
        return constraints.maxWidth
    }
    // Fallback behavior if width constraints are infinite
    val horizontalPadding = (paddingValues.calculateLeftPadding(layoutDirection) +
        paddingValues.calculateRightPadding(layoutDirection)).roundToPx()
    val mainContentWidth = maxOf(
        headlinePlaceable.width,
        widthOrZero(overlinePlaceable),
        widthOrZero(supportingPlaceable),
    )
    return horizontalPadding +
        widthOrZero(leadingPlaceable) +
        mainContentWidth +
        widthOrZero(trailingPlaceable)
}

private fun MeasureScope.calculateHeight(
    leadingPlaceable: Placeable?,
    trailingPlaceable: Placeable?,
    headlinePlaceable: Placeable,
    overlinePlaceable: Placeable?,
    supportingPlaceable: Placeable?,
    listItemType: ListItemType,
    paddingValues: PaddingValues,
    constraints: Constraints,
): Int {
    val defaultMinHeight = when (listItemType) {
        ListItemType.OneLine -> ListTokens.ListItemOneLineContainerHeight
        ListItemType.TwoLine -> ListTokens.ListItemTwoLineContainerHeight
        else /* ListItemType.ThreeLine */ -> ListTokens.ListItemThreeLineContainerHeight
    }
    val minHeight = max(constraints.minHeight, defaultMinHeight.roundToPx())

    val verticalPadding =
        paddingValues.calculateTopPadding() + paddingValues.calculateBottomPadding()

    val mainContentHeight = headlinePlaceable.height +
        heightOrZero(overlinePlaceable) +
        heightOrZero(supportingPlaceable)

    return max(
        minHeight,
        verticalPadding.roundToPx() + maxOf(
            heightOrZero(leadingPlaceable),
            mainContentHeight,
            heightOrZero(trailingPlaceable),
        )
    ).coerceAtMost(constraints.maxHeight)
}

private fun MeasureScope.place(
    width: Int,
    height: Int,
    leadingPlaceable: Placeable?,
    trailingPlaceable: Placeable?,
    headlinePlaceable: Placeable,
    overlinePlaceable: Placeable?,
    supportingPlaceable: Placeable?,
    isThreeLine: Boolean,
    layoutDirection: LayoutDirection,
    paddingValues: PaddingValues,
): MeasureResult {
    return layout(width, height) {
        val startPadding = paddingValues.calculateStartPadding(layoutDirection).roundToPx()
        val endPadding = paddingValues.calculateEndPadding(layoutDirection).roundToPx()
        val topPadding = paddingValues.calculateTopPadding().roundToPx()

        leadingPlaceable?.let {
            it.placeRelative(
                x = startPadding,
                y = if (isThreeLine) topPadding else CenterVertically.align(it.height, height)
            )
        }
        trailingPlaceable?.let {
            it.placeRelative(
                x = width - endPadding - it.width,
                y = if (isThreeLine) topPadding else CenterVertically.align(it.height, height)
            )
        }

        val mainContentX = startPadding + widthOrZero(leadingPlaceable)
        val mainContentY = if (isThreeLine) { topPadding } else {
            val totalHeight = headlinePlaceable.height + heightOrZero(overlinePlaceable) +
                heightOrZero(supportingPlaceable)
            CenterVertically.align(totalHeight, height)
        }
        var currentY = mainContentY

        overlinePlaceable?.placeRelative(mainContentX, currentY)
        currentY += heightOrZero(overlinePlaceable)

        headlinePlaceable.placeRelative(mainContentX, currentY)
        currentY += headlinePlaceable.height

        supportingPlaceable?.placeRelative(mainContentX, currentY)
    }
}

/**
 * Contains the default values used by list items.
 */
object ListItemDefaults {
    /** The default elevation of a list item */
    val Elevation: Dp = ListTokens.ListItemContainerElevation

    /** The default shape of a list item */
    val shape: Shape
        @Composable
        @ReadOnlyComposable get() = ListTokens.ListItemContainerShape.toShape()

    /** The container color of a list item */
    val containerColor: Color
        @Composable
        @ReadOnlyComposable get() = ListTokens.ListItemContainerColor.toColor()

    /** The content color of a list item */
    val contentColor: Color
        @Composable
        @ReadOnlyComposable get() = ListTokens.ListItemLabelTextColor.toColor()

    /**
     * Creates a [ListItemColors] that represents the default container and content colors used in a
     * [ListItem].
     *
     * @param containerColor the container color of this list item when enabled.
     * @param headlineColor the headline text content color of this list item when
     * enabled.
     * @param leadingIconColor the color of this list item's leading content when enabled.
     * @param overlineColor the overline text color of this list item
     * @param supportingColor the supporting text color of this list item
     * @param trailingIconColor the color of this list item's trailing content when enabled.
     * @param disabledHeadlineColor the content color of this list item when not enabled.
     * @param disabledLeadingIconColor the color of this list item's leading content when not
     * enabled.
     * @param disabledTrailingIconColor the color of this list item's trailing content when not
     * enabled.
     */
    @Composable
    fun colors(
        containerColor: Color = ListTokens.ListItemContainerColor.toColor(),
        headlineColor: Color = ListTokens.ListItemLabelTextColor.toColor(),
        leadingIconColor: Color = ListTokens.ListItemLeadingIconColor.toColor(),
        overlineColor: Color = ListTokens.ListItemOverlineColor.toColor(),
        supportingColor: Color = ListTokens.ListItemSupportingTextColor.toColor(),
        trailingIconColor: Color = ListTokens.ListItemTrailingIconColor.toColor(),
        disabledHeadlineColor: Color = ListTokens.ListItemDisabledLabelTextColor.toColor()
            .copy(alpha = ListTokens.ListItemDisabledLabelTextOpacity),
        disabledLeadingIconColor: Color = ListTokens.ListItemDisabledLeadingIconColor.toColor()
            .copy(alpha = ListTokens.ListItemDisabledLeadingIconOpacity),
        disabledTrailingIconColor: Color = ListTokens.ListItemDisabledTrailingIconColor.toColor()
            .copy(alpha = ListTokens.ListItemDisabledTrailingIconOpacity)
    ): ListItemColors =
        ListItemColors(
            containerColor = containerColor,
            headlineColor = headlineColor,
            leadingIconColor = leadingIconColor,
            overlineColor = overlineColor,
            supportingTextColor = supportingColor,
            trailingIconColor = trailingIconColor,
            disabledHeadlineColor = disabledHeadlineColor,
            disabledLeadingIconColor = disabledLeadingIconColor,
            disabledTrailingIconColor = disabledTrailingIconColor,
        )
}

/**
 * Represents the container and content colors used in a list item in different states.
 *
 * @constructor create an instance with arbitrary colors.
 * See [ListItemDefaults.colors] for the default colors used in a [ListItem].
 *
 * @param containerColor the container color of this list item when enabled.
 * @param headlineColor the headline text content color of this list item when
 * enabled.
 * @param leadingIconColor the color of this list item's leading content when enabled.
 * @param overlineColor the overline text color of this list item
 * @param supportingTextColor the supporting text color of this list item
 * @param trailingIconColor the color of this list item's trailing content when enabled.
 * @param disabledHeadlineColor the content color of this list item when not enabled.
 * @param disabledLeadingIconColor the color of this list item's leading content when not
 * enabled.
 * @param disabledTrailingIconColor the color of this list item's trailing content when not
 * enabled.
 */
@Immutable
class ListItemColors constructor(
    val containerColor: Color,
    val headlineColor: Color,
    val leadingIconColor: Color,
    val overlineColor: Color,
    val supportingTextColor: Color,
    val trailingIconColor: Color,
    val disabledHeadlineColor: Color,
    val disabledLeadingIconColor: Color,
    val disabledTrailingIconColor: Color,
) {
    /** The container color of this [ListItem] based on enabled state */
    @Composable
    internal fun containerColor(): State<Color> {
        return rememberUpdatedState(containerColor)
    }

    /** The color of this [ListItem]'s headline text based on enabled state */
    @Composable
    internal fun headlineColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) headlineColor else disabledHeadlineColor
        )
    }

    /** The color of this [ListItem]'s leading content based on enabled state */
    @Composable
    internal fun leadingIconColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) leadingIconColor else disabledLeadingIconColor
        )
    }

    /** The color of this [ListItem]'s overline text based on enabled state */
    @Composable
    internal fun overlineColor(): State<Color> {
        return rememberUpdatedState(overlineColor)
    }

    /** The color of this [ListItem]'s supporting text based on enabled state */
    @Composable
    internal fun supportingColor(): State<Color> {
        return rememberUpdatedState(supportingTextColor)
    }

    /** The color of this [ListItem]'s trailing content based on enabled state */
    @Composable
    internal fun trailingIconColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) trailingIconColor else disabledTrailingIconColor
        )
    }
}

@Composable
private fun ProvideTextStyleFromToken(
    color: Color,
    textToken: TypographyKeyTokens,
    content: @Composable () -> Unit,
) {
    val textStyle = MaterialTheme.typography.fromToken(textToken)
    CompositionLocalProvider(LocalContentColor provides color) {
        ProvideTextStyle(textStyle, content)
    }
}

/**
 * Helper class to define list item type. Used for padding and sizing definition.
 */
@JvmInline
private value class ListItemType private constructor(private val lines: Int) :
    Comparable<ListItemType> {

    override operator fun compareTo(other: ListItemType) = lines.compareTo(other.lines)

    companion object {
        /** One line list item */
        val OneLine = ListItemType(1)

        /** Two line list item */
        val TwoLine = ListItemType(2)

        /** Three line list item */
        val ThreeLine = ListItemType(3)

        internal fun getListItemType(
            hasOverline: Boolean,
            hasSupporting: Boolean,
            isSupportingMultiline: Boolean
        ): ListItemType {
            return when {
                (hasOverline && hasSupporting) || isSupportingMultiline -> ThreeLine
                hasOverline || hasSupporting -> TwoLine
                else -> OneLine
            }
        }
    }
}

// Container related defaults
// TODO: Make sure these values stay up to date until replaced with tokens.
@VisibleForTesting
internal val ListItemVerticalPadding = 8.dp
@VisibleForTesting
internal val ListItemThreeLineVerticalPadding = 12.dp
@VisibleForTesting
internal val ListItemStartPadding = 16.dp
@VisibleForTesting
internal val ListItemEndPadding = 24.dp

// Icon related defaults.
// TODO: Make sure these values stay up to date until replaced with tokens.
@VisibleForTesting
internal val LeadingContentEndPadding = 16.dp

// Trailing related defaults.
// TODO: Make sure these values stay up to date until replaced with tokens.
@VisibleForTesting
internal val TrailingContentStartPadding = 16.dp
