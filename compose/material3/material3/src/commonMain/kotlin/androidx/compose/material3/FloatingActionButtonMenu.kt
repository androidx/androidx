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
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
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
import androidx.compose.ui.graphics.GraphicsLayerScope
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
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layout
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ParentDataModifierNode
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.invalidatePlacement
import androidx.compose.ui.node.invalidateSemantics
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
import kotlinx.coroutines.CoroutineScope
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
 *
 * @material3expressive
 */
@Composable
public fun FloatingActionButtonMenu(
    expanded: Boolean,
    button: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.End,
    content: @Composable FloatingActionButtonMenuScope.() -> Unit,
) {
    var buttonHeight by remember { mutableIntStateOf(0) }
    val focusRequester = remember { FocusRequester() }
    val currentExpanded by rememberUpdatedState(expanded)
    // Compose the items on first expand (then keep them) so a collapsed menu only composes its
    // button. Kept apart from currentExpanded, which is only read during layout so that toggling
    // the menu doesn't invalidate composition.
    val composeItems = remember { mutableStateOf(false) }
    if (expanded) composeItems.value = true

    Layout(
        modifier = modifier.padding(horizontal = FabMenuPaddingHorizontal),
        content = {
            FloatingActionButtonMenuItemColumn(
                Modifier.focusRequester(focusRequester),
                { currentExpanded },
                { composeItems.value },
                horizontalAlignment,
                { buttonHeight },
                content,
            )

            Box(
                Modifier.onKeyEvent {
                    // For keyboard a11y, the focus order should go from the fab menu button to the
                    // first item at the top.
                    if (
                        currentExpanded &&
                            it.type == KeyEventType.KeyDown &&
                            ((it.key == Key.Tab && !it.isShiftPressed) ||
                                it.key == Key.DirectionDown ||
                                it.key == Key.NumPadDirectionDown)
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
    ) { measurables, constraints ->
        val buttonPaddingBottom = FabMenuButtonPaddingBottom.roundToPx()
        val buttonPlaceable =
            measurables.getOrNull(1)?.measure(constraints)?.also { buttonHeight = it.height }
        val menuItemsPlaceable = measurables[0].measure(constraints)

        val suggestedWidth = maxOf(buttonPlaceable?.width ?: 0, menuItemsPlaceable.width)
        val suggestedHeight =
            maxOf(
                buttonPlaceable?.let { it.height + buttonPaddingBottom } ?: 0,
                menuItemsPlaceable.height,
            )
        val width = minOf(suggestedWidth, constraints.maxWidth)
        val height = minOf(suggestedHeight, constraints.maxHeight)

        layout(width, height) {
            menuItemsPlaceable.place(
                horizontalAlignment.align(menuItemsPlaceable.width, width, layoutDirection),
                0,
            )
            buttonPlaceable?.let {
                it.place(
                    horizontalAlignment.align(it.width, width, layoutDirection),
                    height - it.height - buttonPaddingBottom,
                )
            }
        }
    }
}

@Composable
private fun FloatingActionButtonMenuItemColumn(
    modifier: Modifier,
    expanded: () -> Boolean,
    composeItems: () -> Boolean,
    horizontalAlignment: Alignment.Horizontal,
    buttonHeight: () -> Int,
    content: @Composable FloatingActionButtonMenuScope.() -> Unit,
) {
    val scope = remember { FloatingActionButtonMenuScopeImpl(horizontalAlignment) }
    scope.horizontalAlignment = horizontalAlignment
    val coroutineScope = rememberCoroutineScope()
    // TODO Load the motionScheme tokens from the component tokens file
    val slowEffects: FiniteAnimationSpec<Int> = MotionSchemeKeyTokens.SlowEffects.value()
    val staggerAnimSpec =
        remember(slowEffects) {
            if (slowEffects is SpringSpec<Int>) {
                // Apply a small visibilityThreshold to the provided SpringSpec to avoid a delay in
                // the appearance of the last item when the list is populated.
                spring(
                    stiffness = slowEffects.stiffness,
                    dampingRatio = slowEffects.dampingRatio,
                    visibilityThreshold = 1,
                )
            } else {
                slowEffects
            }
        }
    Layout(
        modifier =
            modifier
                .clipToBounds()
                .semantics {
                    isTraversalGroup = true
                    traversalIndex = -0.9f
                }
                .then(
                    if (scope.itemsNeedVerticalScroll) {
                        Modifier.layout { measurable, constraints ->
                                // Capture parent maxHeight before verticalScroll relaxes it to
                                // Infinity.
                                scope.originalConstraintsMaxHeight = constraints.maxHeight
                                val placeable = measurable.measure(constraints)
                                scope.originalConstraintsMaxHeight = Constraints.Infinity
                                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                            }
                            .verticalScroll(state = rememberScrollState(), enabled = expanded())
                    } else {
                        Modifier
                    }
                ),
        content = { if (composeItems()) content(scope) },
    ) { measurables, constraints ->
        val itemCount = measurables.size
        val visibleCount =
            scope.updateTarget(
                target = if (expanded()) itemCount else 0,
                coroutineScope = coroutineScope,
                animationSpec = staggerAnimSpec,
            )

        val placeables = measurables.fastMap { it.measure(constraints) }
        val width = placeables.fastMaxBy { it.width }?.width ?: 0

        val verticalSpacing = FabMenuItemSpacingVertical.roundToPx()
        val verticalSpacingHeight =
            if (placeables.isNotEmpty()) verticalSpacing * (placeables.size - 1) else 0
        val currentButtonHeight = buttonHeight()
        val bottomPadding =
            if (currentButtonHeight > 0) {
                currentButtonHeight +
                    FabMenuButtonPaddingBottom.roundToPx() +
                    FabMenuPaddingBottom.roundToPx()
            } else {
                0
            }
        val height = placeables.fastSumBy { it.height } + verticalSpacingHeight + bottomPadding
        var visibleHeight = bottomPadding.toFloat()
        if (visibleCount > 0) {
            placeables.fastForEachIndexed { index, placeable ->
                if (index >= itemCount - visibleCount) {
                    visibleHeight += placeable.height
                    if (index < placeables.size - 1) visibleHeight += verticalSpacing
                }
            }
        }

        val finalHeight = if (placeables.fastAny { it.isVisible }) height else 0
        scope.itemsNeedVerticalScroll =
            finalHeight > minOf(scope.originalConstraintsMaxHeight, constraints.maxHeight)

        layout(width, finalHeight, rulers = { MenuItemRuler provides height - visibleHeight }) {
            var y = 0
            placeables.fastForEachIndexed { index, placeable ->
                val x = horizontalAlignment.align(placeable.width, width, layoutDirection)
                placeable.place(x, y)
                y += placeable.height
                if (index < placeables.size - 1) y += verticalSpacing
            }
        }
    }
}

/** Scope for the children of [FloatingActionButtonMenu] */
public interface FloatingActionButtonMenuScope {
    public val horizontalAlignment: Alignment.Horizontal
}

@Stable
private class FloatingActionButtonMenuScopeImpl(horizontalAlignment: Alignment.Horizontal) :
    FloatingActionButtonMenuScope {
    override var horizontalAlignment by mutableStateOf(horizontalAlignment)
    var itemsNeedVerticalScroll by mutableStateOf(false)
    var originalConstraintsMaxHeight: Int = Constraints.Infinity

    // Items are only composed before the first measure when the menu starts expanded.
    val isInitiallyVisible: Boolean
        get() = targetItemCount < 0

    private var visibleCount by mutableIntStateOf(0)
    private var targetItemCount = -1
    private var staggerAnim: Animatable<Int, AnimationVector1D>? = null

    fun updateTarget(
        target: Int,
        coroutineScope: CoroutineScope,
        animationSpec: FiniteAnimationSpec<Int>,
    ): Int {
        if (targetItemCount < 0) {
            targetItemCount = target
            visibleCount = target
        } else if (targetItemCount != target) {
            targetItemCount = target
            val anim =
                staggerAnim
                    ?: Animatable(visibleCount, Int.VectorConverter).also { staggerAnim = it }
            coroutineScope.launch {
                anim.animateTo(target, animationSpec) { visibleCount = value }
            }
        }
        return visibleCount
    }
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
 *
 * @material3expressive
 */
@Composable
public fun FloatingActionButtonMenuScope.FloatingActionButtonMenuItem(
    onClick: () -> Unit,
    text: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = contentColorFor(containerColor),
) {
    // TODO Load the motionScheme tokens from the component tokens file
    val widthSpring: FiniteAnimationSpec<Float> = MotionSchemeKeyTokens.FastSpatial.value()
    val alphaSpring: FiniteAnimationSpec<Float> = MotionSchemeKeyTokens.FastEffects.value()
    val widthProgress = remember {
        mutableFloatStateOf(
            if ((this as? FloatingActionButtonMenuScopeImpl)?.isInitiallyVisible == true) 1f else 0f
        )
    }

    // Disable min interactive component size because it interferes with the item expand
    // animation and we know we are meeting the size requirements below.
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        Surface(
            modifier =
                modifier.then(MenuItemAnimationElement(widthProgress, widthSpring, alphaSpring)),
            shape = FabMenuBaselineTokens.ListItemContainerShape.value,
            color = containerColor,
            contentColor = contentColor,
            onClick = onClick,
        ) {
            Row(
                Modifier.layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val width =
                            (placeable.width * maxOf(widthProgress.floatValue, 0f)).roundToInt()
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
 *
 * @material3expressive
 */
@Composable
public fun ToggleFloatingActionButton(
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
            remember(initialSize, density) {
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
                    val radius = containerCornerRadius(checkedProgress()).toPx()
                    drawRoundRect(
                        color = containerColor(checkedProgress()),
                        cornerRadius = CornerRadius(radius),
                    )
                }
                .toggleable(
                    value = checked,
                    onValueChange = onCheckedChange,
                    interactionSource = null,
                    indication = ripple(radius = fabRippleRadius, focusRingShape = shape),
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
public object ToggleFloatingActionButtonDefaults {

    @Composable
    public fun containerColor(
        initialColor: Color = MaterialTheme.colorScheme.primaryContainer,
        finalColor: Color = MaterialTheme.colorScheme.primary,
    ): (Float) -> Color = { progress -> lerp(initialColor, finalColor, progress) }

    public fun containerSize(initialSize: Dp, finalSize: Dp = FabFinalSize): (Float) -> Dp =
        { progress ->
            lerp(initialSize, finalSize, progress)
        }

    public fun containerSize(): (Float) -> Dp = DefaultContainerSize

    public fun containerSizeMedium(): (Float) -> Dp = DefaultContainerSizeMedium

    public fun containerSizeLarge(): (Float) -> Dp = DefaultContainerSizeLarge

    public fun containerCornerRadius(
        initialSize: Dp,
        finalSize: Dp = FabFinalCornerRadius,
    ): (Float) -> Dp = { progress -> lerp(initialSize, finalSize, progress) }

    public fun containerCornerRadius(): (Float) -> Dp = DefaultContainerCornerRadius

    public fun containerCornerRadiusMedium(): (Float) -> Dp = DefaultContainerCornerRadiusMedium

    public fun containerCornerRadiusLarge(): (Float) -> Dp = DefaultContainerCornerRadiusLarge

    @Composable
    public fun iconColor(
        initialColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
        finalColor: Color = MaterialTheme.colorScheme.onPrimary,
    ): (Float) -> Color = { progress -> lerp(initialColor, finalColor, progress) }

    public fun iconSize(initialSize: Dp, finalSize: Dp = FabFinalIconSize): (Float) -> Dp =
        { progress ->
            lerp(initialSize, finalSize, progress)
        }

    public fun iconSize(): (Float) -> Dp = DefaultIconSize

    public fun iconSizeMedium(): (Float) -> Dp = DefaultIconSizeMedium

    public fun iconSizeLarge(): (Float) -> Dp = DefaultIconSizeLarge

    private val DefaultContainerSize: (Float) -> Dp = containerSize(FabInitialSize)
    private val DefaultContainerSizeMedium: (Float) -> Dp = containerSize(FabMediumInitialSize)
    private val DefaultContainerSizeLarge: (Float) -> Dp = containerSize(FabLargeInitialSize)
    private val DefaultContainerCornerRadius: (Float) -> Dp =
        containerCornerRadius(FabInitialCornerRadius)
    private val DefaultContainerCornerRadiusMedium: (Float) -> Dp =
        containerCornerRadius(FabMediumInitialCornerRadius)
    private val DefaultContainerCornerRadiusLarge: (Float) -> Dp =
        containerCornerRadius(FabLargeInitialCornerRadius)
    private val DefaultIconSize: (Float) -> Dp = iconSize(FabInitialIconSize)
    private val DefaultIconSizeMedium: (Float) -> Dp = iconSize(FabMediumInitialIconSize)
    private val DefaultIconSizeLarge: (Float) -> Dp = iconSize(FabLargeInitialIconSize)

    /**
     * Modifier for animating the color and size of an icon within [ToggleFloatingActionButton]
     * based on a progress value.
     *
     * @param checkedProgress callback that provides the progress value for the icon animation
     * @param color the color of the icon, based on the checked progress value from 0-1
     * @param size the size of the icon, based on the checked progress value from 0-1
     */
    @Composable
    public fun Modifier.animateIcon(
        checkedProgress: () -> Float,
        color: (Float) -> Color = iconColor(),
        size: (Float) -> Dp = iconSize(),
    ): Modifier =
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
public interface ToggleFloatingActionButtonScope {

    public val checkedProgress: Float
}

private data class MenuItemAnimationElement(
    private val widthProgress: MutableFloatState,
    private val widthSpring: FiniteAnimationSpec<Float>,
    private val alphaSpring: FiniteAnimationSpec<Float>,
) : ModifierNodeElement<MenuItemAnimationNode>() {
    override fun create() = MenuItemAnimationNode(widthProgress, widthSpring, alphaSpring)

    override fun update(node: MenuItemAnimationNode) {
        if (node.widthProgress !== widthProgress) {
            node.widthProgress = widthProgress
            node.onReset()
            node.invalidatePlacement()
        }
        node.widthSpring = widthSpring
        node.alphaSpring = alphaSpring
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "menuItemAnimation"
        properties["widthProgress"] = widthProgress
        properties["widthSpring"] = widthSpring
        properties["alphaSpring"] = alphaSpring
    }
}

private class MenuItemAnimationNode(
    var widthProgress: MutableFloatState,
    var widthSpring: FiniteAnimationSpec<Float>,
    var alphaSpring: FiniteAnimationSpec<Float>,
) : LayoutModifierNode, ParentDataModifierNode, SemanticsModifierNode, Modifier.Node() {

    private var itemAlpha by
        mutableFloatStateOf(Snapshot.withoutReadObservation { widthProgress.floatValue })
    var isVisible by mutableStateOf(itemAlpha != 0f)
        private set

    private var target = Float.NaN
    private var widthAnim: Animatable<Float, AnimationVector1D>? = null
    private var alphaAnim: Animatable<Float, AnimationVector1D>? = null
    private val layerBlock: GraphicsLayerScope.() -> Unit = { alpha = itemAlpha }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            val newTarget = if (MenuItemRuler.current(Float.POSITIVE_INFINITY) <= 0) 1f else 0f
            if (target.isNaN()) {
                target = newTarget
                widthProgress.floatValue = newTarget
                updateAlpha(newTarget)
            } else if (target != newTarget) {
                target = newTarget
                val wAnim =
                    widthAnim
                        ?: Animatable(widthProgress.floatValue, Float.VectorConverter).also {
                            widthAnim = it
                        }
                coroutineScope.launch {
                    wAnim.animateTo(newTarget, widthSpring) {
                        if (this === widthAnim) widthProgress.floatValue = value
                    }
                }
                val aAnim =
                    alphaAnim
                        ?: Animatable(itemAlpha, Float.VectorConverter).also { alphaAnim = it }
                coroutineScope.launch {
                    aAnim.animateTo(newTarget, alphaSpring) {
                        if (this === alphaAnim) updateAlpha(value)
                    }
                }
            }
            if (isVisible) {
                placeable.placeWithLayer(0, 0, layerBlock = layerBlock)
            }
        }
    }

    private fun updateAlpha(value: Float) {
        itemAlpha = value
        val visible = value != 0f
        if (isVisible != visible) {
            isVisible = visible
            if (isAttached) invalidateSemantics()
        }
    }

    override fun onReset() {
        widthAnim = null
        alphaAnim = null
        target = Float.NaN
        updateAlpha(Snapshot.withoutReadObservation { widthProgress.floatValue })
    }

    override fun Density.modifyParentData(parentData: Any?) = this@MenuItemAnimationNode

    override val shouldClearDescendantSemantics: Boolean
        get() = !isVisible

    override fun SemanticsPropertyReceiver.applySemantics() {}
}

private val Placeable.isVisible: Boolean
    get() = (this.parentData as? MenuItemAnimationNode)?.isVisible != false

private val FabInitialSize
    get() = FabBaselineTokens.ContainerHeight
private val FabInitialCornerRadius
    get() = 16.dp
private val FabInitialIconSize
    get() = FabBaselineTokens.IconSize
private val FabMediumInitialSize
    get() = FabMediumTokens.ContainerHeight
private val FabMediumInitialCornerRadius
    get() = 20.dp
private val FabMediumInitialIconSize
    get() = FabMediumTokens.IconSize
private val FabLargeInitialSize
    get() = FabLargeTokens.ContainerHeight
private val FabLargeInitialCornerRadius
    get() = 28.dp
private val FabLargeInitialIconSize = 36.dp // TODO: FabLargeTokens.IconSize is incorrect
private val FabFinalSize
    get() = FabMenuBaselineTokens.CloseButtonContainerHeight
private val FabFinalCornerRadius = FabFinalSize.div(2)
private val FabFinalIconSize
    get() = FabMenuBaselineTokens.CloseButtonIconSize
private val FabShadowElevation
    get() = FabPrimaryContainerTokens.ContainerElevation
private val FabMenuPaddingHorizontal
    get() = 16.dp
private val FabMenuPaddingBottom
    get() = FabMenuBaselineTokens.CloseButtonBetweenSpace
private val FabMenuButtonPaddingBottom
    get() = 16.dp
private val FabMenuItemMinWidth
    get() = FabMenuBaselineTokens.ListItemContainerHeight
private val FabMenuItemHeight
    get() = FabMenuBaselineTokens.ListItemContainerHeight
private val FabMenuItemSpacingVertical
    get() = FabMenuBaselineTokens.ListItemBetweenSpace
private val FabMenuItemContentPaddingStart
    get() = FabMenuBaselineTokens.ListItemLeadingSpace
private val FabMenuItemContentPaddingEnd
    get() = FabMenuBaselineTokens.ListItemTrailingSpace
private val FabMenuItemContentSpacingHorizontal
    get() = FabMenuBaselineTokens.ListItemIconLabelSpace
