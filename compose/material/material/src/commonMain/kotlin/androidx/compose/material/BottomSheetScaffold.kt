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
package androidx.compose.material

import androidx.annotation.FloatRange
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.material.BottomSheetValue.Collapsed
import androidx.compose.material.BottomSheetValue.Expanded
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxBy
import kotlin.jvm.JvmName
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Possible values of [BottomSheetState]. */
enum class BottomSheetValue {
    /** The bottom sheet is visible, but only showing its peek height. */
    Collapsed,

    /** The bottom sheet is visible at its maximum height. */
    Expanded
}

/**
 * State of the persistent bottom sheet in [BottomSheetScaffold].
 *
 * @param initialValue The initial value of the state.
 * @param density The density that this state can use to convert values to and from dp.
 * @param animationSpec The default animation that will be used to animate to a new state.
 * @param confirmValueChange Optional callback invoked to confirm or veto a pending state change.
 */
@OptIn(ExperimentalMaterialApi::class)
@Stable
class BottomSheetState(
    initialValue: BottomSheetValue,
    density: Density,
    animationSpec: AnimationSpec<Float> = BottomSheetScaffoldDefaults.AnimationSpec,
    confirmValueChange: (BottomSheetValue) -> Boolean = { true }
) {

    internal val anchoredDraggableState =
        AnchoredDraggableState(
            initialValue = initialValue,
            animationSpec = animationSpec,
            confirmValueChange = confirmValueChange,
            positionalThreshold = {
                with(density) { BottomSheetScaffoldPositionalThreshold.toPx() }
            },
            velocityThreshold = { with(density) { BottomSheetScaffoldVelocityThreshold.toPx() } }
        )

    /** The current value of the [BottomSheetState]. */
    val currentValue: BottomSheetValue
        get() = anchoredDraggableState.currentValue

    /**
     * The target value the state will settle at once the current interaction ends, or the
     * [currentValue] if there is no interaction in progress.
     */
    val targetValue: BottomSheetValue
        get() = anchoredDraggableState.targetValue

    /** Whether the bottom sheet is expanded. */
    val isExpanded: Boolean
        get() = anchoredDraggableState.currentValue == Expanded

    /** Whether the bottom sheet is collapsed. */
    val isCollapsed: Boolean
        get() = anchoredDraggableState.currentValue == Collapsed

    /**
     * The fraction of the progress, within [0f..1f] bounds, or 1f if the [AnchoredDraggableState]
     * is in a settled state.
     */
    @Deprecated(
        message = "Please use the progress function to query progress explicitly between targets.",
        replaceWith = ReplaceWith("progress(from = , to = )")
    )
    @get:FloatRange(from = 0.0, to = 1.0)
    @ExperimentalMaterialApi
    val progress: Float
        get() = anchoredDraggableState.progress

    /**
     * The fraction of the offset between [from] and [to], as a fraction between [0f..1f], or 1f if
     * [from] is equal to [to].
     *
     * @param from The starting value used to calculate the distance
     * @param to The end value used to calculate the distance
     */
    @FloatRange(from = 0.0, to = 1.0)
    fun progress(from: BottomSheetValue, to: BottomSheetValue): Float {
        val fromOffset = anchoredDraggableState.anchors.positionOf(from)
        val toOffset = anchoredDraggableState.anchors.positionOf(to)
        val currentOffset =
            anchoredDraggableState.offset.coerceIn(
                min(fromOffset, toOffset), // fromOffset might be > toOffset
                max(fromOffset, toOffset)
            )
        val fraction = (currentOffset - fromOffset) / (toOffset - fromOffset)
        return if (fraction.isNaN()) 1f else abs(fraction)
    }

    /**
     * Expand the bottom sheet with an animation and suspend until the animation finishes or is
     * cancelled. Note: If the peek height is equal to the sheet height, this method will animate to
     * the [Collapsed] state.
     *
     * This method will throw [CancellationException] if the animation is interrupted.
     */
    suspend fun expand() {
        val target =
            if (anchoredDraggableState.anchors.hasAnchorFor(Expanded)) {
                Expanded
            } else {
                Collapsed
            }
        anchoredDraggableState.animateTo(target)
    }

    /**
     * Collapse the bottom sheet with animation and suspend until it if fully collapsed or animation
     * has been cancelled. This method will throw [CancellationException] if the animation is
     * interrupted.
     */
    suspend fun collapse() = anchoredDraggableState.animateTo(Collapsed)

    /**
     * Require the current offset.
     *
     * @throws IllegalStateException If the offset has not been initialized yet
     */
    fun requireOffset() = anchoredDraggableState.requireOffset()

    internal suspend fun animateTo(
        target: BottomSheetValue,
        velocity: Float = anchoredDraggableState.lastVelocity
    ) = anchoredDraggableState.animateTo(target, velocity)

    internal suspend fun snapTo(target: BottomSheetValue) = anchoredDraggableState.snapTo(target)

    companion object {

        /** The default [Saver] implementation for [BottomSheetState]. */
        fun Saver(
            animationSpec: AnimationSpec<Float>,
            confirmStateChange: (BottomSheetValue) -> Boolean,
            density: Density
        ): Saver<BottomSheetState, *> =
            Saver(
                save = { it.anchoredDraggableState.currentValue },
                restore = {
                    BottomSheetState(
                        initialValue = it,
                        density = density,
                        animationSpec = animationSpec,
                        confirmValueChange = confirmStateChange
                    )
                }
            )
    }
}

/**
 * Create a [BottomSheetState] and [remember] it.
 *
 * @param initialValue The initial value of the state.
 * @param animationSpec The default animation that will be used to animate to a new state.
 * @param confirmStateChange Optional callback invoked to confirm or veto a pending state change.
 */
@Composable
fun rememberBottomSheetState(
    initialValue: BottomSheetValue,
    animationSpec: AnimationSpec<Float> = BottomSheetScaffoldDefaults.AnimationSpec,
    confirmStateChange: (BottomSheetValue) -> Boolean = { true }
): BottomSheetState {
    val density = LocalDensity.current
    return rememberSaveable(
        animationSpec,
        saver =
            BottomSheetState.Saver(
                animationSpec = animationSpec,
                confirmStateChange = confirmStateChange,
                density = density
            )
    ) {
        BottomSheetState(
            initialValue = initialValue,
            animationSpec = animationSpec,
            confirmValueChange = confirmStateChange,
            density = density
        )
    }
}

/**
 * State of the [BottomSheetScaffold] composable.
 *
 * @param bottomSheetState The state of the persistent bottom sheet.
 * @param snackbarHostState The [SnackbarHostState] used to show snackbars inside the scaffold.
 */
@Stable
class BottomSheetScaffoldState(
    val bottomSheetState: BottomSheetState,
    val snackbarHostState: SnackbarHostState
)

/**
 * Create and [remember] a [BottomSheetScaffoldState].
 *
 * @param bottomSheetState The state of the persistent bottom sheet.
 * @param snackbarHostState The [SnackbarHostState] used to show snackbars inside the scaffold.
 */
@Composable
fun rememberBottomSheetScaffoldState(
    bottomSheetState: BottomSheetState = rememberBottomSheetState(Collapsed),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
): BottomSheetScaffoldState {
    return remember(bottomSheetState, snackbarHostState) {
        BottomSheetScaffoldState(
            bottomSheetState = bottomSheetState,
            snackbarHostState = snackbarHostState
        )
    }
}

/**
 * <a href="https://material.io/components/sheets-bottom#standard-bottom-sheet" class="external"
 * target="_blank">Material Design standard bottom sheet</a>.
 *
 * Standard bottom sheets co-exist with the screen’s main UI region and allow for simultaneously
 * viewing and interacting with both regions. They are commonly used to keep a feature or secondary
 * content visible on screen when content in main UI region is frequently scrolled or panned.
 *
 * ![Standard bottom sheet
 * image](https://developer.android.com/images/reference/androidx/compose/material/standard-bottom-sheet.png)
 *
 * This component provides an API to put together several material components to construct your
 * screen. For a similar component which implements the basic material design layout strategy with
 * app bars, floating action buttons and navigation drawers, use the standard [Scaffold]. For
 * similar component that uses a backdrop as the centerpiece of the screen, use [BackdropScaffold].
 *
 * A simple example of a bottom sheet scaffold looks like this:
 *
 * @sample androidx.compose.material.samples.BottomSheetScaffoldSample
 * @param sheetContent The content of the bottom sheet.
 * @param modifier An optional [Modifier] for the root of the scaffold.
 * @param scaffoldState The state of the scaffold.
 * @param topBar An optional top app bar.
 * @param snackbarHost The composable hosting the snackbars shown inside the scaffold.
 * @param floatingActionButton An optional floating action button.
 * @param floatingActionButtonPosition The position of the floating action button.
 * @param sheetGesturesEnabled Whether the bottom sheet can be interacted with by gestures.
 * @param sheetShape The shape of the bottom sheet.
 * @param sheetElevation The elevation of the bottom sheet.
 * @param sheetBackgroundColor The background color of the bottom sheet.
 * @param sheetContentColor The preferred content color provided by the bottom sheet to its
 *   children. Defaults to the matching content color for [sheetBackgroundColor], or if that is not
 *   a color from the theme, this will keep the same content color set above the bottom sheet.
 * @param sheetPeekHeight The height of the bottom sheet when it is collapsed. If the peek height
 *   equals the sheet's full height, the sheet will only have a collapsed state.
 * @param backgroundColor The background color of the scaffold body.
 * @param contentColor The color of the content in scaffold body. Defaults to either the matching
 *   content color for [backgroundColor], or, if it is not a color from the theme, this will keep
 *   the same value set above this Surface.
 * @param content The main content of the screen. You should use the provided [PaddingValues] to
 *   properly offset the content, so that it is not obstructed by the bottom sheet when collapsed.
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BottomSheetScaffold(
    sheetContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    scaffoldState: BottomSheetScaffoldState = rememberBottomSheetScaffoldState(),
    topBar: (@Composable () -> Unit)? = null,
    snackbarHost: @Composable (SnackbarHostState) -> Unit = { SnackbarHost(it) },
    floatingActionButton: (@Composable () -> Unit)? = null,
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    sheetGesturesEnabled: Boolean = true,
    sheetShape: Shape = MaterialTheme.shapes.large,
    sheetElevation: Dp = BottomSheetScaffoldDefaults.SheetElevation,
    sheetBackgroundColor: Color = MaterialTheme.colors.surface,
    sheetContentColor: Color = contentColorFor(sheetBackgroundColor),
    sheetPeekHeight: Dp = BottomSheetScaffoldDefaults.SheetPeekHeight,
    backgroundColor: Color = MaterialTheme.colors.background,
    contentColor: Color = contentColorFor(backgroundColor),
    content: @Composable (PaddingValues) -> Unit
) {
    Surface(modifier.fillMaxSize(), color = backgroundColor, contentColor = contentColor) {
        BottomSheetScaffoldLayout(
            topBar = topBar,
            body = { content(PaddingValues(bottom = sheetPeekHeight)) },
            bottomSheet = {
                val nestedScroll =
                    if (sheetGesturesEnabled) {
                        Modifier.nestedScroll(
                            remember(scaffoldState.bottomSheetState.anchoredDraggableState) {
                                ConsumeSwipeWithinBottomSheetBoundsNestedScrollConnection(
                                    state = scaffoldState.bottomSheetState.anchoredDraggableState,
                                    orientation = Orientation.Vertical
                                )
                            }
                        )
                    } else Modifier
                BottomSheet(
                    state = scaffoldState.bottomSheetState,
                    modifier = nestedScroll.fillMaxWidth().requiredHeightIn(min = sheetPeekHeight),
                    sheetBackgroundColor = sheetBackgroundColor,
                    sheetContentColor = sheetContentColor,
                    sheetElevation = sheetElevation,
                    sheetGesturesEnabled = sheetGesturesEnabled,
                    sheetShape = sheetShape,
                    sheetPeekHeight = sheetPeekHeight,
                    content = sheetContent
                )
            },
            floatingActionButton = floatingActionButton,
            snackbarHost = { snackbarHost(scaffoldState.snackbarHostState) },
            sheetPeekHeight = sheetPeekHeight,
            sheetState = scaffoldState.bottomSheetState,
            sheetOffset = { scaffoldState.bottomSheetState.requireOffset() },
            floatingActionButtonPosition = floatingActionButtonPosition
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun BottomSheet(
    state: BottomSheetState,
    sheetGesturesEnabled: Boolean,
    sheetShape: Shape,
    sheetElevation: Dp,
    sheetBackgroundColor: Color,
    sheetContentColor: Color,
    sheetPeekHeight: Dp,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val scope = rememberCoroutineScope()
    val peekHeightPx = with(LocalDensity.current) { sheetPeekHeight.toPx() }
    Surface(
        modifier
            .draggableAnchors(state.anchoredDraggableState, Orientation.Vertical) {
                sheetSize,
                constraints ->
                val layoutHeight = constraints.maxHeight
                val sheetHeight = sheetSize.height.toFloat()
                val newAnchors = DraggableAnchors {
                    Collapsed at layoutHeight - peekHeightPx
                    if (sheetHeight > 0f && sheetHeight != peekHeightPx) {
                        Expanded at layoutHeight - sheetHeight
                    }
                }
                val newTarget =
                    when (state.anchoredDraggableState.targetValue) {
                        Collapsed -> Collapsed
                        Expanded -> if (newAnchors.hasAnchorFor(Expanded)) Expanded else Collapsed
                    }
                return@draggableAnchors newAnchors to newTarget
            }
            .anchoredDraggable(
                state = state.anchoredDraggableState,
                orientation = Orientation.Vertical,
                enabled = sheetGesturesEnabled,
            )
            .semantics {
                // If we don't have anchors yet, or have only one anchor we don't want any
                // accessibility actions
                if (state.anchoredDraggableState.anchors.size > 1) {
                    if (state.isCollapsed) {
                        expand {
                            if (state.anchoredDraggableState.confirmValueChange(Expanded)) {
                                scope.launch { state.expand() }
                            }
                            true
                        }
                    } else {
                        collapse {
                            if (state.anchoredDraggableState.confirmValueChange(Collapsed)) {
                                scope.launch { state.collapse() }
                            }
                            true
                        }
                    }
                }
            },
        shape = sheetShape,
        elevation = sheetElevation,
        color = sheetBackgroundColor,
        contentColor = sheetContentColor,
        content = { Column(content = content) }
    )
}

/** Contains useful defaults for [BottomSheetScaffold]. */
object BottomSheetScaffoldDefaults {
    /** The default elevation used by [BottomSheetScaffold]. */
    val SheetElevation = 8.dp

    /** The default peek height used by [BottomSheetScaffold]. */
    val SheetPeekHeight = 56.dp

    /** The default animation spec used by [BottomSheetScaffoldState]. */
    val AnimationSpec: AnimationSpec<Float> =
        tween(durationMillis = 300, easing = FastOutSlowInEasing)
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun BottomSheetScaffoldLayout(
    topBar: @Composable (() -> Unit)?,
    body: @Composable () -> Unit,
    bottomSheet: @Composable () -> Unit,
    floatingActionButton: (@Composable () -> Unit)?,
    snackbarHost: @Composable () -> Unit,
    sheetPeekHeight: Dp,
    sheetOffset: () -> Float,
    floatingActionButtonPosition: FabPosition,
    sheetState: BottomSheetState,
) {
    Layout(
        contents =
            listOf<@Composable () -> Unit>(
                topBar ?: {},
                body,
                bottomSheet,
                floatingActionButton ?: {},
                snackbarHost
            )
    ) {
        (
            topBarMeasurables,
            bodyMeasurables,
            sheetMeasurables,
            fabMeasurables,
            snackbarHostMeasurables),
        constraints ->
        val layoutWidth = constraints.maxWidth
        val layoutHeight = constraints.maxHeight
        val looseConstraints = constraints.copyMaxDimensions()

        val sheetPlaceables = sheetMeasurables.fastMap { it.measure(looseConstraints) }

        val topBarPlaceables = topBarMeasurables.fastMap { it.measure(looseConstraints) }
        val topBarHeight = topBarPlaceables.fastMaxBy { it.height }?.height ?: 0

        val bodyConstraints = looseConstraints.copy(maxHeight = layoutHeight - topBarHeight)
        val bodyPlaceables = bodyMeasurables.fastMap { it.measure(bodyConstraints) }

        val fabPlaceable = fabMeasurables.fastMap { it.measure(looseConstraints) }
        val fabWidth = fabPlaceable.fastMaxBy { it.width }?.width ?: 0
        val fabHeight = fabPlaceable.fastMaxBy { it.height }?.height ?: 0

        val snackbarPlaceables = snackbarHostMeasurables.fastMap { it.measure(looseConstraints) }
        val snackbarWidth = snackbarPlaceables.fastMaxBy { it.width }?.width ?: 0
        val snackbarHeight = snackbarPlaceables.fastMaxBy { it.height }?.height ?: 0

        layout(layoutWidth, layoutHeight) {
            val sheetOffsetY = sheetOffset().roundToInt()

            val fabOffsetX =
                when (floatingActionButtonPosition) {
                    FabPosition.Start -> FabSpacing.roundToPx()
                    FabPosition.Center -> (layoutWidth - fabWidth) / 2
                    else -> layoutWidth - fabWidth - FabSpacing.roundToPx()
                }

            // In case sheet peek height < (FAB height / 2), give the FAB some minimum space
            val fabOffsetY =
                if (sheetPeekHeight.toPx() < fabHeight / 2) {
                    sheetOffsetY - fabHeight - FabSpacing.roundToPx()
                } else sheetOffsetY - (fabHeight / 2)

            val snackbarOffsetX = (layoutWidth - snackbarWidth) / 2
            val snackbarOffsetY =
                when (sheetState.currentValue) {
                    Collapsed -> fabOffsetY - snackbarHeight
                    Expanded -> layoutHeight - snackbarHeight
                }

            // Placement order is important for elevation
            bodyPlaceables.fastForEach { it.placeRelative(0, topBarHeight) }
            topBarPlaceables.fastForEach { it.placeRelative(0, 0) }
            sheetPlaceables.fastForEach { it.placeRelative(0, 0) }
            fabPlaceable.fastForEach { it.placeRelative(fabOffsetX, fabOffsetY) }
            snackbarPlaceables.fastForEach { it.placeRelative(snackbarOffsetX, snackbarOffsetY) }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
private fun ConsumeSwipeWithinBottomSheetBoundsNestedScrollConnection(
    state: AnchoredDraggableState<*>,
    orientation: Orientation
): NestedScrollConnection =
    object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val delta = available.toFloat()
            return if (delta < 0 && source == NestedScrollSource.UserInput) {
                state.dispatchRawDelta(delta).toOffset()
            } else {
                Offset.Zero
            }
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset {
            return if (source == NestedScrollSource.UserInput) {
                state.dispatchRawDelta(available.toFloat()).toOffset()
            } else {
                Offset.Zero
            }
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            val toFling = available.toFloat()
            val currentOffset = state.requireOffset()
            return if (toFling < 0 && currentOffset > state.anchors.minAnchor()) {
                state.settle(velocity = toFling)
                // since we go to the anchor with tween settling, consume all for the best UX
                available
            } else {
                Velocity.Zero
            }
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            state.settle(velocity = available.toFloat())
            return available
        }

        private fun Float.toOffset(): Offset =
            Offset(
                x = if (orientation == Orientation.Horizontal) this else 0f,
                y = if (orientation == Orientation.Vertical) this else 0f
            )

        @JvmName("velocityToFloat")
        private fun Velocity.toFloat() = if (orientation == Orientation.Horizontal) x else y

        @JvmName("offsetToFloat")
        private fun Offset.toFloat(): Float = if (orientation == Orientation.Horizontal) x else y
    }

private val FabSpacing = 16.dp
private val BottomSheetScaffoldPositionalThreshold = 56.dp
private val BottomSheetScaffoldVelocityThreshold = 125.dp
