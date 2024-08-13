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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.tokens.ColorSchemeKeyTokens
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.material3.tokens.ScrimTokens
import androidx.compose.material3.tokens.ShapeKeyTokens
import androidx.compose.material3.tokens.TypographyKeyTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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
 * Finally, the [WideNavigationRail] also supports automatically animating between the collapsed and
 * expanded values. That can be done like so:
 *
 * @sample androidx.compose.material3.samples.WideNavigationRailResponsiveSample
 *
 * The [WideNavigationRail] supports setting an [WideNavigationRailArrangement] for the items, so
 * that the items can be grouped at the top (the default), at the middle, or at the bottom of the
 * rail. The header will always be at the top.
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
 *
 * TODO: Implement modal expanded option and add relevant params.
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
    val minWidth by
        animateDpAsState(
            targetValue = if (!expanded) CollapsedRailWidth else ExpandedRailMinWidth,
            animationSpec = animationSpec
        )
    val widthFullRange by
        animateDpAsState(
            targetValue = if (!expanded) CollapsedRailWidth else ExpandedRailMaxWidth,
            animationSpec = animationSpec
        )
    val itemVerticalSpacedBy by
        animateDpAsState(
            targetValue = if (!expanded) VerticalPaddingBetweenTopIconItems else 0.dp,
            animationSpec = animationSpec
        )
    val itemMarginStart by
        animateDpAsState(
            targetValue = if (!expanded) 0.dp else ExpandedRailHorizontalItemPadding,
            animationSpec = animationSpec
        )

    Surface(
        color = if (!isModal) colors.containerColor else colors.expandedModalContainerColor,
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
                                                            WNRTopIconItemMinHeight.roundToPx()
                                                        else minimumA11ySize.roundToPx(),
                                                    maxWidth = itemMaxWidthConstraint,
                                                    maxHeight = looseConstraints.maxHeight,
                                                )
                                            )
                                    )
                                val maxIntrinsicWidth = it.maxIntrinsicWidth(constraintsOffset)
                                if (expanded && expandedItemMaxWidth < maxIntrinsicWidth) {
                                    expandedItemMaxWidth =
                                        maxIntrinsicWidth +
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
                                            maximumValue = currentWidth
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
 * A standalone modal expanded wide navigation rail.
 *
 * Wide navigation rails provide access to primary destinations in apps when using tablet and
 * desktop screens.
 *
 * The modal expanded rail blocks interaction with the rest of an app’s content with a scrim. It is
 * elevated above most of the app’s UI and doesn't affect the screen’s layout grid.
 *
 * The modal expanded wide navigation rail should be used to display at least three
 * [WideNavigationRailItem]s with their icon position set to [NavigationItemIconPosition.Start],
 * each representing a singular app destination, and, optionally, a header containing a menu button,
 * a [FloatingActionButton], and/or a logo. Each destination is typically represented by an icon and
 * a text label. A simple example looks like:
 *
 * @sample androidx.compose.material3.samples.ModalExpandedNavigationRailSample
 *
 * See [WideNavigationRailItem] for configuration specific to each item, and not the overall
 * [ModalExpandedNavigationRail] component.
 *
 * @param onDismissRequest Executes when the user rail closes, after it animates to
 *   [ModalExpandedNavigationRailValue.Closed]
 * @param modifier the [Modifier] to be applied to this modal expanded navigation rail
 * @param railState state of the modal expanded navigation rail
 * @param shape defines the shape of this modal expanded navigation rail's container
 * @param colors [WideNavigationRailColors] that will be used to resolve the colors used for this
 *   modal expanded navigation rail. See [WideNavigationRailDefaults.colors]
 * @param header optional header that may hold a [FloatingActionButton] or a logo
 * @param windowInsets a window insets of this modal expanded navigation rail
 * @param arrangement the [WideNavigationRailArrangement] of this modal expanded navigation rail
 * @param gesturesEnabled whether the modal expanded navigation rail can be interacted by gestures
 * @param properties [ModalExpandedNavigationRailProperties] for further customization of this modal
 *   expanded navigation rail's window behavior
 * @param content the content of this modal expanded navigation rail, typically
 *   [WideNavigationRailItem]s with [NavigationItemIconPosition.Start] icon position
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun ModalExpandedNavigationRail(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    railState: ModalExpandedNavigationRailState = rememberModalExpandedNavigationRailState(),
    shape: Shape = WideNavigationRailDefaults.modalContainerShape,
    colors: WideNavigationRailColors = WideNavigationRailDefaults.colors(),
    header: @Composable (() -> Unit)? = null,
    windowInsets: WindowInsets = WideNavigationRailDefaults.windowInsets,
    arrangement: WideNavigationRailArrangement = WideNavigationRailDefaults.Arrangement,
    gesturesEnabled: Boolean = true,
    properties: ModalExpandedNavigationRailProperties =
        ModalExpandedNavigationRailDefaults.Properties,
    content: @Composable () -> Unit
) {
    val animateToDismiss: suspend () -> Unit = {
        if (
            railState.anchoredDraggableState.confirmValueChange(
                ModalExpandedNavigationRailValue.Closed
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

    ModalWideNavigationRailDialog(
        properties = properties,
        // TODO: Implement predictive back behavior.
        onDismissRequest = { scope.launch { animateToDismiss() } },
        onPredictiveBack = { backEvent ->
            scope.launch { predictiveBackProgress.snapTo(backEvent) }
        },
        onPredictiveBackCancelled = { scope.launch { predictiveBackProgress.animateTo(0f) } }
    ) {
        Box(modifier = Modifier.fillMaxSize().imePadding()) {
            Scrim(
                color = colors.expandedModalScrimColor,
                onDismissRequest = animateToDismiss,
                visible = railState.targetValue != ModalExpandedNavigationRailValue.Closed
            )
            ModalWideNavigationRailContent(
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
            indicatorShape = ActiveIndicatorShape.value,
            topIconIndicatorWidth = TopIconItemActiveIndicatorWidth,
            topIconLabelTextStyle = TopIconLabelTextFont.value,
            startIconLabelTextStyle = StartIconLabelTextFont.value,
            topIconIndicatorHorizontalPadding = ItemTopIconIndicatorHorizontalPadding,
            topIconIndicatorVerticalPadding = ItemTopIconIndicatorVerticalPadding,
            topIconIndicatorToLabelVerticalPadding = ItemTopIconIndicatorToLabelPadding,
            startIconIndicatorHorizontalPadding = ItemStartIconIndicatorHorizontalPadding,
            startIconIndicatorVerticalPadding = ItemStartIconIndicatorVerticalPadding,
            startIconToLabelHorizontalPadding = ItemStartIconToLabelPadding,
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
            labelTextStyle = TopIconLabelTextFont.value,
            indicatorShape = ActiveIndicatorShape.value,
            indicatorWidth = TopIconItemActiveIndicatorWidth,
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
 * @param expandedModalContainerColor the color used for the background of a modal expanded
 *   navigation rail. Use [Color.Transparent] to have no color
 * @param expandedModalScrimColor the color used for the scrim overlay for background content of a
 *   modal expanded navigation rail
 */
@Immutable
class WideNavigationRailColors(
    val containerColor: Color,
    val contentColor: Color,
    val expandedModalContainerColor: Color,
    val expandedModalScrimColor: Color,
) {
    /**
     * Returns a copy of this NavigationRailColors, optionally overriding some of the values. This
     * uses the Color.Unspecified to mean “use the value from the source”.
     */
    fun copy(
        containerColor: Color = this.containerColor,
        contentColor: Color = this.contentColor,
        expandedModalContainerColor: Color = this.expandedModalContainerColor,
        modalScrimColor: Color = this.expandedModalScrimColor,
    ) =
        WideNavigationRailColors(
            containerColor = containerColor.takeOrElse { this.containerColor },
            contentColor = contentColor.takeOrElse { this.contentColor },
            expandedModalContainerColor =
                expandedModalContainerColor.takeOrElse { this.expandedModalContainerColor },
            expandedModalScrimColor = modalScrimColor.takeOrElse { this.expandedModalScrimColor },
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is WideNavigationRailColors) return false

        if (containerColor != other.containerColor) return false
        if (contentColor != other.contentColor) return false
        if (expandedModalContainerColor != other.expandedModalContainerColor) return false
        if (expandedModalScrimColor != other.expandedModalScrimColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = containerColor.hashCode()
        result = 31 * result + contentColor.hashCode()
        result = 31 * result + expandedModalContainerColor.hashCode()
        result = 31 * result + expandedModalScrimColor.hashCode()

        return result
    }
}

/** Defaults used in [WideNavigationRail]. */
@ExperimentalMaterial3ExpressiveApi
object WideNavigationRailDefaults {
    /** Default container shape of a wide navigation rail. */
    // TODO: Replace with token.
    val containerShape: Shape
        @Composable get() = ShapeKeyTokens.CornerNone.value

    /** Default container shape of a modal expanded navigation rail. */
    // TODO: Replace with token.
    val modalContainerShape: Shape
        @Composable get() = ShapeKeyTokens.CornerLarge.value

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

    private val ColorScheme.defaultWideWideNavigationRailColors: WideNavigationRailColors
        @Composable
        get() {
            return mDefaultWideWideNavigationRailColorsCached
                ?: WideNavigationRailColors(
                        // TODO: Replace with tokens.
                        containerColor = fromToken(ColorSchemeKeyTokens.Surface),
                        contentColor = fromToken(ColorSchemeKeyTokens.OnSurfaceVariant),
                        expandedModalContainerColor =
                            fromToken(ColorSchemeKeyTokens.SurfaceContainer),
                        expandedModalScrimColor =
                            ScrimTokens.ContainerColor.value.copy(ScrimTokens.ContainerOpacity)
                    )
                    .also { mDefaultWideWideNavigationRailColorsCached = it }
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
                        selectedIconColor = fromToken(ActiveIconColor),
                        selectedTextColor = fromToken(ActiveLabelTextColor),
                        selectedIndicatorColor = fromToken(ActiveIndicatorColor),
                        unselectedIconColor = fromToken(InactiveIconColor),
                        unselectedTextColor = fromToken(InactiveLabelTextColor),
                        disabledIconColor =
                            fromToken(InactiveIconColor).copy(alpha = DisabledAlpha),
                        disabledTextColor =
                            fromToken(InactiveLabelTextColor).copy(alpha = DisabledAlpha),
                    )
                    .also { defaultWideNavigationRailItemColorsCached = it }
        }
}

/** Default values for [ModalExpandedNavigationRail] */
@Immutable
@ExperimentalMaterial3ExpressiveApi
expect object ModalExpandedNavigationRailDefaults {

    /** Properties used to customize the behavior of a [ModalExpandedNavigationRail]. */
    val Properties: ModalExpandedNavigationRailProperties
}

@Immutable
@ExperimentalMaterial3ExpressiveApi
expect class ModalExpandedNavigationRailProperties(
    shouldDismissOnBackPress: Boolean = true,
) {
    val shouldDismissOnBackPress: Boolean
}

@ExperimentalMaterial3ExpressiveApi
@Composable
internal expect fun ModalWideNavigationRailDialog(
    onDismissRequest: () -> Unit,
    properties: ModalExpandedNavigationRailProperties,
    onPredictiveBack: (Float) -> Unit,
    onPredictiveBackCancelled: () -> Unit,
    content: @Composable () -> Unit
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ModalWideNavigationRailContent(
    settleToDismiss: suspend (velocity: Float) -> Unit,
    modifier: Modifier = Modifier,
    railState: ModalExpandedNavigationRailState,
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

    Box(
        modifier =
            modifier
                .fillMaxHeight()
                .widthIn(max = openModalRailMaxWidth)
                .semantics { paneTitle = railPaneTitle }
                .graphicsLayer {
                    // TODO: Implement predictive back behavior.
                }
                .draggableAnchors(railState.anchoredDraggableState, Orientation.Horizontal) {
                    railSize,
                    _ ->
                    val width = railSize.width.toFloat()
                    val minValue = if (isRtl) width else -width
                    val maxValue = 0f
                    return@draggableAnchors DraggableAnchors {
                        ModalExpandedNavigationRailValue.Closed at minValue
                        ModalExpandedNavigationRailValue.Open at maxValue
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
            modifier = modifier,
            expanded = true,
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

private const val HeaderLayoutIdTag: String = "header"

/* TODO: Replace below values with tokens. */
private val IconSize = 24.0.dp
private val TopIconItemActiveIndicatorWidth = 56.dp
private val TopIconItemActiveIndicatorHeight = 32.dp
private val StartIconItemActiveIndicatorHeight = 56.dp
private val NoLabelItemActiveIndicatorHeight = 56.dp
private val TopIconLabelTextFont = TypographyKeyTokens.LabelMedium
private val StartIconLabelTextFont = TypographyKeyTokens.LabelLarge
private val ActiveIndicatorShape = ShapeKeyTokens.CornerFull
private val CollapsedRailWidth = 96.dp
private val ExpandedRailMinWidth = 220.dp
private val ExpandedRailMaxWidth = 360.dp
private val ExpandedRailHorizontalItemPadding = 20.dp
private val ItemStartIconIndicatorHorizontalPadding = 16.dp
private val ItemStartIconToLabelPadding = 8.dp
/*@VisibleForTesting*/
internal val WNRTopIconItemMinHeight = 64.dp

/*@VisibleForTesting*/
// Vertical padding between the contents of the wide navigation rail and its top/bottom.
internal val WNRVerticalPadding = 44.dp
/*@VisibleForTesting*/
// Padding at the bottom of the rail's header. This padding will only be added when the header is
// not null and the rail arrangement is Top.
internal val WNRHeaderPadding: Dp = 40.dp
/*@VisibleForTesting*/
internal val WNRItemNoLabelIndicatorPadding = (NoLabelItemActiveIndicatorHeight - IconSize) / 2

private val VerticalPaddingBetweenTopIconItems = 4.dp
private val ItemMinWidth = CollapsedRailWidth
private val ItemTopIconIndicatorVerticalPadding = (TopIconItemActiveIndicatorHeight - IconSize) / 2
private val ItemTopIconIndicatorHorizontalPadding = (TopIconItemActiveIndicatorWidth - IconSize) / 2
private val ItemStartIconIndicatorVerticalPadding =
    (StartIconItemActiveIndicatorHeight - IconSize) / 2
private val ItemTopIconIndicatorToLabelPadding: Dp = 4.dp

/* TODO: Replace below values with tokens. */
// TODO: Update to OnSecondaryContainer once value matches Secondary.
private val ActiveIconColor = ColorSchemeKeyTokens.Secondary
private val ActiveLabelTextColor = ColorSchemeKeyTokens.Secondary
private val ActiveIndicatorColor = ColorSchemeKeyTokens.SecondaryContainer
private val InactiveIconColor = ColorSchemeKeyTokens.OnSurfaceVariant
private val InactiveLabelTextColor = ColorSchemeKeyTokens.OnSurfaceVariant
