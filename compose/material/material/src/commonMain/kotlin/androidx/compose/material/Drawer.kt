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
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
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
@ExperimentalMaterialApi
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

    internal val swipeableState = SwipeableV2State(
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
            return swipeableState.currentValue
        }

    /**
     * Whether the state is currently animating.
     */
    val isAnimationRunning: Boolean
        get() {
            return swipeableState.isAnimationRunning
        }

    /**
     * Open the drawer with animation and suspend until it if fully opened or animation has been
     * cancelled. This method will throw [CancellationException] if the animation is
     * interrupted
     *
     * @return the reason the open animation ended
     */
    suspend fun open() = swipeableState.animateTo(DrawerValue.Open)

    /**
     * Close the drawer with animation and suspend until it if fully closed or animation has been
     * cancelled. This method will throw [CancellationException] if the animation is
     * interrupted
     *
     * @return the reason the close animation ended
     */
    suspend fun close() = swipeableState.animateTo(DrawerValue.Closed)

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
        swipeableState.animateTo(targetValue)
    }

    /**
     * Set the state without any animation and suspend until it's set
     *
     * @param targetValue The new target value
     */
    suspend fun snapTo(targetValue: DrawerValue) {
        swipeableState.snapTo(targetValue)
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
        get() = swipeableState.targetValue

    /**
     * The current position (in pixels) of the drawer sheet, or null before the offset is
     * initialized.
     * @see [SwipeableV2State.offset] for more information.
     */
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:Suppress("AutoBoxing")
    @ExperimentalMaterialApi
    @get:ExperimentalMaterialApi
    val offset: Float?
        get() = swipeableState.offset

    internal fun requireOffset(): Float = swipeableState.requireOffset()

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
 */
@Suppress("NotCloseable", "Deprecation")
@ExperimentalMaterialApi
fun BottomDrawerState(
    initialValue: BottomDrawerValue,
    density: Density,
    confirmStateChange: (BottomDrawerValue) -> Boolean = { true }
) = BottomDrawerState(
    initialValue = initialValue,
    confirmStateChange = confirmStateChange
).also {
    it.density = density
}

/**
 * State of the [BottomDrawer] composable.
 *
 * @param initialValue The initial value of the state.
 * @param confirmStateChange Optional callback invoked to confirm or veto a pending state change.
 */
@Suppress("NotCloseable")
@ExperimentalMaterialApi
class BottomDrawerState @Deprecated(
    "This constructor is deprecated. Density must be provided by the component. Please use " +
        "the constructor that provides a [Density].",
    ReplaceWith(
        """
            BottomDrawerState(
                initialValue = initialValue,
                density =,
                confirmStateChange = confirmStateChange
            )
            """
    )
) constructor(
    initialValue: BottomDrawerValue,
    confirmStateChange: (BottomDrawerValue) -> Boolean = { true }
) {
    internal val swipeableState = SwipeableV2State(
        initialValue = initialValue,
        animationSpec = AnimationSpec,
        confirmValueChange = confirmStateChange,
        positionalThreshold = { with(requireDensity()) { DrawerPositionalThreshold.toPx() } },
        velocityThreshold = { with(requireDensity()) { DrawerVelocityThreshold.toPx() } },
    )

    /**
     * The target value. This is the closest value to the current offset (taking into account
     * positional thresholds). If no interactions like animations or drags are in progress, this
     * will be the current value.
     */
    val targetValue: BottomDrawerValue
        get() = swipeableState.targetValue

    /**
     * The current offset, or null if it has not been initialized yet.
     **/
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:Suppress("AutoBoxing")
    val offset: Float?
        get() = swipeableState.offset

    internal fun requireOffset(): Float = swipeableState.requireOffset()

    /**
     * The current value of the [BottomDrawerState].
     */
    val currentValue: BottomDrawerValue get() = swipeableState.currentValue

    /**
     * Whether the drawer is open, either in opened or expanded state.
     */
    val isOpen: Boolean
        get() = swipeableState.currentValue != Closed

    /**
     * Whether the drawer is closed.
     */
    val isClosed: Boolean
        get() = swipeableState.currentValue == Closed

    /**
     * Whether the drawer is expanded.
     */
    val isExpanded: Boolean
        get() = swipeableState.currentValue == Expanded

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
        swipeableState.animateTo(targetValue)
    }

    /**
     * Close the drawer with animation and suspend until it if fully closed or animation has been
     * cancelled.
     *
     * @throws [CancellationException] if the animation is interrupted
     *
     */
    suspend fun close() = swipeableState.animateTo(Closed)

    /**
     * Expand the drawer with animation and suspend until it if fully expanded or animation has
     * been cancelled.
     *
     * @throws [CancellationException] if the animation is interrupted
     *
     */
    suspend fun expand() = swipeableState.animateTo(Expanded)
    internal fun confirmStateChange(value: BottomDrawerValue): Boolean =
        swipeableState.confirmValueChange(value)

    private val isOpenEnabled: Boolean
        get() = swipeableState.hasAnchorForValue(Open)

    internal val nestedScrollConnection = ConsumeSwipeWithinBottomSheetBoundsNestedScrollConnection(
        swipeableState
    )

    internal var density: Density? = null

    private fun requireDensity() = requireNotNull(density) {
        "The density on BottomDrawerState ($this) was not set. Did you use BottomDrawer" +
            " with the BottomDrawer composable?"
    }

    companion object {
        /**
         * The default [Saver] implementation for [BottomDrawerState].
         */
        fun Saver(density: Density, confirmStateChange: (BottomDrawerValue) -> Boolean) =
            Saver<BottomDrawerState, BottomDrawerValue>(
                save = { it.swipeableState.currentValue },
                restore = { BottomDrawerState(it, density, confirmStateChange) }
            )

        /**
         * The default [Saver] implementation for [BottomDrawerState].
         */
        @Deprecated(
            message = "This function is deprecated. Please use the overload where Density is" +
                " provided.",
            replaceWith = ReplaceWith(
                "Saver(density, confirmValueChange)"
            )
        )
        @Suppress("Deprecation")
        fun Saver(confirmStateChange: (BottomDrawerValue) -> Boolean) =
            Saver<BottomDrawerState, BottomDrawerValue>(
                save = { it.swipeableState.currentValue },
                restore = { BottomDrawerState(it, confirmStateChange) }
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
 */
@Composable
@ExperimentalMaterialApi
fun rememberBottomDrawerState(
    initialValue: BottomDrawerValue,
    confirmStateChange: (BottomDrawerValue) -> Boolean = { true }
): BottomDrawerState {
    val density = LocalDensity.current
    return rememberSaveable(density, saver = BottomDrawerState.Saver(density, confirmStateChange)) {
        BottomDrawerState(initialValue, density, confirmStateChange)
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
    drawerShape: Shape = MaterialTheme.shapes.large,
    drawerElevation: Dp = DrawerDefaults.Elevation,
    drawerBackgroundColor: Color = MaterialTheme.colors.surface,
    drawerContentColor: Color = contentColorFor(drawerBackgroundColor),
    scrimColor: Color = DrawerDefaults.scrimColor,
    content: @Composable () -> Unit
) {
    // b/278692145 Remove this once deprecated methods without density are removed
    if (drawerState.density == null) {
        val density = LocalDensity.current
        SideEffect {
            drawerState.density = density
        }
    }
    val scope = rememberCoroutineScope()
    BoxWithConstraints(modifier.fillMaxSize()) {
        val modalDrawerConstraints = constraints
        // TODO : think about Infinite max bounds case
        if (!modalDrawerConstraints.hasBoundedWidth) {
            throw IllegalStateException("Drawer shouldn't have infinite width")
        }

        val minValue = -modalDrawerConstraints.maxWidth.toFloat()
        val maxValue = 0f

        val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        Box(
            Modifier
                .swipeableV2(
                    state = drawerState.swipeableState,
                    orientation = Orientation.Horizontal,
                    enabled = gesturesEnabled,
                    reverseDirection = isRtl
                )
                .swipeAnchors(
                    drawerState.swipeableState,
                    possibleValues = setOf(DrawerValue.Closed, DrawerValue.Open)
                ) { value, _ ->
                    when (value) {
                        DrawerValue.Closed -> minValue
                        DrawerValue.Open -> maxValue
                    }
                }
        ) {
            Box {
                content()
            }
            Scrim(
                open = drawerState.isOpen,
                onClose = {
                    if (
                        gesturesEnabled &&
                        drawerState.swipeableState.confirmValueChange(DrawerValue.Closed)
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
                                    drawerState.swipeableState
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
 * @param drawerState state of the drawer
 * @param modifier optional [Modifier] for the entire component
 * @param gesturesEnabled whether or not drawer can be interacted by gestures
 * @param drawerShape shape of the drawer sheet
 * @param drawerElevation drawer sheet elevation. This controls the size of the shadow below the
 * drawer sheet
 * @param drawerContent composable that represents content inside the drawer
 * @param drawerBackgroundColor background color to be used for the drawer sheet
 * @param drawerContentColor color of the content to use inside the drawer sheet. Defaults to
 * either the matching content color for [drawerBackgroundColor], or, if it is not a color from
 * the theme, this will keep the same value set above this Surface.
 * @param scrimColor color of the scrim that obscures content when the drawer is open. If the
 * color passed is [Color.Unspecified], then a scrim will no longer be applied and the bottom
 * drawer will not block interaction with the rest of the screen when visible.
 * @param content content of the rest of the UI
 *
 */
@Composable
@ExperimentalMaterialApi
fun BottomDrawer(
    drawerContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    drawerState: BottomDrawerState = rememberBottomDrawerState(Closed),
    gesturesEnabled: Boolean = true,
    drawerShape: Shape = MaterialTheme.shapes.large,
    drawerElevation: Dp = DrawerDefaults.Elevation,
    drawerBackgroundColor: Color = MaterialTheme.colors.surface,
    drawerContentColor: Color = contentColorFor(drawerBackgroundColor),
    scrimColor: Color = DrawerDefaults.scrimColor,
    content: @Composable () -> Unit
) {
    // b/278692145 Remove this once deprecated methods without density are removed
    if (drawerState.density == null) {
        val density = LocalDensity.current
        SideEffect {
            drawerState.density = density
        }
    }
    val scope = rememberCoroutineScope()

    BoxWithConstraints(modifier.fillMaxSize()) {
        val fullHeight = constraints.maxHeight.toFloat()
        // TODO(b/178630869) Proper landscape support
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
            .swipeableV2(
                state = drawerState.swipeableState,
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
                    .swipeAnchors(
                        drawerState.swipeableState,
                        possibleValues = setOf(
                            Closed,
                            Open,
                            Expanded
                        ),
                        anchorChangeHandler = remember(drawerState, scope) {
                            BottomDrawerAnchorChangeHandler(state = drawerState, scope = scope)
                        }
                    ) { value, layoutSize ->
                        val drawerHeight = layoutSize.height.toFloat()
                        calculateAnchors(
                            fullHeight = fullHeight,
                            drawerHeight = drawerHeight,
                            isLandscape = isLandscape
                        )[value]
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

@OptIn(ExperimentalMaterialApi::class)
private fun calculateAnchors(
    fullHeight: Float,
    drawerHeight: Float,
    isLandscape: Boolean
): Map<BottomDrawerValue, Float?> {
    val peekHeight = fullHeight * BottomDrawerOpenFraction
    val expandedHeight = max(0f, fullHeight - drawerHeight)
    return if (drawerHeight < peekHeight || isLandscape) {
        mapOf(
            Closed to fullHeight,
            Expanded to if (drawerHeight == 0f) null else expandedHeight
        )
    } else {
        mapOf(
            Closed to fullHeight,
            Open to peekHeight,
            Expanded to if (drawerHeight == 0f) null else expandedHeight
        )
    }
}

/**
 * Object to hold default values for [ModalDrawer] and [BottomDrawer]
 */
object DrawerDefaults {

    /**
     * Default Elevation for drawer sheet as specified in material specs
     */
    val Elevation = 16.dp

    val scrimColor: Color
        @Composable
        get() = MaterialTheme.colors.onSurface.copy(alpha = ScrimOpacity)

    /**
     * Default alpha for scrim color
     */
    const val ScrimOpacity = 0.32f
}

private fun calculateFraction(a: Float, b: Float, pos: Float) =
    ((pos - a) / (b - a)).coerceIn(0f, 1f)

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
    state: SwipeableV2State<*>
): NestedScrollConnection = object : NestedScrollConnection {
    val orientation: Orientation = Orientation.Vertical

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val delta = available.toFloat()
        return if (delta < 0 && source == NestedScrollSource.Drag) {
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
        return if (source == NestedScrollSource.Drag) {
            state.dispatchRawDelta(available.toFloat()).toOffset()
        } else {
            Offset.Zero
        }
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        val toFling = available.toFloat()
        val currentOffset = state.requireOffset()
        return if (toFling < 0 && currentOffset > state.minOffset) {
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

@OptIn(ExperimentalMaterialApi::class)
private fun BottomDrawerAnchorChangeHandler(
    state: BottomDrawerState,
    scope: CoroutineScope
) = AnchorChangeHandler<BottomDrawerValue> { previousTarget, previousAnchors, newAnchors ->
    fun animateTo(target: BottomDrawerValue, velocity: Float) {
        scope.launch {
            state.swipeableState.animateTo(
                target,
                velocity = velocity
            )
        }
    }

    fun snapTo(target: BottomDrawerValue) {
        val didSnapSynchronously = state.swipeableState.trySnapTo(target)
        if (!didSnapSynchronously) scope.launch {
            state.swipeableState.snapTo(
                target
            )
        }
    }

    val previousTargetOffset = previousAnchors[previousTarget]
    val newTarget = when (previousTarget) {
        Closed -> Closed
        Open, Expanded -> {
            val hasHalfExpandedState = newAnchors.containsKey(Open)
            val newTarget = if (hasHalfExpandedState) {
                Open
            } else {
                if (newAnchors.containsKey(Expanded)) Expanded else Closed
            }

            newTarget
        }
    }
    val newTargetOffset = newAnchors.getValue(newTarget)
    if (newTargetOffset != previousTargetOffset) {
        if (state.swipeableState.isAnimationRunning) {
            // Re-target the animation to the new offset if it changed
            animateTo(newTarget, state.swipeableState.lastVelocity)
        } else {
            // Snap to the new offset value of the target if no animation was running
            snapTo(newTarget)
        }
    }
}