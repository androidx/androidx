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
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
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
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastSumBy
import androidx.compose.ui.util.lerp
import kotlin.jvm.JvmInline
import kotlin.math.min
import kotlinx.coroutines.launch

/**
 * Material design wide navigation rail.
 *
 * Wide navigation rails provide access to primary destinations in apps when using tablet and
 * desktop screens.
 *
 * The wide navigation rail should be used to display multiple [WideNavigationRailItem]s, each
 * representing a singular app destination, and, optionally, a header containing a menu button, a
 * [FloatingActionButton], and/or a logo. Each destination is typically represented by an icon and a
 * text label.
 *
 * The [WideNavigationRail] is collapsed by default, but it also supports being expanded via the
 * value of [expanded]. When collapsed, the rail should display three to seven navigation items. A
 * simple example looks like:
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
 * For modal variations of the wide navigation rail, see [ModalWideNavigationRail] and
 * [DismissibleModalWideNavigationRail].
 *
 * Finally, the [WideNavigationRail] supports setting a [WideNavigationRailArrangement] for the
 * items, so that the items can be grouped at the top (the default), at the middle, or at the bottom
 * of the rail. The header will always be at the top.
 *
 * See [WideNavigationRailItem] for configuration specific to each item, and not the overall
 * [WideNavigationRail] component.
 *
 * @param modifier the [Modifier] to be applied to this wide navigation rail
 * @param expanded whether this wide navigation rail is expanded or collapsed (default).
 * @param shape defines the shape of this wide navigation rail's container.
 * @param colors [WideNavigationRailColors] that will be used to resolve the colors used for this
 *   wide navigation rail. See [WideNavigationRailDefaults.colors]
 * @param header optional header that may hold a [FloatingActionButton] or a logo
 * @param windowInsets a window insets of the wide navigation rail
 * @param arrangement the [WideNavigationRailArrangement] of this wide navigation rail
 * @param content the content of this wide navigation rail, typically [WideNavigationRailItem]s
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun WideNavigationRail(
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    shape: Shape = WideNavigationRailDefaults.containerShape,
    colors: WideNavigationRailColors = WideNavigationRailDefaults.colors(),
    header: @Composable (() -> Unit)? = null,
    windowInsets: WindowInsets = WideNavigationRailDefaults.windowInsets,
    arrangement: WideNavigationRailArrangement = WideNavigationRailDefaults.Arrangement,
    content: @Composable () -> Unit
) {
    WideNavigationRailLayout(
        modifier = modifier,
        isModal = false,
        expanded = expanded,
        colors = colors,
        shape = shape,
        header = header,
        windowInsets = windowInsets,
        arrangement = arrangement,
        content = content
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WideNavigationRailLayout(
    modifier: Modifier,
    isModal: Boolean,
    expanded: Boolean,
    colors: WideNavigationRailColors,
    shape: Shape,
    header: @Composable (() -> Unit)?,
    windowInsets: WindowInsets,
    arrangement: WideNavigationRailArrangement,
    content: @Composable () -> Unit
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
            animationSpec = if (!isModal) animationSpec else modalAnimationSpec
        )
    val widthFullRange by
        animateDpAsState(
            targetValue = if (!expanded) CollapsedRailWidth else ExpandedRailMaxWidth,
            animationSpec = if (!isModal) animationSpec else modalAnimationSpec
        )
    val itemVerticalSpacedBy by
        animateDpAsState(
            targetValue = if (!expanded) NavigationRailCollapsedTokens.ItemVerticalSpace else 0.dp,
            animationSpec = animationSpec
        )
    val itemMarginStart by
        animateDpAsState(
            targetValue = if (!expanded) 0.dp else ExpandedRailHorizontalItemPadding,
            animationSpec = animationSpec
        )

    Surface(
        color = if (!isModal) colors.containerColor else colors.modalContainerColor,
        contentColor = colors.contentColor,
        shape = shape,
        modifier = modifier,
    ) {
        Layout(
            modifier =
                Modifier.fillMaxHeight()
                    .windowInsetsPadding(windowInsets)
                    .widthIn(max = ExpandedRailMaxWidth)
                    .padding(top = WNRVerticalPadding)
                    .selectableGroup(),
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
                        constraints: Constraints
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
                                                    minWidth =
                                                        min(
                                                            ItemMinWidth.roundToPx(),
                                                            itemMaxWidthConstraint
                                                        ),
                                                    minHeight =
                                                        if (!expanded)
                                                            TopIconItemMinHeight.roundToPx()
                                                        else minimumA11ySize.roundToPx(),
                                                    maxWidth = itemMaxWidthConstraint,
                                                    maxHeight = looseConstraints.maxHeight,
                                                )
                                            )
                                    )
                                val maxItemWidth = measuredItem.measuredWidth
                                if (expanded && expandedItemMaxWidth < maxItemWidth) {
                                    expandedItemMaxWidth =
                                        maxItemWidth +
                                            (ExpandedRailHorizontalItemPadding * 2).roundToPx()
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
                                                currentWidth.coerceAtLeast(actualMinWidth)
                                        )
                            }
                        }
                        currentWidth = width

                        return layout(width, height) {
                            var y = 0
                            var headerHeight = 0
                            if (headerPlaceable != null && headerPlaceable.height > 0) {
                                headerPlaceable.placeRelative(0, y)
                                headerHeight = headerPlaceable.height
                                if (arrangement == WideNavigationRailArrangement.Top) {
                                    y += headerHeight + WNRHeaderPadding.roundToPx()
                                }
                            }

                            val itemsHeight = itemsPlaceables?.fastSumBy { it.height } ?: 0
                            val verticalPadding = itemVerticalSpacedBy.roundToPx()
                            if (arrangement == WideNavigationRailArrangement.Center) {
                                y =
                                    (height -
                                        WNRVerticalPadding.roundToPx() -
                                        (itemsHeight + (itemsCount - 1) * verticalPadding)) / 2
                                y = y.coerceAtLeast(headerHeight)
                            } else if (arrangement == WideNavigationRailArrangement.Bottom) {
                                y =
                                    height -
                                        WNRVerticalPadding.roundToPx() -
                                        (itemsHeight + (itemsCount - 1) * verticalPadding)
                                y = y.coerceAtLeast(headerHeight)
                            }
                            itemsPlaceables?.fastForEach { item ->
                                val x = itemMarginStart.roundToPx()
                                item.placeRelative(x, y)
                                y += item.height + verticalPadding
                            }
                        }
                    }
                }
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
 * [expanded], the modal wide navigation rail blocks interaction with the rest of an app’s content
 * with a scrim. It is elevated above the app’s UI and doesn't affect the screen’s layout grid. That
 * can be achieved like so:
 *
 * @sample androidx.compose.material3.samples.ModalWideNavigationRailSample
 *
 * For a dismissible modal wide rail, that enters from offscreen instead of expanding from the
 * collapsed rail, see [DismissibleModalWideNavigationRail].
 *
 * See [WideNavigationRailItem] for configuration specific to each item, and not the overall
 * [ModalWideNavigationRail] component.
 *
 * @param scrimOnClick executes when the scrim is clicked. Usually it should be a function that
 *   instructs the rail to collapse
 * @param modifier the [Modifier] to be applied to this wide navigation rail
 * @param expanded whether this wide navigation rail is expanded or collapsed (default).
 * @param collapsedShape the shape of this wide navigation rail's container when it's collapsed.
 * @param expandedShape the shape of this wide navigation rail's container when it's [expanded]
 * @param colors [WideNavigationRailColors] that will be used to resolve the colors used for this
 *   wide navigation rail. See [WideNavigationRailDefaults.colors]
 * @param header optional header that may hold a [FloatingActionButton] or a logo
 * @param expandedHeaderTopPadding the padding to be applied to the top of the rail. It's usually
 *   needed in order to align the content of the rail between the collapsed and expanded animation
 * @param windowInsets a window insets of the wide navigation rail
 * @param arrangement the [WideNavigationRailArrangement] of this wide navigation rail
 * @param expandedProperties [ModalWideNavigationRailProperties] for further customization of the
 *   expanded modal wide navigation rail's window behavior
 * @param content the content of this modal wide navigation rail, usually [WideNavigationRailItem]s
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun ModalWideNavigationRail(
    scrimOnClick: (() -> Unit),
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    collapsedShape: Shape = WideNavigationRailDefaults.containerShape,
    expandedShape: Shape = WideNavigationRailDefaults.modalContainerShape,
    colors: WideNavigationRailColors = WideNavigationRailDefaults.colors(),
    header: @Composable (() -> Unit)? = null,
    expandedHeaderTopPadding: Dp = 0.dp,
    windowInsets: WindowInsets = WideNavigationRailDefaults.windowInsets,
    arrangement: WideNavigationRailArrangement = WideNavigationRailDefaults.Arrangement,
    expandedProperties: ModalWideNavigationRailProperties =
        DismissibleModalWideNavigationRailDefaults.Properties,
    content: @Composable () -> Unit
) {
    val rememberContent = remember(content) { movableContentOf(content) }
    val railState = rememberDismissibleModalWideNavigationRailState()
    val positionProgress =
        animateFloatAsState(
            targetValue = if (!expanded) 0f else 1f,
            // TODO: Load the motionScheme tokens from the component tokens file.
            animationSpec = MotionSchemeKeyTokens.DefaultEffects.value()
        )
    val isCollapsed by remember { derivedStateOf { positionProgress.value == 0f } }
    val modalExpanded by remember { derivedStateOf { positionProgress.value >= 0.3f } }
    val onDismissRequest: suspend () -> Unit = { scrimOnClick() }

    // Display a non modal rail when collapsed.
    if (isCollapsed) {
        WideNavigationRailLayout(
            modifier = modifier,
            isModal = false,
            expanded = false,
            colors = colors,
            shape = collapsedShape,
            header = header,
            windowInsets = windowInsets,
            arrangement = arrangement,
            content = rememberContent
        )
    }
    // Display a modal container when expanded.
    if (!isCollapsed) {
        // Have a spacer the size of the collapsed rail so that screen content doesn't shift.
        Box(modifier = Modifier.background(color = colors.containerColor, shape = collapsedShape)) {
            Spacer(modifier = modifier.widthIn(min = CollapsedRailWidth).fillMaxHeight())
        }
        val scope = rememberCoroutineScope()
        val predictiveBackProgress = remember { Animatable(initialValue = 0f) }
        val predictiveBackState = remember { RailPredictiveBackState() }

        ModalWideNavigationRailDialog(
            properties = expandedProperties,
            onDismissRequest = { scope.launch { onDismissRequest() } },
            onPredictiveBack = { backEvent ->
                scope.launch { predictiveBackProgress.snapTo(backEvent) }
            },
            onPredictiveBackCancelled = { scope.launch { predictiveBackProgress.animateTo(0f) } },
            predictiveBackState = predictiveBackState
        ) {
            Box(modifier = Modifier.fillMaxSize().imePadding()) {
                Scrim(
                    color = colors.modalScrimColor,
                    onDismissRequest = onDismissRequest,
                    visible = modalExpanded
                )
                ModalWideNavigationRailContent(
                    expanded = modalExpanded,
                    isStandaloneModal = false,
                    predictiveBackProgress = predictiveBackProgress,
                    predictiveBackState = predictiveBackState,
                    settleToDismiss = {},
                    modifier = modifier,
                    railState = railState,
                    colors = colors,
                    shape = expandedShape,
                    openModalRailMaxWidth = ExpandedRailMaxWidth,
                    header = {
                        Column {
                            Spacer(Modifier.height(expandedHeaderTopPadding))
                            header?.invoke()
                        }
                    },
                    windowInsets = windowInsets,
                    gesturesEnabled = false,
                    arrangement = arrangement,
                    content = rememberContent
                )
            }
        }
    }

    LaunchedEffect(isCollapsed) {
        if (isCollapsed) {
            railState.close()
        } else {
            railState.open()
        }
    }
}

/**
 * A dismissible modal wide navigation rail.
 *
 * Wide navigation rails provide access to primary destinations in apps when using tablet and
 * desktop screens.
 *
 * The dismissible modal wide navigation rail blocks interaction with the rest of an app’s content
 * with a scrim when expanded. It is elevated above most of the app’s UI and doesn't affect the
 * screen’s layout grid. When collapsed, the rail is hidden.
 *
 * The dismissible modal wide navigation rai should be used to display at least three
 * [WideNavigationRailItem]s with their icon position set to [NavigationItemIconPosition.Start],
 * each representing a singular app destination, and, optionally, a header containing a menu button,
 * a [FloatingActionButton], and/or a logo. Each destination is typically represented by an icon and
 * a text label. A simple example looks like:
 *
 * @sample androidx.compose.material3.samples.DismissibleModalWideNavigationRailSample
 *
 * For a modal rail that expands from a collapsed rail, instead of entering from offscreen, see
 * [ModalWideNavigationRail].
 *
 * See [WideNavigationRailItem] for configuration specific to each item, and not the overall
 * [DismissibleModalWideNavigationRail] component.
 *
 * @param onDismissRequest executes when the user closes the rail, after it animates to
 *   [DismissibleModalWideNavigationRailValue.Closed]
 * @param modifier the [Modifier] to be applied to this dismissible modal wide navigation rail
 * @param railState state of the dismissible modal wide navigation rail
 * @param shape defines the shape of this dismissible modal wide navigation rail's container
 * @param colors [WideNavigationRailColors] that will be used to resolve the colors used for this
 *   dismissible modal wide navigation rail. See [WideNavigationRailDefaults.colors]
 * @param header optional header that may hold a [FloatingActionButton] or a logo
 * @param windowInsets a window insets of this dismissible modal wide navigation rail
 * @param arrangement the [WideNavigationRailArrangement] of this dismissible modal wide navigation
 *   rail
 * @param gesturesEnabled whether the dismissible modal wide navigation rail can be interacted by
 *   gestures
 * @param properties [ModalWideNavigationRailProperties] for further customization of this modal
 *   expanded navigation rail's window behavior
 * @param content the content of this dismissible modal wide navigation rail, typically
 *   [WideNavigationRailItem]s with [NavigationItemIconPosition.Start] icon position
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun DismissibleModalWideNavigationRail(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    railState: DismissibleModalWideNavigationRailState =
        rememberDismissibleModalWideNavigationRailState(),
    shape: Shape = WideNavigationRailDefaults.modalContainerShape,
    colors: WideNavigationRailColors = WideNavigationRailDefaults.colors(),
    header: @Composable (() -> Unit)? = null,
    windowInsets: WindowInsets = WideNavigationRailDefaults.windowInsets,
    arrangement: WideNavigationRailArrangement = WideNavigationRailDefaults.Arrangement,
    gesturesEnabled: Boolean = true,
    properties: ModalWideNavigationRailProperties =
        DismissibleModalWideNavigationRailDefaults.Properties,
    content: @Composable () -> Unit
) {
    val animateToDismiss: suspend () -> Unit = {
        if (
            railState.anchoredDraggableState.confirmValueChange(
                DismissibleModalWideNavigationRailValue.Closed
            )
        ) {
            railState.close()
            if (!railState.isOpen) onDismissRequest()
        }
    }
    val settleToDismiss: suspend (velocity: Float) -> Unit = {
        railState.settle(it)
        if (!railState.isOpen) onDismissRequest()
    }
    val scope = rememberCoroutineScope()
    val predictiveBackProgress = remember { Animatable(initialValue = 0f) }
    val predictiveBackState = remember { RailPredictiveBackState() }

    ModalWideNavigationRailDialog(
        properties = properties,
        onDismissRequest = { scope.launch { animateToDismiss() } },
        onPredictiveBack = { backEvent ->
            scope.launch { predictiveBackProgress.snapTo(backEvent) }
        },
        onPredictiveBackCancelled = { scope.launch { predictiveBackProgress.animateTo(0f) } },
        predictiveBackState = predictiveBackState
    ) {
        Box(modifier = Modifier.fillMaxSize().imePadding()) {
            Scrim(
                color = colors.modalScrimColor,
                onDismissRequest = animateToDismiss,
                visible = railState.targetValue != DismissibleModalWideNavigationRailValue.Closed
            )
            ModalWideNavigationRailContent(
                expanded = true,
                isStandaloneModal = true,
                predictiveBackProgress = predictiveBackProgress,
                predictiveBackState = predictiveBackState,
                settleToDismiss = settleToDismiss,
                modifier = modifier,
                railState = railState,
                colors = colors,
                shape = shape,
                openModalRailMaxWidth = ExpandedRailMaxWidth,
                header = header,
                windowInsets = windowInsets,
                gesturesEnabled = gesturesEnabled,
                arrangement = arrangement,
                content = content
            )
        }
    }

    LaunchedEffect(railState) { railState.open() }
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
 * value of the associated [WideNavigationRail]. By default, it'll use the [railExpanded] to follow
 * the configuration described above.
 *
 * @param selected whether this item is selected
 * @param onClick called when this item is clicked
 * @param icon icon for this item, typically an [Icon]
 * @param label text label for this item
 * @param modifier the [Modifier] to be applied to this item
 * @param enabled controls the enabled state of this item. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param badge optional badge to show on this item, typically a [Badge]
 * @param railExpanded whether the associated [WideNavigationRail] is expanded or collapsed
 * @param iconPosition the [NavigationItemIconPosition] for the icon
 * @param colors [NavigationItemColors] that will be used to resolve the colors used for this item
 *   in different states. See [WideNavigationRailItemDefaults.colors]
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this item. You can use this to change the item's appearance or
 *   preview the item in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun WideNavigationRailItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    badge: (@Composable () -> Unit)? = null,
    railExpanded: Boolean = false,
    iconPosition: NavigationItemIconPosition =
        WideNavigationRailItemDefaults.iconPositionFor(railExpanded),
    colors: NavigationItemColors = WideNavigationRailItemDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }

    if (label != null) {
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
            topIconIndicatorToLabelVerticalPadding =
                NavigationRailVerticalItemTokens.IconLabelSpace,
            startIconIndicatorHorizontalPadding =
                NavigationRailHorizontalItemTokens.FullWidthLeadingSpace,
            startIconIndicatorVerticalPadding = ItemStartIconIndicatorVerticalPadding,
            startIconToLabelHorizontalPadding = NavigationRailHorizontalItemTokens.IconLabelSpace,
            startIconItemPadding = ExpandedRailHorizontalItemPadding,
            colors = colors,
            modifier = modifier,
            enabled = enabled,
            label = label,
            badge = badge,
            iconPosition = iconPosition,
            interactionSource = interactionSource,
        )
    } else {
        // If no label, default to circular indicator for the item.
        NavigationItem(
            selected = selected,
            onClick = onClick,
            icon = icon,
            labelTextStyle = NavigationRailVerticalItemTokens.LabelTextFont.value,
            indicatorShape = NavigationRailBaselineItemTokens.ActiveIndicatorShape.value,
            indicatorWidth = NavigationRailVerticalItemTokens.ActiveIndicatorWidth,
            indicatorHorizontalPadding = WNRItemNoLabelIndicatorPadding,
            indicatorVerticalPadding = WNRItemNoLabelIndicatorPadding,
            indicatorToLabelVerticalPadding = 0.dp,
            startIconToLabelHorizontalPadding = 0.dp,
            topIconItemVerticalPadding = 0.dp,
            colors = colors,
            modifier = modifier,
            enabled = enabled,
            label = label,
            badge = badge,
            iconPosition = iconPosition,
            interactionSource = interactionSource,
        )
    }
}

/** Class that describes the different supported item arrangements of the [WideNavigationRail]. */
@ExperimentalMaterial3ExpressiveApi
@JvmInline
value class WideNavigationRailArrangement private constructor(private val value: Int) {
    companion object {
        /* The items are grouped at the top on the wide navigation Rail. */
        val Top = WideNavigationRailArrangement(0)

        /* The items are centered on the wide navigation Rail. */
        val Center = WideNavigationRailArrangement(1)

        /* The items are grouped at the bottom on the wide navigation Rail. */
        val Bottom = WideNavigationRailArrangement(2)
    }

    override fun toString() =
        when (this) {
            Top -> "Top"
            Center -> "Center"
            Bottom -> "Bottom"
            else -> "Unknown"
        }
}

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
 */
@Immutable
class WideNavigationRailColors(
    val containerColor: Color,
    val contentColor: Color,
    val modalContainerColor: Color,
    val modalScrimColor: Color,
) {
    /**
     * Returns a copy of this NavigationRailColors, optionally overriding some of the values. This
     * uses the Color.Unspecified to mean “use the value from the source”.
     */
    fun copy(
        containerColor: Color = this.containerColor,
        contentColor: Color = this.contentColor,
        modalContainerColor: Color = this.modalContainerColor,
        modalScrimColor: Color = this.modalScrimColor,
    ) =
        WideNavigationRailColors(
            containerColor = containerColor.takeOrElse { this.containerColor },
            contentColor = contentColor.takeOrElse { this.contentColor },
            modalContainerColor = modalContainerColor.takeOrElse { this.modalContainerColor },
            modalScrimColor = modalScrimColor.takeOrElse { this.modalScrimColor },
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

        return result
    }
}

/** Defaults used in [WideNavigationRail]. */
@ExperimentalMaterial3ExpressiveApi
object WideNavigationRailDefaults {
    /** Default container shape of a wide navigation rail. */
    val containerShape: Shape
        @Composable get() = NavigationRailCollapsedTokens.ContainerShape.value

    /** Default container shape of a modal wide navigation rail. */
    val modalContainerShape: Shape
        @Composable get() = NavigationRailExpandedTokens.ModalContainerShape.value

    /** Default arrangement for a wide navigation rail. */
    val Arrangement: WideNavigationRailArrangement
        get() = WideNavigationRailArrangement.Top

    /** Default window insets for a wide navigation rail. */
    val windowInsets: WindowInsets
        @Composable
        get() =
            WindowInsets.systemBarsForVisualComponents.only(
                WindowInsetsSides.Vertical + WindowInsetsSides.Start
            )

    /**
     * Creates a [WideNavigationRailColors] with the provided colors according to the Material
     * specification.
     */
    @Composable fun colors() = MaterialTheme.colorScheme.defaultWideWideNavigationRailColors

    private val containerColor: Color
        @Composable get() = NavigationRailCollapsedTokens.ContainerColor.value

    private val ColorScheme.defaultWideWideNavigationRailColors: WideNavigationRailColors
        @Composable
        get() {
            return defaultWideWideNavigationRailColorsCached
                ?: WideNavigationRailColors(
                        containerColor = containerColor,
                        contentColor = contentColorFor(containerColor),
                        modalContainerColor =
                            fromToken(NavigationRailExpandedTokens.ModalContainerColor),
                        modalScrimColor =
                            ScrimTokens.ContainerColor.value.copy(ScrimTokens.ContainerOpacity)
                    )
                    .also { defaultWideWideNavigationRailColorsCached = it }
        }
}

/** Defaults used in [WideNavigationRailItem]. */
@ExperimentalMaterial3ExpressiveApi
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

/** Default values for [DismissibleModalWideNavigationRail] */
@Immutable
@ExperimentalMaterial3ExpressiveApi
expect object DismissibleModalWideNavigationRailDefaults {

    /**
     * Properties used to customize the behavior of a [ModalWideNavigationRail] or of a
     * [DismissibleModalWideNavigationRail].
     */
    val Properties: ModalWideNavigationRailProperties
}

@Immutable
@ExperimentalMaterial3ExpressiveApi
expect class ModalWideNavigationRailProperties(
    shouldDismissOnBackPress: Boolean = true,
) {
    val shouldDismissOnBackPress: Boolean
}

@ExperimentalMaterial3ExpressiveApi
@Composable
internal expect fun ModalWideNavigationRailDialog(
    onDismissRequest: () -> Unit,
    properties: ModalWideNavigationRailProperties,
    onPredictiveBack: (Float) -> Unit,
    onPredictiveBackCancelled: () -> Unit,
    predictiveBackState: RailPredictiveBackState,
    content: @Composable () -> Unit
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ModalWideNavigationRailContent(
    expanded: Boolean,
    isStandaloneModal: Boolean,
    predictiveBackProgress: Animatable<Float, AnimationVector1D>,
    predictiveBackState: RailPredictiveBackState,
    settleToDismiss: suspend (velocity: Float) -> Unit,
    modifier: Modifier,
    railState: DismissibleModalWideNavigationRailState,
    colors: WideNavigationRailColors,
    shape: Shape,
    openModalRailMaxWidth: Dp,
    header: @Composable (() -> Unit)?,
    windowInsets: WindowInsets,
    gesturesEnabled: Boolean,
    arrangement: WideNavigationRailArrangement,
    content: @Composable () -> Unit
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val railPaneTitle = getString(string = Strings.WideNavigationRailPaneTitle)

    Surface(
        shape = shape,
        color = colors.modalContainerColor,
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
                                predictiveBackState.swipeEdgeMatchesRail
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
                        DismissibleModalWideNavigationRailValue.Closed at minValue
                        DismissibleModalWideNavigationRailValue.Open at maxValue
                    } to railState.targetValue
                }
                .draggable(
                    state = railState.anchoredDraggableState.draggableState,
                    orientation = Orientation.Horizontal,
                    enabled = gesturesEnabled,
                    startDragImmediately = railState.anchoredDraggableState.isAnimationRunning,
                    onDragStopped = { settleToDismiss(it) },
                )
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
                            predictiveBackState.swipeEdgeMatchesRail
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
            content = content
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

private fun GraphicsLayerScope.calculatePredictiveBackScaleY(
    progress: Float,
): Float {
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
                animationSpec = MotionSchemeKeyTokens.DefaultEffects.value()
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

private val ExpandedRailHorizontalItemPadding = 20.dp
// Vertical padding between the contents of the wide navigation rail and its top/bottom.
private val WNRVerticalPadding = NavigationRailCollapsedTokens.TopSpace
// Padding at the bottom of the rail's header. This padding will only be added when the header is
// not null and the rail arrangement is Top.
private val WNRHeaderPadding: Dp = NavigationRailBaselineItemTokens.HeaderSpaceMinimum
private val CollapsedRailWidth = NavigationRailCollapsedTokens.ContainerWidth
private val ExpandedRailMinWidth = NavigationRailExpandedTokens.ContainerWidthMinimum
private val ExpandedRailMaxWidth = NavigationRailExpandedTokens.ContainerWidthMaximum
private val ItemMinWidth = NavigationRailCollapsedTokens.ContainerWidth
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
