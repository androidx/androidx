/*
 * Copyright 2019 The Android Open Source Project
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
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.BottomDrawerValue.Closed
import androidx.compose.material.BottomDrawerValue.Expanded
import androidx.compose.material.BottomDrawerValue.Open
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Possible values of [DrawerState].
 */
enum class DrawerValue {
    /**
     * The state of the drawer when it is closed.
     */
    Closed,

    /**
     * The state of the drawer when it is open.
     */
    Open
}

/**
 * Possible values of [BottomDrawerState].
 */
enum class BottomDrawerValue {
    /**
     * The state of the bottom drawer when it is closed.
     */
    Closed,

    /**
     * The state of the bottom drawer when it is open (i.e. at 50% height).
     */
    Open,

    /**
     * The state of the bottom drawer when it is expanded (i.e. at 100% height).
     */
    Expanded
}

/**
 * State of the [ModalDrawer] composable.
 *
 * @param initialValue The initial value of the state.
 * @param confirmStateChange Optional callback invoked to confirm or veto a pending state change.
 */
@Suppress("NotCloseable")
@OptIn(ExperimentalMaterialApi::class)
@Stable
class DrawerState(
    initialValue: DrawerValue,
    confirmStateChange: (DrawerValue) -> Boolean = { true }
) {

    internal val anchoredDraggableState = AnchoredDraggableState(
        initialValue = initialValue,
        animationSpec = AnimationSpec,
        confirmValueChange = confirmStateChange,
        positionalThreshold = { with(requireDensity()) { DrawerPositionalThreshold.toPx() } },
        velocityThreshold = { with(requireDensity()) { DrawerVelocityThreshold.toPx() } },
    )

    /**
     * Whether the drawer is open.
     */
    val isOpen: Boolean
        get() = currentValue == DrawerValue.Open

    /**
     * Whether the drawer is closed.
     */
    val isClosed: Boolean
        get() = currentValue == DrawerValue.Closed

    /**
     * The current value of the state.
     *
     * If no swipe or animation is in progress, this corresponds to the start the drawer
     * currently in. If a swipe or an animation is in progress, this corresponds the state drawer
     * was in before the swipe or animation started.
     */
    val currentValue: DrawerValue
        get() {
            return anchoredDraggableState.currentValue
        }

    /**
     * Whether the state is currently animating.
     */
    val isAnimationRunning: Boolean
        get() {
            return anchoredDraggableState.isAnimationRunning
        }

    /**
     * Open the drawer with animation and suspend until it if fully opened or animation has been
     * cancelled. This method will throw [CancellationException] if the animation is
     * interrupted
     *
     * @return the reason the open animation ended
     */
    suspend fun open() = anchoredDraggableState.animateTo(DrawerValue.Open)

    /**
     * Close the drawer with animation and suspend until it if fully closed or animation has been
     * cancelled. This method will throw [CancellationException] if the animation is
     * interrupted
     *
     * @return the reason the close animation ended
     */
    suspend fun close() = anchoredDraggableState.animateTo(DrawerValue.Closed)

    /**
     * Set the state of the drawer with specific animation
     *
     * @param targetValue The new value to animate to.
     * @param anim Set the state of the drawer with specific animation
     */
    @ExperimentalMaterialApi
    @Deprecated(
        message = "This method has been replaced by the open and close methods. The animation " +
            "spec is now an implementation detail of ModalDrawer.",
        level = DeprecationLevel.ERROR
    )
    suspend fun animateTo(
        targetValue: DrawerValue,
        @Suppress("UNUSED_PARAMETER") anim: AnimationSpec<Float>
    ) {
        anchoredDraggableState.animateTo(targetValue)
    }

    /**
     * Set the state without any animation and suspend until it's set
     *
     * @param targetValue The new target value
     */
    suspend fun snapTo(targetValue: DrawerValue) {
        anchoredDraggableState.snapTo(targetValue)
    }

    /**
     * The target value of the drawer state.
     *
     * If a swipe is in progress, this is the value that the Drawer would animate to if the
     * swipe finishes. If an animation is running, this is the target value of that animation.
     * Finally, if no swipe or animation is in progress, this is the same as the [currentValue].
     */
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @ExperimentalMaterialApi
    @get:ExperimentalMaterialApi
    val targetValue: DrawerValue
        get() = anchoredDraggableState.targetValue

    /**
     * The current position (in pixels) of the drawer sheet, or [Float.NaN] before the offset is
     * initialized.
     * @see [AnchoredDraggableState.offset] for more information.
     */
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @ExperimentalMaterialApi
    @get:ExperimentalMaterialApi
    val offset: Float
        get() = anchoredDraggableState.offset

    internal fun requireOffset(): Float = anchoredDraggableState.requireOffset()

    internal var density: Density? = null
    private fun requireDensity() = requireNotNull(density) {
        "The density on DrawerState ($this) was not set. Did you use DrawerState with the Drawer " +
            "composable?"
    }

    companion object {
        /**
         * The default [Saver] implementation for [DrawerState].
         */
        fun Saver(confirmStateChange: (DrawerValue) -> Boolean) =
            Saver<DrawerState, DrawerValue>(
                save = { it.currentValue },
                restore = { DrawerState(it, confirmStateChange) }
            )
    }
}

/**
 * State of the [BottomDrawer] composable.
 *
 * @param initialValue The initial value of the state.
 * @param density The density that this state can use to convert values to and from dp.
 * @param confirmStateChange Optional callback invoked to confirm or veto a pending state change.
 * @param animationSpec The animation spec to be used for open/close animations, as well as
 * settling when a user lets go.
 */
@OptIn(ExperimentalMaterialApi::class)
@Suppress("NotCloseable")
class BottomDrawerState(
    initialValue: BottomDrawerValue,
    density: Density,
    confirmStateChange: (BottomDrawerValue) -> Boolean = { true },
    animationSpec: AnimationSpec<Float> = DrawerDefaults.AnimationSpec
) {
    internal val anchoredDraggableState = AnchoredDraggableState(
        initialValue = initialValue,
        animationSpec = animationSpec,
        confirmValueChange = confirmStateChange,
        positionalThreshold = { with(density) { DrawerPositionalThreshold.toPx() } },
        velocityThreshold = { with(density) { DrawerVelocityThreshold.toPx() } },
    )

    /**
     * The target value the state will settle at once the current interaction ends, or the
     * [currentValue] if there is no interaction in progress.
     */
    val targetValue: BottomDrawerValue
        get() = anchoredDraggableState.targetValue

    /**
     * The current offset in pixels, or [Float.NaN] if it has not been initialized yet.
     */
    val offset: Float
        get() = anchoredDraggableState.offset

    internal fun requireOffset(): Float = anchoredDraggableState.requireOffset()

    /**
     * The current value of the [BottomDrawerState].
     */
    val currentValue: BottomDrawerValue get() = anchoredDraggableState.currentValue

    /**
     * Whether the drawer is open, either in opened or expanded state.
     */
    val isOpen: Boolean
        get() = anchoredDraggableState.currentValue != Closed

    /**
     * Whether the drawer is closed.
     */
    val isClosed: Boolean
        get() = anchoredDraggableState.currentValue == Closed

    /**
     * Whether the drawer is expanded.
     */
    val isExpanded: Boolean
        get() = anchoredDraggableState.currentValue == Expanded

    /**
     * The fraction of the progress, within [0f..1f] bounds, or 1f if the [AnchoredDraggableState]
     * is in a settled state.
     */
    @Deprecated(
        message = "Please use the progress function to query progress explicitly between targets.",
        replaceWith = ReplaceWith("progress(from = , to = )")
    ) // TODO: Remove in the future b/323882175
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
    fun progress(
        from: BottomDrawerValue,
        to: BottomDrawerValue
    ): Float {
        val fromOffset = anchoredDraggableState.anchors.positionOf(from)
        val toOffset = anchoredDraggableState.anchors.positionOf(to)
        val currentOffset = anchoredDraggableState.offset.coerceIn(
            min(fromOffset, toOffset), // fromOffset might be > toOffset
            max(fromOffset, toOffset)
        )
        val fraction = (currentOffset - fromOffset) / (toOffset - fromOffset)
        return if (fraction.isNaN()) 1f else abs(fraction)
    }

    /**
     * Open the drawer with animation and suspend until it if fully opened or animation has been
     * cancelled. If the content height is less than [BottomDrawerOpenFraction], the drawer state
     * will move to [BottomDrawerValue.Expanded] instead.
     *
     * @throws [CancellationException] if the animation is interrupted
     *
     */
    suspend fun open() {
        val targetValue =
            if (isOpenEnabled) Open else Expanded
        anchoredDraggableState.animateTo(targetValue)
    }

    /**
     * Close the drawer with animation and suspend until it if fully closed or animation has been
     * cancelled.
     *
     * @throws [CancellationException] if the animation is interrupted
     *
     */
    suspend fun close() = anchoredDraggableState.animateTo(Closed)

    /**
     * Expand the drawer with animation and suspend until it if fully expanded or animation has
     * been cancelled.
     *
     * @throws [CancellationException] if the animation is interrupted
     *
     */
    suspend fun expand() = anchoredDraggableState.animateTo(Expanded)

    internal suspend fun animateTo(
        target: BottomDrawerValue,
        velocity: Float = anchoredDraggableState.lastVelocity
    ) = anchoredDraggableState.animateTo(target, velocity)

    internal suspend fun snapTo(target: BottomDrawerValue) = anchoredDraggableState.snapTo(target)

    internal fun confirmStateChange(value: BottomDrawerValue): Boolean =
        anchoredDraggableState.confirmValueChange(value)

    private val isOpenEnabled: Boolean
        get() = anchoredDraggableState.anchors.hasAnchorFor(Open)

    internal val nestedScrollConnection = ConsumeSwipeWithinBottomSheetBoundsNestedScrollConnection(
        anchoredDraggableState
    )

    internal var density: Density? = null

    companion object {
        /**
         * The default [Saver] implementation for [BottomDrawerState].
         */
        fun Saver(
            density: Density,
            confirmStateChange: (BottomDrawerValue) -> Boolean,
            animationSpec: AnimationSpec<Float>
        ) = Saver<BottomDrawerState, BottomDrawerValue>(
                save = { it.anchoredDraggableState.currentValue },
                restore = { BottomDrawerState(it, density, confirmStateChange, animationSpec) }
            )
    }
}

/**
 * Create and [remember] a [DrawerState].
 *
 * @param initialValue The initial value of the state.
 * @param confirmStateChange Optional callback invoked to confirm or veto a pending state change.
 */
@Composable
fun rememberDrawerState(
    initialValue: DrawerValue,
    confirmStateChange: (DrawerValue) -> Boolean = { true }
): DrawerState {
    return rememberSaveable(saver = DrawerState.Saver(confirmStateChange)) {
        DrawerState(initialValue, confirmStateChange)
    }
}

/**
 * Create and [remember] a [BottomDrawerState].
 *
 * @param initialValue The initial value of the state.
 * @param confirmStateChange Optional callback invoked to confirm or veto a pending state change.
 * @param animationSpec The animation spec to be used for open/close animations, as well as
 * settling when a user lets go.
 */
@Composable
fun rememberBottomDrawerState(
    initialValue: BottomDrawerValue,
    confirmStateChange: (BottomDrawerValue) -> Boolean = { true },
    animationSpec: AnimationSpec<Float> = DrawerDefaults.AnimationSpec,
): BottomDrawerState {
    val density = LocalDensity.current
    return rememberSaveable(
        density,
        saver = BottomDrawerState.Saver(density, confirmStateChange, animationSpec)
    ) {
        BottomDrawerState(initialValue, density, confirmStateChange, animationSpec)
    }
}

/**
 * <a href="https://material.io/components/navigation-drawer#modal-drawer" class="external" target="_blank">Material Design modal navigation drawer</a>.
 *
 * Modal navigation drawers block interaction with the rest of an app’s content with a scrim.
 * They are elevated above most of the app’s UI and don’t affect the screen’s layout grid.
 *
 * ![Modal drawer image](https://developer.android.com/images/reference/androidx/compose/material/modal-drawer.png)
 *
 * See [BottomDrawer] for a layout that introduces a bottom drawer, suitable when
 * using bottom navigation.
 *
 * @sample androidx.compose.material.samples.ModalDrawerSample
 *
 * @param drawerContent composable that represents content inside the drawer
 * @param modifier optional modifier for the drawer
 * @param drawerState state of the drawer
 * @param gesturesEnabled whether or not drawer can be interacted by gestures
 * @param drawerShape shape of the drawer sheet
 * @param drawerElevation drawer sheet elevation. This controls the size of the shadow below the
 * drawer sheet
 * @param drawerBackgroundColor background color to be used for the drawer sheet
 * @param drawerContentColor color of the content to use inside the drawer sheet. Defaults to
 * either the matching content color for [drawerBackgroundColor], or, if it is not a color from
 * the theme, this will keep the same value set above this Surface.
 * @param scrimColor color of the scrim that obscures content when the drawer is open
 * @param content content of the rest of the UI
 *
 * @throws IllegalStateException when parent has [Float.POSITIVE_INFINITY] width
 */
@Composable
@OptIn(ExperimentalMaterialApi::class)
fun ModalDrawer(
    drawerContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    gesturesEnabled: Boolean = true,
    drawerShape: Shape = DrawerDefaults.shape,
    drawerElevation: Dp = DrawerDefaults.Elevation,
    drawerBackgroundColor: Color = DrawerDefaults.backgroundColor,
    drawerContentColor: Color = contentColorFor(drawerBackgroundColor),
    scrimColor: Color = DrawerDefaults.scrimColor,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    BoxWithConstraints(modifier.fillMaxSize()) {
        val modalDrawerConstraints = constraints
        // TODO : think about Infinite max bounds case
        if (!modalDrawerConstraints.hasBoundedWidth) {
            throw IllegalStateException("Drawer shouldn't have infinite width")
        }
        val minValue = -modalDrawerConstraints.maxWidth.toFloat()
        val maxValue = 0f

        val density = LocalDensity.current
        SideEffect {
            drawerState.density = density
            val anchors = DraggableAnchors {
                DrawerValue.Closed at minValue
                DrawerValue.Open at maxValue
            }
            drawerState.anchoredDraggableState.updateAnchors(anchors)
        }

        val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        Box(
            Modifier
                .anchoredDraggable(
                    state = drawerState.anchoredDraggableState,
                    orientation = Orientation.Horizontal,
                    enabled = gesturesEnabled,
                    reverseDirection = isRtl
                )
        ) {
            Box {
                content()
            }
            Scrim(
                open = drawerState.isOpen,
                onClose = {
                    if (
                        gesturesEnabled &&
                        drawerState.anchoredDraggableState.confirmValueChange(DrawerValue.Closed)
                    ) {
                        scope.launch { drawerState.close() }
                    }
                },
                fraction = {
                    calculateFraction(minValue, maxValue, drawerState.requireOffset())
                },
                color = scrimColor
            )
            val navigationMenu = getString(Strings.NavigationMenu)
            Surface(
                modifier = with(LocalDensity.current) {
                    Modifier
                        .sizeIn(
                            minWidth = modalDrawerConstraints.minWidth.toDp(),
                            minHeight = modalDrawerConstraints.minHeight.toDp(),
                            maxWidth = modalDrawerConstraints.maxWidth.toDp(),
                            maxHeight = modalDrawerConstraints.maxHeight.toDp()
                        )
                }
                    .offset {
                        IntOffset(
                            drawerState
                                .requireOffset()
                                .roundToInt(), 0
                        )
                    }
                    .padding(end = EndDrawerPadding)
                    .semantics {
                        paneTitle = navigationMenu
                        if (drawerState.isOpen) {
                            dismiss {
                                if (
                                    drawerState.anchoredDraggableState
                                        .confirmValueChange(DrawerValue.Closed)
                                ) {
                                    scope.launch { drawerState.close() }
                                }; true
                            }
                        }
                    },
                shape = drawerShape,
                color = drawerBackgroundColor,
                contentColor = drawerContentColor,
                elevation = drawerElevation
            ) {
                Column(Modifier.fillMaxSize(), content = drawerContent)
            }
        }
    }
}

/**
 * <a href="https://material.io/components/navigation-drawer#bottom-drawer" class="external" target="_blank">Material Design bottom navigation drawer</a>.
 *
 * Bottom navigation drawers are modal drawers that are anchored to the bottom of the screen instead
 * of the left or right edge. They are only used with bottom app bars.
 *
 * ![Bottom drawer image](https://developer.android.com/images/reference/androidx/compose/material/bottom-drawer.png)
 *
 * See [ModalDrawer] for a layout that introduces a classic from-the-side drawer.
 *
 * @sample androidx.compose.material.samples.BottomDrawerSample
 *
 * @param drawerContent composable that represents content inside the drawer
 * @param modifier optional [Modifier] for the entire component
 * @param drawerState state of the drawer
 * @param gesturesEnabled whether or not drawer can be interacted by gestures
 * @param drawerShape shape of the drawer sheet
 * @param drawerElevation drawer sheet elevation. This controls the size of the shadow below the
 * drawer sheet
 * @param drawerBackgroundColor background color to be used for the drawer sheet
 * @param drawerContentColor color of the content to use inside the drawer sheet. Defaults to
 * either the matching content color for [drawerBackgroundColor], or, if it is not a color from
 * the theme, this will keep the same value set above this Surface.
 * @param scrimColor color of the scrim that obscures content when the drawer is open. If the
 * color passed is [Color.Unspecified], then a scrim will no longer be applied and the bottom
 * drawer will not block interaction with the rest of the screen when visible.
 * @param content content of the rest of the UI
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BottomDrawer(
    drawerContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    drawerState: BottomDrawerState = rememberBottomDrawerState(Closed),
    gesturesEnabled: Boolean = true,
    drawerShape: Shape = DrawerDefaults.shape,
    drawerElevation: Dp = DrawerDefaults.Elevation,
    drawerBackgroundColor: Color = DrawerDefaults.backgroundColor,
    drawerContentColor: Color = contentColorFor(drawerBackgroundColor),
    scrimColor: Color = DrawerDefaults.scrimColor,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    BoxWithConstraints(modifier.fillMaxSize()) {
        val fullHeight = constraints.maxHeight.toFloat()
        val isLandscape = constraints.maxWidth > constraints.maxHeight
        val drawerConstraints = with(LocalDensity.current) {
            Modifier
                .sizeIn(
                    maxWidth = constraints.maxWidth.toDp(),
                    maxHeight = constraints.maxHeight.toDp()
                )
        }
        val nestedScroll = if (gesturesEnabled) {
            Modifier.nestedScroll(drawerState.nestedScrollConnection)
        } else {
            Modifier
        }
        val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

        val swipeable = Modifier
            .then(nestedScroll)
            .anchoredDraggable(
                state = drawerState.anchoredDraggableState,
                orientation = Orientation.Vertical,
                enabled = gesturesEnabled,
                reverseDirection = isRtl
            )

        Box(swipeable) {
            content()
            BottomDrawerScrim(
                color = scrimColor,
                onDismiss = {
                    if (
                        gesturesEnabled && drawerState.confirmStateChange(Closed)
                    ) {
                        scope.launch { drawerState.close() }
                    }
                },
                visible = drawerState.targetValue != Closed
            )
            val navigationMenu = getString(Strings.NavigationMenu)
            Surface(
                drawerConstraints
                    .onSizeChanged { drawerSize ->
                        val drawerHeight = drawerSize.height.toFloat()
                        val newAnchors = DraggableAnchors {
                            Closed at fullHeight
                            val peekHeight = fullHeight * BottomDrawerOpenFraction
                            if (drawerHeight > peekHeight || isLandscape) {
                                Open at peekHeight
                            }
                            if (drawerHeight > 0f) {
                                Expanded at max(0f, fullHeight - drawerHeight)
                            }
                        }
                        // If we are setting the anchors for the first time and have an anchor for
                        // the current (initial) value, prefer that
                        val hasAnchors = drawerState.anchoredDraggableState.anchors.size > 0
                        val newTarget = if (!hasAnchors &&
                            newAnchors.hasAnchorFor(drawerState.currentValue)
                        ) {
                            drawerState.currentValue
                        } else {
                            when (drawerState.targetValue) {
                                Closed -> Closed
                                Open, Expanded -> {
                                    val hasHalfExpandedState = newAnchors.hasAnchorFor(Open)
                                    val newTarget = if (hasHalfExpandedState) {
                                        Open
                                    } else {
                                        if (newAnchors.hasAnchorFor(Expanded)) Expanded else Closed
                                    }
                                    newTarget
                                }
                            }
                        }
                        drawerState.anchoredDraggableState.updateAnchors(newAnchors, newTarget)
                    }
                    .offset {
                        IntOffset(
                            x = 0,
                            y = drawerState
                                .requireOffset()
                                .roundToInt()
                        )
                    }
                    .semantics {
                        paneTitle = navigationMenu
                        if (drawerState.isOpen) {
                            // TODO(b/180101663) The action currently doesn't return the correct results
                            dismiss {
                                if (drawerState.confirmStateChange(Closed)) {
                                    scope.launch { drawerState.close() }
                                }; true
                            }
                        }
                    },
                shape = drawerShape,
                color = drawerBackgroundColor,
                contentColor = drawerContentColor,
                elevation = drawerElevation
            ) {
                Column(content = drawerContent)
            }
        }
    }
}

/**
 * Object to hold default values for [ModalDrawer] and [BottomDrawer]
 */
object DrawerDefaults {

    /**
     * Default animation spec used for [ModalDrawer] and [BottomDrawer] open and close animations,
     * as well as settling when a user lets go.
     */
    val AnimationSpec = TweenSpec<Float>(durationMillis = 256)

    /**
     * Default background color for drawer sheets
     */
    val backgroundColor: Color
        @Composable
        get() = MaterialTheme.colors.surface

    /**
     * Default elevation for drawer sheet as specified in material specs
     */
    val Elevation = 16.dp

    /**
     * Default shape for drawer sheets
     */
    val shape: Shape
        @Composable
        get() = MaterialTheme.shapes.large

    /**
     * Default color of the scrim that obscures content when the drawer is open
     */
    val scrimColor: Color
        @Composable
        get() = MaterialTheme.colors.onSurface.copy(alpha = ScrimOpacity)

    /**
     * Default alpha for scrim color
     */
    const val ScrimOpacity = 0.32f
}

private fun calculateFraction(a: Float, b: Float, pos: Float) =
    ((pos - a) / (b - a)).fastCoerceIn(0f, 1f)

@Composable
private fun BottomDrawerScrim(
    color: Color,
    onDismiss: () -> Unit,
    visible: Boolean
) {
    if (color.isSpecified) {
        val alpha by animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = TweenSpec()
        )
        val closeDrawer = getString(Strings.CloseDrawer)
        val dismissModifier = if (visible) {
            Modifier
                .pointerInput(onDismiss) {
                    detectTapGestures { onDismiss() }
                }
                .semantics(mergeDescendants = true) {
                    contentDescription = closeDrawer
                    onClick { onDismiss(); true }
                }
        } else {
            Modifier
        }

        Canvas(
            Modifier
                .fillMaxSize()
                .then(dismissModifier)
        ) {
            drawRect(color = color, alpha = alpha)
        }
    }
}

@Composable
private fun Scrim(
    open: Boolean,
    onClose: () -> Unit,
    fraction: () -> Float,
    color: Color
) {
    val closeDrawer = getString(Strings.CloseDrawer)
    val dismissDrawer = if (open) {
        Modifier
            .pointerInput(onClose) { detectTapGestures { onClose() } }
            .semantics(mergeDescendants = true) {
                contentDescription = closeDrawer
                onClick { onClose(); true }
            }
    } else {
        Modifier
    }

    Canvas(
        Modifier
            .fillMaxSize()
            .then(dismissDrawer)
    ) {
        drawRect(color, alpha = fraction())
    }
}

private val EndDrawerPadding = 56.dp
private val DrawerPositionalThreshold = 56.dp
private val DrawerVelocityThreshold = 400.dp

// TODO: b/177571613 this should be a proper decay settling
// this is taken from the DrawerLayout's DragViewHelper as a min duration.
private val AnimationSpec = TweenSpec<Float>(durationMillis = 256)

private const val BottomDrawerOpenFraction = 0.5f

@OptIn(ExperimentalMaterialApi::class)
private fun ConsumeSwipeWithinBottomSheetBoundsNestedScrollConnection(
    state: AnchoredDraggableState<*>
): NestedScrollConnection = object : NestedScrollConnection {
    val orientation: Orientation = Orientation.Vertical

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

    private fun Float.toOffset(): Offset = Offset(
        x = if (orientation == Orientation.Horizontal) this else 0f,
        y = if (orientation == Orientation.Vertical) this else 0f
    )

    @JvmName("velocityToFloat")
    private fun Velocity.toFloat() = if (orientation == Orientation.Horizontal) x else y

    @JvmName("offsetToFloat")
    private fun Offset.toFloat(): Float = if (orientation == Orientation.Horizontal) x else y
}
