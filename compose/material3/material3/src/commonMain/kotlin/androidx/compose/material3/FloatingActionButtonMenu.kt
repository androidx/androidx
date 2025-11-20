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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.tokens.FabBaselineTokens
import androidx.compose.material3.tokens.FabLargeTokens
import androidx.compose.material3.tokens.FabMediumTokens
import androidx.compose.material3.tokens.FabMenuBaselineTokens
import androidx.compose.material3.tokens.FabPrimaryContainerTokens
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.HorizontalRuler
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layout
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ParentDataModifierNode
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxBy
import androidx.compose.ui.util.fastSumBy
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

// TODO: link to spec and image
/**
 * FAB Menus should be used in conjunction with a [ToggleFloatingActionButton] to provide additional
 * choices to the user after clicking a FAB.
 *
 * @sample androidx.compose.material3.samples.FloatingActionButtonMenuSample
 * @param expanded whether the FAB Menu is expanded, which will trigger a staggered animation of the
 *   FAB Menu Items
 * @param button a composable which triggers the showing and hiding of the FAB Menu Items via the
 *   [expanded] state, typically a [ToggleFloatingActionButton]
 * @param modifier the [Modifier] to be applied to this FAB Menu
 * @param horizontalAlignment the horizontal alignment of the FAB Menu Items
 * @param content the content of this FAB Menu, typically a list of [FloatingActionButtonMenuItem]s
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun FloatingActionButtonMenu(
    expanded: Boolean,
    button: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.End,
    content: @Composable FloatingActionButtonMenuScope.() -> Unit,
) {
    var buttonHeight by remember { mutableIntStateOf(0) }
    val focusRequester = remember { FocusRequester() }

    Layout(
        modifier = modifier.padding(horizontal = FabMenuPaddingHorizontal),
        content = {
            FloatingActionButtonMenuItemColumn(
                Modifier.focusRequester(focusRequester),
                expanded,
                horizontalAlignment,
                { buttonHeight },
                content,
            )

            Box(
                Modifier.onKeyEvent {
                    // For keyboard a11y, the focus order should go from the fab menu button to the
                    // first item at the top.
                    if (
                        expanded &&
                            it.type == KeyEventType.KeyDown &&
                            ((it.key == Key.Tab && !it.isShiftPressed) ||
                                it.key == Key.DirectionDown)
                    ) {
                        focusRequester.requestFocus()
                        return@onKeyEvent true
                    }
                    return@onKeyEvent false
                }
            ) {
                button()
            }
        },
    ) { measureables, constraints ->
        val menuItemsPlaceable = measureables[0].measure(constraints)

        val buttonPaddingBottom = FabMenuButtonPaddingBottom.roundToPx()
        var buttonPlaceable: Placeable? = null
        val suggestedWidth: Int
        val suggestedHeight: Int
        if (measureables.size > 1) {
            buttonPlaceable = measureables[1].measure(constraints)
            buttonHeight = buttonPlaceable.height

            suggestedWidth = maxOf(buttonPlaceable.width, menuItemsPlaceable.width)
            suggestedHeight =
                maxOf(buttonPlaceable.height + buttonPaddingBottom, menuItemsPlaceable.height)
        } else {
            suggestedWidth = menuItemsPlaceable.width
            suggestedHeight = menuItemsPlaceable.height
        }

        val width = minOf(suggestedWidth, constraints.maxWidth)
        val height = minOf(suggestedHeight, constraints.maxHeight)

        layout(width, height) {
            val menuItemsX =
                horizontalAlignment.align(menuItemsPlaceable.width, width, layoutDirection)
            menuItemsPlaceable.place(menuItemsX, 0)

            if (buttonPlaceable != null) {
                val buttonX =
                    horizontalAlignment.align(buttonPlaceable.width, width, layoutDirection)
                val buttonY = height - buttonPlaceable.height - buttonPaddingBottom
                buttonPlaceable.place(buttonX, buttonY)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FloatingActionButtonMenuItemColumn(
    modifier: Modifier,
    expanded: Boolean,
    horizontalAlignment: Alignment.Horizontal,
    buttonHeight: () -> Int,
    content: @Composable FloatingActionButtonMenuScope.() -> Unit,
) {
    var itemCount by remember { mutableIntStateOf(0) }
    var itemsNeedVerticalScroll by remember { mutableStateOf(false) }
    var originalConstraints: Constraints? = null
    var staggerAnim by remember { mutableStateOf<Animatable<Int, AnimationVector1D>?>(null) }
    val coroutineScope = rememberCoroutineScope()
    // TODO Load the motionScheme tokens from the component tokens file
    var staggerAnimSpec: FiniteAnimationSpec<Int> = MotionSchemeKeyTokens.SlowEffects.value()
    if (staggerAnimSpec is SpringSpec<Int>) {
        // Apply a small visibilityThreshold to the provided SpringSpec to avoid a delay in the
        // appearance of the last item when the list is populated.
        staggerAnimSpec =
            spring(
                stiffness = staggerAnimSpec.stiffness,
                dampingRatio = staggerAnimSpec.dampingRatio,
                visibilityThreshold = 1,
            )
    }
    Layout(
        modifier =
            modifier
                .clipToBounds()
                .semantics {
                    isTraversalGroup = true
                    traversalIndex = -0.9f
                }
                .layout { measurable, constraints ->
                    // Use a layout modifier before the verticalScroll to get the original
                    // constraints from the parent, since verticalScroll will cause the constraints
                    // max height to be infinity.
                    originalConstraints = constraints
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                }
                .then(
                    if (itemsNeedVerticalScroll)
                        Modifier.verticalScroll(state = rememberScrollState(), enabled = expanded)
                    else Modifier
                ),
        content = {
            val scope =
                remember(horizontalAlignment) {
                    object : FloatingActionButtonMenuScope {
                        override val horizontalAlignment: Alignment.Horizontal
                            get() = horizontalAlignment
                    }
                }
            content(scope)
        },
    ) { measurables, constraints ->
        itemCount = measurables.size

        val targetItemCount = if (expanded) itemCount else 0
        staggerAnim =
            staggerAnim?.also {
                if (it.targetValue != targetItemCount) {
                    coroutineScope.launch {
                        it.animateTo(targetValue = targetItemCount, animationSpec = staggerAnimSpec)
                    }
                }
            } ?: Animatable(targetItemCount, Int.VectorConverter)

        val placeables = measurables.fastMap { measurable -> measurable.measure(constraints) }
        val width = placeables.fastMaxBy { it.width }?.width ?: 0

        val verticalSpacing = FabMenuItemSpacingVertical.roundToPx()
        val verticalSpacingHeight =
            if (placeables.isNotEmpty()) {
                verticalSpacing * (placeables.size - 1)
            } else {
                0
            }
        val currentButtonHeight = buttonHeight()
        val bottomPadding =
            if (currentButtonHeight > 0)
                currentButtonHeight +
                    FabMenuButtonPaddingBottom.roundToPx() +
                    FabMenuPaddingBottom.roundToPx()
            else 0
        val height = placeables.fastSumBy { it.height } + verticalSpacingHeight + bottomPadding
        var visibleHeight = bottomPadding.toFloat()
        placeables.fastForEachIndexed { index, placeable ->
            val itemVisible = index >= itemCount - (staggerAnim?.value ?: 0)
            if (itemVisible) {
                visibleHeight += placeable.height
                if (index < placeables.size - 1) {
                    visibleHeight += verticalSpacing
                }
            }
        }

        val finalHeight = if (placeables.fastAny { item -> item.isVisible }) height else 0

        itemsNeedVerticalScroll = finalHeight > originalConstraints!!.maxHeight

        layout(width, finalHeight, rulers = { MenuItemRuler provides height - visibleHeight }) {
            var y = 0
            placeables.fastForEachIndexed { index, placeable ->
                val x = horizontalAlignment.align(placeable.width, width, layoutDirection)
                placeable.place(x, y)
                y += placeable.height
                if (index < placeables.size - 1) {
                    y += verticalSpacing
                }
            }
        }
    }
}

/** Scope for the children of [FloatingActionButtonMenu] */
@ExperimentalMaterial3ExpressiveApi
interface FloatingActionButtonMenuScope {
    val horizontalAlignment: Alignment.Horizontal
}

// TODO: link to spec and image
/**
 * FAB Menu Items should be used within a [FloatingActionButtonMenu] to provide additional choices
 * to the user after clicking a FAB.
 *
 * @sample androidx.compose.material3.samples.FloatingActionButtonMenuSample
 * @param onClick called when this FAB Menu Item is clicked
 * @param text label displayed inside this FAB Menu Item
 * @param icon optional icon for this FAB Menu Item, typically an [Icon]
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
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = contentColorFor(containerColor),
) {
    var widthAnim by remember { mutableStateOf<Animatable<Float, AnimationVector1D>?>(null) }
    var alphaAnim by remember { mutableStateOf<Animatable<Float, AnimationVector1D>?>(null) }
    // TODO Load the motionScheme tokens from the component tokens file
    val widthSpring: FiniteAnimationSpec<Float> = MotionSchemeKeyTokens.FastSpatial.value()
    val alphaSpring: FiniteAnimationSpec<Float> = MotionSchemeKeyTokens.FastEffects.value()
    val coroutineScope = rememberCoroutineScope()

    var isVisible by remember { mutableStateOf(false) }
    // Disable min interactive component size because it interferes with the item expand
    // animation and we know we are meeting the size requirements below.
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        Surface(
            modifier =
                modifier.itemVisible({ isVisible }).layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) {
                        val target =
                            if (MenuItemRuler.current(Float.POSITIVE_INFINITY) <= 0) 1f else 0f

                        widthAnim =
                            widthAnim?.also {
                                if (it.targetValue != target) {
                                    coroutineScope.launch { it.animateTo(target, widthSpring) }
                                }
                            } ?: Animatable(target, Float.VectorConverter)

                        val tempAlphaAnim =
                            alphaAnim?.also {
                                if (it.targetValue != target) {
                                    coroutineScope.launch { it.animateTo(target, alphaSpring) }
                                }
                            } ?: Animatable(target, Float.VectorConverter)
                        alphaAnim = tempAlphaAnim
                        isVisible = tempAlphaAnim.value != 0f
                        if (isVisible) {
                            placeable.placeWithLayer(0, 0) { alpha = tempAlphaAnim.value }
                        }
                    }
                },
            shape = FabMenuBaselineTokens.ListItemContainerShape.value,
            color = containerColor,
            contentColor = contentColor,
            onClick = onClick,
        ) {
            Row(
                Modifier.layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val width =
                            (placeable.width * maxOf((widthAnim?.value ?: 0f), 0f)).roundToInt()
                        layout(width, placeable.height) {
                            val x =
                                horizontalAlignment.align(placeable.width, width, layoutDirection)
                            placeable.placeWithLayer(x, 0)
                        }
                    }
                    .sizeIn(minWidth = FabMenuItemMinWidth, minHeight = FabMenuItemHeight)
                    .padding(
                        start = FabMenuItemContentPaddingStart,
                        end = FabMenuItemContentPaddingEnd,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement =
                    Arrangement.spacedBy(
                        FabMenuItemContentSpacingHorizontal,
                        Alignment.CenterHorizontally,
                    ),
            ) {
                icon()
                CompositionLocalProvider(
                    LocalTextStyle provides MaterialTheme.typography.titleMedium,
                    content = text,
                )
            }
        }
    }
}

private val MenuItemRuler = HorizontalRuler()

// TODO: link to spec and image
/**
 * Toggleable FAB supports animating its container size, corner radius, and color when it is
 * toggled, and should be used in conjunction with a [FloatingActionButtonMenu] to provide
 * additional choices to the user after clicking the FAB.
 *
 * Use [ToggleFloatingActionButtonDefaults.animateIcon] to animate the color and size of the icon
 * while the [ToggleFloatingActionButton] is being toggled.
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
fun ToggleFloatingActionButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: (Float) -> Color = ToggleFloatingActionButtonDefaults.containerColor(),
    contentAlignment: Alignment = Alignment.TopEnd,
    containerSize: (Float) -> Dp = ToggleFloatingActionButtonDefaults.containerSize(),
    containerCornerRadius: (Float) -> Dp =
        ToggleFloatingActionButtonDefaults.containerCornerRadius(),
    content: @Composable ToggleFloatingActionButtonScope.() -> Unit,
) {
    val checkedProgress =
        animateFloatAsState(
            targetValue = if (checked) 1f else 0f,
            // TODO Load the motionScheme tokens from the component tokens file
            animationSpec = MotionSchemeKeyTokens.FastSpatial.value(),
        )
    ToggleFloatingActionButton(
        checked,
        onCheckedChange,
        { checkedProgress.value },
        modifier,
        containerColor,
        contentAlignment,
        containerSize,
        containerCornerRadius,
        content,
    )
}

// TODO: link to spec and image
/**
 * Toggleable FAB supports animating its container size, corner radius, and color when it is
 * toggled, and should be used in conjunction with a [FloatingActionButtonMenu] to provide
 * additional choices to the user after clicking the FAB.
 *
 * Use [ToggleFloatingActionButtonDefaults.animateIcon] to animate the color and size of the icon
 * while the [ToggleFloatingActionButton] is being toggled.
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
private fun ToggleFloatingActionButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    checkedProgress: () -> Float,
    modifier: Modifier = Modifier,
    containerColor: (Float) -> Color = ToggleFloatingActionButtonDefaults.containerColor(),
    contentAlignment: Alignment = Alignment.TopEnd,
    containerSize: (Float) -> Dp = ToggleFloatingActionButtonDefaults.containerSize(),
    containerCornerRadius: (Float) -> Dp =
        ToggleFloatingActionButtonDefaults.containerCornerRadius(),
    content: @Composable ToggleFloatingActionButtonScope.() -> Unit,
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
                        cornerRadius = CornerRadius(radius),
                    )
                }
                .toggleable(
                    value = checked,
                    onValueChange = onCheckedChange,
                    interactionSource = null,
                    indication = ripple(radius = fabRippleRadius),
                )
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    val sizePx = containerSize(checkedProgress()).roundToPx()
                    layout(sizePx, sizePx) {
                        placeable.place(
                            (sizePx - placeable.width) / 2,
                            (sizePx - placeable.height) / 2,
                        )
                    }
                }
        ) {
            val scope =
                remember(checkedProgress) {
                    object : ToggleFloatingActionButtonScope {
                        override val checkedProgress: Float
                            get() = checkedProgress()
                    }
                }
            content(scope)
        }
    }
}

/** Contains the default values used by [ToggleFloatingActionButton] */
@ExperimentalMaterial3ExpressiveApi
object ToggleFloatingActionButtonDefaults {

    @Composable
    fun containerColor(
        initialColor: Color = MaterialTheme.colorScheme.primaryContainer,
        finalColor: Color = MaterialTheme.colorScheme.primary,
    ): (Float) -> Color = { progress -> lerp(initialColor, finalColor, progress) }

    fun containerSize(initialSize: Dp, finalSize: Dp = FabFinalSize): (Float) -> Dp = { progress ->
        lerp(initialSize, finalSize, progress)
    }

    fun containerSize() = containerSize(FabInitialSize)

    fun containerSizeMedium() = containerSize(FabMediumInitialSize)

    fun containerSizeLarge() = containerSize(FabLargeInitialSize)

    fun containerCornerRadius(
        initialSize: Dp,
        finalSize: Dp = FabFinalCornerRadius,
    ): (Float) -> Dp = { progress -> lerp(initialSize, finalSize, progress) }

    fun containerCornerRadius() = containerCornerRadius(FabInitialCornerRadius)

    fun containerCornerRadiusMedium() = containerCornerRadius(FabMediumInitialCornerRadius)

    fun containerCornerRadiusLarge() = containerCornerRadius(FabLargeInitialCornerRadius)

    @Composable
    fun iconColor(
        initialColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
        finalColor: Color = MaterialTheme.colorScheme.onPrimary,
    ): (Float) -> Color = { progress -> lerp(initialColor, finalColor, progress) }

    fun iconSize(initialSize: Dp, finalSize: Dp = FabFinalIconSize): (Float) -> Dp = { progress ->
        lerp(initialSize, finalSize, progress)
    }

    fun iconSize() = iconSize(FabInitialIconSize)

    fun iconSizeMedium() = iconSize(FabMediumInitialIconSize)

    fun iconSizeLarge() = iconSize(FabLargeInitialIconSize)

    /**
     * Modifier for animating the color and size of an icon within [ToggleFloatingActionButton]
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

/** Scope for the children of [ToggleFloatingActionButton] */
@ExperimentalMaterial3ExpressiveApi
interface ToggleFloatingActionButtonScope {

    val checkedProgress: Float
}

@Stable
private fun Modifier.itemVisible(isVisible: () -> Boolean) =
    this then MenuItemVisibleElement(isVisible = isVisible)

private class MenuItemVisibleElement(private val isVisible: () -> Boolean) :
    ModifierNodeElement<MenuItemVisibilityModifier>() {
    override fun create() = MenuItemVisibilityModifier(isVisible)

    override fun update(node: MenuItemVisibilityModifier) {
        node.visible = isVisible
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "itemVisible"
        value = isVisible()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as MenuItemVisibleElement

        return isVisible === other.isVisible
    }

    override fun hashCode(): Int {
        return isVisible.hashCode()
    }
}

private class MenuItemVisibilityModifier(isVisible: () -> Boolean) :
    ParentDataModifierNode, SemanticsModifierNode, Modifier.Node() {

    var visible: () -> Boolean = isVisible

    override fun Density.modifyParentData(parentData: Any?): Any? {
        return this@MenuItemVisibilityModifier
    }

    override val shouldClearDescendantSemantics: Boolean
        get() = !visible()

    override fun SemanticsPropertyReceiver.applySemantics() {}
}

private val Placeable.isVisible: Boolean
    get() = (this.parentData as? MenuItemVisibilityModifier)?.visible?.invoke() != false

private val FabInitialSize = FabBaselineTokens.ContainerHeight
private val FabInitialCornerRadius = 16.dp
private val FabInitialIconSize = FabBaselineTokens.IconSize
private val FabMediumInitialSize = FabMediumTokens.ContainerHeight
private val FabMediumInitialCornerRadius = 20.dp
private val FabMediumInitialIconSize = FabMediumTokens.IconSize
private val FabLargeInitialSize = FabLargeTokens.ContainerHeight
private val FabLargeInitialCornerRadius = 28.dp
private val FabLargeInitialIconSize = 36.dp // TODO: FabLargeTokens.IconSize is incorrect
private val FabFinalSize = FabMenuBaselineTokens.CloseButtonContainerHeight
private val FabFinalCornerRadius = FabFinalSize.div(2)
private val FabFinalIconSize = FabMenuBaselineTokens.CloseButtonIconSize
private val FabShadowElevation = FabPrimaryContainerTokens.ContainerElevation
private val FabMenuPaddingHorizontal = 16.dp
private val FabMenuPaddingBottom = FabMenuBaselineTokens.CloseButtonBetweenSpace
private val FabMenuButtonPaddingBottom = 16.dp
private val FabMenuItemMinWidth = FabMenuBaselineTokens.ListItemContainerHeight
private val FabMenuItemHeight = FabMenuBaselineTokens.ListItemContainerHeight
private val FabMenuItemSpacingVertical = FabMenuBaselineTokens.ListItemBetweenSpace
private val FabMenuItemContentPaddingStart = FabMenuBaselineTokens.ListItemLeadingSpace
private val FabMenuItemContentPaddingEnd = FabMenuBaselineTokens.ListItemTrailingSpace
private val FabMenuItemContentSpacingHorizontal = FabMenuBaselineTokens.ListItemIconLabelSpace
