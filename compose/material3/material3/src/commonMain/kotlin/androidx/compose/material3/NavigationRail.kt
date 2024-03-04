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
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.internal.MappedInteractionSource
import androidx.compose.material3.internal.ProvideContentColorTextStyle
import androidx.compose.material3.internal.systemBarsForVisualComponents
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.material3.tokens.NavigationRailTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.util.fastFirstOrNull
import kotlin.math.roundToInt

/**
 * <a href="https://m3.material.io/components/navigation-rail/overview" class="external"
 * target="_blank">Material Design bottom navigation rail</a>.
 *
 * Navigation rails provide access to primary destinations in apps when using tablet and desktop
 * screens.
 *
 * ![Navigation rail
 * image](https://developer.android.com/images/reference/androidx/compose/material3/navigation-rail.png)
 *
 * The navigation rail should be used to display three to seven app destinations and, optionally, a
 * [FloatingActionButton] or a logo header. Each destination is typically represented by an icon and
 * an optional text label.
 *
 * [NavigationRail] should contain multiple [NavigationRailItem]s, each representing a singular
 * destination.
 *
 * A simple example looks like:
 *
 * @sample androidx.compose.material3.samples.NavigationRailSample
 *
 * See [NavigationRailItem] for configuration specific to each item, and not the overall
 * NavigationRail component.
 *
 * @param modifier the [Modifier] to be applied to this navigation rail
 * @param containerColor the color used for the background of this navigation rail. Use
 *   [Color.Transparent] to have no color.
 * @param contentColor the preferred color for content inside this navigation rail. Defaults to
 *   either the matching content color for [containerColor], or to the current [LocalContentColor]
 *   if [containerColor] is not a color from the theme.
 * @param header optional header that may hold a [FloatingActionButton] or a logo
 * @param windowInsets a window insets of the navigation rail.
 * @param content the content of this navigation rail, typically 3-7 [NavigationRailItem]s
 */
@Composable
fun NavigationRail(
    modifier: Modifier = Modifier,
    containerColor: Color = NavigationRailDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    header: @Composable (ColumnScope.() -> Unit)? = null,
    windowInsets: WindowInsets = NavigationRailDefaults.windowInsets,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier,
    ) {
        Column(
            Modifier.fillMaxHeight()
                .windowInsetsPadding(windowInsets)
                .widthIn(min = NavigationRailTokens.ContainerWidth)
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
 * Navigation rails provide access to primary destinations in apps when using tablet and desktop
 * screens.
 *
 * The text label is always shown (if it exists) when selected. Showing text labels if not selected
 * is controlled by [alwaysShowLabel].
 *
 * @param selected whether this item is selected
 * @param onClick called when this item is clicked
 * @param icon icon for this item, typically an [Icon]
 * @param modifier the [Modifier] to be applied to this item
 * @param enabled controls the enabled state of this item. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param label optional text label for this item
 * @param alwaysShowLabel whether to always show the label for this item. If false, the label will
 *   only be shown when this item is selected.
 * @param colors [NavigationRailItemColors] that will be used to resolve the colors used for this
 *   item in different states. See [NavigationRailItemDefaults.colors].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this item. You can use this to change the item's appearance or
 *   preview the item in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NavigationRailItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable (() -> Unit)? = null,
    alwaysShowLabel: Boolean = true,
    colors: NavigationRailItemColors = NavigationRailItemDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    val styledIcon =
        @Composable {
            val iconColor by
                animateColorAsState(
                    targetValue = colors.iconColor(selected = selected, enabled = enabled),
                    // TODO Load the motionScheme tokens from the component tokens file
                    animationSpec = MotionSchemeKeyTokens.DefaultEffects.value()
                )
            // If there's a label, don't have a11y services repeat the icon description.
            val clearSemantics = label != null && (alwaysShowLabel || selected)
            Box(modifier = if (clearSemantics) Modifier.clearAndSetSemantics {} else Modifier) {
                CompositionLocalProvider(LocalContentColor provides iconColor, content = icon)
            }
        }

    val styledLabel: @Composable (() -> Unit)? =
        label?.let {
            @Composable {
                val style = NavigationRailTokens.LabelTextFont.value
                val textColor by
                    animateColorAsState(
                        targetValue = colors.textColor(selected = selected, enabled = enabled),
                        // TODO Load the motionScheme tokens from the component tokens file
                        animationSpec = MotionSchemeKeyTokens.DefaultEffects.value()
                    )
                ProvideContentColorTextStyle(
                    contentColor = textColor,
                    textStyle = style,
                    content = label
                )
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
                indication = null,
            )
            .defaultMinSize(minHeight = NavigationRailItemHeight)
            .widthIn(min = NavigationRailItemWidth),
        contentAlignment = Alignment.Center,
        propagateMinConstraints = true,
    ) {
        val alphaAnimationProgress: State<Float> =
            animateFloatAsState(
                targetValue = if (selected) 1f else 0f,
                // TODO Load the motionScheme tokens from the component tokens file
                animationSpec = MotionSchemeKeyTokens.DefaultEffects.value()
            )
        val sizeAnimationProgress: State<Float> =
            animateFloatAsState(
                targetValue = if (selected) 1f else 0f,
                // TODO Load the motionScheme tokens from the component tokens file
                animationSpec = MotionSchemeKeyTokens.FastSpatial.value()
            )

        // The entire item is selectable, but only the indicator pill shows the ripple. To achieve
        // this, we re-map the coordinates of the item's InteractionSource into the coordinates of
        // the indicator.
        val deltaOffset: Offset
        with(LocalDensity.current) {
            val itemWidth = NavigationRailItemWidth.roundToPx()
            val indicatorWidth = NavigationRailTokens.ActiveIndicatorWidth.roundToPx()
            deltaOffset = Offset((itemWidth - indicatorWidth).toFloat() / 2, 0f)
        }
        val offsetInteractionSource =
            remember(interactionSource, deltaOffset) {
                MappedInteractionSource(interactionSource, deltaOffset)
            }

        val indicatorShape =
            if (label != null) {
                NavigationRailTokens.ActiveIndicatorShape.value
            } else {
                NavigationRailTokens.NoLabelActiveIndicatorShape.value
            }

        // The indicator has a width-expansion animation which interferes with the timing of the
        // ripple, which is why they are separate composables
        val indicatorRipple =
            @Composable {
                Box(
                    Modifier.layoutId(IndicatorRippleLayoutIdTag)
                        .clip(indicatorShape)
                        .indication(offsetInteractionSource, ripple())
                )
            }
        val indicator =
            @Composable {
                Box(
                    Modifier.layoutId(IndicatorLayoutIdTag)
                        .graphicsLayer { alpha = alphaAnimationProgress.value }
                        .background(color = colors.indicatorColor, shape = indicatorShape)
                )
            }

        NavigationRailItemLayout(
            indicatorRipple = indicatorRipple,
            indicator = indicator,
            icon = styledIcon,
            label = styledLabel,
            alwaysShowLabel = alwaysShowLabel,
            alphaAnimationProgress = { alphaAnimationProgress.value },
            sizeAnimationProgress = { sizeAnimationProgress.value },
        )
    }
}

/** Defaults used in [NavigationRail] */
object NavigationRailDefaults {
    /** Default container color of a navigation rail. */
    val ContainerColor: Color
        @Composable get() = NavigationRailTokens.ContainerColor.value

    /** Default window insets for navigation rail. */
    val windowInsets: WindowInsets
        @Composable
        get() =
            WindowInsets.systemBarsForVisualComponents.only(
                WindowInsetsSides.Vertical + WindowInsetsSides.Start
            )
}

/** Defaults used in [NavigationRailItem]. */
object NavigationRailItemDefaults {
    /**
     * Creates a [NavigationRailItemColors] with the provided colors according to the Material
     * specification.
     */
    @Composable fun colors() = MaterialTheme.colorScheme.defaultNavigationRailItemColors

    /**
     * Creates a [NavigationRailItemColors] with the provided colors according to the Material
     * specification.
     *
     * @param selectedIconColor the color to use for the icon when the item is selected.
     * @param selectedTextColor the color to use for the text label when the item is selected.
     * @param indicatorColor the color to use for the indicator when the item is selected.
     * @param unselectedIconColor the color to use for the icon when the item is unselected.
     * @param unselectedTextColor the color to use for the text label when the item is unselected.
     * @param disabledIconColor the color to use for the icon when the item is disabled.
     * @param disabledTextColor the color to use for the text label when the item is disabled.
     * @return the resulting [NavigationRailItemColors] used for [NavigationRailItem]
     */
    @Composable
    fun colors(
        selectedIconColor: Color = NavigationRailTokens.ActiveIconColor.value,
        selectedTextColor: Color = NavigationRailTokens.ActiveLabelTextColor.value,
        indicatorColor: Color = NavigationRailTokens.ActiveIndicatorColor.value,
        unselectedIconColor: Color = NavigationRailTokens.InactiveIconColor.value,
        unselectedTextColor: Color = NavigationRailTokens.InactiveLabelTextColor.value,
        disabledIconColor: Color = unselectedIconColor.copy(alpha = DisabledAlpha),
        disabledTextColor: Color = unselectedTextColor.copy(alpha = DisabledAlpha),
    ): NavigationRailItemColors =
        MaterialTheme.colorScheme.defaultNavigationRailItemColors.copy(
            selectedIconColor = selectedIconColor,
            selectedTextColor = selectedTextColor,
            selectedIndicatorColor = indicatorColor,
            unselectedIconColor = unselectedIconColor,
            unselectedTextColor = unselectedTextColor,
            disabledIconColor = disabledIconColor,
            disabledTextColor = disabledTextColor,
        )

    internal val ColorScheme.defaultNavigationRailItemColors: NavigationRailItemColors
        get() {
            return defaultNavigationRailItemColorsCached
                ?: NavigationRailItemColors(
                        selectedIconColor = fromToken(NavigationRailTokens.ActiveIconColor),
                        selectedTextColor = fromToken(NavigationRailTokens.ActiveLabelTextColor),
                        selectedIndicatorColor =
                            fromToken(NavigationRailTokens.ActiveIndicatorColor),
                        unselectedIconColor = fromToken(NavigationRailTokens.InactiveIconColor),
                        unselectedTextColor =
                            fromToken(NavigationRailTokens.InactiveLabelTextColor),
                        disabledIconColor =
                            fromToken(NavigationRailTokens.InactiveIconColor)
                                .copy(alpha = DisabledAlpha),
                        disabledTextColor =
                            fromToken(NavigationRailTokens.InactiveLabelTextColor)
                                .copy(alpha = DisabledAlpha),
                    )
                    .also { defaultNavigationRailItemColorsCached = it }
        }

    @Deprecated(
        "Use overload with disabledIconColor and disabledTextColor",
        level = DeprecationLevel.HIDDEN
    )
    @Composable
    fun colors(
        selectedIconColor: Color = NavigationRailTokens.ActiveIconColor.value,
        selectedTextColor: Color = NavigationRailTokens.ActiveLabelTextColor.value,
        indicatorColor: Color = NavigationRailTokens.ActiveIndicatorColor.value,
        unselectedIconColor: Color = NavigationRailTokens.InactiveIconColor.value,
        unselectedTextColor: Color = NavigationRailTokens.InactiveLabelTextColor.value,
    ): NavigationRailItemColors =
        NavigationRailItemColors(
            selectedIconColor = selectedIconColor,
            selectedTextColor = selectedTextColor,
            selectedIndicatorColor = indicatorColor,
            unselectedIconColor = unselectedIconColor,
            unselectedTextColor = unselectedTextColor,
            disabledIconColor = unselectedIconColor.copy(alpha = DisabledAlpha),
            disabledTextColor = unselectedTextColor.copy(alpha = DisabledAlpha),
        )
}

/**
 * Represents the colors of the various elements of a navigation item.
 *
 * @param selectedIconColor the color to use for the icon when the item is selected.
 * @param selectedTextColor the color to use for the text label when the item is selected.
 * @param selectedIndicatorColor the color to use for the indicator when the item is selected.
 * @param unselectedIconColor the color to use for the icon when the item is unselected.
 * @param unselectedTextColor the color to use for the text label when the item is unselected.
 * @param disabledIconColor the color to use for the icon when the item is disabled.
 * @param disabledTextColor the color to use for the text label when the item is disabled.
 * @constructor create an instance with arbitrary colors.
 */
@Immutable
class NavigationRailItemColors
constructor(
    val selectedIconColor: Color,
    val selectedTextColor: Color,
    val selectedIndicatorColor: Color,
    val unselectedIconColor: Color,
    val unselectedTextColor: Color,
    val disabledIconColor: Color,
    val disabledTextColor: Color,
) {
    /**
     * Returns a copy of this NavigationRailItemColors, optionally overriding some of the values.
     * This uses the Color.Unspecified to mean “use the value from the source”
     */
    fun copy(
        selectedIconColor: Color = this.selectedIconColor,
        selectedTextColor: Color = this.selectedTextColor,
        selectedIndicatorColor: Color = this.selectedIndicatorColor,
        unselectedIconColor: Color = this.unselectedIconColor,
        unselectedTextColor: Color = this.unselectedTextColor,
        disabledIconColor: Color = this.disabledIconColor,
        disabledTextColor: Color = this.disabledTextColor,
    ) =
        NavigationRailItemColors(
            selectedIconColor.takeOrElse { this.selectedIconColor },
            selectedTextColor.takeOrElse { this.selectedTextColor },
            selectedIndicatorColor.takeOrElse { this.selectedIndicatorColor },
            unselectedIconColor.takeOrElse { this.unselectedIconColor },
            unselectedTextColor.takeOrElse { this.unselectedTextColor },
            disabledIconColor.takeOrElse { this.disabledIconColor },
            disabledTextColor.takeOrElse { this.disabledTextColor },
        )

    /**
     * Represents the icon color for this item, depending on whether it is [selected].
     *
     * @param selected whether the item is selected
     * @param enabled whether the item is enabled
     */
    @Stable
    internal fun iconColor(selected: Boolean, enabled: Boolean): Color =
        when {
            !enabled -> disabledIconColor
            selected -> selectedIconColor
            else -> unselectedIconColor
        }

    /**
     * Represents the text color for this item, depending on whether it is [selected].
     *
     * @param selected whether the item is selected
     * @param enabled whether the item is enabled
     */
    @Stable
    internal fun textColor(selected: Boolean, enabled: Boolean): Color =
        when {
            !enabled -> disabledTextColor
            selected -> selectedTextColor
            else -> unselectedTextColor
        }

    /** Represents the color of the indicator used for selected items. */
    internal val indicatorColor: Color
        get() = selectedIndicatorColor

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is NavigationRailItemColors) return false

        if (selectedIconColor != other.selectedIconColor) return false
        if (unselectedIconColor != other.unselectedIconColor) return false
        if (selectedTextColor != other.selectedTextColor) return false
        if (unselectedTextColor != other.unselectedTextColor) return false
        if (selectedIndicatorColor != other.selectedIndicatorColor) return false
        if (disabledIconColor != other.disabledIconColor) return false
        if (disabledTextColor != other.disabledTextColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = selectedIconColor.hashCode()
        result = 31 * result + unselectedIconColor.hashCode()
        result = 31 * result + selectedTextColor.hashCode()
        result = 31 * result + unselectedTextColor.hashCode()
        result = 31 * result + selectedIndicatorColor.hashCode()
        result = 31 * result + disabledIconColor.hashCode()
        result = 31 * result + disabledTextColor.hashCode()

        return result
    }
}

/**
 * Base layout for a [NavigationRailItem].
 *
 * @param indicatorRipple indicator ripple for this item when it is selected
 * @param indicator indicator for this item when it is selected
 * @param icon icon for this item
 * @param label text label for this item
 * @param alwaysShowLabel whether to always show the label for this item. If false, the label will
 *   only be shown when this item is selected.
 * @param alphaAnimationProgress progress of the animation, where 0 represents the unselected state
 *   of this item and 1 represents the selected state. This value controls the indicator's color
 *   alpha.
 * @param sizeAnimationProgress progress of the animation, where 0 represents the unselected state
 *   of this item and 1 represents the selected state. This value controls other values such as
 *   indicator size, icon and label positions, etc.
 */
@Composable
private fun NavigationRailItemLayout(
    indicatorRipple: @Composable () -> Unit,
    indicator: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable (() -> Unit)?,
    alwaysShowLabel: Boolean,
    alphaAnimationProgress: () -> Float,
    sizeAnimationProgress: () -> Float,
) {
    Layout(
        modifier = Modifier.badgeBounds(),
        content = {
            indicatorRipple()
            indicator()

            Box(Modifier.layoutId(IconLayoutIdTag)) { icon() }

            if (label != null) {
                Box(
                    Modifier.layoutId(LabelLayoutIdTag).graphicsLayer {
                        alpha = if (alwaysShowLabel) 1f else alphaAnimationProgress()
                    }
                ) {
                    label()
                }
            }
        }
    ) { measurables, constraints ->
        @Suppress("NAME_SHADOWING")
        // Ensure that the progress is >= 0. It may be negative on bouncy springs, for example.
        val animationProgress = sizeAnimationProgress().coerceAtLeast(0f)
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val iconPlaceable =
            measurables.fastFirst { it.layoutId == IconLayoutIdTag }.measure(looseConstraints)

        val totalIndicatorWidth = iconPlaceable.width + (IndicatorHorizontalPadding * 2).roundToPx()
        val animatedIndicatorWidth = (totalIndicatorWidth * animationProgress).roundToInt()
        val indicatorVerticalPadding =
            if (label == null) {
                IndicatorVerticalPaddingNoLabel
            } else {
                IndicatorVerticalPaddingWithLabel
            }
        val indicatorHeight = iconPlaceable.height + (indicatorVerticalPadding * 2).roundToPx()

        val indicatorRipplePlaceable =
            measurables
                .fastFirst { it.layoutId == IndicatorRippleLayoutIdTag }
                .measure(Constraints.fixed(width = totalIndicatorWidth, height = indicatorHeight))
        val indicatorPlaceable =
            measurables
                .fastFirstOrNull { it.layoutId == IndicatorLayoutIdTag }
                ?.measure(
                    Constraints.fixed(width = animatedIndicatorWidth, height = indicatorHeight)
                )

        val labelPlaceable =
            label?.let {
                measurables.fastFirst { it.layoutId == LabelLayoutIdTag }.measure(looseConstraints)
            }

        if (label == null) {
            placeIcon(iconPlaceable, indicatorRipplePlaceable, indicatorPlaceable, constraints)
        } else {
            placeLabelAndIcon(
                labelPlaceable!!,
                iconPlaceable,
                indicatorRipplePlaceable,
                indicatorPlaceable,
                constraints,
                alwaysShowLabel,
                animationProgress,
            )
        }
    }
}

/** Places the provided [Placeable]s in the center of the provided [constraints]. */
private fun MeasureScope.placeIcon(
    iconPlaceable: Placeable,
    indicatorRipplePlaceable: Placeable,
    indicatorPlaceable: Placeable?,
    constraints: Constraints,
): MeasureResult {
    val width =
        constraints.constrainWidth(
            maxOf(
                iconPlaceable.width,
                indicatorRipplePlaceable.width,
                indicatorPlaceable?.width ?: 0
            )
        )
    val height = constraints.constrainHeight(NavigationRailItemHeight.roundToPx())

    val iconX = (width - iconPlaceable.width) / 2
    val iconY = (height - iconPlaceable.height) / 2

    val rippleX = (width - indicatorRipplePlaceable.width) / 2
    val rippleY = (height - indicatorRipplePlaceable.height) / 2

    return layout(width, height) {
        indicatorPlaceable?.let {
            val indicatorX = (width - it.width) / 2
            val indicatorY = (height - it.height) / 2
            it.placeRelative(indicatorX, indicatorY)
        }
        iconPlaceable.placeRelative(iconX, iconY)
        indicatorRipplePlaceable.placeRelative(rippleX, rippleY)
    }
}

/**
 * Places the provided [Placeable]s in the correct position, depending on [alwaysShowLabel] and
 * [animationProgress].
 *
 * When [alwaysShowLabel] is true, the positions do not move. The [iconPlaceable] and
 * [labelPlaceable] will be placed together in the center with padding between them, according to
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
 * [indicatorRipplePlaceable] and [indicatorPlaceable] will always be placed in such a way that to
 * share the same center as [iconPlaceable].
 *
 * @param labelPlaceable text label placeable inside this item
 * @param iconPlaceable icon placeable inside this item
 * @param indicatorRipplePlaceable indicator ripple placeable inside this item
 * @param indicatorPlaceable indicator placeable inside this item, if it exists
 * @param constraints constraints of the item
 * @param alwaysShowLabel whether to always show the label for this item. If true, icon and label
 *   positions will not change. If false, positions transition between 'centered icon with no label'
 *   and 'top aligned icon with label'.
 * @param animationProgress progress of the animation, where 0 represents the unselected state of
 *   this item and 1 represents the selected state. Values between 0 and 1 interpolate positions of
 *   the icon and label.
 */
private fun MeasureScope.placeLabelAndIcon(
    labelPlaceable: Placeable,
    iconPlaceable: Placeable,
    indicatorRipplePlaceable: Placeable,
    indicatorPlaceable: Placeable?,
    constraints: Constraints,
    alwaysShowLabel: Boolean,
    animationProgress: Float,
): MeasureResult {
    val contentHeight =
        iconPlaceable.height +
            IndicatorVerticalPaddingWithLabel.toPx() +
            NavigationRailItemVerticalPadding.toPx() +
            labelPlaceable.height
    val contentVerticalPadding =
        ((constraints.minHeight - contentHeight) / 2).coerceAtLeast(
            IndicatorVerticalPaddingWithLabel.toPx()
        )
    val height = contentHeight + contentVerticalPadding * 2

    // Icon (when selected) should be `contentVerticalPadding` from the top
    val selectedIconY = contentVerticalPadding
    val unselectedIconY =
        if (alwaysShowLabel) selectedIconY else (height - iconPlaceable.height) / 2

    // How far the icon needs to move between unselected and selected states
    val iconDistance = unselectedIconY - selectedIconY

    // The interpolated fraction of iconDistance that all placeables need to move based on
    // animationProgress, since the icon is higher in the selected state.
    val offset = iconDistance * (1 - animationProgress)

    // Label should be fixed padding below icon
    val labelY =
        selectedIconY +
            iconPlaceable.height +
            IndicatorVerticalPaddingWithLabel.toPx() +
            NavigationRailItemVerticalPadding.toPx()

    val width =
        constraints.constrainWidth(
            maxOf(iconPlaceable.width, labelPlaceable.width, indicatorPlaceable?.width ?: 0)
        )
    val labelX = (width - labelPlaceable.width) / 2
    val iconX = (width - iconPlaceable.width) / 2
    val rippleX = (width - indicatorRipplePlaceable.width) / 2
    val rippleY = selectedIconY - IndicatorVerticalPaddingWithLabel.toPx()

    return layout(width, height.roundToInt()) {
        indicatorPlaceable?.let {
            val indicatorX = (width - it.width) / 2
            val indicatorY = selectedIconY - IndicatorVerticalPaddingWithLabel.toPx()
            it.placeRelative(indicatorX, (indicatorY + offset).roundToInt())
        }
        if (alwaysShowLabel || animationProgress != 0f) {
            labelPlaceable.placeRelative(labelX, (labelY + offset).roundToInt())
        }
        iconPlaceable.placeRelative(iconX, (selectedIconY + offset).roundToInt())
        indicatorRipplePlaceable.placeRelative(rippleX, (rippleY + offset).roundToInt())
    }
}

private const val IndicatorRippleLayoutIdTag: String = "indicatorRipple"

private const val IndicatorLayoutIdTag: String = "indicator"

private const val IconLayoutIdTag: String = "icon"

private const val LabelLayoutIdTag: String = "label"

/**
 * Vertical padding between the contents of the [NavigationRail] and its top/bottom, and internally
 * between items.
 */
internal val NavigationRailVerticalPadding: Dp = 4.dp

/**
 * Padding at the bottom of the [NavigationRail]'s header. This padding will only be added when the
 * header is not null.
 */
private val NavigationRailHeaderPadding: Dp = 8.dp

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
