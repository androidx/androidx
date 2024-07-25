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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import kotlin.math.hypot

// TODO: link to spec and image
/**
 * FAB Menus should be used in conjunction with a [ToggleableFloatingActionButton] to provide
 * additional choices to the user after clicking a FAB.
 *
 * @sample androidx.compose.material3.samples.FloatingActionButtonMenuSample
 * @param expanded whether the FAB Menu is expanded, which will trigger a staggered animation of the
 *   FAB Menu Items
 * @param itemsCount the total number of FAB Menu Items in this FAB Menu
 * @param modifier the [Modifier] to be applied to this FAB Menu
 * @param horizontalAlignment the horizontal alignment of the FAB Menu Items
 * @param content the content of this FAB Menu, typically a list of [FloatingActionButtonMenuItem]s
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun FloatingActionButtonMenu(
    expanded: Boolean,
    itemsCount: Int,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.End,
    content: @Composable FloatingActionButtonMenuScope.() -> Unit
) =
    Column(
        modifier =
            modifier
                .verticalScroll(state = rememberScrollState())
                .padding(bottom = FabMenuPaddingBottom),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = Arrangement.spacedBy(FabMenuItemSpacingVertical)
    ) {
        val staggerDelayMillis = if (expanded) StaggerEnterDelayMillis else StaggerExitDelayMillis
        val staggerProgress =
            animateFloatAsState(
                targetValue = if (expanded) 1f else 0f,
                animationSpec =
                    tween(durationMillis = itemsCount * staggerDelayMillis, easing = LinearEasing)
            )
        val scope =
            remember(horizontalAlignment) {
                object : FloatingActionButtonMenuScopeWrapper(this) {
                    override val itemsCount: Int
                        get() = itemsCount

                    override val staggerProgress: Float
                        get() = staggerProgress.value

                    override val horizontalAlignment: Alignment.Horizontal
                        get() = horizontalAlignment
                }
            }
        content(scope)
    }

/** Scope for the children of [FloatingActionButtonMenu] */
@ExperimentalMaterial3ExpressiveApi
interface FloatingActionButtonMenuScope : ColumnScope {
    val itemsCount: Int

    val staggerProgress: Float

    val horizontalAlignment: Alignment.Horizontal
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private abstract class FloatingActionButtonMenuScopeWrapper(scope: ColumnScope) :
    FloatingActionButtonMenuScope, ColumnScope by scope

// TODO: link to spec and image
/**
 * FAB Menu Items should be used within a [FloatingActionButtonMenu] to provide additional choices
 * to the user after clicking a FAB.
 *
 * @sample androidx.compose.material3.samples.FloatingActionButtonMenuSample
 * @param onClick called when this FAB Menu Item is clicked
 * @param text label displayed inside this FAB Menu Item
 * @param icon optional icon for this FAB Menu Item, typically an [Icon]
 * @param itemIndex the index of this FAB Menu Item within the list of all items
 * @param modifier the [Modifier] to be applied to this FAB Menu Item
 * @param containerColor the color used for the background of this FAB Menu Item
 * @param contentColor the preferred color for content inside this FAB Menu Item. Defaults to either
 *   the matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun FloatingActionButtonMenuScope.FloatingActionButtonMenuItem(
    onClick: () -> Unit,
    text: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    itemIndex: Int,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = contentColorFor(containerColor)
) {
    val stagger = 1f / itemsCount
    val visible by remember {
        derivedStateOf { (itemsCount - 1 - itemIndex) * stagger < staggerProgress }
    }
    AnimatedVisibility(
        modifier = modifier.semantics { isTraversalGroup = true },
        visible = visible,
        enter = fadeIn(animationSpec = springFastFlat()),
        exit = fadeOut(animationSpec = springFastFlat())
    ) {
        // Disable min interactive component size because it interferes with the item expand
        // animation and we know we are meeting the size requirements below.
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            Surface(
                shape = CircleShape,
                color = containerColor,
                contentColor = contentColor,
                onClick = onClick
            ) {
                Row(
                    Modifier.animateEnterExit(
                            enter =
                                expandHorizontally(
                                    expandFrom = getItemAnimationAlignment(horizontalAlignment),
                                    animationSpec = springFastBouncy(),
                                    clip = false
                                ),
                            exit =
                                shrinkHorizontally(
                                    shrinkTowards = getItemAnimationAlignment(horizontalAlignment),
                                    animationSpec = springFastBouncy(),
                                    clip = false
                                )
                        )
                        .sizeIn(minWidth = FabMenuItemMinWidth, minHeight = FabMenuItemHeight)
                        .padding(horizontal = FabMenuItemContentPaddingHorizontal),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            FabMenuItemContentSpacingHorizontal,
                            Alignment.CenterHorizontally
                        )
                ) {
                    icon()
                    text()
                }
            }
        }
    }
}

/* Workaround for issue where the content itself (icon/text) slides in RTL. */
@Composable
private fun getItemAnimationAlignment(
    horizontalAlignment: Alignment.Horizontal
): Alignment.Horizontal {
    if (LocalLayoutDirection.current == LayoutDirection.Rtl) {
        if (horizontalAlignment == Alignment.End) return Alignment.Start
        if (horizontalAlignment == Alignment.Start) return Alignment.End
    }
    return horizontalAlignment
}

// TODO: link to spec and image
/**
 * Toggleable FAB supports animating its container size, corner radius, and color when it is
 * toggled, and should be used in conjunction with a [FloatingActionButtonMenu] to provide
 * additional choices to the user after clicking the FAB.
 *
 * Use [ToggleableFloatingActionButtonDefaults.animateIcon] to animate the color and size of the
 * icon while the [ToggleableFloatingActionButton] is being toggled.
 *
 * @sample androidx.compose.material3.samples.FloatingActionButtonMenuSample
 * @param checked whether this Toggleable FAB is checked
 * @param onCheckedChange callback to be invoked when this Toggleable FAB is clicked, therefore the
 *   change of the state in requested
 * @param modifier the [Modifier] to be applied to this Toggleable FAB
 * @param containerColor the color used for the background of this Toggleable FAB, based on the
 *   checked progress value from 0-1
 * @param contentAlignment the alignment of this Toggleable FAB when checked
 * @param containerSize the size of this Toggleable FAB, based on the checked progress value from
 *   0-1
 * @param containerCornerRadius the corner radius of this Toggleable FAB, based on the checked
 *   progress value from 0-1
 * @param content the content of this Toggleable FAB, typically an [Icon] that switches from an Add
 *   to a Close sign at 50% checked progress
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun ToggleableFloatingActionButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: (Float) -> Color = ToggleableFloatingActionButtonDefaults.containerColor(),
    contentAlignment: Alignment = Alignment.TopEnd,
    containerSize: (Float) -> Dp = ToggleableFloatingActionButtonDefaults.containerSize(),
    containerCornerRadius: (Float) -> Dp =
        ToggleableFloatingActionButtonDefaults.containerCornerRadius(),
    content: @Composable ToggleableFloatingActionButtonScope.() -> Unit,
) {
    val checkedProgress =
        animateFloatAsState(
            targetValue = if (checked) 1f else 0f,
            animationSpec = springFastBouncy()
        )
    ToggleableFloatingActionButton(
        checked,
        onCheckedChange,
        { checkedProgress.value },
        modifier,
        containerColor,
        contentAlignment,
        containerSize,
        containerCornerRadius,
        content
    )
}

// TODO: link to spec and image
/**
 * Toggleable FAB supports animating its container size, corner radius, and color when it is
 * toggled, and should be used in conjunction with a [FloatingActionButtonMenu] to provide
 * additional choices to the user after clicking the FAB.
 *
 * Use [ToggleableFloatingActionButtonDefaults.animateIcon] to animate the color and size of the
 * icon while the [ToggleableFloatingActionButton] is being toggled.
 *
 * This overload of Toggleable FAB also supports a [checkedProgress] param which is used to drive
 * the toggle animation.
 *
 * @sample androidx.compose.material3.samples.FloatingActionButtonMenuSample
 * @param checked whether this Toggleable FAB is checked
 * @param onCheckedChange callback to be invoked when this Toggleable FAB is clicked, therefore the
 *   change of the state in requested
 * @param checkedProgress callback that provides the progress value for the checked and unchecked
 *   animations
 * @param modifier the [Modifier] to be applied to this Toggleable FAB
 * @param containerColor the color used for the background of this Toggleable FAB, based on the
 *   checked progress value from 0-1
 * @param contentAlignment the alignment of this Toggleable FAB when checked
 * @param containerSize the size of this Toggleable FAB, based on the checked progress value from
 *   0-1
 * @param containerCornerRadius the corner radius of this Toggleable FAB, based on the checked
 *   progress value from 0-1
 * @param content the content of this Toggleable FAB, typically an [Icon] that switches from an Add
 *   to a Close sign at 50% checked progress
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
private fun ToggleableFloatingActionButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    checkedProgress: () -> Float,
    modifier: Modifier = Modifier,
    containerColor: (Float) -> Color = ToggleableFloatingActionButtonDefaults.containerColor(),
    contentAlignment: Alignment = Alignment.TopEnd,
    containerSize: (Float) -> Dp = ToggleableFloatingActionButtonDefaults.containerSize(),
    containerCornerRadius: (Float) -> Dp =
        ToggleableFloatingActionButtonDefaults.containerCornerRadius(),
    content: @Composable ToggleableFloatingActionButtonScope.() -> Unit,
) {
    val initialSize = remember(containerSize) { containerSize(0f) }
    Box(Modifier.size(initialSize), contentAlignment = contentAlignment) {
        val density = LocalDensity.current
        val fabRippleRadius =
            remember(initialSize) {
                with(density) {
                    val fabSizeHalf = initialSize.toPx() / 2
                    hypot(fabSizeHalf, fabSizeHalf).toDp()
                }
            }
        val shape =
            remember(density, checkedProgress, containerCornerRadius) {
                GenericShape { size, _ ->
                    val radius = with(density) { containerCornerRadius(checkedProgress()).toPx() }
                    addRoundRect(RoundRect(size.toRect(), CornerRadius(radius)))
                }
            }
        Box(
            modifier
                .graphicsLayer {
                    this.shadowElevation = FabShadowElevation.toPx()
                    this.shape = shape
                    this.clip = true
                }
                .drawBehind {
                    val radius = with(density) { containerCornerRadius(checkedProgress()).toPx() }
                    drawRoundRect(
                        color = containerColor(checkedProgress()),
                        cornerRadius = CornerRadius(radius)
                    )
                }
                .toggleable(
                    value = checked,
                    onValueChange = onCheckedChange,
                    interactionSource = null,
                    indication = ripple(radius = fabRippleRadius)
                )
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    val sizePx = containerSize(checkedProgress()).roundToPx()
                    layout(sizePx, sizePx) {
                        placeable.place(
                            (sizePx - placeable.width) / 2,
                            (sizePx - placeable.height) / 2
                        )
                    }
                }
        ) {
            val scope =
                remember(checkedProgress) {
                    object : ToggleableFloatingActionButtonScope {
                        override val checkedProgress: Float
                            get() = checkedProgress()
                    }
                }
            content(scope)
        }
    }
}

/** Contains the default values used by [ToggleableFloatingActionButton] */
@ExperimentalMaterial3ExpressiveApi
object ToggleableFloatingActionButtonDefaults {

    @Composable
    fun containerColor(
        initialColor: Color = MaterialTheme.colorScheme.primaryContainer,
        finalColor: Color = MaterialTheme.colorScheme.primary
    ): (Float) -> Color = { progress -> lerp(initialColor, finalColor, progress) }

    fun containerSize(initialSize: Dp, finalSize: Dp = FabFinalSize): (Float) -> Dp = { progress ->
        lerp(initialSize, finalSize, progress)
    }

    fun containerSize() = containerSize(FabInitialSize)

    fun containerSizeMedium() = containerSize(FabMediumInitialSize)

    fun containerSizeLarge() = containerSize(FabLargeInitialSize)

    fun containerCornerRadius(
        initialSize: Dp,
        finalSize: Dp = FabFinalCornerRadius
    ): (Float) -> Dp = { progress -> lerp(initialSize, finalSize, progress) }

    fun containerCornerRadius() = containerCornerRadius(FabInitialCornerRadius)

    fun containerCornerRadiusMedium() = containerCornerRadius(FabMediumInitialCornerRadius)

    fun containerCornerRadiusLarge() = containerCornerRadius(FabLargeInitialCornerRadius)

    @Composable
    fun iconColor(
        initialColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
        finalColor: Color = MaterialTheme.colorScheme.onPrimary
    ): (Float) -> Color = { progress -> lerp(initialColor, finalColor, progress) }

    fun iconSize(initialSize: Dp, finalSize: Dp = FabFinalIconSize): (Float) -> Dp = { progress ->
        lerp(initialSize, finalSize, progress)
    }

    fun iconSize() = iconSize(FabInitialIconSize)

    fun iconSizeMedium() = iconSize(FabMediumInitialIconSize)

    fun iconSizeLarge() = iconSize(FabLargeInitialIconSize)

    /**
     * Modifier for animating the color and size of an icon within [ToggleableFloatingActionButton]
     * based on a progress value.
     *
     * @param checkedProgress callback that provides the progress value for the icon animation
     * @param color the color of the icon, based on the checked progress value from 0-1
     * @param size the size of the icon, based on the checked progress value from 0-1
     */
    @Composable
    fun Modifier.animateIcon(
        checkedProgress: () -> Float,
        color: (Float) -> Color = iconColor(),
        size: (Float) -> Dp = iconSize(),
    ) =
        this.layout { measurable, _ ->
                val sizePx = size(checkedProgress()).roundToPx()
                val placeable = measurable.measure(Constraints.fixed(sizePx, sizePx))
                layout(sizePx, sizePx) { placeable.place(0, 0) }
            }
            .drawWithCache {
                val layer = obtainGraphicsLayer()
                layer.apply {
                    record { drawContent() }
                    this.colorFilter = ColorFilter.tint(color(checkedProgress()))
                }

                onDrawWithContent { drawLayer(graphicsLayer = layer) }
            }
}

/** Scope for the children of [ToggleableFloatingActionButton] */
@ExperimentalMaterial3ExpressiveApi
interface ToggleableFloatingActionButtonScope {

    val checkedProgress: Float
}

private val FabInitialSize = 56.dp
private val FabInitialCornerRadius = 16.dp
private val FabInitialIconSize = 24.dp
private val FabMediumInitialSize = 80.dp
private val FabMediumInitialCornerRadius = 24.dp
private val FabMediumInitialIconSize = 28.dp
private val FabLargeInitialSize = 96.dp
private val FabLargeInitialCornerRadius = 28.dp
private val FabLargeInitialIconSize = 36.dp
private val FabFinalSize = 56.dp
private val FabFinalCornerRadius = FabFinalSize.div(2)
private val FabFinalIconSize = 20.dp
private val FabShadowElevation = 6.dp
private val FabMenuPaddingBottom = 8.dp
private val FabMenuItemMinWidth = 56.dp
private val FabMenuItemHeight = 56.dp
private val FabMenuItemSpacingVertical = 4.dp
private val FabMenuItemContentPaddingHorizontal = 16.dp
private val FabMenuItemContentSpacingHorizontal = 8.dp
private const val StaggerEnterDelayMillis = 35
private const val StaggerExitDelayMillis = 25

private fun <T> springFastBouncy() = spring<T>(0.6f, 800f)

private fun <T> springFastFlat() = spring<T>(1f, 3800f)
