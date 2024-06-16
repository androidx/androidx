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
import androidx.compose.material3.internal.MappedInteractionSource
import androidx.compose.material3.internal.ProvideContentColorTextStyle
import androidx.compose.material3.internal.layoutId
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
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
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
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
import androidx.compose.ui.util.fastFirstOrNull
import kotlin.jvm.JvmInline
import kotlin.math.max
import kotlin.math.roundToInt

// TODO: Remove "internal".
/** Class that describes the different supported icon positions of the navigation item. */
@JvmInline
@ExperimentalMaterial3Api
internal value class NavigationItemIconPosition private constructor(private val value: Int) {
    companion object {
        /* The icon is positioned on top of the label. */
        val Top = NavigationItemIconPosition(0)

        /* The icon is positioned at the start of the label. */
        val Start = NavigationItemIconPosition(1)
    }

    override fun toString() =
        when (this) {
            Top -> "Top"
            Start -> "Start"
            else -> "Unknown"
        }
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
 *
 * TODO: Remove "internal".
 */
@Immutable
internal class NavigationItemColors
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
     * Returns a copy of this NavigationItemColors, optionally overriding some of the values. This
     * uses the Color.Unspecified to mean “use the value from the source”.
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
        NavigationItemColors(
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
    @Stable
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
 * @param indicatorToLabelVerticalPadding the padding between the indicator and the label when there
 *   is a top icon for this item (the iconPosition is Top)
 * @param startIconToLabelHorizontalPadding the padding between the start icon and the label of the
 *   item (the iconPosition is Start)
 * @param topIconItemVerticalPadding the vertical padding of the item when the iconPosition is Top
 * @param colors [NavigationItemColors] that will be used to resolve the colors used for this item
 *   in different states
 * @param modifier the [Modifier] to be applied to this item
 * @param enabled controls the enabled state of this item. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services
 * @param label the text label for this item
 * @param badge optional badge to show on this item, typically a [Badge]
 * @param iconPosition the [NavigationItemIconPosition] for this icon
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 *   for this item. You can create and pass in your own `remember`ed instance to observe
 *   [Interaction]s and customize the appearance / behavior of this item in different states
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
    indicatorToLabelVerticalPadding: Dp,
    startIconToLabelHorizontalPadding: Dp,
    topIconItemVerticalPadding: Dp,
    colors: NavigationItemColors,
    modifier: Modifier,
    enabled: Boolean,
    label: @Composable (() -> Unit)?,
    badge: (@Composable () -> Unit)?,
    iconPosition: NavigationItemIconPosition,
    interactionSource: MutableInteractionSource
) {
    val styledIcon =
        @Composable {
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

    val styledLabel: @Composable (() -> Unit)? =
        label?.let {
            @Composable {
                val textColor = colors.textColor(selected = selected, enabled = enabled)
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
        val animationProgress: State<Float> =
            animateFloatAsState(
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
                deltaOffset =
                    Offset(
                        (itemWidth - indicatorWidth.roundToPx()).toFloat() / 2,
                        IndicatorVerticalOffset.toPx()
                    )
            }
            offsetInteractionSource =
                remember(interactionSource, deltaOffset) {
                    MappedInteractionSource(interactionSource, deltaOffset)
                }
        }

        NavigationItemLayout(
            interactionSource = offsetInteractionSource ?: interactionSource,
            indicatorColor = colors.selectedIndicatorColor,
            indicatorShape = indicatorShape,
            icon = iconWithBadge,
            iconPosition = iconPosition,
            label = styledLabel,
            animationProgress = { animationProgress.value },
            indicatorHorizontalPadding = indicatorHorizontalPadding,
            indicatorVerticalPadding = indicatorVerticalPadding,
            indicatorToLabelVerticalPadding = indicatorToLabelVerticalPadding,
            startIconToLabelHorizontalPadding = startIconToLabelHorizontalPadding,
            topIconItemVerticalPadding = topIconItemVerticalPadding
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NavigationItemLayout(
    interactionSource: InteractionSource,
    indicatorColor: Color,
    indicatorShape: Shape,
    icon: @Composable () -> Unit,
    iconPosition: NavigationItemIconPosition,
    label: @Composable (() -> Unit)?,
    animationProgress: () -> Float,
    indicatorHorizontalPadding: Dp,
    indicatorVerticalPadding: Dp,
    indicatorToLabelVerticalPadding: Dp,
    startIconToLabelHorizontalPadding: Dp,
    topIconItemVerticalPadding: Dp
) {
    Layout(
        content = {
            // Create the indicator ripple.
            Box(
                Modifier.layoutId(IndicatorRippleLayoutIdTag)
                    .clip(indicatorShape)
                    .indication(interactionSource, rippleOrFallbackImplementation())
            )
            // Create the indicator. The indicator has a width-expansion animation which interferes
            // with
            // the timing of the ripple, which is why they are separate composables.
            Box(
                Modifier.layoutId(IndicatorLayoutIdTag)
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
        },
        measurePolicy =
            if (label == null || iconPosition == NavigationItemIconPosition.Top) {
                TopIconOrIconOnlyMeasurePolicy(
                    label != null,
                    animationProgress,
                    indicatorHorizontalPadding,
                    indicatorVerticalPadding,
                    indicatorToLabelVerticalPadding,
                    topIconItemVerticalPadding
                )
            } else {
                StartIconMeasurePolicy(
                    animationProgress,
                    indicatorHorizontalPadding,
                    indicatorVerticalPadding,
                    startIconToLabelHorizontalPadding,
                )
            }
    )
}

private class TopIconOrIconOnlyMeasurePolicy(
    val hasLabel: Boolean,
    val animationProgress: () -> Float,
    val indicatorHorizontalPadding: Dp,
    val indicatorVerticalPadding: Dp,
    val indicatorToLabelVerticalPadding: Dp,
    val topIconItemVerticalPadding: Dp,
) : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        @Suppress("NAME_SHADOWING") val animationProgress = animationProgress()
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        // When measuring icon, account for the indicator in its constraints.
        val iconPlaceable =
            measurables
                .fastFirst { it.layoutId == IconLayoutIdTag }
                .measure(
                    looseConstraints.offset(
                        horizontal = -(indicatorHorizontalPadding * 2).roundToPx(),
                        vertical = -(indicatorVerticalPadding * 2).roundToPx()
                    )
                )
        // Next, when measuring the indicator and ripple, still need to obey looseConstraints.
        val totalIndicatorWidth = iconPlaceable.width + (indicatorHorizontalPadding * 2).roundToPx()
        val indicatorHeight = iconPlaceable.height + (indicatorVerticalPadding * 2).roundToPx()
        val animatedIndicatorWidth = (totalIndicatorWidth * animationProgress).roundToInt()
        val indicatorRipplePlaceable =
            measurables
                .fastFirst { it.layoutId == IndicatorRippleLayoutIdTag }
                .measure(
                    looseConstraints.constrain(
                        Constraints.fixed(width = totalIndicatorWidth, height = indicatorHeight)
                    )
                )
        val indicatorPlaceable =
            measurables
                .fastFirst { it.layoutId == IndicatorLayoutIdTag }
                .measure(
                    looseConstraints.constrain(
                        Constraints.fixed(width = animatedIndicatorWidth, height = indicatorHeight)
                    )
                )

        return if (hasLabel) {
            // When measuring label, account for the indicator and the padding between indicator and
            // label.
            val labelPlaceable =
                measurables
                    .fastFirst { it.layoutId == LabelLayoutIdTag }
                    .measure(
                        looseConstraints.offset(
                            vertical =
                                -(indicatorPlaceable.height +
                                    indicatorToLabelVerticalPadding.roundToPx())
                        )
                    )

            placeLabelAndTopIcon(
                labelPlaceable,
                iconPlaceable,
                indicatorRipplePlaceable,
                indicatorPlaceable,
                constraints,
                indicatorToLabelVerticalPadding,
                indicatorVerticalPadding,
                topIconItemVerticalPadding
            )
        } else {
            placeIcon(iconPlaceable, indicatorRipplePlaceable, indicatorPlaceable, constraints)
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int
    ): Int {
        val iconHeight =
            measurables.fastFirst { it.layoutId == IconLayoutIdTag }.maxIntrinsicHeight(width)
        val labelHeight =
            measurables
                .fastFirstOrNull { it.layoutId == LabelLayoutIdTag }
                ?.maxIntrinsicHeight(width) ?: 0
        val paddings =
            (topIconItemVerticalPadding * 2 +
                    indicatorVerticalPadding * 2 +
                    indicatorToLabelVerticalPadding)
                .roundToPx()

        return iconHeight + labelHeight + paddings
    }
}

private class StartIconMeasurePolicy(
    val animationProgress: () -> Float,
    val indicatorHorizontalPadding: Dp,
    val indicatorVerticalPadding: Dp,
    val startIconToLabelHorizontalPadding: Dp,
) : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        @Suppress("NAME_SHADOWING") val animationProgress = animationProgress()
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        // When measuring icon, account for the indicator in its constraints.
        val iconConstraints =
            looseConstraints.offset(
                horizontal = -(indicatorHorizontalPadding * 2).roundToPx(),
                vertical = -(indicatorVerticalPadding * 2).roundToPx()
            )
        val iconPlaceable =
            measurables.fastFirst { it.layoutId == IconLayoutIdTag }.measure(iconConstraints)
        // When measuring the label, account for the indicator, the icon, and the padding between
        // icon and label.
        val labelPlaceable =
            measurables
                .fastFirst { it.layoutId == LabelLayoutIdTag }
                .measure(
                    iconConstraints.offset(
                        horizontal =
                            -(iconPlaceable.width + startIconToLabelHorizontalPadding.roundToPx())
                    )
                )

        val totalIndicatorWidth =
            iconPlaceable.width +
                labelPlaceable.width +
                (startIconToLabelHorizontalPadding + indicatorHorizontalPadding * 2).roundToPx()
        val indicatorHeight =
            max(iconPlaceable.height, labelPlaceable.height) +
                (indicatorVerticalPadding * 2).roundToPx()
        val animatedIndicatorWidth = (totalIndicatorWidth * animationProgress).roundToInt()
        // When measuring the indicator and ripple, still need to obey looseConstraints.
        val indicatorRipplePlaceable =
            measurables
                .fastFirst { it.layoutId == IndicatorRippleLayoutIdTag }
                .measure(
                    looseConstraints.constrain(
                        Constraints.fixed(width = totalIndicatorWidth, height = indicatorHeight)
                    )
                )
        val indicatorPlaceable =
            measurables
                .fastFirst { it.layoutId == IndicatorLayoutIdTag }
                .measure(
                    looseConstraints.constrain(
                        Constraints.fixed(width = animatedIndicatorWidth, height = indicatorHeight)
                    )
                )

        return placeLabelAndStartIcon(
            labelPlaceable,
            iconPlaceable,
            indicatorRipplePlaceable,
            indicatorPlaceable,
            constraints,
            startIconToLabelHorizontalPadding
        )
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int
    ): Int {
        val iconWidth =
            measurables.fastFirst { it.layoutId == IconLayoutIdTag }.maxIntrinsicWidth(height)
        val labelWidth =
            measurables.fastFirst { it.layoutId == LabelLayoutIdTag }.maxIntrinsicWidth(height)
        val paddings =
            (indicatorHorizontalPadding * 2 + startIconToLabelHorizontalPadding).roundToPx()

        return iconWidth + labelWidth + paddings
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int
    ): Int {
        val iconHeight =
            measurables.fastFirst { it.layoutId == IconLayoutIdTag }.maxIntrinsicHeight(width)
        val labelHeight =
            measurables.fastFirst { it.layoutId == LabelLayoutIdTag }.maxIntrinsicHeight(width)
        val paddings = (indicatorVerticalPadding * 2).roundToPx()

        return max(iconHeight, labelHeight) + paddings
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
 * @param indicatorToLabelVerticalPadding the padding between the bottom of the indicator and the
 *   top of the label
 * @param indicatorVerticalPadding vertical padding of the indicator
 * @param topIconItemVerticalPadding vertical padding of the item
 */
private fun MeasureScope.placeLabelAndTopIcon(
    labelPlaceable: Placeable,
    iconPlaceable: Placeable,
    indicatorRipplePlaceable: Placeable,
    indicatorPlaceable: Placeable,
    constraints: Constraints,
    indicatorToLabelVerticalPadding: Dp,
    indicatorVerticalPadding: Dp,
    topIconItemVerticalPadding: Dp,
): MeasureResult {
    val width =
        constraints.constrainWidth(maxOf(labelPlaceable.width, indicatorRipplePlaceable.width))
    val contentHeight =
        indicatorRipplePlaceable.height +
            indicatorToLabelVerticalPadding.toPx() +
            labelPlaceable.height
    val height =
        constraints.constrainHeight(
            (contentHeight + topIconItemVerticalPadding.toPx() * 2).roundToInt()
        )

    val iconY = (topIconItemVerticalPadding + indicatorVerticalPadding).roundToPx()
    val iconX = (width - iconPlaceable.width) / 2
    val indicatorX = (width - indicatorPlaceable.width) / 2
    val indicatorY = iconY - indicatorVerticalPadding.roundToPx()
    val labelX = (width - labelPlaceable.width) / 2
    // Label should be fixed padding below icon.
    val labelY =
        iconY +
            iconPlaceable.height +
            (indicatorVerticalPadding + indicatorToLabelVerticalPadding).roundToPx()
    val rippleX = (width - indicatorRipplePlaceable.width) / 2
    val rippleY = indicatorY

    return layout(width, height) {
        indicatorPlaceable.placeRelative(indicatorX, indicatorY)
        labelPlaceable.placeRelative(labelX, labelY)
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
 * @param indicatorPlaceable indicator placeable inside this item
 * @param constraints constraints of the item
 * @param startIconToLabelHorizontalPadding the padding between end of the icon and the start of the
 *   label of this item
 */
private fun MeasureScope.placeLabelAndStartIcon(
    labelPlaceable: Placeable,
    iconPlaceable: Placeable,
    indicatorRipplePlaceable: Placeable,
    indicatorPlaceable: Placeable,
    constraints: Constraints,
    startIconToLabelHorizontalPadding: Dp
): MeasureResult {
    val width = constraints.constrainWidth(indicatorRipplePlaceable.width)
    val height = constraints.constrainHeight(indicatorRipplePlaceable.height)

    val indicatorX = (width - indicatorPlaceable.width) / 2
    val indicatorY = (height - indicatorPlaceable.height) / 2
    val iconY = (height - iconPlaceable.height) / 2
    val labelY = (height - labelPlaceable.height) / 2
    val itemContentWidth =
        iconPlaceable.width + startIconToLabelHorizontalPadding.roundToPx() + labelPlaceable.width
    val iconX = (width - itemContentWidth) / 2
    val labelX = iconX + iconPlaceable.width + startIconToLabelHorizontalPadding.roundToPx()
    val rippleX = (width - indicatorRipplePlaceable.width) / 2
    val rippleY = (height - indicatorRipplePlaceable.height) / 2

    return layout(width, height) {
        indicatorPlaceable.placeRelative(indicatorX, indicatorY)
        labelPlaceable.placeRelative(labelX, labelY)
        iconPlaceable.placeRelative(iconX, iconY)
        indicatorRipplePlaceable.placeRelative(rippleX, rippleY)
    }
}

/*@VisibleForTesting*/
internal val NavigationItemMinWidth = NavigationRailItemWidth
/*@VisibleForTesting*/
internal val NavigationItemMinHeight = NavigationRailItemHeight

private const val IndicatorRippleLayoutIdTag: String = "indicatorRipple"
private const val IndicatorLayoutIdTag: String = "indicator"
private const val IconLayoutIdTag: String = "icon"
private const val LabelLayoutIdTag: String = "label"
private const val ItemAnimationDurationMillis: Int = 100

private val IndicatorVerticalOffset: Dp = 12.dp
