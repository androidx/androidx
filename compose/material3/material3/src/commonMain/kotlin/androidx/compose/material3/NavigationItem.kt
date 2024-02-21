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

package androidx.compose.material3

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrain
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.util.fastFirst
import kotlin.math.roundToInt

/**
 * Class that describes the different supported icon positions of the navigation item.
 *
 * TODO: Remove "internal".
 * TODO: Add Start IconPosition.
 */
@JvmInline
@ExperimentalMaterial3Api
internal value class NavigationItemIconPosition private constructor(private val value: Int) {
    companion object {
        /* The icon is positioned on top of the label. */
        val Top = NavigationItemIconPosition(0)
    }

    override fun toString() = when (this) {
        Top -> "Top"
        else -> "Unknown"
    }
}

/**
 * Represents the colors of the various elements of a navigation item.
 *
 * @constructor create an instance with arbitrary colors.
 *
 * @param selectedIconColor the color to use for the icon when the item is selected.
 * @param selectedTextColor the color to use for the text label when the item is selected.
 * @param selectedIndicatorColor the color to use for the indicator when the item is selected.
 * @param unselectedIconColor the color to use for the icon when the item is unselected.
 * @param unselectedTextColor the color to use for the text label when the item is unselected.
 * @param disabledIconColor the color to use for the icon when the item is disabled.
 * @param disabledTextColor the color to use for the text label when the item is disabled.
 *
 * TODO: Remove "internal".
 */
@Immutable
internal class NavigationItemColors constructor(
    val selectedIconColor: Color,
    val selectedTextColor: Color,
    val selectedIndicatorColor: Color,
    val unselectedIconColor: Color,
    val unselectedTextColor: Color,
    val disabledIconColor: Color,
    val disabledTextColor: Color,
) {
    /**
     * Returns a copy of this NavigationItemColors, optionally overriding some of the values.
     * This uses the Color.Unspecified to mean “use the value from the source”.
     */
    fun copy(
        selectedIconColor: Color = this.selectedIconColor,
        selectedTextColor: Color = this.selectedTextColor,
        selectedIndicatorColor: Color = this.selectedIndicatorColor,
        unselectedIconColor: Color = this.unselectedIconColor,
        unselectedTextColor: Color = this.unselectedTextColor,
        disabledIconColor: Color = this.disabledIconColor,
        disabledTextColor: Color = this.disabledTextColor,
    ) = NavigationItemColors(
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
    @Composable
    fun iconColor(selected: Boolean, enabled: Boolean): Color {
        return when {
            !enabled -> disabledIconColor
            selected -> selectedIconColor
            else -> unselectedIconColor
        }
    }

    /**
     * Represents the text color for this item, depending on whether it is [selected].
     *
     * @param selected whether the item is selected
     * @param enabled whether the item is enabled
     */
    @Composable
    fun textColor(selected: Boolean, enabled: Boolean): Color {
        return when {
            !enabled -> disabledTextColor
            selected -> selectedTextColor
            else -> unselectedTextColor
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is NavigationItemColors) return false

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
 * Internal function to make a navigation item to be used with the Navigation Bar item or the
 * Navigation Rail item, depending on the passed in param values.
 *
 * @param selected whether this item is selected
 * @param onClick called when this item is clicked
 * @param icon icon for this item, typically an [Icon]
 * @param labelTextStyle the text style of the label of this item
 * @param indicatorShape the shape of the indicator when the item is selected
 * @param indicatorWidth the width of the indicator when the item is selected
 * @param indicatorHorizontalPadding the horizontal padding of the indicator
 * @param indicatorVerticalPadding the vertical padding of the indicator
 * @param colors [NavigationItemColors] that will be used to resolve the colors used for this item
 * in different states
 * @param modifier the [Modifier] to be applied to this item
 * @param enabled controls the enabled state of this item. When `false`, this component will not
 * respond to user input, and it will appear visually disabled and disabled to accessibility
 * services
 * @param label the text label for this item
 * @param badge optional badge to show on this item, typically a [Badge]
 * @param iconPosition the [NavigationItemIconPosition] for this icon
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this item. You can create and pass in your own `remember`ed instance to observe
 * [Interaction]s and customize the appearance / behavior of this item in different states
 *
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NavigationItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    labelTextStyle: TextStyle,
    indicatorShape: Shape,
    indicatorWidth: Dp,
    indicatorHorizontalPadding: Dp,
    indicatorVerticalPadding: Dp,
    colors: NavigationItemColors,
    modifier: Modifier,
    enabled: Boolean,
    label: @Composable (() -> Unit)?,
    badge: (@Composable () -> Unit)?,
    iconPosition: NavigationItemIconPosition,
    interactionSource: MutableInteractionSource
) {
    val styledIcon = @Composable {
        val iconColor = colors.iconColor(selected = selected, enabled = enabled)
        // If there's a label, don't have a11y services repeat the icon description.
        val clearSemantics = label != null
        Box(modifier = if (clearSemantics) Modifier.clearAndSetSemantics {} else Modifier) {
            CompositionLocalProvider(LocalContentColor provides iconColor, content = icon)
        }
    }
    val iconWithBadge =
        if (badge != null) {
            { BadgedBox(badge = { badge() }) { styledIcon() } }
        } else {
            styledIcon
        }

    val styledLabel: @Composable (() -> Unit)? = label?.let {
        @Composable {
            val textColor = colors.textColor(
                selected = selected,
                enabled = enabled
            )
            ProvideContentColorTextStyle(
                contentColor = textColor,
                textStyle = labelTextStyle,
                content = label
            )
        }
    }

    var itemWidth by remember { mutableIntStateOf(0) }

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
            .defaultMinSize(minWidth = NavigationItemMinWidth, minHeight = NavigationItemMinHeight)
            .onSizeChanged { itemWidth = it.width },
        contentAlignment = Alignment.Center,
        propagateMinConstraints = true,
    ) {
        val animationProgress: State<Float> = animateFloatAsState(
            targetValue = if (selected) 1f else 0f,
            animationSpec = tween(ItemAnimationDurationMillis)
        )

        var offsetInteractionSource: MappedInteractionSource? = null
        if (iconPosition == NavigationItemIconPosition.Top) {
            // The entire item is selectable, but only the indicator pill shows the ripple. To
            // achieve this, we re-map the coordinates of the item's InteractionSource into the
            // coordinates of the indicator.
            val deltaOffset: Offset
            with(LocalDensity.current) {
                deltaOffset = Offset(
                    (itemWidth - indicatorWidth.roundToPx()).toFloat() / 2,
                    IndicatorVerticalOffset.toPx()
                )
            }
            offsetInteractionSource = remember(interactionSource, deltaOffset) {
                MappedInteractionSource(interactionSource, deltaOffset)
            }
        }

        NavigationItemLayout(
            interactionSource = offsetInteractionSource ?: interactionSource,
            indicatorColor = colors.selectedIndicatorColor,
            indicatorShape = indicatorShape,
            icon = iconWithBadge,
            label = styledLabel,
            animationProgress = { animationProgress.value },
            indicatorHorizontalPadding = indicatorHorizontalPadding,
            indicatorVerticalPadding = indicatorVerticalPadding,
        )
    }
}

@Composable
private fun NavigationItemLayout(
    interactionSource: InteractionSource,
    indicatorColor: Color,
    indicatorShape: Shape,
    icon: @Composable () -> Unit,
    label: @Composable (() -> Unit)?,
    animationProgress: () -> Float,
    indicatorHorizontalPadding: Dp,
    indicatorVerticalPadding: Dp,
) {
    Layout({
        // Create the indicator ripple.
        Box(
            Modifier
                .layoutId(IndicatorRippleLayoutIdTag)
                .clip(indicatorShape)
                .indication(interactionSource, rippleOrFallbackImplementation())
        )
        // Create the indicator. The indicator has a width-expansion animation which interferes with
        // the timing of the ripple, which is why they are separate composables.
        Box(
            Modifier
                .layoutId(IndicatorLayoutIdTag)
                .graphicsLayer { alpha = animationProgress() }
                .background(
                    color = indicatorColor,
                    shape = indicatorShape,
                )
        )
        Box(Modifier.layoutId(IconLayoutIdTag)) { icon() }
        if (label != null) {
            Box(Modifier.layoutId(LabelLayoutIdTag)) { label() }
        }
    }) { measurables, constraints ->
        @Suppress("NAME_SHADOWING")
        val animationProgress = animationProgress()
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        // When measuring icon, account for the indicator in its constraints.
        val iconPlaceable =
            measurables.fastFirst { it.layoutId == IconLayoutIdTag }.measure(
                looseConstraints.offset(
                    horizontal = -indicatorHorizontalPadding.roundToPx() * 2,
                    vertical = -indicatorVerticalPadding.roundToPx() * 2)
            )
        // Next, when measuring the indicator and ripple, still need to obey looseConstraints.
        val totalIndicatorWidth = iconPlaceable.width + (indicatorHorizontalPadding * 2).roundToPx()
        val indicatorHeight =
            iconPlaceable.height + (indicatorVerticalPadding * 2).roundToPx()
        val animatedIndicatorWidth = (totalIndicatorWidth * animationProgress).roundToInt()
        val indicatorRipplePlaceable =
            measurables
                .fastFirst { it.layoutId == IndicatorRippleLayoutIdTag }
                .measure(
                    looseConstraints.constrain(
                        Constraints.fixed(
                            width = totalIndicatorWidth,
                            height = indicatorHeight))
                )
        val indicatorPlaceable =
            measurables
                .fastFirst { it.layoutId == IndicatorLayoutIdTag }
                .measure(
                    looseConstraints.constrain(
                        Constraints.fixed(
                            width = animatedIndicatorWidth,
                            height = indicatorHeight
                        )
                    )
                )
        // Finally, when measuring label, account for the indicator and the padding between
        // indicator and label.
        val labelPlaceable =
            label?.let {
                measurables
                    .fastFirst { it.layoutId == LabelLayoutIdTag }
                    .measure(
                        looseConstraints.offset(
                            vertical = -(indicatorVerticalPadding +
                                VerticalItemIndicatorToLabelPadding).roundToPx()
                        )
                    )
            }

        if (label == null) {
            placeIcon(iconPlaceable, indicatorRipplePlaceable, indicatorPlaceable, constraints)
        } else {
            placeLabelAndTopIcon(
                labelPlaceable!!,
                iconPlaceable,
                indicatorRipplePlaceable,
                indicatorPlaceable,
                constraints,
                indicatorVerticalPadding,
            )
        }
    }
}

/**
 * Places the provided [Placeable]s in the correct position.
 *
 * @param iconPlaceable icon placeable inside this item
 * @param indicatorRipplePlaceable indicator ripple placeable inside this item
 * @param indicatorPlaceable indicator placeable inside this item
 * @param constraints constraints of the item
 */
private fun MeasureScope.placeIcon(
    iconPlaceable: Placeable,
    indicatorRipplePlaceable: Placeable,
    indicatorPlaceable: Placeable,
    constraints: Constraints
): MeasureResult {
    val width = constraints.constrainWidth(indicatorRipplePlaceable.width)
    val height = constraints.constrainHeight(indicatorRipplePlaceable.height)

    val indicatorX = (width - indicatorPlaceable.width) / 2
    val indicatorY = (height - indicatorPlaceable.height) / 2
    val iconX = (width - iconPlaceable.width) / 2
    val iconY = (height - iconPlaceable.height) / 2
    val rippleX = (width - indicatorRipplePlaceable.width) / 2
    val rippleY = (height - indicatorRipplePlaceable.height) / 2

    return layout(width, height) {
        indicatorPlaceable.placeRelative(indicatorX, indicatorY)
        iconPlaceable.placeRelative(iconX, iconY)
        indicatorRipplePlaceable.placeRelative(rippleX, rippleY)
    }
}

/**
 * Places the provided [Placeable]s in the correct position.
 *
 * @param labelPlaceable text label placeable inside this item
 * @param iconPlaceable icon placeable inside this item
 * @param indicatorRipplePlaceable indicator ripple placeable inside this item
 * @param indicatorPlaceable indicator placeable inside this item, if it exists
 * @param constraints constraints of the item
 * @param indicatorVerticalPadding vertical padding of the indicator
 */
private fun MeasureScope.placeLabelAndTopIcon(
    labelPlaceable: Placeable,
    iconPlaceable: Placeable,
    indicatorRipplePlaceable: Placeable,
    indicatorPlaceable: Placeable,
    constraints: Constraints,
    indicatorVerticalPadding: Dp,
): MeasureResult {
    val width =
        constraints.constrainWidth(maxOf(labelPlaceable.width, indicatorRipplePlaceable.width))
    val contentHeight = indicatorRipplePlaceable.height +
        VerticalItemIndicatorToLabelPadding.toPx() +
        labelPlaceable.height
    val height = constraints.constrainHeight(contentHeight.roundToInt())

    val contentVerticalPadding = (height -
        // Vertical padding is apportioned based on icon + label, not indicator + label, so subtract
        // padding from content height (which is based on indicator + label).
        (contentHeight - indicatorVerticalPadding.toPx())) / 2
    // Icon should be `contentVerticalPadding` from top.
    val iconY = contentVerticalPadding.roundToInt()
    val iconX = (width - iconPlaceable.width) / 2
    val indicatorX = (width - indicatorPlaceable.width) / 2
    val indicatorY = iconY - indicatorVerticalPadding.roundToPx()
    val labelX = (width - labelPlaceable.width) / 2
    // Label should be fixed padding below icon.
    val labelY = iconY + iconPlaceable.height + indicatorVerticalPadding.toPx() +
        VerticalItemIndicatorToLabelPadding.toPx()
    val rippleX = (width - indicatorRipplePlaceable.width) / 2
    val rippleY = indicatorY

    return layout(width, height) {
        indicatorPlaceable.placeRelative(indicatorX, indicatorY)
        labelPlaceable.placeRelative(labelX, labelY.roundToInt())
        iconPlaceable.placeRelative(iconX, iconY)
        indicatorRipplePlaceable.placeRelative(rippleX, rippleY)
    }
}

private const val IndicatorRippleLayoutIdTag: String = "indicatorRipple"
private const val IndicatorLayoutIdTag: String = "indicator"
private const val IconLayoutIdTag: String = "icon"
private const val LabelLayoutIdTag: String = "label"
private const val ItemAnimationDurationMillis: Int = 100

private val IndicatorVerticalOffset: Dp = 12.dp
private val VerticalItemIndicatorToLabelPadding: Dp = 4.dp
private val NavigationItemMinWidth = NavigationRailItemWidth
private val NavigationItemMinHeight = NavigationRailItemHeight
