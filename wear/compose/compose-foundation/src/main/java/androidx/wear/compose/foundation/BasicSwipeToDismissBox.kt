/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.compose.foundation

import android.os.Build
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.lerp
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive

/**
 * [BasicSwipeToDismissBox] that handles the swipe-to-dismiss gesture. Takes a single slot for the
 * background (only displayed during the swipe gesture) and the foreground content.
 *
 * Example of a [BasicSwipeToDismissBox] with stateful composables:
 *
 * @sample androidx.wear.compose.foundation.samples.StatefulSwipeToDismissBox
 *
 * Example of using [Modifier.edgeSwipeToDismiss] with [BasicSwipeToDismissBox]
 *
 * @sample androidx.wear.compose.foundation.samples.EdgeSwipeForSwipeToDismiss
 *
 * For more information, see the
 * [Swipe to dismiss](https://developer.android.com/training/wearables/components/swipe-to-dismiss)
 * guide.
 *
 * To set the custom values of background scrim color and content scrim color, provide the
 * composition locals - [LocalSwipeToDismissBackgroundScrimColor] and
 * [LocalSwipeToDismissContentScrimColor].
 *
 * @param state [State] containing information about ongoing swipe or animation.
 * @param modifier [Modifier] for this component.
 * @param backgroundKey [key] which identifies the content currently composed in the [content] block
 *   when isBackground == true. Provide the backgroundKey if your background content will be
 *   displayed as a foreground after the swipe animation ends (as is common when
 *   [BasicSwipeToDismissBox] is used for the navigation). This allows remembered state to be
 *   correctly moved between background and foreground.
 * @param contentKey [key] which identifies the content currently composed in the [content] block
 *   when isBackground == false. See [backgroundKey].
 * @param userSwipeEnabled Whether the swipe gesture is enabled. (e.g. when there is no background
 *   screen, set userSwipeEnabled = false)
 * @param content Slot for content, with the isBackground parameter enabling content to be displayed
 *   behind the foreground content - the background is normally hidden, is shown behind a scrim
 *   during the swipe gesture, and is shown without scrim once the finger passes the
 *   swipe-to-dismiss threshold.
 */
@OptIn(ExperimentalWearFoundationApi::class)
@Composable
@Suppress("PrimitiveInCollection")
fun BasicSwipeToDismissBox(
    state: SwipeToDismissBoxState,
    modifier: Modifier = Modifier,
    backgroundKey: Any = SwipeToDismissKeys.Background,
    contentKey: Any = SwipeToDismissKeys.Content,
    userSwipeEnabled: Boolean = true,
    content: @Composable BoxScope.(isBackground: Boolean) -> Unit
) {
    val density = LocalDensity.current
    val maxWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    SideEffect {
        val anchors =
            mapOf(SwipeToDismissValue.Default to 0f, SwipeToDismissValue.Dismissed to maxWidthPx)
        state.swipeableState.density = density
        state.swipeableState.updateAnchors(anchors)
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .then(
                    if (userSwipeEnabled && Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
                        Modifier.systemGestureExclusion()
                    } else {
                        Modifier
                    }
                )
                .swipeableV2(
                    state = state.swipeableState,
                    orientation = Orientation.Horizontal,
                    enabled = userSwipeEnabled
                )
    ) {
        val isRound = isRoundDevice()
        val backgroundScrimColor = LocalSwipeToDismissBackgroundScrimColor.current
        val contentScrimColor = LocalSwipeToDismissContentScrimColor.current

        val progress by
            remember(state) {
                derivedStateOf {
                    if (state.swipeableState.offset?.isNaN() == true || maxWidthPx == 0f) {
                        0f
                    } else {
                        ((state.swipeableState.offset ?: 0f) / maxWidthPx).coerceIn(0f, 1f)
                    }
                }
            }
        val isSwiping by remember { derivedStateOf { progress > 0 } }
        var squeezeMode by remember { mutableStateOf(true) }
        LaunchedEffect(state.isAnimationRunning) {
            if (state.targetValue == SwipeToDismissValue.Dismissed) {
                squeezeMode = false
            }
        }
        LaunchedEffect(state.targetValue) {
            if (!squeezeMode && state.targetValue == SwipeToDismissValue.Default) {
                squeezeMode = true
            }
        }

        repeat(2) {
            val isBackground = it == 0

            key(if (isBackground) backgroundKey else contentKey) {
                if (!isBackground || (userSwipeEnabled && isSwiping)) {
                    HierarchicalFocusCoordinator(requiresFocus = { !isBackground }) {
                        Box(
                            Modifier.fillMaxSize()
                                .then(
                                    if (!isBackground) {
                                        Modifier.graphicsLayer {
                                                val scale =
                                                    lerp(SCALE_MAX, SCALE_MIN, progress)
                                                        .coerceIn(SCALE_MIN, SCALE_MAX)
                                                val squeezeOffset =
                                                    max(0f, (1f - scale) * maxWidthPx / 2f)

                                                val translationX =
                                                    if (squeezeMode) {
                                                        // Squeeze
                                                        squeezeOffset
                                                    } else {
                                                        // slide
                                                        lerp(
                                                            squeezeOffset,
                                                            maxWidthPx,
                                                            max(0f, progress - 0.7f) / 0.3f
                                                        )
                                                    }

                                                this.translationX = translationX
                                                scaleX = scale
                                                scaleY = scale
                                                clip = isRound && translationX > 0
                                                shape = if (isRound) CircleShape else RectangleShape
                                            }
                                            .background(backgroundScrimColor)
                                    } else Modifier
                                )
                        ) {
                            // We use the repeat loop above and call content at this location
                            // for both background and foreground so that any persistence
                            // within the content composable has the same call stack which is used
                            // as part of the hash identity for saveable state.
                            content(isBackground)

                            Canvas(Modifier.fillMaxSize()) {
                                val color =
                                    if (isBackground) {
                                        backgroundScrimColor.copy(
                                            alpha =
                                                (MAX_BACKGROUND_SCRIM_ALPHA * (1 - progress))
                                                    .coerceIn(0f, 1f)
                                        )
                                    } else {
                                        contentScrimColor.copy(
                                            alpha =
                                                min(MAX_CONTENT_SCRIM_ALPHA, progress / 2f)
                                                    .coerceIn(0f, 1f)
                                        )
                                    }
                                drawRect(color = color)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * [BasicSwipeToDismissBox] that handles the swipe-to-dismiss gesture. This overload takes an
 * [onDismissed] parameter which is used to execute a command when the swipe to dismiss has
 * completed, such as navigating to another screen.
 *
 * Example of a simple SwipeToDismissBox:
 *
 * @sample androidx.wear.compose.foundation.samples.SimpleSwipeToDismissBox
 *
 * Example of using [Modifier.edgeSwipeToDismiss] with [BasicSwipeToDismissBox]
 *
 * @sample androidx.wear.compose.foundation.samples.EdgeSwipeForSwipeToDismiss
 *
 * For more information, see the
 * [Swipe to dismiss](https://developer.android.com/training/wearables/components/swipe-to-dismiss)
 * guide.
 *
 * To set the custom values of background scrim color and content scrim color, provide the
 * composition locals - [LocalSwipeToDismissBackgroundScrimColor] and
 * [LocalSwipeToDismissContentScrimColor].
 *
 * @param onDismissed Executes when the swipe to dismiss has completed.
 * @param modifier [Modifier] for this component.
 * @param state [State] containing information about ongoing swipe or animation.
 * @param backgroundKey [key] which identifies the content currently composed in the [content] block
 *   when isBackground == true. Provide the backgroundKey if your background content will be
 *   displayed as a foreground after the swipe animation ends (as is common when
 *   [BasicSwipeToDismissBox] is used for the navigation). This allows remembered state to be
 *   correctly moved between background and foreground.
 * @param contentKey [key] which identifies the content currently composed in the [content] block
 *   when isBackground == false. See [backgroundKey].
 * @param userSwipeEnabled Whether the swipe gesture is enabled. (e.g. when there is no background
 *   screen, set userSwipeEnabled = false)
 * @param content Slot for content, with the isBackground parameter enabling content to be displayed
 *   behind the foreground content - the background is normally hidden, is shown behind a scrim
 *   during the swipe gesture, and is shown without scrim once the finger passes the
 *   swipe-to-dismiss threshold.
 */
@OptIn(ExperimentalWearFoundationApi::class)
@Composable
fun BasicSwipeToDismissBox(
    onDismissed: () -> Unit,
    modifier: Modifier = Modifier,
    state: SwipeToDismissBoxState = rememberSwipeToDismissBoxState(),
    backgroundKey: Any = SwipeToDismissKeys.Background,
    contentKey: Any = SwipeToDismissKeys.Content,
    userSwipeEnabled: Boolean = true,
    content: @Composable BoxScope.(isBackground: Boolean) -> Unit
) {
    LaunchedEffect(state.currentValue) {
        if (state.currentValue == SwipeToDismissValue.Dismissed) {
            state.snapTo(SwipeToDismissValue.Default)
            onDismissed()
        }
    }
    BasicSwipeToDismissBox(
        state = state,
        modifier = modifier,
        backgroundKey = backgroundKey,
        contentKey = contentKey,
        userSwipeEnabled = userSwipeEnabled,
        content = content
    )
}

/**
 * State for [BasicSwipeToDismissBox].
 *
 * @param animationSpec The default animation that will be used to animate to a new state.
 * @param confirmStateChange callback invoked to confirm or veto a pending state change.
 */
@Stable
@OptIn(ExperimentalWearFoundationApi::class)
class SwipeToDismissBoxState(
    animationSpec: AnimationSpec<Float> = SwipeToDismissBoxDefaults.AnimationSpec,
    confirmStateChange: (SwipeToDismissValue) -> Boolean = { true },
) {
    /**
     * The current value of the state.
     *
     * Before and during a swipe, corresponds to [SwipeToDismissValue.Default], then switches to
     * [SwipeToDismissValue.Dismissed] if the swipe has been completed.
     */
    val currentValue: SwipeToDismissValue
        get() = swipeableState.currentValue

    /**
     * The target value of the state.
     *
     * If a swipe is in progress, this is the value that the state would animate to if the swipe
     * finished. If an animation is running, this is the target value of that animation. Finally, if
     * no swipe or animation is in progress, this is the same as the [currentValue].
     */
    val targetValue: SwipeToDismissValue
        get() = swipeableState.targetValue

    /** Whether the state is currently animating. */
    val isAnimationRunning: Boolean
        get() = swipeableState.isAnimationRunning

    internal fun edgeNestedScrollConnection(
        edgeSwipeState: State<EdgeSwipeState>
    ): NestedScrollConnection = swipeableState.edgeNestedScrollConnection(edgeSwipeState)

    /**
     * Set the state without any animation and suspend until it's set
     *
     * @param targetValue The new target value to set [currentValue] to.
     */
    suspend fun snapTo(targetValue: SwipeToDismissValue) = swipeableState.snapTo(targetValue)

    private companion object {
        private fun <T> SwipeableV2State<T>.edgeNestedScrollConnection(
            edgeSwipeState: State<EdgeSwipeState>
        ): NestedScrollConnection =
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    val delta = available.x
                    // If swipeState = SwipeState.SWIPING_TO_DISMISS - perform swipeToDismiss
                    // drag and consume everything
                    return if (
                        edgeSwipeState.value == EdgeSwipeState.SwipingToDismiss &&
                            source == NestedScrollSource.UserInput
                    ) {
                        dispatchRawDelta(delta)
                        available
                    } else {
                        Offset.Zero
                    }
                }

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource
                ): Offset = Offset.Zero

                override suspend fun onPreFling(available: Velocity): Velocity {
                    val toFling = available.x
                    // Consumes fling by SwipeToDismiss
                    return if (
                        edgeSwipeState.value == EdgeSwipeState.SwipingToDismiss ||
                            edgeSwipeState.value == EdgeSwipeState.SwipeToDismissInProgress
                    ) {
                        settle(velocity = toFling)
                        available
                    } else Velocity.Zero
                }

                override suspend fun onPostFling(
                    consumed: Velocity,
                    available: Velocity
                ): Velocity {
                    settle(velocity = available.x)
                    return available
                }
            }
    }

    internal val swipeableState =
        SwipeableV2State(
            initialValue = SwipeToDismissValue.Default,
            animationSpec = animationSpec,
            confirmValueChange = confirmStateChange,
            positionalThreshold = fractionalPositionalThreshold(SWIPE_THRESHOLD)
        )
}

/**
 * Create a [SwipeToDismissBoxState] and remember it.
 *
 * @param animationSpec The default animation used to animate to a new state.
 * @param confirmStateChange callback to confirm or veto a pending state change.
 */
@Composable
fun rememberSwipeToDismissBoxState(
    animationSpec: AnimationSpec<Float> = SWIPE_TO_DISMISS_BOX_ANIMATION_SPEC,
    confirmStateChange: (SwipeToDismissValue) -> Boolean = { true },
): SwipeToDismissBoxState {
    return remember(animationSpec, confirmStateChange) {
        SwipeToDismissBoxState(animationSpec, confirmStateChange)
    }
}

/** Contains defaults for [BasicSwipeToDismissBox]. */
object SwipeToDismissBoxDefaults {
    /**
     * The default animation that will be used to animate to a new state after the swipe gesture.
     */
    @OptIn(ExperimentalWearFoundationApi::class)
    val AnimationSpec = SwipeableV2Defaults.AnimationSpec

    /**
     * The default width of the area which might trigger a swipe with [edgeSwipeToDismiss] modifier
     */
    val EdgeWidth = 30.dp
}

/** Keys used to persistent state in [BasicSwipeToDismissBox]. */
enum class SwipeToDismissKeys {
    /**
     * The default background key to identify the content displayed by the content block when
     * isBackground == true. Specifying a background key instead of using the default allows
     * remembered state to be correctly moved between background and foreground.
     */
    Background,

    /**
     * The default content key to identify the content displayed by the content block when
     * isBackground == false. Specifying a background key instead of using the default allows
     * remembered state to be correctly moved between background and foreground.
     */
    Content
}

/** States used as targets for the anchor points for swipe-to-dismiss. */
enum class SwipeToDismissValue {
    /** The state of the SwipeToDismissBox before the swipe started. */
    Default,

    /** The state of the SwipeToDismissBox after the swipe passes the swipe-to-dismiss threshold. */
    Dismissed
}

/**
 * Limits swipe to dismiss to be active from the edge of the viewport only. Used when the center of
 * the screen needs to be able to handle horizontal paging, such as 2-d scrolling a Map or swiping
 * horizontally between pages. Swipe to the right is intercepted on the left part of the viewport
 * with width specified by [edgeWidth], with other touch events ignored - vertical scroll, click,
 * long click, etc.
 *
 * Currently Edge swipe, like swipe to dismiss, is only supported on the left part of the viewport
 * regardless of layout direction as content is swiped away from left to right.
 *
 * Requires that the element to which this modifier is applied exists within a
 * [BasicSwipeToDismissBox] which is using the same [SwipeToDismissBoxState] instance.
 *
 * Example of a modifier usage with SwipeToDismiss
 *
 * @sample androidx.wear.compose.foundation.samples.EdgeSwipeForSwipeToDismiss
 * @param swipeToDismissBoxState State of [BasicSwipeToDismissBox]. Used to trigger swipe gestures
 *   on SwipeToDismissBox.
 * @param edgeWidth Width of the edge zone in which the swipe will be recognised.
 */
fun Modifier.edgeSwipeToDismiss(
    swipeToDismissBoxState: SwipeToDismissBoxState,
    edgeWidth: Dp = SwipeToDismissBoxDefaults.EdgeWidth
): Modifier =
    composed(
        inspectorInfo =
            debugInspectorInfo {
                name = "edgeSwipeToDismiss"
                properties["swipeToDismissBoxState"] = swipeToDismissBoxState
                properties["edgeWidth"] = edgeWidth
            }
    ) {
        // Tracks the current swipe status
        val edgeSwipeState = remember { mutableStateOf(EdgeSwipeState.WaitingForTouch) }
        val nestedScrollConnection =
            remember(swipeToDismissBoxState) {
                swipeToDismissBoxState.edgeNestedScrollConnection(edgeSwipeState)
            }

        val nestedPointerInput: suspend PointerInputScope.() -> Unit = {
            coroutineScope {
                awaitPointerEventScope {
                    while (isActive) {
                        awaitPointerEvent(PointerEventPass.Initial).changes.fastForEach { change ->
                            // By default swipeState is WaitingForTouch.
                            // If it is in this state and a first touch hit an edge area, we
                            // set swipeState to EdgeClickedWaitingForDirection.
                            // After that to track which direction the swipe will go, we check
                            // the next touch. If it lands to the left of the first, we consider
                            // it as a swipe left and set the state to SwipingToPage. Otherwise,
                            // set the state to SwipingToDismiss
                            when (edgeSwipeState.value) {
                                EdgeSwipeState.SwipeToDismissInProgress,
                                EdgeSwipeState.WaitingForTouch -> {
                                    edgeSwipeState.value =
                                        if (change.position.x < edgeWidth.toPx())
                                            EdgeSwipeState.EdgeClickedWaitingForDirection
                                        else EdgeSwipeState.SwipingToPage
                                }
                                EdgeSwipeState.EdgeClickedWaitingForDirection -> {
                                    edgeSwipeState.value =
                                        if (change.position.x < change.previousPosition.x)
                                            EdgeSwipeState.SwipingToPage
                                        else EdgeSwipeState.SwipingToDismiss
                                }
                                else -> {} // Do nothing
                            }
                            // When finger is up - reset swipeState to WaitingForTouch
                            // or to SwipeToDismissInProgress if current
                            // state is SwipingToDismiss
                            if (change.changedToUp()) {
                                edgeSwipeState.value =
                                    if (edgeSwipeState.value == EdgeSwipeState.SwipingToDismiss)
                                        EdgeSwipeState.SwipeToDismissInProgress
                                    else EdgeSwipeState.WaitingForTouch
                            }
                        }
                    }
                }
            }
        }
        pointerInput(edgeWidth, nestedPointerInput).nestedScroll(nestedScrollConnection)
    }

/** An enum which represents a current state of swipe action. */
internal enum class EdgeSwipeState {
    // Waiting for touch, edge was not touched before.
    WaitingForTouch,

    // Edge was touched, now waiting for the second touch
    // to determine whether we swipe left or right.
    EdgeClickedWaitingForDirection,

    // Direction was determined, swiping to dismiss.
    SwipingToDismiss,

    // Direction was determined, all gestures are handled by the page itself.
    SwipingToPage,

    // Swipe was finished, used to handle fling.
    SwipeToDismissInProgress
}

private const val SWIPE_THRESHOLD = 0.5f
private const val SCALE_MAX = 1f
private const val SCALE_MIN = 0.7f
private const val MAX_CONTENT_SCRIM_ALPHA = 0.3f
private const val MAX_BACKGROUND_SCRIM_ALPHA = 0.5f
private val SWIPE_TO_DISMISS_BOX_ANIMATION_SPEC = TweenSpec<Float>(200, 0, LinearOutSlowInEasing)
