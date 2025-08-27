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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.internal.DraggableAnchors
import androidx.compose.material3.internal.Strings
import androidx.compose.material3.internal.draggableAnchors
import androidx.compose.material3.internal.getString
import androidx.compose.material3.internal.systemBarsForVisualComponents
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.material3.tokens.NavigationRailBaselineItemTokens
import androidx.compose.material3.tokens.NavigationRailCollapsedTokens
import androidx.compose.material3.tokens.NavigationRailColorTokens
import androidx.compose.material3.tokens.NavigationRailExpandedTokens
import androidx.compose.material3.tokens.NavigationRailHorizontalItemTokens
import androidx.compose.material3.tokens.NavigationRailVerticalItemTokens
import androidx.compose.material3.tokens.ScrimTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrain
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.lerp
import kotlin.math.min
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * Material design wide navigation rail.
 *
 * Wide navigation rails provide access to primary destinations in apps when using tablet and
 * desktop screens.
 *
 * ![Wide navigation rail collapsed
 * image](https://developer.android.com/images/reference/androidx/compose/material3/wide-navigation-rail-collapsed.png)
 *
 * ![Wide navigation rail expanded
 * image](https://developer.android.com/images/reference/androidx/compose/material3/wide-navigation-rail-expanded.png)
 *
 * The wide navigation rail should be used to display multiple [WideNavigationRailItem]s, each
 * representing a singular app destination, and, optionally, a header containing a menu button, a
 * [FloatingActionButton], and/or a logo. Each destination is typically represented by an icon and a
 * text label.
 *
 * The [WideNavigationRail] is collapsed by default, but it also supports being expanded via a
 * [WideNavigationRailState]. When collapsed, the rail should display three to seven navigation
 * items. A simple example looks like:
 *
 * @sample androidx.compose.material3.samples.WideNavigationRailCollapsedSample
 *
 * When expanded, the rail should display at least three navigation items. A simple example looks
 * like:
 *
 * @sample androidx.compose.material3.samples.WideNavigationRailExpandedSample
 *
 * The [WideNavigationRail] also supports automatically animating between the collapsed and expanded
 * values. That can be done like so:
 *
 * @sample androidx.compose.material3.samples.WideNavigationRailResponsiveSample
 *
 * For a modal variation of the wide navigation rail, see [ModalWideNavigationRail].
 *
 * Finally, the [WideNavigationRail] supports setting an [Arrangement.Vertical] for the items, with
 * [Arrangement.Top] being the default. The header will always be at the top.
 *
 * See [WideNavigationRailItem] for configuration specific to each item, and not the overall
 * [WideNavigationRail] component.
 *
 * @param modifier the [Modifier] to be applied to this wide navigation rail
 * @param state the [WideNavigationRailState] of this wide navigation rail
 * @param shape defines the shape of this wide navigation rail's container.
 * @param colors [WideNavigationRailColors] that will be used to resolve the colors used for this
 *   wide navigation rail. See [WideNavigationRailDefaults.colors]
 * @param header optional header that may hold a [FloatingActionButton] or a logo
 * @param windowInsets a window insets of the wide navigation rail
 * @param arrangement the [Arrangement.Vertical] of this wide navigation rail for its content. Note
 *   that if there's a header present, the items will be arranged on the remaining space below it,
 *   except for the center arrangement which considers the entire height of the container
 * @param content the content of this wide navigation rail, typically [WideNavigationRailItem]s
 */
@OptIn(ExperimentalMaterial3ComponentOverrideApi::class)
@Composable
fun WideNavigationRail(
    modifier: Modifier = Modifier,
    state: WideNavigationRailState = rememberWideNavigationRailState(),
    shape: Shape = WideNavigationRailDefaults.shape,
    colors: WideNavigationRailColors = WideNavigationRailDefaults.colors(),
    header: @Composable (() -> Unit)? = null,
    windowInsets: WindowInsets = WideNavigationRailDefaults.windowInsets,
    arrangement: Arrangement.Vertical = WideNavigationRailDefaults.arrangement,
    content: @Composable () -> Unit,
) {
    with(LocalWideNavigationRailOverride.current) {
        WideNavigationRailOverrideScope(
                modifier = modifier,
                state = state,
                shape = shape,
                colors = colors,
                header = header,
                windowInsets = windowInsets,
                arrangement = arrangement,
                content = content,
            )
            .WideNavigationRail()
    }
}

/**
 * This override provides the default behavior of the [WideNavigationRail] component.
 *
 * [WideNavigationRailOverride] used when no override is specified.
 */
@ExperimentalMaterial3ComponentOverrideApi
object DefaultWideNavigationRailOverride : WideNavigationRailOverride {
    @Composable
    override fun WideNavigationRailOverrideScope.WideNavigationRail() {
        WideNavigationRailLayout(
            modifier = modifier,
            isModal = false,
            expanded = state.targetValue.isExpanded,
            colors = colors,
            shape = shape,
            header = header,
            windowInsets = windowInsets,
            arrangement = arrangement,
            content = content,
        )
    }
}

@Composable
private fun WideNavigationRailLayout(
    modifier: Modifier,
    isModal: Boolean,
    expanded: Boolean,
    colors: WideNavigationRailColors,
    shape: Shape,
    header: @Composable (() -> Unit)?,
    windowInsets: WindowInsets,
    arrangement: Arrangement.Vertical,
    content: @Composable () -> Unit,
) {
    var currentWidth by remember { mutableIntStateOf(0) }
    var actualMaxExpandedWidth by remember { mutableIntStateOf(0) }
    val minimumA11ySize =
        if (LocalMinimumInteractiveComponentSize.current == Dp.Unspecified) {
            0.dp
        } else {
            LocalMinimumInteractiveComponentSize.current
        }

    // TODO: Load the motionScheme tokens from the component tokens file.
    val animationSpec = MotionSchemeKeyTokens.DefaultSpatial.value<Dp>()
    val modalAnimationSpec = MotionSchemeKeyTokens.FastSpatial.value<Dp>()
    val minWidth by
        animateDpAsState(
            targetValue = if (!expanded) CollapsedRailWidth else ExpandedRailMinWidth,
            animationSpec = if (!isModal) animationSpec else modalAnimationSpec,
        )
    val widthFullRange by
        animateDpAsState(
            targetValue = if (!expanded) CollapsedRailWidth else ExpandedRailMaxWidth,
            animationSpec = if (!isModal) animationSpec else modalAnimationSpec,
        )
    val itemVerticalSpacedBy by
        animateDpAsState(
            targetValue = if (!expanded) NavigationRailCollapsedTokens.ItemVerticalSpace else 0.dp,
            animationSpec = animationSpec,
        )
    val itemMinHeight by
        animateDpAsState(
            targetValue = if (!expanded) TopIconItemMinHeight else minimumA11ySize,
            animationSpec = animationSpec,
        )

    Surface(
        color = if (!isModal) colors.containerColor else colors.modalContainerColor,
        contentColor = if (!isModal) colors.contentColor else colors.modalContentColor,
        shape = shape,
        modifier = modifier,
    ) {
        Layout(
            modifier =
                Modifier.fillMaxHeight()
                    .windowInsetsPadding(windowInsets)
                    .widthIn(max = ExpandedRailMaxWidth)
                    .padding(top = WNRVerticalPadding)
                    .selectableGroup()
                    .semantics { isTraversalGroup = true },
            content = {
                if (header != null) {
                    Box(Modifier.layoutId(HeaderLayoutIdTag)) { header() }
                }
                content()
            },
            measurePolicy =
                object : MeasurePolicy {
                    override fun MeasureScope.measure(
                        measurables: List<Measurable>,
                        constraints: Constraints,
                    ): MeasureResult {
                        val height = constraints.maxHeight
                        var itemsCount = measurables.size
                        var actualExpandedMinWidth = constraints.minWidth
                        val actualMinWidth =
                            if (constraints.minWidth == 0) {
                                actualExpandedMinWidth =
                                    ExpandedRailMinWidth.roundToPx()
                                        .coerceAtMost(constraints.maxWidth)
                                minWidth.roundToPx().coerceAtMost(constraints.maxWidth)
                            } else {
                                constraints.minWidth
                            }
                        // If there are no items, rail will be empty.
                        if (itemsCount < 1) {
                            return layout(actualMinWidth, height) {}
                        }
                        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
                        var itemsMeasurables = measurables

                        var constraintsOffset = 0
                        var headerPlaceable: Placeable? = null
                        if (header != null) {
                            headerPlaceable =
                                measurables
                                    .fastFirst { it.layoutId == HeaderLayoutIdTag }
                                    .measure(looseConstraints)
                            // Header is always first element in measurables list.
                            if (itemsCount > 1)
                                itemsMeasurables = measurables.subList(1, itemsCount)
                            // Real item count doesn't include the header.
                            itemsCount--
                            constraintsOffset = headerPlaceable.height
                        }

                        val itemsPlaceables =
                            if (itemsCount > 0) mutableListOf<Placeable>() else null
                        val itemMaxWidthConstraint =
                            if (expanded) looseConstraints.maxWidth else actualMinWidth
                        var expandedItemMaxWidth = 0
                        if (itemsPlaceables != null) {
                            itemsMeasurables.fastMap {
                                val measuredItem =
                                    it.measure(
                                        looseConstraints
                                            .offset(vertical = -constraintsOffset)
                                            .constrain(
                                                Constraints.fitPrioritizingWidth(
                                                    minWidth = minimumA11ySize.roundToPx(),
                                                    minHeight = itemMinHeight.roundToPx(),
                                                    maxWidth = itemMaxWidthConstraint,
                                                    maxHeight = looseConstraints.maxHeight,
                                                )
                                            )
                                    )
                                val maxItemWidth = measuredItem.measuredWidth
                                if (expanded && expandedItemMaxWidth < maxItemWidth) {
                                    expandedItemMaxWidth =
                                        maxItemWidth + ItemHorizontalPadding.roundToPx()
                                }
                                constraintsOffset = measuredItem.height
                                itemsPlaceables.add(measuredItem)
                            }
                        }

                        var width = actualMinWidth
                        // Limit collapsed rail to fixed width, but expanded rail can be as wide as
                        // constraints.maxWidth
                        if (expanded) {
                            val widestElementWidth =
                                maxOf(expandedItemMaxWidth, headerPlaceable?.width ?: 0)

                            if (
                                widestElementWidth > actualMinWidth &&
                                    widestElementWidth > actualExpandedMinWidth
                            ) {
                                val widthConstrain =
                                    maxOf(widestElementWidth, actualExpandedMinWidth)
                                        .coerceAtMost(constraints.maxWidth)
                                // Use widthFullRange so there's no jump in animation for when the
                                // expanded width has to be wider than actualExpandedMinWidth.
                                width = widthFullRange.roundToPx().coerceAtMost(widthConstrain)
                                actualMaxExpandedWidth = width
                            }
                        } else {
                            if (actualMaxExpandedWidth > 0) {
                                // Use widthFullRange so there's no jump in animation for the case
                                // when the expanded width was wider than actualExpandedMinWidth.
                                width =
                                    widthFullRange
                                        .roundToPx()
                                        .coerceIn(
                                            minimumValue = actualMinWidth,
                                            maximumValue =
                                                currentWidth.coerceAtLeast(actualMinWidth),
                                        )
                            }
                        }
                        currentWidth = width

                        return layout(width, height) {
                            val railHeight = height - WNRVerticalPadding.roundToPx()
                            var headerOffset = 0
                            if (headerPlaceable != null && headerPlaceable.height > 0) {
                                headerPlaceable.placeRelative(0, 0)
                                headerOffset +=
                                    headerPlaceable.height + WNRHeaderPadding.roundToPx()
                            }

                            if (itemsPlaceables != null) {
                                val layoutSize =
                                    if (arrangement == Arrangement.Center) {
                                        // For centered arrangement the items will be centered in
                                        // the container, not in the remaining space below the
                                        // header.
                                        railHeight
                                    } else {
                                        railHeight - headerOffset
                                    }
                                val sizes = IntArray(itemsPlaceables.size)
                                itemsPlaceables.fastForEachIndexed { index, item ->
                                    sizes[index] = item.height
                                    if (index < itemsPlaceables.size - 1) {
                                        sizes[index] += itemVerticalSpacedBy.roundToPx()
                                    }
                                }
                                val y = IntArray(itemsPlaceables.size)
                                with(arrangement) { arrange(layoutSize, sizes, y) }

                                val offset =
                                    if (arrangement == Arrangement.Center) 0 else headerOffset
                                itemsPlaceables.fastForEachIndexed { index, item ->
                                    item.placeRelative(0, y[index] + offset)
                                }
                            }
                        }
                    }
                },
        )
    }
}

/**
 * Material design modal wide navigation rail.
 *
 * Wide navigation rails provide access to primary destinations in apps when using tablet and
 * desktop screens.
 *
 * The modal wide navigation rail should be used to display multiple [WideNavigationRailItem]s, each
 * representing a singular app destination, and, optionally, a header containing a menu button, a
 * [FloatingActionButton], and/or a logo. Each destination is typically represented by an icon and a
 * text label.
 *
 * The [ModalWideNavigationRail] when collapsed behaves like a collapsed [WideNavigationRail]. When
 * expanded, the modal wide navigation rail blocks interaction with the rest of an app’s content
 * with a scrim. It is elevated above the app’s UI and doesn't affect the screen’s layout grid. That
 * can be achieved like so:
 *
 * @sample androidx.compose.material3.samples.ModalWideNavigationRailSample
 *
 * For a dismissible [ModalWideNavigationRail], that enters from offscreen instead of expanding from
 * the collapsed rail, set [hideOnCollapse] to true. That can be achieved like so:
 *
 * @sample androidx.compose.material3.samples.DismissibleModalWideNavigationRailSample
 *
 * See [WideNavigationRailItem] for configuration specific to each item, and not the overall
 * [ModalWideNavigationRail] component.
 *
 * @param modifier the [Modifier] to be applied to this wide navigation rail
 * @param state the [WideNavigationRailState] of this wide navigation rail
 * @param hideOnCollapse whether this wide navigation rail should slide offscreen when it collapses
 *   and be hidden, or stay on screen as a collapsed wide navigation rail (default)
 * @param collapsedShape the shape of this wide navigation rail's container when it's collapsed
 * @param expandedShape the shape of this wide navigation rail's container when it's expanded
 * @param colors [WideNavigationRailColors] that will be used to resolve the colors used for this
 *   wide navigation rail. See [WideNavigationRailDefaults.colors]
 * @param header optional header that may hold a [FloatingActionButton] or a logo
 * @param expandedHeaderTopPadding the padding to be applied to the top of the rail. It's usually
 *   needed in order to align the content of the rail between the collapsed and expanded animation
 * @param windowInsets a window insets of the wide navigation rail
 * @param arrangement the [Arrangement.Vertical] of this wide navigation rail
 * @param expandedProperties [ModalWideNavigationRailProperties] for further customization of the
 *   expanded modal wide navigation rail's window behavior
 * @param content the content of this modal wide navigation rail, usually [WideNavigationRailItem]s
 */
@OptIn(ExperimentalMaterial3ComponentOverrideApi::class)
@Composable
fun ModalWideNavigationRail(
    modifier: Modifier = Modifier,
    state: WideNavigationRailState = rememberWideNavigationRailState(),
    hideOnCollapse: Boolean = false,
    collapsedShape: Shape = WideNavigationRailDefaults.modalCollapsedShape,
    expandedShape: Shape = WideNavigationRailDefaults.modalExpandedShape,
    colors: WideNavigationRailColors = WideNavigationRailDefaults.colors(),
    header: @Composable (() -> Unit)? = null,
    expandedHeaderTopPadding: Dp = 0.dp,
    windowInsets: WindowInsets = WideNavigationRailDefaults.windowInsets,
    arrangement: Arrangement.Vertical = WideNavigationRailDefaults.arrangement,
    expandedProperties: ModalWideNavigationRailProperties =
        WideNavigationRailDefaults.ModalExpandedProperties,
    content: @Composable () -> Unit,
) {
    val scope =
        ModalWideNavigationRailOverrideScope(
            modifier = modifier,
            state = state,
            shouldHideOnCollapse = hideOnCollapse,
            collapsedShape = collapsedShape,
            expandedShape = expandedShape,
            colors = colors,
            header = header,
            expandedHeaderTopPadding = expandedHeaderTopPadding,
            windowInsets = windowInsets,
            arrangement = arrangement,
            expandedProperties = expandedProperties,
            content = content,
        )
    with(LocalModalWideNavigationRailOverride.current) { scope.ModalWideNavigationRail() }
}

/**
 * This override provides the default behavior of the [ModalWideNavigationRail] component.
 *
 * [ModalWideNavigationRailOverride] used when no override is specified.
 */
@ExperimentalMaterial3ComponentOverrideApi
object DefaultModalWideNavigationRailOverride : ModalWideNavigationRailOverride {
    @Composable
    override fun ModalWideNavigationRailOverrideScope.ModalWideNavigationRail() {
        val rememberContent =
            if (shouldHideOnCollapse) {
                content
            } else remember(content) { movableContentOf(content) }

        val density = LocalDensity.current
        // TODO: Load the motionScheme tokens from the component tokens file.
        val modalStateAnimationSpec = MotionSchemeKeyTokens.DefaultSpatial.value<Float>()
        val modalState =
            remember(state) {
                ModalWideNavigationRailState(
                    state = state,
                    density = density,
                    animationSpec = modalStateAnimationSpec,
                )
            }
        val positionProgress =
            animateFloatAsState(
                targetValue = if (!state.targetValue.isExpanded) 0f else 1f,
                // TODO: Load the motionScheme tokens from the component tokens file.
                animationSpec = MotionSchemeKeyTokens.DefaultEffects.value(),
            )
        val isCollapsed: Boolean by remember { derivedStateOf { positionProgress.value == 0f } }
        val modalExpanded: Boolean by remember { derivedStateOf { positionProgress.value >= 0.3f } }
        val animateToDismiss: suspend () -> Unit = {
            if (shouldHideOnCollapse) {
                modalState.collapse()
            }
            state.collapse()
        }

        val settleToDismiss: suspend (velocity: Float) -> Unit = {
            if (shouldHideOnCollapse) {
                modalState.settle(it)
                if (!modalState.targetValue.isExpanded) state.collapse()
            }
        }

        // Display a non modal rail if it shouldn't hide on collapsed.
        if (!shouldHideOnCollapse) {
            // Keep this rail even when expanded so that screen layout doesn't change.
            WideNavigationRailLayout(
                modifier = modifier,
                isModal = false,
                expanded = false,
                colors = colors,
                shape = collapsedShape,
                header = header,
                windowInsets = windowInsets,
                arrangement = arrangement,
                content = {
                    // Only display content if it's collapsed, so that it doesn't affect this
                    // collapsed rail or the screen layout.
                    if (isCollapsed) {
                        rememberContent()
                    }
                },
            )
        }

        val channel = remember { Channel<Boolean>(Channel.CONFLATED) }
        if (shouldHideOnCollapse) {
            LaunchedEffect(channel) {
                for (target in channel) {
                    val newTarget = channel.tryReceive().getOrNull() ?: target
                    launch {
                        if (newTarget) {
                            modalState.expand()
                        } else {
                            modalState.collapse()
                        }
                    }
                }
            }
        }

        // Display a modal container when expanded.
        if (!isCollapsed) {
            val scope = rememberCoroutineScope()
            val predictiveBackProgress = remember { Animatable(initialValue = 0f) }
            val predictiveBackState = remember { RailPredictiveBackState() }

            SideEffect { channel.trySend(state.targetValue.isExpanded) }

            ModalWideNavigationRailDialog(
                properties = expandedProperties,
                onDismissRequest = { scope.launch { state.collapse() } },
                onPredictiveBack = { backEvent ->
                    scope.launch { predictiveBackProgress.snapTo(backEvent) }
                },
                onPredictiveBackCancelled = {
                    scope.launch { predictiveBackProgress.animateTo(0f) }
                },
                predictiveBackState = predictiveBackState,
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxSize().imePadding().onKeyEvent {
                            if (it.type == KeyEventType.KeyUp && it.key == Key.Escape) {
                                scope.launch { state.collapse() }
                                return@onKeyEvent true
                            }
                            return@onKeyEvent false
                        }
                ) {
                    val isScrimVisible =
                        if (shouldHideOnCollapse) {
                            (modalState.targetValue != WideNavigationRailValue.Collapsed)
                        } else {
                            modalExpanded
                        }

                    Scrim(
                        color = colors.modalScrimColor,
                        onDismissRequest = animateToDismiss,
                        visible = isScrimVisible,
                    )

                    ModalWideNavigationRailContent(
                        expanded = shouldHideOnCollapse || modalExpanded,
                        isStandaloneModal = shouldHideOnCollapse,
                        predictiveBackProgress = predictiveBackProgress,
                        predictiveBackState = predictiveBackState,
                        settleToDismiss = settleToDismiss,
                        modifier = modifier,
                        railState = modalState,
                        colors = colors,
                        shape = expandedShape,
                        openModalRailMaxWidth = ExpandedRailMaxWidth,
                        header = {
                            Box(modifier = Modifier.padding(top = expandedHeaderTopPadding)) {
                                header?.invoke()
                            }
                        },
                        windowInsets = windowInsets,
                        gesturesEnabled = shouldHideOnCollapse,
                        arrangement = arrangement,
                        content = rememberContent,
                    )
                }
            }
        }
    }
}

/**
 * Material Design wide navigation rail item.
 *
 * It's recommend for navigation items to always have a text label. A [WideNavigationRailItem]
 * always displays labels (if they exist) when selected and unselected.
 *
 * The [WideNavigationRailItem] supports two different icon positions, top and start, which is
 * controlled by the [iconPosition] param:
 * - If the icon position is [NavigationItemIconPosition.Top] the icon will be displayed above the
 *   label. This configuration should be used with collapsed wide navigation rails.
 * - If the icon position is [NavigationItemIconPosition.Start] the icon will be displayed to the
 *   start of the label. This configuration should be used with expanded wide navigation rails.
 *
 * However, if an animated item is desired, the [iconPosition] can be controlled via the expanded
 * value of the associated [WideNavigationRail] or [ModalWideNavigationRail]. By default, it'll use
 * the [railExpanded] to follow the configuration described above.
 *
 * @param selected whether this item is selected
 * @param onClick called when this item is clicked
 * @param icon icon for this item, typically an [Icon]
 * @param label text label for this item
 * @param railExpanded whether the associated [WideNavigationRail] is expanded or collapsed
 * @param modifier the [Modifier] to be applied to this item
 * @param enabled controls the enabled state of this item. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param iconPosition the [NavigationItemIconPosition] for the icon
 * @param colors [NavigationItemColors] that will be used to resolve the colors used for this item
 *   in different states. See [WideNavigationRailItemDefaults.colors]
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this item. You can use this to change the item's appearance or
 *   preview the item in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 */
@Composable
fun WideNavigationRailItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable (() -> Unit)?,
    railExpanded: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconPosition: NavigationItemIconPosition =
        WideNavigationRailItemDefaults.iconPositionFor(railExpanded),
    colors: NavigationItemColors = WideNavigationRailItemDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }

    AnimatedNavigationItem(
        selected = selected,
        onClick = onClick,
        icon = icon,
        indicatorShape = NavigationRailBaselineItemTokens.ActiveIndicatorShape.value,
        topIconIndicatorWidth = NavigationRailVerticalItemTokens.ActiveIndicatorWidth,
        topIconLabelTextStyle = NavigationRailVerticalItemTokens.LabelTextFont.value,
        startIconLabelTextStyle = NavigationRailHorizontalItemTokens.LabelTextFont.value,
        topIconIndicatorHorizontalPadding = ItemTopIconIndicatorHorizontalPadding,
        topIconIndicatorVerticalPadding = ItemTopIconIndicatorVerticalPadding,
        topIconIndicatorToLabelVerticalPadding = NavigationRailVerticalItemTokens.IconLabelSpace,
        startIconIndicatorHorizontalPadding =
            NavigationRailHorizontalItemTokens.FullWidthLeadingSpace,
        startIconIndicatorVerticalPadding = ItemStartIconIndicatorVerticalPadding,
        noLabelIndicatorPadding = WNRItemNoLabelIndicatorPadding,
        startIconToLabelHorizontalPadding = NavigationRailHorizontalItemTokens.IconLabelSpace,
        itemHorizontalPadding = ItemHorizontalPadding,
        colors = colors,
        modifier = modifier,
        enabled = enabled,
        label = label,
        iconPosition = iconPosition,
        interactionSource = interactionSource,
    )
}

/**
 * Material Design wide navigation rail item.
 *
 * It's recommend for navigation items to always have a text label. A [WideNavigationRailItem]
 * always displays labels (if they exist) when selected and unselected.
 *
 * The [WideNavigationRailItem] supports two different icon positions, top and start, which is
 * controlled by the [iconPosition] param:
 * - If the icon position is [NavigationItemIconPosition.Top] the icon will be displayed above the
 *   label. This configuration should be used with collapsed wide navigation rails.
 * - If the icon position is [NavigationItemIconPosition.Start] the icon will be displayed to the
 *   start of the label. This configuration should be used with expanded wide navigation rails.
 *
 * However, if an animated item is desired, the [iconPosition] can be controlled via the expanded
 * value of the associated [WideNavigationRail] or [ModalWideNavigationRail]. By default, it'll use
 * the [railExpanded] to follow the configuration described above.
 *
 * @param selected whether this item is selected
 * @param onClick called when this item is clicked
 * @param icon icon for this item, typically an [Icon]
 * @param label text label for this item
 * @param modifier the [Modifier] to be applied to this item
 * @param enabled controls the enabled state of this item. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param railExpanded whether the associated [WideNavigationRail] is expanded or collapsed
 * @param iconPosition the [NavigationItemIconPosition] for the icon
 * @param colors [NavigationItemColors] that will be used to resolve the colors used for this item
 *   in different states. See [WideNavigationRailItemDefaults.colors]
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this item. You can use this to change the item's appearance or
 *   preview the item in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 */
@Deprecated(
    message = "Deprecated in favor of function with required railExpanded parameter",
    level = DeprecationLevel.HIDDEN,
)
@ExperimentalMaterial3ExpressiveApi
@Composable
fun WideNavigationRailItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    railExpanded: Boolean = false,
    iconPosition: NavigationItemIconPosition =
        WideNavigationRailItemDefaults.iconPositionFor(railExpanded),
    colors: NavigationItemColors = WideNavigationRailItemDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) =
    WideNavigationRailItem(
        selected,
        onClick,
        icon,
        label,
        railExpanded,
        modifier,
        enabled,
        iconPosition,
        colors,
        interactionSource,
    )

/**
 * Represents the colors of the various elements of a wide navigation rail.
 *
 * @param containerColor the color used for the background of a non-modal wide navigation rail. Use
 *   [Color.Transparent] to have no color
 * @param contentColor the preferred color for content inside a wide navigation rail. Defaults to
 *   either the matching content color for [containerColor], or to the current [LocalContentColor]
 *   if [containerColor] is not a color from the theme
 * @param modalContainerColor the color used for the background of a modal wide navigation rail. Use
 *   [Color.Transparent] to have no color
 * @param modalScrimColor the color used for the scrim overlay for background content of a modal
 *   wide navigation rail
 * @param modalContentColor the preferred color for content inside a modal wide navigation rail.
 *   Defaults to either the matching content color for [modalContainerColor], or to the current
 *   [LocalContentColor]
 */
@Immutable
class WideNavigationRailColors
constructor(
    val containerColor: Color,
    val contentColor: Color,
    val modalContainerColor: Color,
    val modalScrimColor: Color,
    val modalContentColor: Color,
) {

    @Deprecated(
        message = "Deprecated in favor of constructor with modalContentColor parameter",
        replaceWith =
            ReplaceWith(
                "WideNavigationRailColors(containerColor, contentColor, modalContainerColor, " +
                    "modalScrimColor, modalContentColor)"
            ),
        level = DeprecationLevel.WARNING,
    )
    @ExperimentalMaterial3ExpressiveApi
    constructor(
        containerColor: Color,
        contentColor: Color,
        modalContainerColor: Color,
        modalScrimColor: Color,
    ) : this(containerColor, contentColor, modalContainerColor, modalScrimColor, contentColor)

    /**
     * Returns a copy of this NavigationRailColors, optionally overriding some of the values. This
     * uses the Color.Unspecified to mean “use the value from the source”.
     */
    fun copy(
        containerColor: Color = this.containerColor,
        contentColor: Color = this.contentColor,
        modalContainerColor: Color = this.modalContainerColor,
        modalScrimColor: Color = this.modalScrimColor,
        modalContentColor: Color = this.modalContentColor,
    ) =
        WideNavigationRailColors(
            containerColor = containerColor.takeOrElse { this.containerColor },
            contentColor = contentColor.takeOrElse { this.contentColor },
            modalContainerColor = modalContainerColor.takeOrElse { this.modalContainerColor },
            modalScrimColor = modalScrimColor.takeOrElse { this.modalScrimColor },
            modalContentColor = modalContentColor.takeOrElse { this.modalContentColor },
        )

    /**
     * Returns a copy of this NavigationRailColors, optionally overriding some of the values. This
     * uses the Color.Unspecified to mean “use the value from the source”.
     */
    @Deprecated(
        message = "Deprecated in favor of function with modalContentColor parameter",
        level = DeprecationLevel.HIDDEN,
    )
    @ExperimentalMaterial3ExpressiveApi
    fun copy(
        containerColor: Color = this.containerColor,
        contentColor: Color = this.contentColor,
        modalContainerColor: Color = this.modalContainerColor,
        modalScrimColor: Color = this.modalScrimColor,
    ) =
        copy(
            containerColor = containerColor.takeOrElse { this.containerColor },
            contentColor = contentColor.takeOrElse { this.contentColor },
            modalContainerColor = modalContainerColor.takeOrElse { this.modalContainerColor },
            modalScrimColor = modalScrimColor.takeOrElse { this.modalScrimColor },
            modalContentColor = contentColor.takeOrElse { this.contentColor },
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is WideNavigationRailColors) return false

        if (containerColor != other.containerColor) return false
        if (contentColor != other.contentColor) return false
        if (modalContainerColor != other.modalContainerColor) return false
        if (modalScrimColor != other.modalScrimColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = containerColor.hashCode()
        result = 31 * result + contentColor.hashCode()
        result = 31 * result + modalContainerColor.hashCode()
        result = 31 * result + modalScrimColor.hashCode()
        result = 31 * result + modalContentColor.hashCode()

        return result
    }
}

/** Defaults used in [WideNavigationRail]. */
object WideNavigationRailDefaults {
    /** Default container shape of a wide navigation rail. */
    val shape: Shape
        @Composable get() = NavigationRailCollapsedTokens.ContainerShape.value

    /** Default arrangement for a wide navigation rail. */
    val arrangement: Arrangement.Vertical
        get() = Arrangement.Top

    /** Default window insets for a wide navigation rail. */
    val windowInsets: WindowInsets
        @Composable
        get() =
            WindowInsets.systemBarsForVisualComponents.only(
                WindowInsetsSides.Vertical + WindowInsetsSides.Start
            )

    /** Default container shape of a collapsed [ModalWideNavigationRail]. */
    val modalCollapsedShape: Shape
        @Composable get() = shape

    /** Default container shape of a expanded [ModalWideNavigationRail]. */
    val modalExpandedShape: Shape
        @Composable get() = NavigationRailExpandedTokens.ModalContainerShape.value

    /** Properties used to customize the window behavior of a [ModalWideNavigationRail]. */
    val ModalExpandedProperties: ModalWideNavigationRailProperties =
        createDefaultModalWideNavigationRailProperties()

    /**
     * Creates a [WideNavigationRailColors] with the provided colors according to the Material
     * specification.
     */
    @Composable fun colors() = MaterialTheme.colorScheme.defaultWideWideNavigationRailColors

    /**
     * Creates a [WideNavigationRailColors] with the provided colors according to the Material
     * specification.
     *
     * @param containerColor the color used for the background of a non-modal wide navigation rail.
     * @param contentColor the preferred color for content inside a wide navigation rail. Defaults
     *   to either the matching content color for [containerColor], or to the current
     *   [LocalContentColor] if [containerColor] is not a color from the theme
     * @param modalContainerColor the color used for the background of a modal wide navigation rail.
     * @param modalScrimColor the color used for the scrim overlay for background content of a modal
     *   wide navigation rail
     * @param modalContentColor the preferred color for content inside a modal wide navigation rail.
     *   Defaults to either the matching content color for [modalContainerColor], or to the current
     *   [LocalContentColor] if [modalContainerColor] is not a color from the theme
     */
    @Composable
    fun colors(
        containerColor: Color = WideNavigationRailDefaults.containerColor,
        contentColor: Color = contentColorFor(containerColor),
        modalContainerColor: Color = NavigationRailExpandedTokens.ModalContainerColor.value,
        modalScrimColor: Color =
            ScrimTokens.ContainerColor.value.copy(ScrimTokens.ContainerOpacity),
        modalContentColor: Color = contentColorFor(modalContainerColor),
    ): WideNavigationRailColors =
        MaterialTheme.colorScheme.defaultWideWideNavigationRailColors.copy(
            containerColor = containerColor,
            contentColor = contentColor,
            modalContainerColor = modalContainerColor,
            modalScrimColor = modalScrimColor,
            modalContentColor = modalContentColor,
        )

    /**
     * Creates a [WideNavigationRailColors] with the provided colors according to the Material
     * specification.
     *
     * @param containerColor the color used for the background of a non-modal wide navigation rail.
     * @param contentColor the preferred color for content inside a wide navigation rail. Defaults
     *   to either the matching content color for [containerColor], or to the current
     *   [LocalContentColor] if [containerColor] is not a color from the theme
     * @param modalContainerColor the color used for the background of a modal wide navigation rail.
     * @param modalScrimColor the color used for the scrim overlay for background content of a modal
     *   wide navigation rail
     */
    @Deprecated(
        message = "Deprecated in favor of function with modalContentColor parameter",
        level = DeprecationLevel.HIDDEN,
    )
    @ExperimentalMaterial3ExpressiveApi
    @Composable
    fun colors(
        containerColor: Color = WideNavigationRailDefaults.containerColor,
        contentColor: Color = contentColorFor(containerColor),
        modalContainerColor: Color = NavigationRailExpandedTokens.ModalContainerColor.value,
        modalScrimColor: Color = ScrimTokens.ContainerColor.value.copy(ScrimTokens.ContainerOpacity),
    ): WideNavigationRailColors =
        MaterialTheme.colorScheme.defaultWideWideNavigationRailColors.copy(
            containerColor = containerColor,
            contentColor = contentColor,
            modalContainerColor = modalContainerColor,
            modalScrimColor = modalScrimColor,
            modalContentColor = contentColorFor(modalContainerColor),
        )

    /** Default container shape of a wide navigation rail. */
    @Deprecated(
        message = "Deprecated in favor of shape.",
        replaceWith = ReplaceWith("WideNavigationRailDefaults.shape"),
        level = DeprecationLevel.WARNING,
    )
    @ExperimentalMaterial3ExpressiveApi
    val containerShape: Shape
        @Composable get() = NavigationRailCollapsedTokens.ContainerShape.value

    @Deprecated(
        message = "Deprecated in favor of modalExpandedShape.",
        replaceWith = ReplaceWith("WideNavigationRailDefaults.modalExpandedShape"),
        level = DeprecationLevel.WARNING,
    )
    @ExperimentalMaterial3ExpressiveApi
    /** Default container shape of a modal wide navigation rail. */
    val modalContainerShape: Shape
        @Composable get() = NavigationRailExpandedTokens.ModalContainerShape.value

    private val containerColor: Color
        @Composable get() = NavigationRailCollapsedTokens.ContainerColor.value

    private val modalContainerColor: Color
        @Composable get() = NavigationRailExpandedTokens.ModalContainerColor.value

    private val ColorScheme.defaultWideWideNavigationRailColors: WideNavigationRailColors
        @Composable
        get() {
            return defaultWideWideNavigationRailColorsCached
                ?: WideNavigationRailColors(
                        containerColor = containerColor,
                        contentColor = contentColorFor(containerColor),
                        modalContainerColor = modalContainerColor,
                        modalContentColor = contentColorFor(modalContainerColor),
                        modalScrimColor =
                            ScrimTokens.ContainerColor.value.copy(ScrimTokens.ContainerOpacity),
                    )
                    .also { defaultWideWideNavigationRailColorsCached = it }
        }
}

/** Defaults used in [WideNavigationRailItem]. */
object WideNavigationRailItemDefaults {
    /**
     * The default icon position of a [WideNavigationRailItem] given whether the associated
     * [WideNavigationRail] is collapsed or expanded.
     */
    fun iconPositionFor(railExpanded: Boolean) =
        if (railExpanded) NavigationItemIconPosition.Start else NavigationItemIconPosition.Top

    /**
     * Creates a [NavigationItemColors] with the provided colors according to the Material
     * specification.
     */
    @Composable fun colors() = MaterialTheme.colorScheme.defaultWideNavigationRailItemColors

    /**
     * Creates a [NavigationItemColors] with the provided colors according to the Material
     * specification.
     *
     * @param selectedIconColor the color to use for the icon when the item is selected.
     * @param selectedTextColor the color to use for the text label when the item is selected.
     * @param selectedIndicatorColor the color to use for the indicator when the item is selected.
     * @param unselectedIconColor the color to use for the icon when the item is unselected.
     * @param unselectedTextColor the color to use for the text label when the item is unselected.
     * @param disabledIconColor the color to use for the icon when the item is disabled.
     * @param disabledTextColor the color to use for the text label when the item is disabled.
     * @return the resulting [NavigationItemColors] used for [WideNavigationRailItem]
     */
    @Composable
    fun colors(
        selectedIconColor: Color = NavigationRailColorTokens.ItemActiveIcon.value,
        selectedTextColor: Color = NavigationRailColorTokens.ItemActiveLabelText.value,
        selectedIndicatorColor: Color = NavigationRailColorTokens.ItemActiveIndicator.value,
        unselectedIconColor: Color = NavigationRailColorTokens.ItemInactiveIcon.value,
        unselectedTextColor: Color = NavigationRailColorTokens.ItemInactiveLabelText.value,
        disabledIconColor: Color = unselectedIconColor.copy(alpha = DisabledAlpha),
        disabledTextColor: Color = unselectedTextColor.copy(alpha = DisabledAlpha),
    ): NavigationItemColors =
        MaterialTheme.colorScheme.defaultWideNavigationRailItemColors.copy(
            selectedIconColor = selectedIconColor,
            selectedTextColor = selectedTextColor,
            selectedIndicatorColor = selectedIndicatorColor,
            unselectedIconColor = unselectedIconColor,
            unselectedTextColor = unselectedTextColor,
            disabledIconColor = disabledIconColor,
            disabledTextColor = disabledTextColor,
        )

    private val ColorScheme.defaultWideNavigationRailItemColors: NavigationItemColors
        get() {
            return defaultWideNavigationRailItemColorsCached
                ?: NavigationItemColors(
                        selectedIconColor = fromToken(NavigationRailColorTokens.ItemActiveIcon),
                        selectedTextColor =
                            fromToken(NavigationRailColorTokens.ItemActiveLabelText),
                        selectedIndicatorColor =
                            fromToken(NavigationRailColorTokens.ItemActiveIndicator),
                        unselectedIconColor = fromToken(NavigationRailColorTokens.ItemInactiveIcon),
                        unselectedTextColor =
                            fromToken(NavigationRailColorTokens.ItemInactiveLabelText),
                        disabledIconColor =
                            fromToken(NavigationRailColorTokens.ItemInactiveIcon)
                                .copy(alpha = DisabledAlpha),
                        disabledTextColor =
                            fromToken(NavigationRailColorTokens.ItemInactiveLabelText)
                                .copy(alpha = DisabledAlpha),
                    )
                    .also { defaultWideNavigationRailItemColorsCached = it }
        }
}

/** Default values for [ModalWideNavigationRail]. */
@Deprecated(
    message = "Deprecated in favor of default values in WideNavigationRailDefaults",
    replaceWith = ReplaceWith("WideNavigationRailDefaults"),
    level = DeprecationLevel.WARNING,
)
@Immutable
@ExperimentalMaterial3ExpressiveApi
object ModalWideNavigationRailDefaults {
    /** Properties used to customize the window behavior of a [ModalWideNavigationRail]. */
    @Deprecated(
        message =
            "Deprecated in favor of function with " +
                "WideNavigationRailDefaults.ModalExpandedProperties",
        replaceWith = ReplaceWith("WideNavigationRailDefaults.ModalExpandedProperties"),
        level = DeprecationLevel.WARNING,
    )
    @ExperimentalMaterial3ExpressiveApi
    val Properties: ModalWideNavigationRailProperties =
        createDefaultModalWideNavigationRailProperties()
}

internal expect fun createDefaultModalWideNavigationRailProperties():
    ModalWideNavigationRailProperties

/**
 * Properties used to customize the behavior of a [ModalWideNavigationRail].
 *
 * @param shouldDismissOnBackPress Whether the modal navigation rail can be dismissed by pressing
 *   the back button. If true, pressing the back button will call onDismissRequest.
 */
@Immutable
expect class ModalWideNavigationRailProperties(shouldDismissOnBackPress: Boolean = true) {
    val shouldDismissOnBackPress: Boolean
}

@Composable
internal expect fun ModalWideNavigationRailDialog(
    onDismissRequest: () -> Unit,
    properties: ModalWideNavigationRailProperties,
    onPredictiveBack: (Float) -> Unit,
    onPredictiveBackCancelled: () -> Unit,
    predictiveBackState: RailPredictiveBackState,
    content: @Composable () -> Unit,
)

@Composable
private fun ModalWideNavigationRailContent(
    expanded: Boolean,
    isStandaloneModal: Boolean,
    predictiveBackProgress: Animatable<Float, AnimationVector1D>,
    predictiveBackState: RailPredictiveBackState,
    settleToDismiss: suspend (velocity: Float) -> Unit,
    modifier: Modifier,
    railState: ModalWideNavigationRailState,
    colors: WideNavigationRailColors,
    shape: Shape,
    openModalRailMaxWidth: Dp,
    header: @Composable (() -> Unit)?,
    windowInsets: WindowInsets,
    gesturesEnabled: Boolean,
    arrangement: Arrangement.Vertical,
    content: @Composable () -> Unit,
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val railPaneTitle = getString(string = Strings.WideNavigationRailPaneTitle)

    Surface(
        shape = shape,
        color = colors.modalContainerColor,
        contentColor = colors.modalContentColor,
        modifier =
            modifier
                .widthIn(max = openModalRailMaxWidth)
                .fillMaxHeight()
                .semantics { paneTitle = railPaneTitle }
                .graphicsLayer {
                    val progress = predictiveBackProgress.value
                    if (progress <= 0f) {
                        return@graphicsLayer
                    }
                    val offset = railState.currentOffset
                    val width = size.width
                    if (!offset.isNaN() && !width.isNaN() && width != 0f) {
                        // Apply the predictive back animation.
                        scaleX =
                            calculatePredictiveBackScaleX(
                                progress,
                                predictiveBackState.swipeEdgeMatchesRail,
                            )
                        scaleY = calculatePredictiveBackScaleY(progress)
                        transformOrigin =
                            TransformOrigin(if (isRtl) 1f else 0f, PredictiveBackPivotFractionY)
                    }
                }
                .draggableAnchors(railState.anchoredDraggableState, Orientation.Horizontal) {
                    railSize,
                    _ ->
                    val width = railSize.width.toFloat()
                    val minValue =
                        if (isStandaloneModal) {
                            if (isRtl) width else -width
                        } else {
                            0f
                        }
                    val maxValue = 0f
                    return@draggableAnchors DraggableAnchors {
                        WideNavigationRailValue.Collapsed at minValue
                        WideNavigationRailValue.Expanded at maxValue
                    } to railState.targetValue
                }
                .draggable(
                    state = railState.anchoredDraggableState.draggableState,
                    orientation = Orientation.Horizontal,
                    enabled = gesturesEnabled,
                    startDragImmediately = railState.anchoredDraggableState.isAnimationRunning,
                    onDragStopped = { settleToDismiss(it) },
                ),
    ) {
        WideNavigationRailLayout(
            modifier =
                Modifier.graphicsLayer {
                    val progress = predictiveBackProgress.value
                    if (progress <= 0) {
                        return@graphicsLayer
                    }
                    // Preserve the original aspect ratio and alignment due to the predictive back
                    // animation.
                    val predictiveBackScaleX =
                        calculatePredictiveBackScaleX(
                            progress,
                            predictiveBackState.swipeEdgeMatchesRail,
                        )
                    val predictiveBackScaleY = calculatePredictiveBackScaleY(progress)
                    scaleX =
                        if (predictiveBackScaleX != 0f) predictiveBackScaleY / predictiveBackScaleX
                        else 1f
                    transformOrigin =
                        TransformOrigin(if (isRtl) 0f else 1f, PredictiveBackPivotFractionY)
                },
            expanded = expanded,
            shape = shape,
            colors = colors,
            header = header,
            windowInsets = windowInsets,
            arrangement = arrangement,
            isModal = true,
            content = content,
        )
    }
}

private fun GraphicsLayerScope.calculatePredictiveBackScaleX(
    progress: Float,
    swipeEdgeMatchesRail: Boolean,
): Float {
    val width = size.width
    return if (width.isNaN() || width == 0f) {
        1f
    } else {
        val scaleXDirection = if (swipeEdgeMatchesRail) 1f else -1f
        1f +
            (scaleXDirection *
                lerp(0f, min(PredictiveBackMaxScaleXDistance.toPx(), width), progress)) / width
    }
}

private fun GraphicsLayerScope.calculatePredictiveBackScaleY(progress: Float): Float {
    val height = size.height
    return if (height.isNaN() || height == 0f) {
        1f
    } else {
        1f - lerp(0f, min(PredictiveBackMaxScaleYDistance.toPx(), height), progress) / height
    }
}

@Composable
private fun Scrim(color: Color, onDismissRequest: suspend () -> Unit, visible: Boolean) {
    if (color.isSpecified) {
        val alpha by
            animateFloatAsState(
                targetValue = if (visible) 1f else 0f,
                // TODO: Load the motionScheme tokens from the component tokens file.
                animationSpec = MotionSchemeKeyTokens.DefaultEffects.value(),
            )
        var dismiss by remember { mutableStateOf(false) }
        val closeModalRail = getString(Strings.CloseRail)
        val dismissModalRail =
            if (visible) {
                Modifier.pointerInput(onDismissRequest) { detectTapGestures { dismiss = true } }
                    .semantics(mergeDescendants = true) {
                        contentDescription = closeModalRail
                        onClick {
                            dismiss = true
                            true
                        }
                    }
            } else {
                Modifier
            }
        Canvas(Modifier.fillMaxSize().then(dismissModalRail)) {
            drawRect(color = color, alpha = alpha.coerceIn(0f, 1f))
        }

        LaunchedEffect(dismiss) { if (dismiss) onDismissRequest() }
    }
}

/*@VisibleForTesting*/
internal val WNRItemNoLabelIndicatorPadding =
    (NavigationRailVerticalItemTokens.ActiveIndicatorWidth -
        NavigationRailBaselineItemTokens.IconSize) / 2

private val ItemHorizontalPadding = 20.dp
// Vertical padding between the contents of the wide navigation rail and its top/bottom.
private val WNRVerticalPadding = NavigationRailCollapsedTokens.TopSpace
// Padding at the bottom of the rail's header. This padding will only be added when the header is
// not null and the rail arrangement is Top.
private val WNRHeaderPadding: Dp = NavigationRailBaselineItemTokens.HeaderSpaceMinimum
private val CollapsedRailWidth = NavigationRailCollapsedTokens.ContainerWidth
private val ExpandedRailMinWidth = NavigationRailExpandedTokens.ContainerWidthMinimum
private val ExpandedRailMaxWidth = NavigationRailExpandedTokens.ContainerWidthMaximum
private val TopIconItemMinHeight = NavigationRailBaselineItemTokens.ContainerHeight
private val ItemTopIconIndicatorVerticalPadding =
    (NavigationRailVerticalItemTokens.ActiveIndicatorHeight -
        NavigationRailBaselineItemTokens.IconSize) / 2
private val ItemTopIconIndicatorHorizontalPadding =
    (NavigationRailVerticalItemTokens.ActiveIndicatorWidth -
        NavigationRailBaselineItemTokens.IconSize) / 2
private val ItemStartIconIndicatorVerticalPadding =
    (NavigationRailHorizontalItemTokens.ActiveIndicatorHeight -
        NavigationRailBaselineItemTokens.IconSize) / 2
private val PredictiveBackMaxScaleXDistance = 24.dp
private val PredictiveBackMaxScaleYDistance = 48.dp

private const val PredictiveBackPivotFractionY = 0.5f
private const val HeaderLayoutIdTag: String = "header"

/**
 * Interface that allows libraries to override the behavior of the [WideNavigationRail] component.
 *
 * To override this component, implement the member function of this interface, then provide the
 * implementation to [LocalWideNavigationRailOverride] in the Compose hierarchy.
 */
@ExperimentalMaterial3ComponentOverrideApi
interface WideNavigationRailOverride {
    /** Behavior function that is called by the [WideNavigationRail] component. */
    @Composable fun WideNavigationRailOverrideScope.WideNavigationRail()
}

/**
 * Parameters available to [WideNavigationRail].
 *
 * @param modifier the [Modifier] to be applied to this wide navigation rail
 * @param state the [WideNavigationRailState] of this wide navigation rail
 * @param shape defines the shape of this wide navigation rail's container.
 * @param colors [WideNavigationRailColors] that will be used to resolve the colors used for this
 *   wide navigation rail. See [WideNavigationRailDefaults.colors]
 * @param header optional header that may hold a [FloatingActionButton] or a logo
 * @param windowInsets a window insets of the wide navigation rail
 * @param arrangement the [Arrangement.Vertical] of this wide navigation rail for its content. Note
 *   that if there's a header present, the items will be arranged on the remaining space below it,
 *   except for the center arrangement which considers the entire height of the container
 * @param content the content of this wide navigation rail, typically [WideNavigationRailItem]s
 */
@ExperimentalMaterial3ComponentOverrideApi
class WideNavigationRailOverrideScope
internal constructor(
    val modifier: Modifier,
    val state: WideNavigationRailState,
    val shape: Shape,
    val colors: WideNavigationRailColors,
    val header: @Composable (() -> Unit)?,
    val windowInsets: WindowInsets,
    val arrangement: Arrangement.Vertical,
    val content: @Composable () -> Unit,
)

/** CompositionLocal containing the currently-selected [WideNavigationRailOverride]. */
@ExperimentalMaterial3ComponentOverrideApi
val LocalWideNavigationRailOverride: ProvidableCompositionLocal<WideNavigationRailOverride> =
    compositionLocalOf {
        DefaultWideNavigationRailOverride
    }

/**
 * Interface that allows libraries to override the behavior of the [ModalWideNavigationRail]
 * component.
 *
 * To override this component, implement the member function of this interface, then provide the
 * implementation to [LocalModalWideNavigationRailOverride] in the Compose hierarchy.
 */
@ExperimentalMaterial3ComponentOverrideApi
interface ModalWideNavigationRailOverride {
    /** Behavior function that is called by the [WideNavigationRail] component. */
    @Composable fun ModalWideNavigationRailOverrideScope.ModalWideNavigationRail()
}

/**
 * Parameters available to [ModalWideNavigationRail].
 *
 * @param modifier the [Modifier] to be applied to this wide navigation rail
 * @param state the [WideNavigationRailState] of this wide navigation rail
 * @param shouldHideOnCollapse whether this wide navigation rail should slide offscreen when it
 *   collapses and be hidden, or stay on screen as a collapsed wide navigation rail (default)
 * @param collapsedShape the shape of this wide navigation rail's container when it's collapsed
 * @param expandedShape the shape of this wide navigation rail's container when it's expanded
 * @param colors [WideNavigationRailColors] that will be used to resolve the colors used for this
 *   wide navigation rail. See [WideNavigationRailDefaults.colors]
 * @param header optional header that may hold a [FloatingActionButton] or a logo
 * @param expandedHeaderTopPadding the padding to be applied to the top of the rail. It's usually
 *   needed in order to align the content of the rail between the collapsed and expanded animation
 * @param windowInsets a window insets of the wide navigation rail
 * @param arrangement the [Arrangement.Vertical] of this wide navigation rail
 * @param expandedProperties [ModalWideNavigationRailProperties] for further customization of the
 *   expanded modal wide navigation rail's window behavior
 * @param content the content of this modal wide navigation rail, usually [WideNavigationRailItem]s
 */
@ExperimentalMaterial3ComponentOverrideApi
class ModalWideNavigationRailOverrideScope
internal constructor(
    val modifier: Modifier,
    val state: WideNavigationRailState,
    @get:Suppress("GetterSetterNames") val shouldHideOnCollapse: Boolean,
    val collapsedShape: Shape,
    val expandedShape: Shape,
    val colors: WideNavigationRailColors,
    val header: @Composable (() -> Unit)?,
    val expandedHeaderTopPadding: Dp,
    val windowInsets: WindowInsets,
    val arrangement: Arrangement.Vertical,
    val expandedProperties: ModalWideNavigationRailProperties,
    val content: @Composable () -> Unit,
)

/** CompositionLocal containing the currently-selected [ModalWideNavigationRailOverride]. */
@ExperimentalMaterial3ComponentOverrideApi
val LocalModalWideNavigationRailOverride:
    ProvidableCompositionLocal<ModalWideNavigationRailOverride> =
    compositionLocalOf {
        DefaultModalWideNavigationRailOverride
    }
