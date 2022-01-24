/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.tokens.NavigationRailTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * ![Navigation rail image](https://developer.android.com/images/reference/androidx/compose/material3/navigation-rail.png)
 *
 * Material Design navigation rail.
 *
 * [NavigationRail] is a side navigation component that allows movement between primary destinations
 * in an app. The navigation rail should be used to display three to seven app destinations and,
 * optionally, a Floating Action Button or a logo header. Each destination is typically represented
 * by an icon and an optional text label.
 *
 * [NavigationRail] should contain multiple [NavigationRailItem]s, each representing a singular
 * destination.
 *
 * A simple example looks like:
 * @sample androidx.compose.material3.samples.NavigationRailSample
 *
 * See [NavigationRailItem] for configuration specific to each item, and not the overall
 * NavigationRail component.
 *
 * @param modifier optional [Modifier] for this NavigationRail
 * @param containerColor the container color for this NavigationRail
 * @param contentColor the preferred content color provided by this NavigationRail to its children.
 * Defaults to either the matching content color for [containerColor], or if [containerColor] is not
 * a color from the theme, this will keep the same value set above this NavigationRail
 * @param header optional header that may hold a Floating Action Button or a logo
 * @param content destinations inside this NavigationRail. This should contain multiple
 * [NavigationRailItem]s
 */
@Composable
fun NavigationRail(
    modifier: Modifier = Modifier,
    containerColor: Color = NavigationRailTokens.ContainerColor.toColor(),
    contentColor: Color = contentColorFor(containerColor),
    header: @Composable (ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier,
    ) {
        Column(
            Modifier.fillMaxHeight()
                .width(NavigationRailTokens.ContainerWidth)
                .padding(vertical = NavigationRailVerticalPadding)
                .selectableGroup(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(NavigationRailVerticalPadding)
        ) {
            if (header != null) {
                header()
                Spacer(Modifier.height(NavigationRailHeaderPadding))
            }
            content()
        }
    }
}

/**
 * Material Design navigation rail item.
 *
 * A [NavigationRailItem] represents a destination within a [NavigationRail].
 *
 * The text label is always shown (if it exists) when selected. Showing text labels if not selected
 * is controlled by [alwaysShowLabel].
 *
 * @param selected whether this item is selected
 * @param onClick the callback to be invoked when this item is selected
 * @param icon icon for this item, typically an [Icon]
 * @param modifier optional [Modifier] for this item
 * @param enabled controls the enabled state of this item. When false, this item will not be
 * clickable and will appear disabled to accessibility services
 * @param label optional text label for this item
 * @param alwaysShowLabel whether to always show the label for this item. If false, the label will
 * only be shown when this item is selected.
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this NavigationRailItem. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the appearance /
 * behavior of this NavigationRailItem in different [Interaction]s.
 * @param colors the various colors used in elements of this item
 */
@Composable
fun NavigationRailItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable (() -> Unit)? = null,
    alwaysShowLabel: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: NavigationRailItemColors = NavigationRailItemDefaults.colors(),
) {
    val styledIcon = @Composable {
        val iconColor by colors.iconColor(selected = selected)
        CompositionLocalProvider(LocalContentColor provides iconColor, content = icon)
    }

    val styledLabel: @Composable (() -> Unit)? = label?.let {
        @Composable {
            val style = MaterialTheme.typography.fromToken(NavigationRailTokens.LabelTextFont)
            val textColor by colors.textColor(selected = selected)
            CompositionLocalProvider(LocalContentColor provides textColor) {
                ProvideTextStyle(style, content = label)
            }
        }
    }

    Box(
        modifier
            .selectable(
                selected = selected,
                onClick = onClick,
                enabled = enabled,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = rememberRipple(),
            )
            .size(width = NavigationRailItemWidth, height = NavigationRailItemHeight),
        contentAlignment = Alignment.Center
    ) {
        val animationProgress: Float by animateFloatAsState(
            targetValue = if (selected) 1f else 0f,
            animationSpec = tween(ItemAnimationDurationMillis)
        )

        val indicator = @Composable {
            Box(
                Modifier.layoutId(IndicatorLayoutIdTag)
                    .background(
                        color = colors.indicatorColor.copy(alpha = animationProgress),
                        shape = if (label != null) {
                            NavigationRailTokens.ActiveIndicatorShape
                        } else {
                            NavigationRailTokens.NoLabelActiveIndicatorShape
                        }
                    )
            )
        }

        NavigationRailItemBaselineLayout(
            indicator = indicator,
            icon = styledIcon,
            label = styledLabel,
            alwaysShowLabel = alwaysShowLabel,
            animationProgress = animationProgress,
        )
    }
}

/** Defaults used in [NavigationRailItem]. */
object NavigationRailItemDefaults {
    /**
     * Creates a [NavigationRailItemColors] with the provided colors according to the Material
     * specification.
     *
     * @param selectedIconColor the color to use for the icon when the item is selected.
     * @param unselectedIconColor the color to use for the icon when the item is unselected.
     * @param selectedTextColor the color to use for the text label when the item is selected.
     * @param unselectedTextColor the color to use for the text label when the item is unselected.
     * @param indicatorColor the color to use for the indicator when the item is selected.
     * @return the resulting [NavigationRailItemColors] used for [NavigationRailItem]
     */
    @Composable
    fun colors(
        selectedIconColor: Color = NavigationRailTokens.ActiveIconColor.toColor(),
        unselectedIconColor: Color = NavigationRailTokens.InactiveIconColor.toColor(),
        selectedTextColor: Color = NavigationRailTokens.ActiveLabelTextColor.toColor(),
        unselectedTextColor: Color = NavigationRailTokens.InactiveLabelTextColor.toColor(),
        indicatorColor: Color = NavigationRailTokens.ActiveIndicatorColor.toColor(),
    ): NavigationRailItemColors = remember(
        selectedIconColor,
        unselectedIconColor,
        selectedTextColor,
        unselectedTextColor,
        indicatorColor
    ) {
        DefaultNavigationRailItemColors(
            selectedIconColor = selectedIconColor,
            unselectedIconColor = unselectedIconColor,
            selectedTextColor = selectedTextColor,
            unselectedTextColor = unselectedTextColor,
            selectedIndicatorColor = indicatorColor,
        )
    }
}

/** Represents the colors of the various elements of a navigation item. */
@Stable
interface NavigationRailItemColors {
    /**
     * Represents the icon color for this item, depending on whether it is [selected].
     *
     * @param selected whether the item is selected
     */
    @Composable
    fun iconColor(selected: Boolean): State<Color>

    /**
     * Represents the text color for this item, depending on whether it is [selected].
     *
     * @param selected whether the item is selected
     */
    @Composable
    fun textColor(selected: Boolean): State<Color>

    /** Represents the color of the indicator used for selected items. */
    val indicatorColor: Color
        @Composable get
}

@Stable
private class DefaultNavigationRailItemColors(
    private val selectedIconColor: Color,
    private val unselectedIconColor: Color,
    private val selectedTextColor: Color,
    private val unselectedTextColor: Color,
    private val selectedIndicatorColor: Color,
) : NavigationRailItemColors {
    @Composable
    override fun iconColor(selected: Boolean): State<Color> {
        return animateColorAsState(
            targetValue = if (selected) selectedIconColor else unselectedIconColor,
            animationSpec = tween(ItemAnimationDurationMillis)
        )
    }

    @Composable
    override fun textColor(selected: Boolean): State<Color> {
        return animateColorAsState(
            targetValue = if (selected) selectedTextColor else unselectedTextColor,
            animationSpec = tween(ItemAnimationDurationMillis)
        )
    }

    override val indicatorColor: Color
        @Composable
        get() = selectedIndicatorColor
}

/**
 * Base layout for a [NavigationRailItem].
 *
 * @param indicator indicator for this item when it is selected
 * @param icon icon for this item
 * @param label text label for this item
 * @param alwaysShowLabel whether to always show the label for this item. If false, the label will
 * only be shown when this item is selected.
 * @param animationProgress progress of the animation, where 0 represents the unselected state of
 * this item and 1 represents the selected state. This value controls other values such as indicator
 * size, icon and label positions, etc.
 */
@Composable
private fun NavigationRailItemBaselineLayout(
    indicator: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable (() -> Unit)?,
    alwaysShowLabel: Boolean,
    animationProgress: Float,
) {
    Layout({
        if (animationProgress > 0) {
            indicator()
        }

        Box(Modifier.layoutId(IconLayoutIdTag)) { icon() }

        if (label != null) {
            Box(
                Modifier.layoutId(LabelLayoutIdTag)
                    .alpha(if (alwaysShowLabel) 1f else animationProgress)
            ) { label() }
        }
    }) { measurables, constraints ->
        val iconPlaceable =
            measurables.first { it.layoutId == IconLayoutIdTag }.measure(constraints)

        val totalIndicatorWidth = iconPlaceable.width + (IndicatorHorizontalPadding * 2).roundToPx()
        val animatedIndicatorWidth = (totalIndicatorWidth * animationProgress).roundToInt()
        val indicatorVerticalPadding = if (label == null) {
            IndicatorVerticalPaddingNoLabel
        } else {
            IndicatorVerticalPaddingWithLabel
        }
        val indicatorHeight = iconPlaceable.height + (indicatorVerticalPadding * 2).roundToPx()

        val indicatorPlaceable =
            measurables
                .firstOrNull { it.layoutId == IndicatorLayoutIdTag }
                ?.measure(
                    Constraints.fixed(
                        width = animatedIndicatorWidth,
                        height = indicatorHeight
                    )
                )

        val labelPlaceable =
            label?.let {
                measurables
                    .first { it.layoutId == LabelLayoutIdTag }
                    .measure(
                        // Measure with loose constraints for height as we don't want the label to
                        // take up more space than it needs
                        constraints.copy(minHeight = 0)
                    )
            }

        if (label == null) {
            placeIcon(iconPlaceable, indicatorPlaceable, constraints)
        } else {
            placeLabelAndIcon(
                labelPlaceable!!,
                iconPlaceable,
                indicatorPlaceable,
                constraints,
                alwaysShowLabel,
                animationProgress,
            )
        }
    }
}

/**
 * Places the provided [iconPlaceable], and possibly [indicatorPlaceable] if it exists, in the
 * center of the provided [constraints].
 */
private fun MeasureScope.placeIcon(
    iconPlaceable: Placeable,
    indicatorPlaceable: Placeable?,
    constraints: Constraints,
): MeasureResult {
    val width = constraints.maxWidth
    val height = constraints.maxHeight

    val iconX = (width - iconPlaceable.width) / 2
    val iconY = (height - iconPlaceable.height) / 2

    return layout(width, height) {
        indicatorPlaceable?.let {
            val indicatorX = (width - it.width) / 2
            val indicatorY = (height - it.height) / 2
            it.placeRelative(indicatorX, indicatorY)
        }
        iconPlaceable.placeRelative(iconX, iconY)
    }
}

/**
 * Places the provided [labelPlaceable], [iconPlaceable], and [indicatorPlaceable] in the correct
 * position, depending on [alwaysShowLabel] and [animationProgress].
 *
 * When [alwaysShowLabel] is true, the positions do not move. The [iconPlaceable] will be placed
 * near the top of the item and the [labelPlaceable] will be placed near the bottom, according to
 * the spec.
 *
 * When [animationProgress] is 1 (representing the selected state), the positions will be the same
 * as above.
 *
 * Otherwise, when [animationProgress] is 0, [iconPlaceable] will be placed in the center, like in
 * [placeIcon], and [labelPlaceable] will not be shown.
 *
 * When [animationProgress] is animating between these values, [iconPlaceable] and [labelPlaceable]
 * will be placed at a corresponding interpolated position.
 *
 * [indicatorPlaceable] will always be placed in such a way that it shares the same center as
 * [iconPlaceable].
 *
 * @param labelPlaceable text label placeable inside this item
 * @param iconPlaceable icon placeable inside this item
 * @param indicatorPlaceable indicator placeable inside this item, if it exists
 * @param constraints constraints of the item
 * @param alwaysShowLabel whether to always show the label for this item. If true, icon and label
 * positions will not change. If false, positions transition between 'centered icon with no label'
 * and 'top aligned icon with label'.
 * @param animationProgress progress of the animation, where 0 represents the unselected state of
 * this item and 1 represents the selected state. Values between 0 and 1 interpolate positions of
 * the icon and label.
 */
private fun MeasureScope.placeLabelAndIcon(
    labelPlaceable: Placeable,
    iconPlaceable: Placeable,
    indicatorPlaceable: Placeable?,
    constraints: Constraints,
    alwaysShowLabel: Boolean,
    animationProgress: Float,
): MeasureResult {
    val height = constraints.maxHeight

    // Label should be `ItemVerticalPadding` from the bottom
    val labelY = height - labelPlaceable.height - NavigationRailItemVerticalPadding.roundToPx()

    // Icon (when selected) should be `ItemVerticalPadding` from the top
    val selectedIconY = NavigationRailItemVerticalPadding.roundToPx()
    val unselectedIconY =
        if (alwaysShowLabel) selectedIconY else (height - iconPlaceable.height) / 2

    // How far the icon needs to move between unselected and selected states
    val iconDistance = unselectedIconY - selectedIconY

    // The interpolated fraction of iconDistance that all placeables need to move based on
    // animationProgress, since the icon is higher in the selected state.
    val offset = (iconDistance * (1 - animationProgress)).roundToInt()

    val width = constraints.maxWidth
    val labelX = (width - labelPlaceable.width) / 2
    val iconX = (width - iconPlaceable.width) / 2

    return layout(width, height) {
        indicatorPlaceable?.let {
            val indicatorX = (width - it.width) / 2
            val indicatorY = selectedIconY - IndicatorVerticalPaddingWithLabel.roundToPx()
            it.placeRelative(indicatorX, indicatorY + offset)
        }
        if (alwaysShowLabel || animationProgress != 0f) {
            labelPlaceable.placeRelative(labelX, labelY + offset)
        }
        iconPlaceable.placeRelative(iconX, selectedIconY + offset)
    }
}

private const val IndicatorLayoutIdTag: String = "indicator"

private const val IconLayoutIdTag: String = "icon"

private const val LabelLayoutIdTag: String = "label"

/**
 * Vertical padding between the contents of the [NavigationRail] and its top/bottom, and internally
 * between items.
 */
private val NavigationRailVerticalPadding: Dp = 4.dp

/**
 * Padding at the bottom of the [NavigationRail]'s header. This padding will only be added when the
 * header is not null.
 */
private val NavigationRailHeaderPadding: Dp = 8.dp

private const val ItemAnimationDurationMillis: Int = 150

/*@VisibleForTesting*/
/** Width of an individual [NavigationRailItem]. */
internal val NavigationRailItemWidth: Dp = NavigationRailTokens.ContainerWidth

/*@VisibleForTesting*/
/** Height of an individual [NavigationRailItem]. */
internal val NavigationRailItemHeight: Dp = NavigationRailTokens.NoLabelActiveIndicatorHeight

/*@VisibleForTesting*/
/** Vertical padding between the contents of a [NavigationRailItem] and its top/bottom. */
internal val NavigationRailItemVerticalPadding: Dp = 4.dp

private val IndicatorHorizontalPadding: Dp =
    (NavigationRailTokens.ActiveIndicatorWidth - NavigationRailTokens.IconSize) / 2

private val IndicatorVerticalPaddingWithLabel: Dp =
    (NavigationRailTokens.ActiveIndicatorHeight - NavigationRailTokens.IconSize) / 2

private val IndicatorVerticalPaddingNoLabel: Dp =
    (NavigationRailTokens.NoLabelActiveIndicatorHeight - NavigationRailTokens.IconSize) / 2