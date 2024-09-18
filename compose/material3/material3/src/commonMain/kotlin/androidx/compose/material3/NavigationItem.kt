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

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
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
import androidx.compose.ui.util.lerp
import kotlin.jvm.JvmInline
import kotlin.math.max
import kotlin.math.roundToInt

/** Class that describes the different supported icon positions of the navigation item. */
@JvmInline
@ExperimentalMaterial3ExpressiveApi
value class NavigationItemIconPosition private constructor(private val value: Int) {
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
 */
@Immutable
class NavigationItemColors
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
 * Internal function to make a navigation suite component, such as the [ShortNavigationBarItem].
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
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
    val iconWithBadge: @Composable () -> Unit = {
        StyledIcon(selected, icon, colors, enabled, badge)
    }
    val styledLabel: @Composable (() -> Unit)? =
        if (label == null) {
            null
        } else {
            { StyledLabel(selected, labelTextStyle, colors, enabled, label) }
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
            .defaultMinSize(
                minWidth = LocalMinimumInteractiveComponentSize.current,
                minHeight = LocalMinimumInteractiveComponentSize.current
            )
            .onSizeChanged { itemWidth = it.width },
        contentAlignment = Alignment.Center,
        propagateMinConstraints = true,
    ) {
        val indicatorAnimationProgress = animateIndicatorProgressAsState(selected)
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
            indicatorAnimationProgress = { indicatorAnimationProgress.value.coerceAtLeast(0f) },
            indicatorHorizontalPadding = indicatorHorizontalPadding,
            indicatorVerticalPadding = indicatorVerticalPadding,
            indicatorToLabelVerticalPadding = indicatorToLabelVerticalPadding,
            startIconToLabelHorizontalPadding = startIconToLabelHorizontalPadding,
            topIconItemVerticalPadding = topIconItemVerticalPadding
        )
    }
}

/**
 * Internal function to make an animated navigation item to be used with a navigation suite
 * component, such as the [WideNavigationRailItem].
 *
 * This item will animate its elements when the value of [iconPosition] changes.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun AnimatedNavigationItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    indicatorShape: Shape,
    topIconIndicatorWidth: Dp,
    topIconLabelTextStyle: TextStyle,
    startIconLabelTextStyle: TextStyle,
    topIconIndicatorHorizontalPadding: Dp,
    topIconIndicatorVerticalPadding: Dp,
    topIconIndicatorToLabelVerticalPadding: Dp,
    startIconIndicatorHorizontalPadding: Dp,
    startIconIndicatorVerticalPadding: Dp,
    startIconToLabelHorizontalPadding: Dp,
    startIconItemPadding: Dp,
    colors: NavigationItemColors,
    modifier: Modifier,
    enabled: Boolean,
    label: @Composable (() -> Unit),
    badge: (@Composable () -> Unit)?,
    iconPosition: NavigationItemIconPosition,
    interactionSource: MutableInteractionSource
) {
    val iconWithBadge: @Composable () -> Unit = {
        StyledIcon(selected, icon, colors, enabled, badge)
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
            .defaultMinSize(
                minWidth = LocalMinimumInteractiveComponentSize.current,
                minHeight = LocalMinimumInteractiveComponentSize.current
            )
            .onSizeChanged { itemWidth = it.width },
        contentAlignment = Alignment.Center,
        propagateMinConstraints = true,
    ) {
        val isIconPositionTop = iconPosition == NavigationItemIconPosition.Top
        val indicatorAnimationProgress = animateIndicatorProgressAsState(selected)
        val iconPositionProgress by
            animateFloatAsState(
                targetValue = if (isIconPositionTop) 0f else 1f,
                // TODO Load the motionScheme tokens from the component tokens file
                animationSpec = MotionSchemeKeyTokens.DefaultSpatial.value()
            )

        // We'll always display only one label, but for the animation to be correct we need two
        // separate composables that will fade in/out appropriately.
        val labelTopIconAlphaProgress by
            animateFloatAsState(
                targetValue = if (isIconPositionTop) 1f else 0f,
                // TODO Load the motionScheme tokens from the component tokens file
                animationSpec = MotionSchemeKeyTokens.DefaultEffects.value(),
                visibilityThreshold =
                    if (isIconPositionTop) Spring.DefaultDisplacementThreshold
                    else LabelAnimationVisibilityThreshold
            )
        val labelTopIcon: @Composable (() -> Unit) = {
            Box(
                modifier =
                    Modifier.graphicsLayer { alpha = labelTopIconAlphaProgress }
                        .then(
                            if (isIconPositionTop) {
                                Modifier
                            } else {
                                // If this label is not being displayed, remove semantics so item's
                                // label isn't announced twice.
                                Modifier.clearAndSetSemantics {}
                            }
                        )
            ) {
                StyledLabel(selected, topIconLabelTextStyle, colors, enabled, label)
            }
        }
        val labelStartIcon =
            @Composable {
                Box(
                    modifier =
                        Modifier.graphicsLayer { alpha = 1f - labelTopIconAlphaProgress }
                            .then(
                                if (isIconPositionTop) {
                                    // If this label is not being displayed, remove semantics so
                                    // item's label isn't announced twice.
                                    Modifier.clearAndSetSemantics {}
                                } else {
                                    Modifier
                                }
                            )
                ) {
                    StyledLabel(selected, startIconLabelTextStyle, colors, enabled, label)
                }
            }

        var offsetInteractionSource: MappedInteractionSource? = null
        if (isIconPositionTop) {
            // The entire item is selectable, but only the indicator pill shows the ripple. To
            // achieve this, we re-map the coordinates of the item's InteractionSource into the
            // coordinates of the indicator.
            val deltaOffset: Offset
            with(LocalDensity.current) {
                deltaOffset =
                    Offset(
                        (itemWidth - topIconIndicatorWidth.roundToPx()).toFloat() / 2,
                        IndicatorVerticalOffset.toPx()
                    )
            }
            offsetInteractionSource =
                remember(interactionSource, deltaOffset) {
                    MappedInteractionSource(interactionSource, deltaOffset)
                }
        }

        AnimatedNavigationItemLayout(
            interactionSource = offsetInteractionSource ?: interactionSource,
            indicatorColor = colors.selectedIndicatorColor,
            indicatorShape = indicatorShape,
            indicatorAnimationProgress = { indicatorAnimationProgress.value.coerceAtLeast(0f) },
            icon = iconWithBadge,
            iconPosition = iconPosition,
            iconPositionProgress = { iconPositionProgress.coerceAtLeast(0f) },
            labelTopIcon = labelTopIcon,
            labelStartIcon = labelStartIcon,
            topIconIndicatorHorizontalPadding = topIconIndicatorHorizontalPadding,
            topIconIndicatorVerticalPadding = topIconIndicatorVerticalPadding,
            topIconIndicatorToLabelVerticalPadding = topIconIndicatorToLabelVerticalPadding,
            startIconIndicatorHorizontalPadding = startIconIndicatorHorizontalPadding,
            startIconIndicatorVerticalPadding = startIconIndicatorVerticalPadding,
            startIconToLabelHorizontalPadding = startIconToLabelHorizontalPadding,
            startIconItemPadding = startIconItemPadding
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NavigationItemLayout(
    interactionSource: InteractionSource,
    indicatorColor: Color,
    indicatorShape: Shape,
    icon: @Composable () -> Unit,
    iconPosition: NavigationItemIconPosition,
    label: @Composable (() -> Unit)?,
    indicatorAnimationProgress: () -> Float,
    indicatorHorizontalPadding: Dp,
    indicatorVerticalPadding: Dp,
    indicatorToLabelVerticalPadding: Dp,
    startIconToLabelHorizontalPadding: Dp,
    topIconItemVerticalPadding: Dp
) {
    Layout(
        modifier = Modifier.badgeBounds(),
        content = {
            // Create the indicator ripple.
            IndicatorRipple(interactionSource, indicatorShape)
            // Create the indicator. The indicator has a width-expansion animation which interferes
            // with the timing of the ripple, which is why they are separate composables.
            Indicator(indicatorColor, indicatorShape, indicatorAnimationProgress)

            Box(Modifier.layoutId(IconLayoutIdTag)) { icon() }

            if (label != null) {
                Box(Modifier.layoutId(LabelLayoutIdTag)) { label() }
            }
        },
        measurePolicy =
            if (label == null || iconPosition == NavigationItemIconPosition.Top) {
                TopIconOrIconOnlyMeasurePolicy(
                    label != null,
                    indicatorAnimationProgress,
                    indicatorHorizontalPadding,
                    indicatorVerticalPadding,
                    indicatorToLabelVerticalPadding,
                    topIconItemVerticalPadding
                )
            } else {
                StartIconMeasurePolicy(
                    indicatorAnimationProgress,
                    indicatorHorizontalPadding,
                    indicatorVerticalPadding,
                    startIconToLabelHorizontalPadding,
                )
            }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AnimatedNavigationItemLayout(
    interactionSource: InteractionSource,
    indicatorColor: Color,
    indicatorShape: Shape,
    indicatorAnimationProgress: () -> Float,
    icon: @Composable () -> Unit,
    iconPosition: NavigationItemIconPosition,
    iconPositionProgress: () -> Float,
    labelTopIcon: @Composable (() -> Unit),
    labelStartIcon: @Composable (() -> Unit),
    topIconIndicatorHorizontalPadding: Dp,
    topIconIndicatorVerticalPadding: Dp,
    topIconIndicatorToLabelVerticalPadding: Dp,
    startIconIndicatorHorizontalPadding: Dp,
    startIconIndicatorVerticalPadding: Dp,
    startIconToLabelHorizontalPadding: Dp,
    startIconItemPadding: Dp,
) {
    Layout(
        modifier = Modifier.badgeBounds(),
        content = {
            // Create the indicator ripple.
            IndicatorRipple(interactionSource, indicatorShape)
            // Create the indicator. The indicator has a width-expansion animation which interferes
            // with the timing of the ripple, which is why they are separate composables.
            Indicator(indicatorColor, indicatorShape, indicatorAnimationProgress)

            Box(Modifier.layoutId(IconLayoutIdTag)) { icon() }

            Box(Modifier.layoutId(AnimatedLabelTopIconLayoutIdTag)) { labelTopIcon() }
            Box(Modifier.layoutId(AnimatedLabelStartIconLayoutIdTag)) { labelStartIcon() }
        },
        measurePolicy =
            AnimatedMeasurePolicy(
                iconPosition = iconPosition,
                iconPositionProgress = iconPositionProgress,
                indicatorAnimationProgress = indicatorAnimationProgress,
                topIconIndicatorHorizontalPadding = topIconIndicatorHorizontalPadding,
                topIconIndicatorVerticalPadding = topIconIndicatorVerticalPadding,
                topIconIndicatorToLabelVerticalPadding = topIconIndicatorToLabelVerticalPadding,
                startIconIndicatorHorizontalPadding = startIconIndicatorHorizontalPadding,
                startIconIndicatorVerticalPadding = startIconIndicatorVerticalPadding,
                startIconToLabelHorizontalPadding = startIconToLabelHorizontalPadding,
                startIconItemPadding = startIconItemPadding
            )
    )
}

private class TopIconOrIconOnlyMeasurePolicy(
    val hasLabel: Boolean,
    val indicatorAnimationProgress: () -> Float,
    val indicatorHorizontalPadding: Dp,
    val indicatorVerticalPadding: Dp,
    val indicatorToLabelVerticalPadding: Dp,
    val topIconItemVerticalPadding: Dp,
) : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        @Suppress("NAME_SHADOWING") val indicatorAnimationProgress = indicatorAnimationProgress()
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
        val animatedIndicatorWidth = (totalIndicatorWidth * indicatorAnimationProgress).roundToInt()
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
    val indicatorAnimationProgress: () -> Float,
    val indicatorHorizontalPadding: Dp,
    val indicatorVerticalPadding: Dp,
    val startIconToLabelHorizontalPadding: Dp,
) : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        @Suppress("NAME_SHADOWING") val indicatorAnimationProgress = indicatorAnimationProgress()
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        // When measuring icon, account for the indicator in its constraints.
        val iconPlaceable =
            measurables.fastFirst { it.layoutId == IconLayoutIdTag }.measure(looseConstraints)
        // When measuring the label, account for the indicator, the icon, and the padding between
        // icon and label.
        val labelPlaceable =
            measurables
                .fastFirst { it.layoutId == LabelLayoutIdTag }
                .measure(
                    looseConstraints.offset(
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
        val animatedIndicatorWidth = (totalIndicatorWidth * indicatorAnimationProgress).roundToInt()
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private class AnimatedMeasurePolicy(
    val iconPosition: NavigationItemIconPosition,
    val iconPositionProgress: () -> Float,
    val indicatorAnimationProgress: () -> Float,
    val topIconIndicatorHorizontalPadding: Dp,
    val topIconIndicatorVerticalPadding: Dp,
    val topIconIndicatorToLabelVerticalPadding: Dp,
    val startIconIndicatorHorizontalPadding: Dp,
    val startIconIndicatorVerticalPadding: Dp,
    val startIconToLabelHorizontalPadding: Dp,
    val startIconItemPadding: Dp,
) : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        @Suppress("NAME_SHADOWING") val indicatorAnimationProgress = indicatorAnimationProgress()
        val iconPositionProgressValue = iconPositionProgress()
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)

        val iconPlaceable =
            measurables.fastFirst { it.layoutId == IconLayoutIdTag }.measure(looseConstraints)

        val labelPlaceableTopIcon =
            measurables
                .fastFirst { it.layoutId == AnimatedLabelTopIconLayoutIdTag }
                .measure(looseConstraints)
        val labelPlaceableStartIcon =
            measurables
                .fastFirst { it.layoutId == AnimatedLabelStartIconLayoutIdTag }
                .measure(looseConstraints)

        val topIconIndicatorWidth =
            iconPlaceable.width + (topIconIndicatorHorizontalPadding * 2).roundToPx()
        val topIconIndicatorHeight =
            iconPlaceable.height + (topIconIndicatorVerticalPadding * 2).roundToPx()

        val startIconIndicatorWidth =
            iconPlaceable.width +
                labelPlaceableStartIcon.width +
                (startIconToLabelHorizontalPadding + startIconIndicatorHorizontalPadding * 2)
                    .roundToPx()
        val startIconIndicatorHeight =
            max(iconPlaceable.height, labelPlaceableStartIcon.height) +
                (startIconIndicatorVerticalPadding * 2).roundToPx()

        val indicatorWidthProgress =
            lerp(topIconIndicatorWidth, startIconIndicatorWidth, iconPositionProgressValue)
        val animatedIndicatorWidth =
            (indicatorWidthProgress * indicatorAnimationProgress).roundToInt()
        val indicatorHeightProgress =
            lerp(topIconIndicatorHeight, startIconIndicatorHeight, iconPositionProgressValue)

        val indicatorRipplePlaceable =
            measurables
                .fastFirst { it.layoutId == IndicatorRippleLayoutIdTag }
                .measure(
                    looseConstraints.constrain(
                        Constraints.fixed(
                            width = indicatorWidthProgress,
                            height = indicatorHeightProgress
                        )
                    )
                )
        val indicatorPlaceable =
            measurables
                .fastFirst { it.layoutId == IndicatorLayoutIdTag }
                .measure(
                    looseConstraints.constrain(
                        Constraints.fixed(
                            width = animatedIndicatorWidth,
                            height = indicatorHeightProgress
                        )
                    )
                )

        return placeAnimatedLabelAndIcon(
            iconPosition = iconPosition,
            iconPositionProgress = iconPositionProgress,
            labelPlaceableTopIcon = labelPlaceableTopIcon,
            labelPlaceableStartIcon = labelPlaceableStartIcon,
            iconPlaceable = iconPlaceable,
            indicatorRipplePlaceable = indicatorRipplePlaceable,
            indicatorPlaceable = indicatorPlaceable,
            topIconIndicatorWidth = topIconIndicatorWidth,
            constraints = looseConstraints,
            topIconIndicatorToLabelVerticalPadding = topIconIndicatorToLabelVerticalPadding,
            topIconIndicatorVerticalPadding = topIconIndicatorVerticalPadding,
            startIconIndicatorHorizontalPadding = startIconIndicatorHorizontalPadding,
            startIconIndicatorVerticalPadding = startIconIndicatorVerticalPadding,
            startIconToLabelHorizontalPadding = startIconToLabelHorizontalPadding,
            startIconItemPadding = startIconItemPadding
        )
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int
    ): Int {
        val iconWidth =
            measurables.fastFirst { it.layoutId == IconLayoutIdTag }.maxIntrinsicWidth(height)
        if (iconPosition == NavigationItemIconPosition.Top) {
            val labelWidth =
                measurables
                    .fastFirst { it.layoutId == AnimatedLabelTopIconLayoutIdTag }
                    .maxIntrinsicWidth(height)
            val paddings = (topIconIndicatorHorizontalPadding * 2).roundToPx()

            return maxOf(labelWidth, (iconWidth + paddings))
        } else {
            val labelWidth =
                measurables
                    .fastFirst { it.layoutId == AnimatedLabelStartIconLayoutIdTag }
                    .maxIntrinsicWidth(height)
            val paddings =
                (startIconIndicatorHorizontalPadding * 2 + startIconToLabelHorizontalPadding)
                    .roundToPx()

            return iconWidth + labelWidth + paddings
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun MeasureScope.placeAnimatedLabelAndIcon(
    iconPosition: NavigationItemIconPosition,
    iconPositionProgress: () -> Float,
    labelPlaceableTopIcon: Placeable,
    labelPlaceableStartIcon: Placeable,
    iconPlaceable: Placeable,
    indicatorRipplePlaceable: Placeable,
    indicatorPlaceable: Placeable,
    topIconIndicatorWidth: Int,
    constraints: Constraints,
    topIconIndicatorToLabelVerticalPadding: Dp,
    topIconIndicatorVerticalPadding: Dp,
    startIconIndicatorHorizontalPadding: Dp,
    startIconIndicatorVerticalPadding: Dp,
    startIconToLabelHorizontalPadding: Dp,
    startIconItemPadding: Dp,
): MeasureResult {
    @Suppress("NAME_SHADOWING") val iconPositionProgress = iconPositionProgress()
    val isIconPositionTop = iconPosition == NavigationItemIconPosition.Top
    val widthTopIcon =
        constraints.constrainWidth(maxOf(labelPlaceableTopIcon.width, topIconIndicatorWidth))
    val widthStartIcon = constraints.constrainWidth(indicatorRipplePlaceable.width)
    val width = widthTopIcon + (widthStartIcon - widthTopIcon) * iconPositionProgress

    val heightTopIcon =
        constraints.constrainHeight(
            (indicatorRipplePlaceable.height +
                    topIconIndicatorToLabelVerticalPadding.toPx() +
                    labelPlaceableTopIcon.height)
                .roundToInt()
        )
    val heightStartIcon = constraints.constrainHeight(indicatorRipplePlaceable.height)
    val height = lerp(heightTopIcon, heightStartIcon, iconPositionProgress)

    val topIconHorizontalOffset = startIconItemPadding.roundToPx() * iconPositionProgress

    val indicatorXTopIcon =
        ((widthTopIcon - indicatorPlaceable.width) / 2 + topIconHorizontalOffset).roundToInt()
    val indicatorXStartIcon = (widthStartIcon - indicatorPlaceable.width) / 2
    val indicatorX = lerp(indicatorXTopIcon, indicatorXStartIcon, iconPositionProgress)

    val rippleXTopIcon = (width - indicatorRipplePlaceable.width) / 2 + topIconHorizontalOffset
    val rippleXStartIcon = (width - indicatorRipplePlaceable.width) / 2
    val rippleX = lerp(rippleXTopIcon, rippleXStartIcon, iconPositionProgress)

    val iconXTopIcon = (widthTopIcon - iconPlaceable.width) / 2
    val iconXStartIcon = startIconIndicatorHorizontalPadding.roundToPx()

    val iconYTopIcon = topIconIndicatorVerticalPadding.roundToPx()
    val iconYStartIcon = startIconIndicatorVerticalPadding.roundToPx()

    val iconX = lerp(iconXTopIcon, iconXStartIcon, iconPositionProgress)
    val iconY = lerp(iconYTopIcon, iconYStartIcon, iconPositionProgress)

    val labelXTopIcon = (widthTopIcon - labelPlaceableTopIcon.width) / 2
    val labelYTopIcon =
        iconY +
            iconPlaceable.height +
            (topIconIndicatorToLabelVerticalPadding + topIconIndicatorToLabelVerticalPadding)
                .roundToPx()

    val labelXStartIconHorizontalOffset =
        if (isIconPositionTop && iconPositionProgress > 0f) {
            0f
        } else {
            startIconItemPadding.roundToPx() * (1f - iconPositionProgress)
        }
    val labelXStartIcon =
        iconX + iconPlaceable.width + startIconToLabelHorizontalPadding.roundToPx() -
            labelXStartIconHorizontalOffset
    val labelYStartIcon = (height - labelPlaceableStartIcon.height) / 2

    return layout(width.roundToInt(), height) {
        indicatorPlaceable.placeRelative(indicatorX, 0)
        iconPlaceable.placeRelative(iconX, iconY)
        labelPlaceableTopIcon.placeRelative(labelXTopIcon, labelYTopIcon)
        labelPlaceableStartIcon.placeRelative(labelXStartIcon.roundToInt(), labelYStartIcon)
        indicatorRipplePlaceable.placeRelative(rippleX.roundToInt(), 0)
    }
}

@Composable
private fun StyledIcon(
    selected: Boolean,
    icon: @Composable () -> Unit,
    colors: NavigationItemColors,
    enabled: Boolean,
    badge: (@Composable () -> Unit)?,
) {
    val iconColor = colors.iconColor(selected = selected, enabled = enabled)
    val styledIcon: @Composable () -> Unit = {
        CompositionLocalProvider(LocalContentColor provides iconColor, content = icon)
    }

    if (badge != null) {
        BadgedBox(badge = { badge() }) { styledIcon() }
    } else {
        styledIcon()
    }
}

@Composable
private fun StyledLabel(
    selected: Boolean,
    labelTextStyle: TextStyle,
    colors: NavigationItemColors,
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    val textColor = colors.textColor(selected = selected, enabled = enabled)
    ProvideContentColorTextStyle(
        contentColor = textColor,
        textStyle = labelTextStyle,
        content = content
    )
}

@Composable
private fun animateIndicatorProgressAsState(selected: Boolean) =
    animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        // TODO Load the motionScheme tokens from the component tokens file
        animationSpec = MotionSchemeKeyTokens.DefaultSpatial.value()
    )

@Composable
private fun IndicatorRipple(interactionSource: InteractionSource, indicatorShape: Shape) {
    Box(
        Modifier.layoutId(IndicatorRippleLayoutIdTag)
            .clip(indicatorShape)
            .indication(interactionSource, ripple())
    )
}

@Composable
private fun Indicator(
    indicatorColor: Color,
    indicatorShape: Shape,
    indicatorAnimationProgress: () -> Float,
) {
    Box(
        Modifier.layoutId(IndicatorLayoutIdTag)
            .graphicsLayer { alpha = indicatorAnimationProgress() }
            .background(
                color = indicatorColor,
                shape = indicatorShape,
            )
    )
}

private const val IndicatorRippleLayoutIdTag: String = "indicatorRipple"
private const val IndicatorLayoutIdTag: String = "indicator"
private const val IconLayoutIdTag: String = "icon"
private const val LabelLayoutIdTag: String = "label"
private const val AnimatedLabelTopIconLayoutIdTag: String = "animatedLabelTopIcon"
private const val AnimatedLabelStartIconLayoutIdTag: String = "animatedLabelStartIcon"
private const val LabelAnimationVisibilityThreshold: Float = 0.35f

private val IndicatorVerticalOffset: Dp = 12.dp
