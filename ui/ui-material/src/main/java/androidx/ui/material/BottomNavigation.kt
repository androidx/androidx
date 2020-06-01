/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.material

import androidx.animation.FastOutSlowInEasing
import androidx.animation.TweenBuilder
import androidx.annotation.FloatRange
import androidx.compose.Composable
import androidx.compose.Providers
import androidx.compose.emptyContent
import androidx.ui.animation.animate
import androidx.ui.core.Constraints
import androidx.ui.core.Layout
import androidx.ui.core.MeasureScope
import androidx.ui.core.Modifier
import androidx.ui.core.Placeable
import androidx.ui.core.drawOpacity
import androidx.ui.core.tag
import androidx.ui.foundation.Box
import androidx.ui.foundation.ContentColorAmbient
import androidx.ui.foundation.ContentGravity
import androidx.ui.foundation.ProvideTextStyle
import androidx.ui.foundation.contentColor
import androidx.ui.foundation.selection.selectable
import androidx.ui.graphics.Color
import androidx.ui.graphics.lerp
import androidx.ui.layout.Arrangement
import androidx.ui.layout.Row
import androidx.ui.layout.RowScope
import androidx.ui.layout.fillMaxWidth
import androidx.ui.layout.preferredHeight
import androidx.ui.text.LastBaseline
import androidx.ui.text.style.TextAlign
import androidx.ui.unit.Dp
import androidx.ui.unit.IntPx
import androidx.ui.unit.dp
import androidx.ui.unit.max

// TODO: b/149825331 add documentation references to Scaffold here and samples for using
// BottomNavigation inside a Scaffold
/**
 * BottomNavigation is a component placed at the bottom of the screen that represents primary
 * destinations in your application.
 *
 * BottomNavigation should contain multiple [BottomNavigationItem]s, each representing a singular
 * destination.
 *
 * A simple example looks like:
 *
 * @sample androidx.ui.material.samples.BottomNavigationSample
 *
 * See [BottomNavigationItem] for configuration specific to each item, and not the overall
 * BottomNavigation component.
 *
 * For more information, see [Bottom Navigation](https://material.io/components/bottom-navigation/)
 *
 * @param modifier optional [Modifier] for this BottomNavigation
 * @param backgroundColor The background color for this BottomNavigation
 * @param contentColor The preferred content color provided by this BottomNavigation to its
 * children. Defaults to either the matching `onFoo` color for [backgroundColor], or if
 * [backgroundColor] is not a color from the theme, this will keep the same value set above this
 * BottomNavigation.
 * @param elevation elevation for this BottomNavigation
 * @param content destinations inside this BottomNavigation, this should contain multiple
 * [BottomNavigationItem]s
 */
@Composable
fun BottomNavigation(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colors.primarySurface,
    contentColor: Color = contentColorFor(backgroundColor),
    elevation: Dp = BottomNavigationElevation,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        color = backgroundColor,
        contentColor = contentColor,
        elevation = elevation,
        modifier = modifier
    ) {
        Row(
            Modifier.fillMaxWidth().preferredHeight(BottomNavigationHeight),
            horizontalArrangement = Arrangement.SpaceBetween,
            children = content
        )
    }
}

/**
 * A BottomNavigationItem represents a singular primary destination in your application.
 *
 * The recommended configuration for a BottomNavigationItem depends on how many items there are
 * inside a [BottomNavigation]:
 *
 * - Three destinations: Display icons and text labels for all destinations.
 * - Four destinations: Active destinations display an icon and text label. Inactive destinations
 * display icons, and text labels are recommended.
 * - Five destinations: Active destinations display an icon and text label. Inactive destinations
 * use icons, and use text labels if space permits.
 *
 * A BottomNavigationItem always shows text labels (if it exists) when selected. Showing text
 * labels if not selected is controlled by [alwaysShowLabels].
 *
 * @param icon icon for this item, typically this will be a [androidx.ui.foundation.Icon]
 * @param text optional text for this item
 * @param selected whether this item is selected
 * @param onSelected the callback to be invoked when this item is selected
 * @param modifier optional [Modifier] for this item
 * @param alwaysShowLabels whether to always show labels for this item. If false, labels will
 * only be shown when this item is selected.
 * @param activeColor the color of the text and icon when this item is selected
 * @param inactiveColor the color of the text and icon when this item is not selected
 */
@Composable
fun BottomNavigationItem(
    icon: @Composable () -> Unit,
    text: @Composable () -> Unit = emptyContent(),
    selected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier,
    alwaysShowLabels: Boolean = true,
    activeColor: Color = contentColor(),
    inactiveColor: Color = EmphasisAmbient.current.medium.applyEmphasis(activeColor)
) {
    val styledText = @Composable {
        val style = MaterialTheme.typography.caption.copy(textAlign = TextAlign.Center)
        ProvideTextStyle(style, children = text)
    }
    // TODO This composable has magic behavior within a Row; reconsider this behavior later
    Box(with(RowScope) {
        modifier
            .selectable(selected = selected, onClick = onSelected)
            .weight(1f)
    }, gravity = ContentGravity.Center) {
        BottomNavigationTransition(activeColor, inactiveColor, selected) { progress ->
            val animationProgress = if (alwaysShowLabels) 1f else progress

            BottomNavigationItemBaselineLayout(
                icon = icon,
                text = styledText,
                iconPositionAnimationProgress = animationProgress
            )
        }
    }
}

/**
 * Transition that animates [contentColor] between [inactiveColor] and [activeColor], depending
 * on [selected]. This component also provides the animation fraction as a parameter to [content],
 * to allow animating the position of the icon and the scale of the text alongside this color
 * animation.
 *
 * @param activeColor [contentColor] when this item is [selected]
 * @param inactiveColor [contentColor] when this item is not [selected]
 * @param selected whether this item is selected
 * @param content the content of the [BottomNavigationItem] to animate [contentColor] for, where
 * the animationProgress is the current progress of the animation from 0f to 1f.
 */
@Composable
private fun BottomNavigationTransition(
    activeColor: Color,
    inactiveColor: Color,
    selected: Boolean,
    content: @Composable (animationProgress: Float) -> Unit
) {
    val animationProgress = animate(
        target = if (selected) 1f else 0f,
        animBuilder = BottomNavigationAnimationBuilder
    )

    val color = lerp(inactiveColor, activeColor, animationProgress)

    Providers(ContentColorAmbient provides color) {
        content(animationProgress)
    }
}

/**
 * Base layout for a [BottomNavigationItem]
 *
 * @param icon icon for this item
 * @param text text for this item
 * @param iconPositionAnimationProgress progress of the animation the controls icon position,
 * where 0 represents its unselected position and 1 represents its selected position. If both the
 * [icon] and [text] should be shown at all times, this will always be 1, as the icon position
 * should remain constant.
 */
@Composable
private fun BottomNavigationItemBaselineLayout(
    icon: @Composable () -> Unit,
    text: @Composable () -> Unit,
    @FloatRange(from = 0.0, to = 1.0) iconPositionAnimationProgress: Float
) {
    Layout(
        {
            Box(Modifier.tag("icon"), children = icon)
            Box(
                Modifier.tag("text").drawOpacity(iconPositionAnimationProgress),
                paddingStart = BottomNavigationItemHorizontalPadding,
                paddingEnd = BottomNavigationItemHorizontalPadding,
                children = text
            )
        }
    ) { measurables, constraints, _ ->
        val iconPlaceable = measurables.first { it.tag == "icon" }.measure(constraints)

        val textPlaceable = measurables.first { it.tag == "text" }.measure(
            // Measure with loose constraints for height as we don't want the text to take up more
            // space than it needs
            constraints.copy(minHeight = IntPx.Zero)
        )

        // If the text is empty, just place the icon.
        if (textPlaceable.width <= BottomNavigationItemHorizontalPadding.toIntPx() * 2 &&
            textPlaceable.height == IntPx.Zero
        ) {
            placeIcon(iconPlaceable, constraints)
        } else {
            placeTextAndIcon(
                textPlaceable,
                iconPlaceable,
                constraints,
                iconPositionAnimationProgress
            )
        }
    }
}

/**
 * Places the provided [iconPlaceable] in the vertical center of the provided [constraints]
 */
private fun MeasureScope.placeIcon(
    iconPlaceable: Placeable,
    constraints: Constraints
): MeasureScope.MeasureResult {
    val height = constraints.maxHeight
    val iconY = (height - iconPlaceable.height) / 2
    return layout(iconPlaceable.width, height) {
        iconPlaceable.place(IntPx.Zero, iconY)
    }
}

/**
 * Places the provided [textPlaceable] and [iconPlaceable] in the correct position, depending on
 * [iconPositionAnimationProgress].
 *
 * When [iconPositionAnimationProgress] is 0, [iconPlaceable] will be placed in the center, as with
 * [placeIcon], and [textPlaceable] will not be shown.
 *
 * When [iconPositionAnimationProgress] is 1, [iconPlaceable] will be placed near the top of item,
 * and [textPlaceable] will be placed at the bottom of the item, according to the spec.
 *
 * When [iconPositionAnimationProgress] is animating between these values, [iconPlaceable] will be
 * placed at an interpolated position between its centered position and final resting position.
 *
 * @param textPlaceable text placeable inside this item
 * @param iconPlaceable icon placeable inside this item
 * @param constraints constraints of the item
 * @param iconPositionAnimationProgress the progress of the icon position animation, where 0
 * represents centered icon and no text, and 1 represents top aligned icon with text.
 * Values between 0 and 1 interpolate the icon position so we can smoothly move the icon.
 */
private fun MeasureScope.placeTextAndIcon(
    textPlaceable: Placeable,
    iconPlaceable: Placeable,
    constraints: Constraints,
    @FloatRange(from = 0.0, to = 1.0) iconPositionAnimationProgress: Float
): MeasureScope.MeasureResult {
    val height = constraints.maxHeight

    // TODO: consider multiple lines of text here, not really supported by spec but we should
    // have a better strategy than overlapping the icon and text
    val baseline = requireNotNull(textPlaceable[LastBaseline]) { "No text baselines found" }

    val baselineOffset = CombinedItemTextBaseline.toIntPx()

    // Text should be [baselineOffset] from the bottom
    val textY = height - baseline - baselineOffset

    val unselectedIconY = (height - iconPlaceable.height) / 2

    // Icon should be [baselineOffset] from the text baseline, which is itself
    // [baselineOffset] from the bottom
    val selectedIconY = height - (baselineOffset * 2) - iconPlaceable.height

    val containerWidth = max(textPlaceable.width, iconPlaceable.width)

    val textX = (containerWidth - textPlaceable.width) / 2
    val iconX = (containerWidth - iconPlaceable.width) / 2

    // How far the icon needs to move between unselected and selected states
    val iconDistance = unselectedIconY - selectedIconY

    // When selected the icon is above the unselected position, so we will animate moving
    // downwards from the selected state, so when progress is 1, the total distance is 0, and we
    // are at the selected state.
    val offset = iconDistance * (1 - iconPositionAnimationProgress)

    return layout(containerWidth, height) {
        if (iconPositionAnimationProgress != 0f) {
            textPlaceable.place(textX, textY + offset)
        }
        iconPlaceable.place(iconX, selectedIconY + offset)
    }
}

/**
 * [AnimationBuilder] controlling the transition between unselected and selected
 * [BottomNavigationItem]s.
 */
private val BottomNavigationAnimationBuilder = TweenBuilder<Float>().apply {
    duration = 300
    easing = FastOutSlowInEasing
}

/**
 * Height of a [BottomNavigation] component
 */
private val BottomNavigationHeight = 56.dp

/**
 * Default elevation of a [BottomNavigation] component
 */
private val BottomNavigationElevation = 8.dp

/**
 * Padding at the start and end of a [BottomNavigationItem]
 */
private val BottomNavigationItemHorizontalPadding = 12.dp

/**
 * The space between the text baseline and the bottom of the [BottomNavigationItem], and between
 * the text baseline and the bottom of the icon placed above it.
 */
private val CombinedItemTextBaseline = 12.dp
