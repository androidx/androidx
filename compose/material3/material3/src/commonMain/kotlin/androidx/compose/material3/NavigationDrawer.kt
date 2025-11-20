/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.snap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.internal.BackEventCompat
import androidx.compose.material3.internal.FloatProducer
import androidx.compose.material3.internal.PredictiveBack
import androidx.compose.material3.internal.PredictiveBackHandler
import androidx.compose.material3.internal.Strings
import androidx.compose.material3.internal.getString
import androidx.compose.material3.internal.systemBarsForVisualComponents
import androidx.compose.material3.tokens.ElevationTokens
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.material3.tokens.NavigationDrawerTokens
import androidx.compose.material3.tokens.ScrimTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxOfOrNull
import androidx.compose.ui.util.lerp
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Possible values of [DrawerState]. */
enum class DrawerValue {
    /** The state of the drawer when it is closed. */
    Closed,

    /** The state of the drawer when it is open. */
    Open,
}

/**
 * State of the [ModalNavigationDrawer] and [DismissibleNavigationDrawer] composable.
 *
 * @param initialValue The initial value of the state.
 * @param confirmStateChange Optional callback invoked to confirm or veto a pending state change.
 */
@Suppress("NotCloseable")
@Stable
class DrawerState(
    initialValue: DrawerValue,
    internal val confirmStateChange: (DrawerValue) -> Boolean = { true },
) {

    internal var anchoredDraggableMotionSpec: FiniteAnimationSpec<Float> =
        AnchoredDraggableDefaultAnimationSpec

    @Suppress("Deprecation")
    internal val anchoredDraggableState =
        AnchoredDraggableState(
            initialValue = initialValue,
            snapAnimationSpec = anchoredDraggableMotionSpec,
            decayAnimationSpec = AnchoredDraggableDefaults.DecayAnimationSpec,
            confirmValueChange = confirmStateChange,
            positionalThreshold = { distance: Float -> distance * DrawerPositionalThreshold },
            velocityThreshold = { with(requireDensity()) { DrawerVelocityThreshold.toPx() } },
        )

    /** Whether the drawer is open. */
    val isOpen: Boolean
        get() = currentValue == DrawerValue.Open

    /** Whether the drawer is closed. */
    val isClosed: Boolean
        get() = currentValue == DrawerValue.Closed

    /**
     * The current value of the state.
     *
     * If no swipe or animation is in progress, this corresponds to the start the drawer currently
     * in. If a swipe or an animation is in progress, this corresponds the state drawer was in
     * before the swipe or animation started.
     */
    val currentValue: DrawerValue
        get() {
            return anchoredDraggableState.settledValue
        }

    /** Whether the state is currently animating. */
    val isAnimationRunning: Boolean
        get() {
            return anchoredDraggableState.isAnimationRunning
        }

    /**
     * Open the drawer with animation and suspend until it if fully opened or animation has been
     * cancelled. This method will throw [CancellationException] if the animation is interrupted
     *
     * @return the reason the open animation ended
     */
    suspend fun open() =
        animateTo(targetValue = DrawerValue.Open, animationSpec = openDrawerMotionSpec)

    /**
     * Close the drawer with animation and suspend until it if fully closed or animation has been
     * cancelled. This method will throw [CancellationException] if the animation is interrupted
     *
     * @return the reason the close animation ended
     */
    suspend fun close() =
        animateTo(targetValue = DrawerValue.Closed, animationSpec = closeDrawerMotionSpec)

    /**
     * Set the state of the drawer with specific animation
     *
     * @param targetValue The new value to animate to.
     * @param anim The animation that will be used to animate to the new value.
     */
    @Deprecated(
        message =
            "This method has been replaced by the open and close methods. The animation " +
                "spec is now an implementation detail of ModalDrawer."
    )
    suspend fun animateTo(targetValue: DrawerValue, anim: AnimationSpec<Float>) {
        animateTo(targetValue = targetValue, animationSpec = anim)
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
     * If a swipe is in progress, this is the value that the Drawer would animate to if the swipe
     * finishes. If an animation is running, this is the target value of that animation. Finally, if
     * no swipe or animation is in progress, this is the same as the [currentValue].
     */
    val targetValue: DrawerValue
        get() = anchoredDraggableState.targetValue

    /**
     * The current position (in pixels) of the drawer sheet, or Float.NaN before the offset is
     * initialized.
     *
     * @see [AnchoredDraggableState.offset] for more information.
     */
    @Deprecated(
        message =
            "Please access the offset through currentOffset, which returns the value " +
                "directly instead of wrapping it in a state object.",
        replaceWith = ReplaceWith("currentOffset"),
    )
    val offset: State<Float> =
        object : State<Float> {
            override val value: Float
                get() = anchoredDraggableState.offset
        }

    /**
     * The current position (in pixels) of the drawer sheet, or Float.NaN before the offset is
     * initialized.
     *
     * @see [AnchoredDraggableState.offset] for more information.
     */
    val currentOffset: Float
        get() = anchoredDraggableState.offset

    internal var density: Density? by mutableStateOf(null)

    internal var openDrawerMotionSpec: FiniteAnimationSpec<Float> = snap()

    internal var closeDrawerMotionSpec: FiniteAnimationSpec<Float> = snap()

    private fun requireDensity() =
        requireNotNull(density) {
            "The density on DrawerState ($this) was not set. Did you use DrawerState" +
                " with the ModalNavigationDrawer or DismissibleNavigationDrawer composables?"
        }

    internal fun requireOffset(): Float = anchoredDraggableState.requireOffset()

    private suspend fun animateTo(
        targetValue: DrawerValue,
        animationSpec: AnimationSpec<Float>,
        velocity: Float = anchoredDraggableState.lastVelocity,
    ) {
        anchoredDraggableState.anchoredDrag(targetValue = targetValue) { anchors, latestTarget ->
            val targetOffset = anchors.positionOf(latestTarget)
            if (!targetOffset.isNaN()) {
                var prev = if (currentOffset.isNaN()) 0f else currentOffset
                animate(prev, targetOffset, velocity, animationSpec) { value, velocity ->
                    // Our onDrag coerces the value within the bounds, but an animation may
                    // overshoot, for example a spring animation or an overshooting interpolator
                    // We respect the user's intention and allow the overshoot, but still use
                    // DraggableState's drag for its mutex.
                    dragTo(value, velocity)
                    prev = value
                }
            }
        }
    }

    companion object {
        /** The default [Saver] implementation for [DrawerState]. */
        fun Saver(confirmStateChange: (DrawerValue) -> Boolean) =
            Saver<DrawerState, DrawerValue>(
                save = { it.currentValue },
                restore = { DrawerState(it, confirmStateChange) },
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
    confirmStateChange: (DrawerValue) -> Boolean = { true },
): DrawerState {
    return rememberSaveable(saver = DrawerState.Saver(confirmStateChange)) {
        DrawerState(initialValue, confirmStateChange)
    }
}

/**
 * [Material Design navigation drawer](https://m3.material.io/components/navigation-drawer/overview)
 *
 * Navigation drawers provide ergonomic access to destinations in an app.
 *
 * Modal navigation drawers block interaction with the rest of an app’s content with a scrim. They
 * are elevated above most of the app’s UI and don’t affect the screen’s layout grid.
 *
 * ![Navigation drawer
 * image](https://developer.android.com/images/reference/androidx/compose/material3/navigation-drawer.png)
 *
 * @sample androidx.compose.material3.samples.ModalNavigationDrawerSample
 * @param drawerContent content inside this drawer
 * @param modifier the [Modifier] to be applied to this drawer
 * @param drawerState state of the drawer
 * @param gesturesEnabled whether or not the drawer can be interacted by gestures
 * @param scrimColor color of the scrim that obscures content when the drawer is open
 * @param content content of the rest of the UI
 */
@Composable
fun ModalNavigationDrawer(
    drawerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    gesturesEnabled: Boolean = true,
    scrimColor: Color = DrawerDefaults.scrimColor,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val navigationMenu = getString(Strings.NavigationMenu)
    val density = LocalDensity.current
    var anchorsInitialized by remember { mutableStateOf(false) }
    var minValue by remember(density) { mutableFloatStateOf(0f) }
    val maxValue = 0f
    val focusRequester = remember { FocusRequester() }

    // TODO Load the motionScheme tokens from the component tokens file
    val anchoredDraggableMotion: FiniteAnimationSpec<Float> =
        MotionSchemeKeyTokens.DefaultSpatial.value()
    val openMotion: FiniteAnimationSpec<Float> = MotionSchemeKeyTokens.DefaultSpatial.value()
    val closeMotion: FiniteAnimationSpec<Float> = MotionSchemeKeyTokens.FastEffects.value()

    SideEffect {
        drawerState.density = density
        drawerState.openDrawerMotionSpec = openMotion
        drawerState.closeDrawerMotionSpec = closeMotion
        drawerState.anchoredDraggableMotionSpec = anchoredDraggableMotion
    }

    LaunchedEffect(drawerState.isOpen) {
        if (drawerState.isOpen) {
            // Keyboard focus should go to first element of the drawer when it opens
            focusRequester.requestFocus()
        }
    }

    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Box(
        modifier
            .fillMaxSize()
            .anchoredDraggable(
                state = drawerState.anchoredDraggableState,
                orientation = Orientation.Horizontal,
                enabled = gesturesEnabled,
                reverseDirection = isRtl,
            )
    ) {
        Box { content() }
        Scrim(
            open = drawerState.isOpen,
            onClose = {
                if (gesturesEnabled && drawerState.confirmStateChange(DrawerValue.Closed)) {
                    scope.launch { drawerState.close() }
                }
            },
            fraction = { calculateFraction(minValue, maxValue, drawerState.requireOffset()) },
            color = scrimColor,
        )
        Layout(
            content = drawerContent,
            modifier =
                Modifier.offset {
                        drawerState.currentOffset.let { offset ->
                            val offsetX =
                                when {
                                    !offset.isNaN() -> offset.roundToInt()
                                    // If offset is NaN, set offset based on open/closed state
                                    drawerState.isOpen -> 0
                                    else -> -DrawerDefaults.MaximumDrawerWidth.roundToPx()
                                }
                            IntOffset(offsetX, 0)
                        }
                    }
                    .semantics {
                        paneTitle = navigationMenu
                        if (drawerState.isOpen) {
                            dismiss {
                                if (drawerState.confirmStateChange(DrawerValue.Closed)) {
                                    scope.launch { drawerState.close() }
                                }
                                true
                            }
                        }
                    }
                    .onKeyEvent {
                        // Drawer should close via escape key.
                        if (
                            drawerState.isOpen &&
                                it.type == KeyEventType.KeyUp &&
                                it.key == Key.Escape
                        ) {
                            scope.launch { drawerState.close() }
                            return@onKeyEvent true
                        }
                        return@onKeyEvent false
                    }
                    .focusRequester(focusRequester),
        ) { measurables, constraints ->
            val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
            val placeables = measurables.fastMap { it.measure(looseConstraints) }
            val width = placeables.fastMaxOfOrNull { it.width } ?: 0
            val height = placeables.fastMaxOfOrNull { it.height } ?: 0

            layout(width, height) {
                val currentClosedAnchor =
                    drawerState.anchoredDraggableState.anchors.positionOf(DrawerValue.Closed)
                val calculatedClosedAnchor = -width.toFloat()

                if (!anchorsInitialized || currentClosedAnchor != calculatedClosedAnchor) {
                    if (!anchorsInitialized) {
                        anchorsInitialized = true
                    }
                    minValue = calculatedClosedAnchor
                    drawerState.anchoredDraggableState.updateAnchors(
                        DraggableAnchors {
                            DrawerValue.Closed at minValue
                            DrawerValue.Open at maxValue
                        }
                    )
                }
                val isDrawerVisible =
                    calculateFraction(minValue, maxValue, drawerState.requireOffset()) != 0f
                if (isDrawerVisible) {
                    // Only place the drawer when it's visible so that keyboard focus doesn't
                    // navigate to an offscreen element.
                    placeables.fastForEach { it.placeRelative(0, 0) }
                }
            }
        }
    }
}

/**
 * [Material Design navigation drawer](https://m3.material.io/components/navigation-drawer/overview)
 *
 * Navigation drawers provide ergonomic access to destinations in an app. They’re often next to app
 * content and affect the screen’s layout grid.
 *
 * ![Navigation drawer
 * image](https://developer.android.com/images/reference/androidx/compose/material3/navigation-drawer.png)
 *
 * Dismissible standard drawers can be used for layouts that prioritize content (such as a photo
 * gallery) or for apps where users are unlikely to switch destinations often. They should use a
 * visible navigation menu icon to open and close the drawer.
 *
 * @sample androidx.compose.material3.samples.DismissibleNavigationDrawerSample
 * @param drawerContent content inside this drawer
 * @param modifier the [Modifier] to be applied to this drawer
 * @param drawerState state of the drawer
 * @param gesturesEnabled whether or not the drawer can be interacted by gestures
 * @param content content of the rest of the UI
 */
@Composable
fun DismissibleNavigationDrawer(
    drawerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    gesturesEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    var anchorsInitialized by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val focusRequester = remember { FocusRequester() }

    // TODO Load the motionScheme tokens from the component tokens file
    val openMotion: FiniteAnimationSpec<Float> = MotionSchemeKeyTokens.DefaultSpatial.value()
    val closeMotion: FiniteAnimationSpec<Float> = MotionSchemeKeyTokens.FastEffects.value()

    SideEffect {
        drawerState.density = density
        drawerState.openDrawerMotionSpec = openMotion
        drawerState.closeDrawerMotionSpec = closeMotion
    }

    LaunchedEffect(drawerState.isOpen) {
        if (drawerState.isOpen) {
            // Keyboard focus should go to first element of the drawer when it opens
            focusRequester.requestFocus()
        }
    }

    val scope = rememberCoroutineScope()
    val navigationMenu = getString(Strings.NavigationMenu)

    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Box(
        modifier.anchoredDraggable(
            state = drawerState.anchoredDraggableState,
            orientation = Orientation.Horizontal,
            enabled = gesturesEnabled,
            reverseDirection = isRtl,
        )
    ) {
        Layout(
            content = {
                Box(
                    Modifier.semantics {
                            paneTitle = navigationMenu
                            if (drawerState.isOpen) {
                                dismiss {
                                    if (drawerState.confirmStateChange(DrawerValue.Closed)) {
                                        scope.launch { drawerState.close() }
                                    }
                                    true
                                }
                            }
                        }
                        .onKeyEvent {
                            // Drawer should close via escape key.
                            if (
                                drawerState.isOpen &&
                                    it.type == KeyEventType.KeyUp &&
                                    it.key == Key.Escape
                            ) {
                                scope.launch { drawerState.close() }
                                return@onKeyEvent true
                            }
                            return@onKeyEvent false
                        }
                        .focusRequester(focusRequester)
                ) {
                    drawerContent()
                }
                Box { content() }
            }
        ) { measurables, constraints ->
            val sheetPlaceable = measurables[0].measure(constraints)
            val contentPlaceable = measurables[1].measure(constraints)
            layout(contentPlaceable.width, contentPlaceable.height) {
                val currentClosedAnchor =
                    drawerState.anchoredDraggableState.anchors.positionOf(DrawerValue.Closed)
                val calculatedClosedAnchor = -sheetPlaceable.width.toFloat()

                if (!anchorsInitialized || currentClosedAnchor != calculatedClosedAnchor) {
                    if (!anchorsInitialized) {
                        anchorsInitialized = true
                    }
                    drawerState.anchoredDraggableState.updateAnchors(
                        DraggableAnchors {
                            DrawerValue.Closed at calculatedClosedAnchor
                            DrawerValue.Open at 0f
                        }
                    )
                }

                val contentX = sheetPlaceable.width + drawerState.requireOffset().roundToInt()
                contentPlaceable.placeRelative(contentX, 0)
                // The drawer is visible when the content has been offset.
                if (contentX != 0) {
                    // Only place the drawer when it's visible so that keyboard focus doesn't
                    // navigate to an offscreen element.
                    sheetPlaceable.placeRelative(drawerState.requireOffset().roundToInt(), 0)
                }
            }
        }
    }
}

/**
 * [Material Design navigation permanent
 * drawer](https://m3.material.io/components/navigation-drawer/overview)
 *
 * Navigation drawers provide ergonomic access to destinations in an app. They’re often next to app
 * content and affect the screen’s layout grid.
 *
 * ![Navigation drawer
 * image](https://developer.android.com/images/reference/androidx/compose/material3/navigation-drawer.png)
 *
 * The permanent navigation drawer is always visible and usually used for frequently switching
 * destinations. On mobile screens, use [ModalNavigationDrawer] instead.
 *
 * @sample androidx.compose.material3.samples.PermanentNavigationDrawerSample
 * @param drawerContent content inside this drawer
 * @param modifier the [Modifier] to be applied to this drawer
 * @param content content of the rest of the UI
 */
@Composable
fun PermanentNavigationDrawer(
    drawerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Row(modifier.fillMaxSize()) {
        drawerContent()
        Box { content() }
    }
}

/**
 * Content inside of a modal navigation drawer.
 *
 * Note: This version of [ModalDrawerSheet] does not handle back by default. For automatic back
 * handling and predictive back animations on Android 14+, use the [ModalDrawerSheet] that accepts
 * `drawerState` as a param.
 *
 * @param modifier the [Modifier] to be applied to this drawer's content
 * @param drawerShape defines the shape of this drawer's container
 * @param drawerContainerColor the color used for the background of this drawer. Use
 *   [Color.Transparent] to have no color.
 * @param drawerContentColor the preferred color for content inside this drawer. Defaults to either
 *   the matching content color for [drawerContainerColor], or to the current [LocalContentColor] if
 *   [drawerContainerColor] is not a color from the theme.
 * @param drawerTonalElevation when [drawerContainerColor] is [ColorScheme.surface], a translucent
 *   primary color overlay is applied on top of the container. A higher tonal elevation value will
 *   result in a darker color in light theme and lighter color in dark theme. See also: [Surface].
 * @param windowInsets a window insets for the sheet.
 * @param content content inside of a modal navigation drawer
 */
@Composable
fun ModalDrawerSheet(
    modifier: Modifier = Modifier,
    drawerShape: Shape = DrawerDefaults.shape,
    drawerContainerColor: Color = DrawerDefaults.modalContainerColor,
    drawerContentColor: Color = contentColorFor(drawerContainerColor),
    drawerTonalElevation: Dp = DrawerDefaults.ModalDrawerElevation,
    windowInsets: WindowInsets = DrawerDefaults.windowInsets,
    content: @Composable ColumnScope.() -> Unit,
) {
    DrawerSheet(
        drawerPredictiveBackState = null,
        windowInsets = windowInsets,
        modifier = modifier,
        drawerShape = drawerShape,
        drawerContainerColor = drawerContainerColor,
        drawerContentColor = drawerContentColor,
        drawerTonalElevation = drawerTonalElevation,
        content = content,
    )
}

/**
 * Content inside of a modal navigation drawer.
 *
 * Note: This version of [ModalDrawerSheet] requires a [drawerState] to be provided and will handle
 * back by default for all Android versions, as well as animate during predictive back on Android
 * 14+.
 *
 * @param drawerState state of the drawer
 * @param modifier the [Modifier] to be applied to this drawer's content
 * @param drawerShape defines the shape of this drawer's container
 * @param drawerContainerColor the color used for the background of this drawer. Use
 *   [Color.Transparent] to have no color.
 * @param drawerContentColor the preferred color for content inside this drawer. Defaults to either
 *   the matching content color for [drawerContainerColor], or to the current [LocalContentColor] if
 *   [drawerContainerColor] is not a color from the theme.
 * @param drawerTonalElevation when [drawerContainerColor] is [ColorScheme.surface], a translucent
 *   primary color overlay is applied on top of the container. A higher tonal elevation value will
 *   result in a darker color in light theme and lighter color in dark theme. See also: [Surface].
 * @param windowInsets a window insets for the sheet.
 * @param content content inside of a modal navigation drawer
 */
@Composable
fun ModalDrawerSheet(
    drawerState: DrawerState,
    modifier: Modifier = Modifier,
    drawerShape: Shape = DrawerDefaults.shape,
    drawerContainerColor: Color = DrawerDefaults.modalContainerColor,
    drawerContentColor: Color = contentColorFor(drawerContainerColor),
    drawerTonalElevation: Dp = DrawerDefaults.ModalDrawerElevation,
    windowInsets: WindowInsets = DrawerDefaults.windowInsets,
    content: @Composable ColumnScope.() -> Unit,
) {
    DrawerPredictiveBackHandler(drawerState) { drawerPredictiveBackState ->
        DrawerSheet(
            drawerPredictiveBackState = drawerPredictiveBackState,
            windowInsets = windowInsets,
            modifier = modifier,
            drawerShape = drawerShape,
            drawerContainerColor = drawerContainerColor,
            drawerContentColor = drawerContentColor,
            drawerTonalElevation = drawerTonalElevation,
            drawerOffset = { drawerState.anchoredDraggableState.offset },
            content = content,
        )
    }
}

/**
 * Content inside of a dismissible navigation drawer.
 *
 * Note: This version of [DismissibleDrawerSheet] does not handle back by default. For automatic
 * back handling and predictive back animations on Android 14+, use the [DismissibleDrawerSheet]
 * that accepts `drawerState` as a param.
 *
 * @param modifier the [Modifier] to be applied to this drawer's content
 * @param drawerShape defines the shape of this drawer's container
 * @param drawerContainerColor the color used for the background of this drawer. Use
 *   [Color.Transparent] to have no color.
 * @param drawerContentColor the preferred color for content inside this drawer. Defaults to either
 *   the matching content color for [drawerContainerColor], or to the current [LocalContentColor] if
 *   [drawerContainerColor] is not a color from the theme.
 * @param drawerTonalElevation when [drawerContainerColor] is [ColorScheme.surface], a translucent
 *   primary color overlay is applied on top of the container. A higher tonal elevation value will
 *   result in a darker color in light theme and lighter color in dark theme. See also: [Surface].
 * @param windowInsets a window insets for the sheet.
 * @param content content inside of a dismissible navigation drawer
 */
@Composable
fun DismissibleDrawerSheet(
    modifier: Modifier = Modifier,
    drawerShape: Shape = RectangleShape,
    drawerContainerColor: Color = DrawerDefaults.standardContainerColor,
    drawerContentColor: Color = contentColorFor(drawerContainerColor),
    drawerTonalElevation: Dp = DrawerDefaults.DismissibleDrawerElevation,
    windowInsets: WindowInsets = DrawerDefaults.windowInsets,
    content: @Composable ColumnScope.() -> Unit,
) {
    DrawerSheet(
        drawerPredictiveBackState = null,
        windowInsets = windowInsets,
        modifier = modifier,
        drawerShape = drawerShape,
        drawerContainerColor = drawerContainerColor,
        drawerContentColor = drawerContentColor,
        drawerTonalElevation = drawerTonalElevation,
        content = content,
    )
}

/**
 * Content inside of a dismissible navigation drawer.
 *
 * Note: This version of [DismissibleDrawerSheet] requires a [drawerState] to be provided and will
 * handle back by default for all Android versions, as well as animate during predictive back on
 * Android 14+.
 *
 * @param drawerState state of the drawer
 * @param modifier the [Modifier] to be applied to this drawer's content
 * @param drawerShape defines the shape of this drawer's container
 * @param drawerContainerColor the color used for the background of this drawer. Use
 *   [Color.Transparent] to have no color.
 * @param drawerContentColor the preferred color for content inside this drawer. Defaults to either
 *   the matching content color for [drawerContainerColor], or to the current [LocalContentColor] if
 *   [drawerContainerColor] is not a color from the theme.
 * @param drawerTonalElevation when [drawerContainerColor] is [ColorScheme.surface], a translucent
 *   primary color overlay is applied on top of the container. A higher tonal elevation value will
 *   result in a darker color in light theme and lighter color in dark theme. See also: [Surface].
 * @param windowInsets a window insets for the sheet.
 * @param content content inside of a dismissible navigation drawer
 */
@Composable
fun DismissibleDrawerSheet(
    drawerState: DrawerState,
    modifier: Modifier = Modifier,
    drawerShape: Shape = RectangleShape,
    drawerContainerColor: Color = DrawerDefaults.standardContainerColor,
    drawerContentColor: Color = contentColorFor(drawerContainerColor),
    drawerTonalElevation: Dp = DrawerDefaults.DismissibleDrawerElevation,
    windowInsets: WindowInsets = DrawerDefaults.windowInsets,
    content: @Composable ColumnScope.() -> Unit,
) {
    DrawerPredictiveBackHandler(drawerState) { drawerPredictiveBackState ->
        DrawerSheet(
            drawerPredictiveBackState = drawerPredictiveBackState,
            windowInsets = windowInsets,
            modifier = modifier,
            drawerShape = drawerShape,
            drawerContainerColor = drawerContainerColor,
            drawerContentColor = drawerContentColor,
            drawerTonalElevation = drawerTonalElevation,
            drawerOffset = { drawerState.anchoredDraggableState.offset },
            content = content,
        )
    }
}

/**
 * Content inside of a permanent navigation drawer.
 *
 * @param modifier the [Modifier] to be applied to this drawer's content
 * @param drawerShape defines the shape of this drawer's container
 * @param drawerContainerColor the color used for the background of this drawer. Use
 *   [Color.Transparent] to have no color.
 * @param drawerContentColor the preferred color for content inside this drawer. Defaults to either
 *   the matching content color for [drawerContainerColor], or to the current [LocalContentColor] if
 *   [drawerContainerColor] is not a color from the theme.
 * @param drawerTonalElevation when [drawerContainerColor] is [ColorScheme.surface], a translucent
 *   primary color overlay is applied on top of the container. A higher tonal elevation value will
 *   result in a darker color in light theme and lighter color in dark theme. See also: [Surface].
 * @param windowInsets a window insets for the sheet.
 * @param content content inside a permanent navigation drawer
 */
@Composable
fun PermanentDrawerSheet(
    modifier: Modifier = Modifier,
    drawerShape: Shape = RectangleShape,
    drawerContainerColor: Color = DrawerDefaults.standardContainerColor,
    drawerContentColor: Color = contentColorFor(drawerContainerColor),
    drawerTonalElevation: Dp = DrawerDefaults.PermanentDrawerElevation,
    windowInsets: WindowInsets = DrawerDefaults.windowInsets,
    content: @Composable ColumnScope.() -> Unit,
) {
    val navigationMenu = getString(Strings.NavigationMenu)
    DrawerSheet(
        drawerPredictiveBackState = null,
        windowInsets = windowInsets,
        modifier = modifier.semantics { paneTitle = navigationMenu },
        drawerShape = drawerShape,
        drawerContainerColor = drawerContainerColor,
        drawerContentColor = drawerContentColor,
        drawerTonalElevation = drawerTonalElevation,
        content = content,
    )
}

@Composable
internal fun DrawerSheet(
    drawerPredictiveBackState: DrawerPredictiveBackState?,
    windowInsets: WindowInsets,
    modifier: Modifier = Modifier,
    drawerShape: Shape = RectangleShape,
    drawerContainerColor: Color = DrawerDefaults.standardContainerColor,
    drawerContentColor: Color = contentColorFor(drawerContainerColor),
    drawerTonalElevation: Dp = DrawerDefaults.PermanentDrawerElevation,
    drawerOffset: FloatProducer = FloatProducer { 0F },
    content: @Composable ColumnScope.() -> Unit,
) {
    val density = LocalDensity.current
    val maxWidth = NavigationDrawerTokens.ContainerWidth
    val maxWidthPx = with(density) { maxWidth.toPx() }
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val predictiveBackDrawerContainerModifier =
        if (drawerPredictiveBackState != null) {
            Modifier.predictiveBackDrawerContainer(drawerPredictiveBackState, isRtl)
        } else {
            Modifier
        }
    Surface(
        modifier =
            modifier
                .sizeIn(minWidth = MinimumDrawerWidth, maxWidth = maxWidth)
                // Scale up the Surface horizontally in case the drawer offset it greater than zero.
                // This is done to avoid showing a gap when the drawer opens and bounces when it's
                // applied with a bouncy motion. Note that the content inside the Surface is scaled
                // back down to maintain its aspect ratio (see below).
                .horizontalScaleUp(
                    drawerOffset = drawerOffset,
                    drawerWidth = maxWidthPx,
                    isRtl = isRtl,
                )
                .then(predictiveBackDrawerContainerModifier)
                .fillMaxHeight(),
        shape = drawerShape,
        color = drawerContainerColor,
        contentColor = drawerContentColor,
        tonalElevation = drawerTonalElevation,
    ) {
        val predictiveBackDrawerChildModifier =
            if (drawerPredictiveBackState != null)
                Modifier.predictiveBackDrawerChild(drawerPredictiveBackState, isRtl)
            else Modifier
        Column(
            Modifier.sizeIn(minWidth = MinimumDrawerWidth, maxWidth = maxWidth)
                // Scale the content down in case the drawer offset is greater than one. The
                // wrapping Surface is scaled up, so this is done to maintain the content's aspect
                // ratio.
                .horizontalScaleDown(
                    drawerOffset = drawerOffset,
                    drawerWidth = maxWidthPx,
                    isRtl = isRtl,
                )
                .then(predictiveBackDrawerChildModifier)
                .windowInsetsPadding(windowInsets),
            content = content,
        )
    }
}

/**
 * A [Modifier] that scales up the drawing layer on the X axis in case the [drawerOffset] is greater
 * than zero. The scaling will ensure that there is no visible gap between the drawer and the edge
 * of the screen in case the drawer bounces when it opens due to a more expressive motion setting.
 *
 * A [horizontalScaleDown] should be applied to the content of the drawer to maintain the content
 * aspect ratio as the container scales up.
 *
 * @see horizontalScaleDown
 */
private fun Modifier.horizontalScaleUp(
    drawerOffset: FloatProducer,
    drawerWidth: Float,
    isRtl: Boolean,
) = graphicsLayer {
    val offset = drawerOffset()
    scaleX = if (offset > 0f) 1f + offset / drawerWidth else 1f
    transformOrigin = TransformOrigin(if (isRtl) 0f else 1f, 0.5f)
}

/**
 * A [Modifier] that scales down the drawing layer on the X axis in case the [drawerOffset] is
 * greater than zero. This modifier should be applied to the content inside a component that was
 * scaled up with a [horizontalScaleUp] modifier. It will ensure that the content maintains its
 * aspect ratio as the container scales up.
 *
 * @see horizontalScaleUp
 */
private fun Modifier.horizontalScaleDown(
    drawerOffset: FloatProducer,
    drawerWidth: Float,
    isRtl: Boolean,
) = graphicsLayer {
    val offset = drawerOffset()
    scaleX = if (offset > 0f) 1 / (1f + offset / drawerWidth) else 1f
    transformOrigin = TransformOrigin(if (isRtl) 0f else 1f, 0f)
}

private fun Modifier.predictiveBackDrawerContainer(
    drawerPredictiveBackState: DrawerPredictiveBackState,
    isRtl: Boolean,
) = graphicsLayer {
    scaleX = calculatePredictiveBackScaleX(drawerPredictiveBackState)
    scaleY = calculatePredictiveBackScaleY(drawerPredictiveBackState)
    transformOrigin = TransformOrigin(if (isRtl) 1f else 0f, 0.5f)
}

private fun Modifier.predictiveBackDrawerChild(
    drawerPredictiveBackState: DrawerPredictiveBackState,
    isRtl: Boolean,
) = graphicsLayer {
    // Preserve the original aspect ratio and container alignment of the child
    // content, and add content margins.
    val containerScaleX = calculatePredictiveBackScaleX(drawerPredictiveBackState)
    val containerScaleY = calculatePredictiveBackScaleY(drawerPredictiveBackState)
    scaleX = if (containerScaleX != 0f) containerScaleY / containerScaleX else 1f
    transformOrigin = TransformOrigin(if (isRtl) 0f else 1f, 0f)
}

private fun GraphicsLayerScope.calculatePredictiveBackScaleX(
    drawerPredictiveBackState: DrawerPredictiveBackState
): Float {
    val width = size.width
    return if (width.isNaN() || width == 0f) {
        1f
    } else {
        val scaleXDirection = if (drawerPredictiveBackState.swipeEdgeMatchesDrawer) 1 else -1
        1f + drawerPredictiveBackState.scaleXDistance * scaleXDirection / width
    }
}

private fun GraphicsLayerScope.calculatePredictiveBackScaleY(
    drawerPredictiveBackState: DrawerPredictiveBackState
): Float {
    val height = size.height
    return if (height.isNaN() || height == 0f) {
        1f
    } else {
        1f - drawerPredictiveBackState.scaleYDistance / height
    }
}

/**
 * Registers a [PredictiveBackHandler] and provides animation values in [DrawerPredictiveBackState]
 * based on back progress.
 *
 * @param drawerState state of the drawer
 * @param content content of the rest of the UI
 */
@Composable
internal fun DrawerPredictiveBackHandler(
    drawerState: DrawerState,
    content: @Composable (DrawerPredictiveBackState) -> Unit,
) {
    val drawerPredictiveBackState = remember { DrawerPredictiveBackState() }
    val scope = rememberCoroutineScope()
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val maxScaleXDistanceGrow: Float
    val maxScaleXDistanceShrink: Float
    val maxScaleYDistance: Float
    with(LocalDensity.current) {
        maxScaleXDistanceGrow = PredictiveBackDrawerMaxScaleXDistanceGrow.toPx()
        maxScaleXDistanceShrink = PredictiveBackDrawerMaxScaleXDistanceShrink.toPx()
        maxScaleYDistance = PredictiveBackDrawerMaxScaleYDistance.toPx()
    }

    PredictiveBackHandler(enabled = drawerState.isOpen) { progress ->
        try {
            progress.collect { backEvent ->
                drawerPredictiveBackState.update(
                    PredictiveBack.transform(backEvent.progress),
                    backEvent.swipeEdge == BackEventCompat.EDGE_LEFT,
                    isRtl,
                    maxScaleXDistanceGrow,
                    maxScaleXDistanceShrink,
                    maxScaleYDistance,
                )
            }
        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
            drawerPredictiveBackState.clear()
        } finally {
            if (drawerPredictiveBackState.swipeEdgeMatchesDrawer) {
                // If swipe edge matches drawer gravity and we've stretched the drawer horizontally,
                // un-stretch it smoothly so that it hides completely during the drawer close.
                scope.launch {
                    animate(
                        initialValue = drawerPredictiveBackState.scaleXDistance,
                        targetValue = 0f,
                    ) { value, _ ->
                        drawerPredictiveBackState.scaleXDistance = value
                    }
                    drawerPredictiveBackState.clear()
                }
            }
            drawerState.close()
        }
    }

    LaunchedEffect(drawerState.isClosed) {
        if (drawerState.isClosed) {
            drawerPredictiveBackState.clear()
        }
    }

    content(drawerPredictiveBackState)
}

/** Object to hold default values for [ModalNavigationDrawer] */
object DrawerDefaults {
    /** Default Elevation for drawer container in the [ModalNavigationDrawer]. */
    val ModalDrawerElevation = ElevationTokens.Level0

    /** Default Elevation for drawer container in the [PermanentNavigationDrawer]. */
    val PermanentDrawerElevation = NavigationDrawerTokens.StandardContainerElevation

    /** Default Elevation for drawer container in the [DismissibleNavigationDrawer]. */
    val DismissibleDrawerElevation = NavigationDrawerTokens.StandardContainerElevation

    /** Default shape for a navigation drawer. */
    val shape: Shape
        @Composable get() = NavigationDrawerTokens.ContainerShape.value

    /** Default color of the scrim that obscures content when the drawer is open */
    val scrimColor: Color
        @Composable get() = ScrimTokens.ContainerColor.value.copy(ScrimTokens.ContainerOpacity)

    /** Default container color for a navigation drawer */
    @Deprecated(
        message = "Please use standardContainerColor or modalContainerColor instead.",
        replaceWith = ReplaceWith("standardContainerColor"),
        level = DeprecationLevel.WARNING,
    )
    val containerColor: Color
        @Composable get() = NavigationDrawerTokens.StandardContainerColor.value

    /**
     * Default container color for a [DismissibleNavigationDrawer] and [PermanentNavigationDrawer]
     */
    val standardContainerColor: Color
        @Composable get() = NavigationDrawerTokens.StandardContainerColor.value

    /** Default container color for a [ModalNavigationDrawer] */
    val modalContainerColor: Color
        @Composable get() = NavigationDrawerTokens.ModalContainerColor.value

    /** Default and maximum width of a navigation drawer */
    val MaximumDrawerWidth = NavigationDrawerTokens.ContainerWidth

    /** Default window insets for drawer sheets */
    val windowInsets: WindowInsets
        @Composable
        get() =
            WindowInsets.systemBarsForVisualComponents.only(
                WindowInsetsSides.Vertical + WindowInsetsSides.Start
            )
}

/**
 * Material Design navigation drawer item.
 *
 * A [NavigationDrawerItem] represents a destination within drawers, either [ModalNavigationDrawer],
 * [PermanentNavigationDrawer] or [DismissibleNavigationDrawer].
 *
 * @sample androidx.compose.material3.samples.ModalNavigationDrawerSample
 * @param label text label for this item
 * @param selected whether this item is selected
 * @param onClick called when this item is clicked
 * @param modifier the [Modifier] to be applied to this item
 * @param icon optional icon for this item, typically an [Icon]
 * @param badge optional badge to show on this item from the end side
 * @param shape optional shape for the active indicator
 * @param colors [NavigationDrawerItemColors] that will be used to resolve the colors used for this
 *   item in different states. See [NavigationDrawerItemDefaults.colors].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this item. You can use this to change the item's appearance or
 *   preview the item in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 */
@Composable
fun NavigationDrawerItem(
    label: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    badge: (@Composable () -> Unit)? = null,
    shape: Shape = NavigationDrawerTokens.ActiveIndicatorShape.value,
    colors: NavigationDrawerItemColors = NavigationDrawerItemDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    Surface(
        selected = selected,
        onClick = onClick,
        modifier =
            modifier
                .semantics { role = Role.Tab }
                .heightIn(min = NavigationDrawerTokens.ActiveIndicatorHeight)
                .fillMaxWidth(),
        shape = shape,
        color = colors.containerColor(selected).value,
        interactionSource = interactionSource,
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                val iconColor = colors.iconColor(selected).value
                CompositionLocalProvider(LocalContentColor provides iconColor, content = icon)
                Spacer(Modifier.width(12.dp))
            }
            Box(Modifier.weight(1f)) {
                val labelColor = colors.textColor(selected).value
                CompositionLocalProvider(LocalContentColor provides labelColor, content = label)
            }
            if (badge != null) {
                Spacer(Modifier.width(12.dp))
                val badgeColor = colors.badgeColor(selected).value
                CompositionLocalProvider(LocalContentColor provides badgeColor, content = badge)
            }
        }
    }
}

/** Represents the colors of the various elements of a drawer item. */
@Stable
interface NavigationDrawerItemColors {
    /**
     * Represents the icon color for this item, depending on whether it is [selected].
     *
     * @param selected whether the item is selected
     */
    @Composable fun iconColor(selected: Boolean): State<Color>

    /**
     * Represents the text color for this item, depending on whether it is [selected].
     *
     * @param selected whether the item is selected
     */
    @Composable fun textColor(selected: Boolean): State<Color>

    /**
     * Represents the badge color for this item, depending on whether it is [selected].
     *
     * @param selected whether the item is selected
     */
    @Composable fun badgeColor(selected: Boolean): State<Color>

    /**
     * Represents the container color for this item, depending on whether it is [selected].
     *
     * @param selected whether the item is selected
     */
    @Composable fun containerColor(selected: Boolean): State<Color>
}

/** Defaults used in [NavigationDrawerItem]. */
object NavigationDrawerItemDefaults {
    /**
     * Creates a [NavigationDrawerItemColors] with the provided colors according to the Material
     * specification.
     *
     * @param selectedContainerColor the color to use for the background of the item when selected
     * @param unselectedContainerColor the color to use for the background of the item when
     *   unselected
     * @param selectedIconColor the color to use for the icon when the item is selected.
     * @param unselectedIconColor the color to use for the icon when the item is unselected.
     * @param selectedTextColor the color to use for the text label when the item is selected.
     * @param unselectedTextColor the color to use for the text label when the item is unselected.
     * @param selectedBadgeColor the color to use for the badge when the item is selected.
     * @param unselectedBadgeColor the color to use for the badge when the item is unselected.
     * @return the resulting [NavigationDrawerItemColors] used for [NavigationDrawerItem]
     */
    @Composable
    fun colors(
        selectedContainerColor: Color = NavigationDrawerTokens.ActiveIndicatorColor.value,
        unselectedContainerColor: Color = Color.Transparent,
        selectedIconColor: Color = NavigationDrawerTokens.ActiveIconColor.value,
        unselectedIconColor: Color = NavigationDrawerTokens.InactiveIconColor.value,
        selectedTextColor: Color = NavigationDrawerTokens.ActiveLabelTextColor.value,
        unselectedTextColor: Color = NavigationDrawerTokens.InactiveLabelTextColor.value,
        selectedBadgeColor: Color = selectedTextColor,
        unselectedBadgeColor: Color = unselectedTextColor,
    ): NavigationDrawerItemColors =
        DefaultDrawerItemsColor(
            selectedIconColor,
            unselectedIconColor,
            selectedTextColor,
            unselectedTextColor,
            selectedContainerColor,
            unselectedContainerColor,
            selectedBadgeColor,
            unselectedBadgeColor,
        )

    /**
     * Default external padding for a [NavigationDrawerItem] according to the Material
     * specification.
     */
    val ItemPadding = PaddingValues(horizontal = 12.dp)
}

@Stable
internal class DrawerPredictiveBackState {

    var swipeEdgeMatchesDrawer by mutableStateOf(true)

    var scaleXDistance by mutableFloatStateOf(0f)

    var scaleYDistance by mutableFloatStateOf(0f)

    fun update(
        progress: Float,
        swipeEdgeLeft: Boolean,
        isRtl: Boolean,
        maxScaleXDistanceGrow: Float,
        maxScaleXDistanceShrink: Float,
        maxScaleYDistance: Float,
    ) {
        swipeEdgeMatchesDrawer = swipeEdgeLeft != isRtl
        val maxScaleXDistance =
            if (swipeEdgeMatchesDrawer) maxScaleXDistanceGrow else maxScaleXDistanceShrink
        scaleXDistance = lerp(0f, maxScaleXDistance, progress)
        scaleYDistance = lerp(0f, maxScaleYDistance, progress)
    }

    fun clear() {
        swipeEdgeMatchesDrawer = true
        scaleXDistance = 0f
        scaleYDistance = 0f
    }
}

private class DefaultDrawerItemsColor(
    val selectedIconColor: Color,
    val unselectedIconColor: Color,
    val selectedTextColor: Color,
    val unselectedTextColor: Color,
    val selectedContainerColor: Color,
    val unselectedContainerColor: Color,
    val selectedBadgeColor: Color,
    val unselectedBadgeColor: Color,
) : NavigationDrawerItemColors {
    @Composable
    override fun iconColor(selected: Boolean): State<Color> {
        return rememberUpdatedState(if (selected) selectedIconColor else unselectedIconColor)
    }

    @Composable
    override fun textColor(selected: Boolean): State<Color> {
        return rememberUpdatedState(if (selected) selectedTextColor else unselectedTextColor)
    }

    @Composable
    override fun containerColor(selected: Boolean): State<Color> {
        return rememberUpdatedState(
            if (selected) selectedContainerColor else unselectedContainerColor
        )
    }

    @Composable
    override fun badgeColor(selected: Boolean): State<Color> {
        return rememberUpdatedState(if (selected) selectedBadgeColor else unselectedBadgeColor)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DefaultDrawerItemsColor) return false

        if (selectedIconColor != other.selectedIconColor) return false
        if (unselectedIconColor != other.unselectedIconColor) return false
        if (selectedTextColor != other.selectedTextColor) return false
        if (unselectedTextColor != other.unselectedTextColor) return false
        if (selectedContainerColor != other.selectedContainerColor) return false
        if (unselectedContainerColor != other.unselectedContainerColor) return false
        if (selectedBadgeColor != other.selectedBadgeColor) return false
        return unselectedBadgeColor == other.unselectedBadgeColor
    }

    override fun hashCode(): Int {
        var result = selectedIconColor.hashCode()
        result = 31 * result + unselectedIconColor.hashCode()
        result = 31 * result + selectedTextColor.hashCode()
        result = 31 * result + unselectedTextColor.hashCode()
        result = 31 * result + selectedContainerColor.hashCode()
        result = 31 * result + unselectedContainerColor.hashCode()
        result = 31 * result + selectedBadgeColor.hashCode()
        result = 31 * result + unselectedBadgeColor.hashCode()
        return result
    }
}

private fun calculateFraction(a: Float, b: Float, pos: Float) =
    ((pos - a) / (b - a)).coerceIn(0f, 1f)

@Composable
private fun Scrim(open: Boolean, onClose: () -> Unit, fraction: () -> Float, color: Color) {
    val closeDrawer = getString(Strings.CloseDrawer)
    val dismissDrawer =
        if (open) {
            Modifier.pointerInput(onClose) { detectTapGestures { onClose() } }
                .semantics(mergeDescendants = true) {
                    contentDescription = closeDrawer
                    onClick {
                        onClose()
                        true
                    }
                }
        } else {
            Modifier
        }

    Canvas(Modifier.fillMaxSize().then(dismissDrawer)) { drawRect(color, alpha = fraction()) }
}

private val DrawerPositionalThreshold = 0.5f
private val DrawerVelocityThreshold = 400.dp
private val MinimumDrawerWidth = 240.dp

internal val PredictiveBackDrawerMaxScaleXDistanceGrow = 12.dp
internal val PredictiveBackDrawerMaxScaleXDistanceShrink = 24.dp
internal val PredictiveBackDrawerMaxScaleYDistance = 48.dp

// TODO: b/177571613 this should be a proper decay settling
// this is taken from the DrawerLayout's DragViewHelper as a min duration.
private val AnchoredDraggableDefaultAnimationSpec = TweenSpec<Float>(durationMillis = 256)
