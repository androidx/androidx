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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.internal.ProvideContentColorTextStyle
import androidx.compose.material3.internal.heightOrZero
import androidx.compose.material3.internal.subtractConstraintSafely
import androidx.compose.material3.internal.widthOrZero
import androidx.compose.material3.tokens.ListTokens
import androidx.compose.material3.tokens.TypographyKeyTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.MultiContentMeasurePolicy
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.sp
import kotlin.jvm.JvmInline
import kotlin.math.max

/**
 * <a href="https://m3.material.io/components/lists/overview" class="external"
 * target="_blank">Material Design list item.</a>
 *
 * Lists are continuous, vertical indexes of text or images.
 *
 * ![Lists
 * image](https://developer.android.com/images/reference/androidx/compose/material3/lists.png)
 *
 * This component can be used to achieve the list item templates existing in the spec. One-line list
 * items have a singular line of headline content. Two-line list items additionally have either
 * supporting or overline content. Three-line list items have either both supporting and overline
 * content, or extended (two-line) supporting text. For example:
 * - one-line item
 *
 * @sample androidx.compose.material3.samples.OneLineListItem
 * - two-line item
 *
 * @sample androidx.compose.material3.samples.TwoLineListItem
 * - three-line item with both overline and supporting content
 *
 * @sample androidx.compose.material3.samples.ThreeLineListItemWithOverlineAndSupporting
 * - three-line item with extended supporting content
 *
 * @sample androidx.compose.material3.samples.ThreeLineListItemWithExtendedSupporting
 * @param headlineContent the headline content of the list item
 * @param modifier [Modifier] to be applied to the list item
 * @param overlineContent the content displayed above the headline content
 * @param supportingContent the supporting content of the list item
 * @param leadingContent the leading content of the list item
 * @param trailingContent the trailing meta text, icon, switch or checkbox
 * @param colors [ListItemColors] that will be used to resolve the background and content color for
 *   this list item in different states. See [ListItemDefaults.colors]
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
            colors.headlineColor(enabled = true),
            ListTokens.ListItemLabelTextFont,
            headlineContent
        )
    }
    val decoratedSupportingContent: @Composable (() -> Unit)? =
        supportingContent?.let {
            {
                ProvideTextStyleFromToken(
                    colors.supportingColor(),
                    ListTokens.ListItemSupportingTextFont,
                    it
                )
            }
        }
    val decoratedOverlineContent: @Composable (() -> Unit)? =
        overlineContent?.let {
            {
                ProvideTextStyleFromToken(
                    colors.overlineColor(),
                    ListTokens.ListItemOverlineFont,
                    it
                )
            }
        }
    val decoratedLeadingContent: @Composable (() -> Unit)? =
        leadingContent?.let {
            {
                Box(Modifier.padding(end = LeadingContentEndPadding)) {
                    CompositionLocalProvider(
                        LocalContentColor provides colors.leadingIconColor(enabled = true),
                        content = it
                    )
                }
            }
        }
    val decoratedTrailingContent: @Composable (() -> Unit)? =
        trailingContent?.let {
            {
                Box(Modifier.padding(start = TrailingContentStartPadding)) {
                    ProvideTextStyleFromToken(
                        colors.trailingIconColor(enabled = true),
                        ListTokens.ListItemTrailingSupportingTextFont,
                        content = it
                    )
                }
            }
        }

    Surface(
        modifier = Modifier.semantics(mergeDescendants = true) {}.then(modifier),
        shape = ListItemDefaults.shape,
        color = colors.containerColor(),
        contentColor = colors.headlineColor(enabled = true),
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
    val measurePolicy = remember { ListItemMeasurePolicy() }
    Layout(
        contents =
            listOf(
                headline,
                overline ?: {},
                supporting ?: {},
                leading ?: {},
                trailing ?: {},
            ),
        measurePolicy = measurePolicy,
    )
}

private class ListItemMeasurePolicy : MultiContentMeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<List<Measurable>>,
        constraints: Constraints
    ): MeasureResult {
        val (
            headlineMeasurable,
            overlineMeasurable,
            supportingMeasurable,
            leadingMeasurable,
            trailingMeasurable) =
            measurables
        var currentTotalWidth = 0

        val looseConstraints = constraints.copyMaxDimensions()
        val startPadding = ListItemStartPadding
        val endPadding = ListItemEndPadding
        val horizontalPadding = (startPadding + endPadding).roundToPx()

        // ListItem layout has a cycle in its dependencies which we use
        // intrinsic measurements to break:
        // 1. Intrinsic leading/trailing width
        // 2. Intrinsic supporting height
        // 3. Intrinsic vertical padding
        // 4. Actual leading/trailing measurement
        // 5. Actual supporting measurement
        // 6. Actual vertical padding
        val intrinsicLeadingWidth =
            leadingMeasurable.firstOrNull()?.minIntrinsicWidth(constraints.maxHeight) ?: 0
        val intrinsicTrailingWidth =
            trailingMeasurable.firstOrNull()?.minIntrinsicWidth(constraints.maxHeight) ?: 0
        val intrinsicSupportingWidthConstraint =
            looseConstraints.maxWidth.subtractConstraintSafely(
                intrinsicLeadingWidth + intrinsicTrailingWidth + horizontalPadding
            )
        val intrinsicSupportingHeight =
            supportingMeasurable
                .firstOrNull()
                ?.minIntrinsicHeight(intrinsicSupportingWidthConstraint) ?: 0
        val intrinsicIsSupportingMultiline =
            isSupportingMultilineHeuristic(intrinsicSupportingHeight)
        val intrinsicListItemType =
            ListItemType(
                hasOverline = overlineMeasurable.firstOrNull() != null,
                hasSupporting = supportingMeasurable.firstOrNull() != null,
                isSupportingMultiline = intrinsicIsSupportingMultiline,
            )
        val intrinsicVerticalPadding = (verticalPadding(intrinsicListItemType) * 2).roundToPx()

        val paddedLooseConstraints =
            looseConstraints.offset(
                horizontal = -horizontalPadding,
                vertical = -intrinsicVerticalPadding,
            )

        val leadingPlaceable = leadingMeasurable.firstOrNull()?.measure(paddedLooseConstraints)
        currentTotalWidth += leadingPlaceable.widthOrZero

        val trailingPlaceable =
            trailingMeasurable
                .firstOrNull()
                ?.measure(paddedLooseConstraints.offset(horizontal = -currentTotalWidth))
        currentTotalWidth += trailingPlaceable.widthOrZero

        var currentTotalHeight = 0

        val headlinePlaceable =
            headlineMeasurable
                .firstOrNull()
                ?.measure(paddedLooseConstraints.offset(horizontal = -currentTotalWidth))
        currentTotalHeight += headlinePlaceable.heightOrZero

        val supportingPlaceable =
            supportingMeasurable
                .firstOrNull()
                ?.measure(
                    paddedLooseConstraints.offset(
                        horizontal = -currentTotalWidth,
                        vertical = -currentTotalHeight
                    )
                )
        currentTotalHeight += supportingPlaceable.heightOrZero
        val isSupportingMultiline =
            supportingPlaceable != null &&
                (supportingPlaceable[FirstBaseline] != supportingPlaceable[LastBaseline])

        val overlinePlaceable =
            overlineMeasurable
                .firstOrNull()
                ?.measure(
                    paddedLooseConstraints.offset(
                        horizontal = -currentTotalWidth,
                        vertical = -currentTotalHeight
                    )
                )

        val listItemType =
            ListItemType(
                hasOverline = overlinePlaceable != null,
                hasSupporting = supportingPlaceable != null,
                isSupportingMultiline = isSupportingMultiline,
            )
        val topPadding = verticalPadding(listItemType)
        val verticalPadding = topPadding * 2

        val width =
            calculateWidth(
                leadingWidth = leadingPlaceable.widthOrZero,
                trailingWidth = trailingPlaceable.widthOrZero,
                headlineWidth = headlinePlaceable.widthOrZero,
                overlineWidth = overlinePlaceable.widthOrZero,
                supportingWidth = supportingPlaceable.widthOrZero,
                horizontalPadding = horizontalPadding,
                constraints = constraints,
            )
        val height =
            calculateHeight(
                leadingHeight = leadingPlaceable.heightOrZero,
                trailingHeight = trailingPlaceable.heightOrZero,
                headlineHeight = headlinePlaceable.heightOrZero,
                overlineHeight = overlinePlaceable.heightOrZero,
                supportingHeight = supportingPlaceable.heightOrZero,
                listItemType = listItemType,
                verticalPadding = verticalPadding.roundToPx(),
                constraints = constraints,
            )

        return place(
            width = width,
            height = height,
            leadingPlaceable = leadingPlaceable,
            trailingPlaceable = trailingPlaceable,
            headlinePlaceable = headlinePlaceable,
            overlinePlaceable = overlinePlaceable,
            supportingPlaceable = supportingPlaceable,
            isThreeLine = listItemType == ListItemType.ThreeLine,
            startPadding = startPadding.roundToPx(),
            endPadding = endPadding.roundToPx(),
            topPadding = topPadding.roundToPx(),
        )
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurables: List<List<IntrinsicMeasurable>>,
        width: Int
    ): Int = calculateIntrinsicHeight(measurables, width, IntrinsicMeasurable::maxIntrinsicHeight)

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurables: List<List<IntrinsicMeasurable>>,
        height: Int
    ): Int = calculateIntrinsicWidth(measurables, height, IntrinsicMeasurable::maxIntrinsicWidth)

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurables: List<List<IntrinsicMeasurable>>,
        width: Int
    ): Int = calculateIntrinsicHeight(measurables, width, IntrinsicMeasurable::minIntrinsicHeight)

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurables: List<List<IntrinsicMeasurable>>,
        height: Int
    ): Int = calculateIntrinsicWidth(measurables, height, IntrinsicMeasurable::minIntrinsicWidth)

    private fun IntrinsicMeasureScope.calculateIntrinsicWidth(
        measurables: List<List<IntrinsicMeasurable>>,
        height: Int,
        intrinsicMeasure: IntrinsicMeasurable.(height: Int) -> Int,
    ): Int {
        val (
            headlineMeasurable,
            overlineMeasurable,
            supportingMeasurable,
            leadingMeasurable,
            trailingMeasurable) =
            measurables
        return calculateWidth(
            leadingWidth = leadingMeasurable.firstOrNull()?.intrinsicMeasure(height) ?: 0,
            trailingWidth = trailingMeasurable.firstOrNull()?.intrinsicMeasure(height) ?: 0,
            headlineWidth = headlineMeasurable.firstOrNull()?.intrinsicMeasure(height) ?: 0,
            overlineWidth = overlineMeasurable.firstOrNull()?.intrinsicMeasure(height) ?: 0,
            supportingWidth = supportingMeasurable.firstOrNull()?.intrinsicMeasure(height) ?: 0,
            horizontalPadding = (ListItemStartPadding + ListItemEndPadding).roundToPx(),
            constraints = Constraints(),
        )
    }

    private fun IntrinsicMeasureScope.calculateIntrinsicHeight(
        measurables: List<List<IntrinsicMeasurable>>,
        width: Int,
        intrinsicMeasure: IntrinsicMeasurable.(width: Int) -> Int,
    ): Int {
        val (
            headlineMeasurable,
            overlineMeasurable,
            supportingMeasurable,
            leadingMeasurable,
            trailingMeasurable) =
            measurables

        var remainingWidth =
            width.subtractConstraintSafely((ListItemStartPadding + ListItemEndPadding).roundToPx())
        val leadingHeight =
            leadingMeasurable.firstOrNull()?.let {
                val height = it.intrinsicMeasure(remainingWidth)
                remainingWidth =
                    remainingWidth.subtractConstraintSafely(
                        it.maxIntrinsicWidth(Constraints.Infinity)
                    )
                height
            } ?: 0
        val trailingHeight =
            trailingMeasurable.firstOrNull()?.let {
                val height = it.intrinsicMeasure(remainingWidth)
                remainingWidth =
                    remainingWidth.subtractConstraintSafely(
                        it.maxIntrinsicWidth(Constraints.Infinity)
                    )
                height
            } ?: 0
        val overlineHeight = overlineMeasurable.firstOrNull()?.intrinsicMeasure(remainingWidth) ?: 0
        val supportingHeight =
            supportingMeasurable.firstOrNull()?.intrinsicMeasure(remainingWidth) ?: 0
        val isSupportingMultiline = isSupportingMultilineHeuristic(supportingHeight)
        val listItemType =
            ListItemType(
                hasOverline = overlineHeight > 0,
                hasSupporting = supportingHeight > 0,
                isSupportingMultiline = isSupportingMultiline,
            )

        return calculateHeight(
            leadingHeight = leadingHeight,
            trailingHeight = trailingHeight,
            headlineHeight = headlineMeasurable.firstOrNull()?.intrinsicMeasure(width) ?: 0,
            overlineHeight = overlineHeight,
            supportingHeight = supportingHeight,
            listItemType = listItemType,
            verticalPadding = (verticalPadding(listItemType) * 2).roundToPx(),
            constraints = Constraints(),
        )
    }
}

private fun IntrinsicMeasureScope.calculateWidth(
    leadingWidth: Int,
    trailingWidth: Int,
    headlineWidth: Int,
    overlineWidth: Int,
    supportingWidth: Int,
    horizontalPadding: Int,
    constraints: Constraints,
): Int {
    if (constraints.hasBoundedWidth) {
        return constraints.maxWidth
    }
    // Fallback behavior if width constraints are infinite
    val mainContentWidth = maxOf(headlineWidth, overlineWidth, supportingWidth)
    return horizontalPadding + leadingWidth + mainContentWidth + trailingWidth
}

private fun IntrinsicMeasureScope.calculateHeight(
    leadingHeight: Int,
    trailingHeight: Int,
    headlineHeight: Int,
    overlineHeight: Int,
    supportingHeight: Int,
    listItemType: ListItemType,
    verticalPadding: Int,
    constraints: Constraints,
): Int {
    val defaultMinHeight =
        when (listItemType) {
            ListItemType.OneLine -> ListTokens.ListItemOneLineContainerHeight
            ListItemType.TwoLine -> ListTokens.ListItemTwoLineContainerHeight
            else /* ListItemType.ThreeLine */ -> ListTokens.ListItemThreeLineContainerHeight
        }
    val minHeight = max(constraints.minHeight, defaultMinHeight.roundToPx())

    val mainContentHeight = headlineHeight + overlineHeight + supportingHeight

    return max(minHeight, verticalPadding + maxOf(leadingHeight, mainContentHeight, trailingHeight))
        .coerceAtMost(constraints.maxHeight)
}

private fun MeasureScope.place(
    width: Int,
    height: Int,
    leadingPlaceable: Placeable?,
    trailingPlaceable: Placeable?,
    headlinePlaceable: Placeable?,
    overlinePlaceable: Placeable?,
    supportingPlaceable: Placeable?,
    isThreeLine: Boolean,
    startPadding: Int,
    endPadding: Int,
    topPadding: Int,
): MeasureResult {
    return layout(width, height) {
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

        val mainContentX = startPadding + leadingPlaceable.widthOrZero
        val mainContentY =
            if (isThreeLine) {
                topPadding
            } else {
                val totalHeight =
                    headlinePlaceable.heightOrZero +
                        overlinePlaceable.heightOrZero +
                        supportingPlaceable.heightOrZero
                CenterVertically.align(totalHeight, height)
            }
        var currentY = mainContentY

        overlinePlaceable?.placeRelative(mainContentX, currentY)
        currentY += overlinePlaceable.heightOrZero

        headlinePlaceable?.placeRelative(mainContentX, currentY)
        currentY += headlinePlaceable.heightOrZero

        supportingPlaceable?.placeRelative(mainContentX, currentY)
    }
}

/** Contains the default values used by list items. */
object ListItemDefaults {
    /** The default elevation of a list item */
    val Elevation: Dp = ListTokens.ListItemContainerElevation

    /** The default shape of a list item */
    val shape: Shape
        @Composable @ReadOnlyComposable get() = ListTokens.ListItemContainerShape.value

    /** The container color of a list item */
    val containerColor: Color
        @Composable @ReadOnlyComposable get() = ListTokens.ListItemContainerColor.value

    /** The content color of a list item */
    val contentColor: Color
        @Composable @ReadOnlyComposable get() = ListTokens.ListItemLabelTextColor.value

    /**
     * Creates a [ListItemColors] that represents the default container and content colors used in a
     * [ListItem].
     */
    @Composable fun colors() = MaterialTheme.colorScheme.defaultListItemColors

    /**
     * Creates a [ListItemColors] that represents the default container and content colors used in a
     * [ListItem].
     *
     * @param containerColor the container color of this list item when enabled.
     * @param headlineColor the headline text content color of this list item when enabled.
     * @param leadingIconColor the color of this list item's leading content when enabled.
     * @param overlineColor the overline text color of this list item
     * @param supportingColor the supporting text color of this list item
     * @param trailingIconColor the color of this list item's trailing content when enabled.
     * @param disabledHeadlineColor the content color of this list item when not enabled.
     * @param disabledLeadingIconColor the color of this list item's leading content when not
     *   enabled.
     * @param disabledTrailingIconColor the color of this list item's trailing content when not
     *   enabled.
     */
    @Composable
    fun colors(
        containerColor: Color = Color.Unspecified,
        headlineColor: Color = Color.Unspecified,
        leadingIconColor: Color = Color.Unspecified,
        overlineColor: Color = Color.Unspecified,
        supportingColor: Color = Color.Unspecified,
        trailingIconColor: Color = Color.Unspecified,
        disabledHeadlineColor: Color = Color.Unspecified,
        disabledLeadingIconColor: Color = Color.Unspecified,
        disabledTrailingIconColor: Color = Color.Unspecified,
    ): ListItemColors =
        MaterialTheme.colorScheme.defaultListItemColors.copy(
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

    internal val ColorScheme.defaultListItemColors: ListItemColors
        get() {
            return defaultListItemColorsCached
                ?: ListItemColors(
                        containerColor = fromToken(ListTokens.ListItemContainerColor),
                        headlineColor = fromToken(ListTokens.ListItemLabelTextColor),
                        leadingIconColor = fromToken(ListTokens.ListItemLeadingIconColor),
                        overlineColor = fromToken(ListTokens.ListItemOverlineColor),
                        supportingTextColor = fromToken(ListTokens.ListItemSupportingTextColor),
                        trailingIconColor = fromToken(ListTokens.ListItemTrailingIconColor),
                        disabledHeadlineColor =
                            fromToken(ListTokens.ListItemDisabledLabelTextColor)
                                .copy(alpha = ListTokens.ListItemDisabledLabelTextOpacity),
                        disabledLeadingIconColor =
                            fromToken(ListTokens.ListItemDisabledLeadingIconColor)
                                .copy(alpha = ListTokens.ListItemDisabledLeadingIconOpacity),
                        disabledTrailingIconColor =
                            fromToken(ListTokens.ListItemDisabledTrailingIconColor)
                                .copy(alpha = ListTokens.ListItemDisabledTrailingIconOpacity),
                    )
                    .also { defaultListItemColorsCached = it }
        }
}

/**
 * Represents the container and content colors used in a list item in different states.
 *
 * @param containerColor the container color of this list item when enabled.
 * @param headlineColor the headline text content color of this list item when enabled.
 * @param leadingIconColor the color of this list item's leading content when enabled.
 * @param overlineColor the overline text color of this list item
 * @param supportingTextColor the supporting text color of this list item
 * @param trailingIconColor the color of this list item's trailing content when enabled.
 * @param disabledHeadlineColor the content color of this list item when not enabled.
 * @param disabledLeadingIconColor the color of this list item's leading content when not enabled.
 * @param disabledTrailingIconColor the color of this list item's trailing content when not enabled.
 * @constructor create an instance with arbitrary colors. See [ListItemDefaults.colors] for the
 *   default colors used in a [ListItem].
 */
@Immutable
class ListItemColors
constructor(
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
    /**
     * Returns a copy of this ListItemColors, optionally overriding some of the values. This uses
     * the Color.Unspecified to mean “use the value from the source”
     */
    fun copy(
        containerColor: Color = this.containerColor,
        headlineColor: Color = this.headlineColor,
        leadingIconColor: Color = this.leadingIconColor,
        overlineColor: Color = this.overlineColor,
        supportingTextColor: Color = this.supportingTextColor,
        trailingIconColor: Color = this.trailingIconColor,
        disabledHeadlineColor: Color = this.disabledHeadlineColor,
        disabledLeadingIconColor: Color = this.disabledLeadingIconColor,
        disabledTrailingIconColor: Color = this.disabledTrailingIconColor,
    ) =
        ListItemColors(
            containerColor = containerColor.takeOrElse { this.containerColor },
            headlineColor = headlineColor.takeOrElse { this.headlineColor },
            leadingIconColor = leadingIconColor.takeOrElse { this.leadingIconColor },
            overlineColor = overlineColor.takeOrElse { this.overlineColor },
            supportingTextColor = supportingTextColor.takeOrElse { this.supportingTextColor },
            trailingIconColor = trailingIconColor.takeOrElse { this.trailingIconColor },
            disabledHeadlineColor = disabledHeadlineColor.takeOrElse { this.disabledHeadlineColor },
            disabledLeadingIconColor =
                disabledLeadingIconColor.takeOrElse { this.disabledLeadingIconColor },
            disabledTrailingIconColor =
                disabledTrailingIconColor.takeOrElse { this.disabledTrailingIconColor },
        )

    /** The container color of this [ListItem] based on enabled state */
    internal fun containerColor(): Color {
        return containerColor
    }

    /** The color of this [ListItem]'s headline text based on enabled state */
    @Stable
    internal fun headlineColor(enabled: Boolean): Color {
        return if (enabled) headlineColor else disabledHeadlineColor
    }

    /** The color of this [ListItem]'s leading content based on enabled state */
    @Stable
    internal fun leadingIconColor(enabled: Boolean): Color =
        if (enabled) leadingIconColor else disabledLeadingIconColor

    /** The color of this [ListItem]'s overline text based on enabled state */
    @Stable internal fun overlineColor(): Color = overlineColor

    /** The color of this [ListItem]'s supporting text based on enabled state */
    @Stable internal fun supportingColor(): Color = supportingTextColor

    /** The color of this [ListItem]'s trailing content based on enabled state */
    @Stable
    internal fun trailingIconColor(enabled: Boolean): Color =
        if (enabled) trailingIconColor else disabledTrailingIconColor
}

@Composable
private fun ProvideTextStyleFromToken(
    color: Color,
    textToken: TypographyKeyTokens,
    content: @Composable () -> Unit,
) =
    ProvideContentColorTextStyle(
        contentColor = color,
        textStyle = textToken.value,
        content = content
    )

/** Helper class to define list item type. Used for padding and sizing definition. */
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

        internal operator fun invoke(
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
@VisibleForTesting internal val ListItemVerticalPadding = 8.dp

@VisibleForTesting internal val ListItemThreeLineVerticalPadding = 12.dp

@VisibleForTesting internal val ListItemStartPadding = 16.dp

@VisibleForTesting internal val ListItemEndPadding = 16.dp

// Icon related defaults.
// TODO: Make sure these values stay up to date until replaced with tokens.
@VisibleForTesting internal val LeadingContentEndPadding = 16.dp

// Trailing related defaults.
// TODO: Make sure these values stay up to date until replaced with tokens.
@VisibleForTesting internal val TrailingContentStartPadding = 16.dp

// In the actual layout phase, we can query supporting baselines,
// but for an intrinsic measurement pass, we have to estimate.
private fun Density.isSupportingMultilineHeuristic(estimatedSupportingHeight: Int): Boolean =
    estimatedSupportingHeight > 30.sp.roundToPx()

private fun verticalPadding(listItemType: ListItemType): Dp =
    when (listItemType) {
        ListItemType.ThreeLine -> ListItemThreeLineVerticalPadding
        else -> ListItemVerticalPadding
    }
