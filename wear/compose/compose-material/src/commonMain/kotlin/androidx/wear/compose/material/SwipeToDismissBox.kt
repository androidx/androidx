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

package androidx.wear.compose.material

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive

/**
 * Wear Material [SwipeToDismissBox] that handles the swipe-to-dismiss gesture. Takes a single
 * slot for the background (only displayed during the swipe gesture) and the foreground content.
 *
 * Example of a [SwipeToDismissBox] with stateful composables:
 * @sample androidx.wear.compose.material.samples.StatefulSwipeToDismissBox
 *
 * Example of using [Modifier.edgeSwipeToDismiss] with [SwipeToDismissBox]
 * @sample androidx.wear.compose.material.samples.EdgeSwipeForSwipeToDismiss
 *
 * For more information, see the
 * [Swipe to dismiss](https://developer.android.com/training/wearables/components/swipe-to-dismiss)
 * guide.
 *
 * @param state State containing information about ongoing swipe or animation.
 * @param modifier Optional [Modifier] for this component.
 * @param backgroundScrimColor Color for background scrim
 * @param contentScrimColor Optional [Color] used for the scrim over the
 * content composable during the swipe gesture.
 * @param backgroundKey Optional [key] which identifies the content currently composed in
 * the [content] block when isBackground == true. Provide the backgroundKey if your background
 * content will be displayed as a foreground after the swipe animation ends
 * (as is common when [SwipeToDismissBox] is used for the navigation). This allows
 * remembered state to be correctly moved between background and foreground.
 * @Param contentKey Optional [key] which identifies the content currently composed in the
 * [content] block when isBackground == false. See [backgroundKey].
 * @Param hasBackground Optional [Boolean] used to indicate if the content has no background,
 * in which case the swipe gesture is disabled (since there is no parent destination).
 * @param content Slot for content, with the isBackground parameter enabling content to be
 * displayed behind the foreground content - the background is normally hidden,
 * is shown behind a scrim during the swipe gesture,
 * and is shown without scrim once the finger passes the swipe-to-dismiss threshold.
 */
@Composable
@OptIn(ExperimentalWearMaterialApi::class)
public fun SwipeToDismissBox(
    state: SwipeToDismissBoxState,
    modifier: Modifier = Modifier,
    backgroundScrimColor: Color = MaterialTheme.colors.background,
    contentScrimColor: Color = contentColorFor(backgroundScrimColor),
    backgroundKey: Any = SwipeToDismissKeys.Background,
    contentKey: Any = SwipeToDismissKeys.Content,
    hasBackground: Boolean = true,
    content: @Composable BoxScope.(isBackground: Boolean) -> Unit
) {
    // Will be updated in onSizeChanged, initialise to any value other than zero
    // so that it is different to the other anchor used for the swipe gesture.
    var maxWidth by remember { mutableStateOf(1f) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { maxWidth = it.width.toFloat() }
            .swipeable(
                state = state.swipeableState,
                enabled = hasBackground,
                anchors = anchors(maxWidth),
                thresholds = { _, _ -> FractionalThreshold(SwipeThreshold) },
                resistance = ResistanceConfig(
                    basis = maxWidth,
                    factorAtMin = TotalResistance,
                    factorAtMax = TotalResistance,
                ),
                orientation = Orientation.Horizontal,
            )
    ) {
        val dismissAnimatable = remember { Animatable(0f) }

        LaunchedEffect(state.isAnimationRunning) {
            if (state.targetValue == SwipeToDismissValue.Dismissed) {
                dismissAnimatable.animateTo(1f, SpringSpec())
            } else {
                // because SwipeToDismiss remains alive, it worth resetting animation to 0
                // when [targetValue] becomes [Original] again
                dismissAnimatable.snapTo(0f)
            }
        }

        // Use remember { derivedStateOf{ ... } } idiom to re-use modifiers where possible.
        val isRound = isRoundDevice()
        val modifiers by remember(isRound, backgroundScrimColor) {
            derivedStateOf {
                val squeezeMotion =
                    SqueezeMotion(state.swipeableState.offset.value.roundToInt(), maxWidth)

                Modifiers(
                    contentForeground =
                    Modifier.offset { IntOffset(squeezeMotion.contentOffset, 0) }
                        .fillMaxSize()
                        .scale(squeezeMotion.scale(dismissAnimatable.value))
                        .then(
                            if (isRound && squeezeMotion.contentOffset > 0) {
                                Modifier.clip(CircleShape)
                            } else {
                                Modifier
                            }
                        )
                        .alpha(1 - dismissAnimatable.value)
                        .background(backgroundScrimColor),
                    scrimForeground =
                    Modifier.background(
                        contentScrimColor.copy(alpha = squeezeMotion.contentScrimAlpha)
                    ).fillMaxSize(),
                    scrimBackground =
                    Modifier.matchParentSize()
                        .background(
                            backgroundScrimColor
                                .copy(
                                    alpha = squeezeMotion.backgroundScrimAlpha(
                                        dismissAnimatable.value
                                    )
                                )
                        )
                )
            }
        }

        repeat(2) {
            val isBackground = it == 0
            val contentModifier = if (isBackground) {
                Modifier.fillMaxSize()
            } else {
                modifiers.contentForeground
            }

            val scrimModifier = if (isBackground) {
                modifiers.scrimBackground
            } else {
                modifiers.scrimForeground
            }

            key(if (isBackground) backgroundKey else contentKey) {
                if (!isBackground ||
                    (hasBackground && state.swipeableState.offset.value.roundToInt() > 0)
                ) {
                    Box(contentModifier) {
                        // We use the repeat loop above and call content at this location
                        // for both background and foreground so that any persistence
                        // within the content composable has the same call stack which is used
                        // as part of the hash identity for saveable state.
                        content(isBackground)
                        Box(modifier = scrimModifier)
                    }
                }
            }
        }
    }
}

/**
 * Wear Material [SwipeToDismissBox] that handles the swipe-to-dismiss gesture.
 * This overload takes an [onDismissed] parameter which is used to execute a command when the
 * swipe to dismiss has completed, such as navigating to another screen.
 *
 * Example of a simple SwipeToDismissBox:
 * @sample androidx.wear.compose.material.samples.SimpleSwipeToDismissBox
 *
 * Example of using [Modifier.edgeSwipeToDismiss] with [SwipeToDismissBox]
 * @sample androidx.wear.compose.material.samples.EdgeSwipeForSwipeToDismiss
 *
 * For more information, see the
 * [Swipe to dismiss](https://developer.android.com/training/wearables/components/swipe-to-dismiss)
 * guide.
 *
 * @param onDismissed Executes when the swipe to dismiss has completed.
 * @param modifier Optional [Modifier] for this component.
 * @param state State containing information about ongoing swipe or animation.
 * @param backgroundScrimColor Color for background scrim
 * @param contentScrimColor Optional [Color] used for the scrim over the
 * content composable during the swipe gesture.
 * @param backgroundKey Optional [key] which identifies the content currently composed in
 * the [content] block when isBackground == true. Provide the backgroundKey if your background
 * content will be displayed as a foreground after the swipe animation ends
 * (as is common when [SwipeToDismissBox] is used for the navigation). This allows
 * remembered state to be correctly moved between background and foreground.
 * @Param contentKey Optional [key] which identifies the content currently composed in the
 * [content] block when isBackground == false. See [backgroundKey].
 * @Param hasBackground Optional [Boolean] used to indicate if the content has no background,
 * in which case the swipe gesture is disabled (since there is no parent destination).
 * @param content Slot for content, with the isBackground parameter enabling content to be
 * displayed behind the foreground content - the background is normally hidden,
 * is shown behind a scrim during the swipe gesture,
 * and is shown without scrim once the finger passes the swipe-to-dismiss threshold.
 */
@Composable
public fun SwipeToDismissBox(
    onDismissed: () -> Unit,
    modifier: Modifier = Modifier,
    state: SwipeToDismissBoxState = rememberSwipeToDismissBoxState(),
    backgroundScrimColor: Color = MaterialTheme.colors.background,
    contentScrimColor: Color = contentColorFor(backgroundScrimColor),
    backgroundKey: Any = SwipeToDismissKeys.Background,
    contentKey: Any = SwipeToDismissKeys.Content,
    hasBackground: Boolean = true,
    content: @Composable BoxScope.(isBackground: Boolean) -> Unit
) {
    LaunchedEffect(state.currentValue) {
        if (state.currentValue == SwipeToDismissValue.Dismissed) {
            state.snapTo(SwipeToDismissValue.Default)
            onDismissed()
        }
    }
    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        backgroundScrimColor = backgroundScrimColor,
        contentScrimColor = contentScrimColor,
        backgroundKey = backgroundKey,
        contentKey = contentKey,
        hasBackground = hasBackground,
        content = content
    )
}

@Stable
/**
 * State for [SwipeToDismissBox].
 *
 * @param animationSpec The default animation that will be used to animate to a new state.
 * @param confirmStateChange Optional callback invoked to confirm or veto a pending state change.
 */
@OptIn(ExperimentalWearMaterialApi::class)
public class SwipeToDismissBoxState(
    animationSpec: AnimationSpec<Float> = SwipeToDismissBoxDefaults.AnimationSpec,
    confirmStateChange: (SwipeToDismissValue) -> Boolean = { true },
) {
    /**
     * The current value of the state.
     *
     * Before and during a swipe, corresponds to [SwipeToDismissValue.Default], then switches to
     * [SwipeToDismissValue.Dismissed] if the swipe has been completed.
     */
    public val currentValue: SwipeToDismissValue
        get() = swipeableState.currentValue

    /**
     * The target value of the state.
     *
     * If a swipe is in progress, this is the value that the state would animate to if the
     * swipe finished. If an animation is running, this is the target value of that animation.
     * Finally, if no swipe or animation is in progress, this is the same as the [currentValue].
     */
    public val targetValue: SwipeToDismissValue
        get() = swipeableState.targetValue

    /**
     * Whether the state is currently animating.
     */
    public val isAnimationRunning: Boolean
        get() = swipeableState.isAnimationRunning

    internal fun edgeNestedScrollConnection(
        edgeTouched: State<Boolean>
    ): NestedScrollConnection =
        swipeableState.edgeNestedScrollConnection(edgeTouched)

    /**
     * Set the state without any animation and suspend until it's set
     *
     * @param targetValue The new target value to set [currentValue] to.
     */
    public suspend fun snapTo(targetValue: SwipeToDismissValue) = swipeableState.snapTo(targetValue)

    companion object {
        private fun <T> SwipeableState<T>.edgeNestedScrollConnection(
            edgeTouched: State<Boolean>
        ): NestedScrollConnection =
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    val delta = available.x
                    // If edge was clicked - perform swipe drag and consume everything
                    if (edgeTouched.value && delta > 0) {
                        performDrag(delta)
                        return available
                    } else {
                        // Swipe back if drag has started but swiped in a different direction
                        return if (delta < 0 && source == NestedScrollSource.Drag) {
                            performDrag(delta).toOffsetX()
                        } else {
                            Offset.Zero
                        }
                    }
                }

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {
                    // If edge was touched and not all scroll was consumed, assume it was
                    // consumed by the parent
                    if (edgeTouched.value && available.x > 0) {
                        return available
                    } else {
                        // Finish scroll if it wasn't an edge touch
                        return if (source == NestedScrollSource.Drag) {
                            performDrag(available.x).toOffsetX()
                        } else {
                            Offset.Zero
                        }
                    }
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    val toFling = available.x
                    // Consumes fling by SwipeToDismiss
                    return if (edgeTouched.value && toFling > 0 ||
                        toFling < 0 && offset.value > minBound
                    ) {
                        performFling(velocity = toFling)
                        available
                    } else
                        Velocity.Zero
                }

                override suspend fun onPostFling(
                    consumed: Velocity,
                    available: Velocity
                ): Velocity {
                    performFling(velocity = available.x)
                    return available
                }

                private fun Float.toOffsetX(): Offset = Offset(this, 0f)
            }
    }

    internal val swipeableState = SwipeableState(
        initialValue = SwipeToDismissValue.Default,
        animationSpec = animationSpec,
        confirmStateChange = confirmStateChange,
    )
}

/**
 * Create a [SwipeToDismissBoxState] and remember it.
 *
 * @param animationSpec The default animation used to animate to a new state.
 * @param confirmStateChange Optional callback to confirm or veto a pending state change.
 */
@Composable
public fun rememberSwipeToDismissBoxState(
    animationSpec: AnimationSpec<Float> = SwipeToDismissBoxDefaults.AnimationSpec,
    confirmStateChange: (SwipeToDismissValue) -> Boolean = { true },
): SwipeToDismissBoxState {
    return remember(animationSpec, confirmStateChange) {
        SwipeToDismissBoxState(animationSpec, confirmStateChange)
    }
}

/**
 * Contains defaults for [SwipeToDismissBox].
 */
public object SwipeToDismissBoxDefaults {
    /**
     * The default animation that will be used to animate to a new state after the swipe gesture.
     */
    @OptIn(ExperimentalWearMaterialApi::class)
    public val AnimationSpec = SwipeableDefaults.AnimationSpec

    /**
     * The default width of the area which might trigger a swipe
     * with [edgeSwipeToDismiss] modifier
     */
    public val EdgeWidth = 30.dp
}

/**
 * Keys used to persistent state in [SwipeToDismissBox].
 */
public enum class SwipeToDismissKeys {
    /**
     * The default background key to identify the content displayed by the content block
     * when isBackground == true. Specifying a background key instead of using the default
     * allows remembered state to be correctly moved between background and foreground.
     */
    Background,

    /**
     * The default content key to identify the content displayed by the content block
     * when isBackground == false. Specifying a background key instead of using the default
     * allows remembered state to be correctly moved between background and foreground.
     */
    Content
}

/**
 * States used as targets for the anchor points for swipe-to-dismiss.
 */
public enum class SwipeToDismissValue {
    /**
     * The state of the SwipeToDismissBox before the swipe started.
     */
    Default,

    /**
     * The state of the SwipeToDismissBox after the swipe passes the swipe-to-dismiss threshold.
     */
    Dismissed
}

/**
 * Limits swipe to dismiss to be active from the edge of the viewport only. Used when the center
 * of the screen needs to be able to handle horizontal paging, such as 2-d scrolling a Map
 * or swiping horizontally between pages. Swipe to the right is intercepted on the left
 * part of the viewport with width specified by [edgeWidth], with other touch events
 * ignored - vertical scroll, click, long click, etc.
 *
 * Currently Edge swipe, like swipe to dismiss, is only supported on the left part of the viewport
 * regardless of layout direction as content is swiped away from left to right.
 *
 * Example of a modifier usage with SwipeToDismiss
 * @sample androidx.wear.compose.material.samples.EdgeSwipeForSwipeToDismiss
 *
 * @param swipeToDismissBoxState A state of SwipeToDismissBox. Used to trigger swipe gestures
 * on SwipeToDismissBox
 * @param edgeWidth A width of edge, where swipe should be recognised
 */
public fun Modifier.edgeSwipeToDismiss(
    swipeToDismissBoxState: SwipeToDismissBoxState,
    edgeWidth: Dp = SwipeToDismissBoxDefaults.EdgeWidth
): Modifier =
    composed(
        inspectorInfo = debugInspectorInfo {
            name = "edgeSwipeToDismiss"
            properties["swipeToDismissBoxState"] = swipeToDismissBoxState
            properties["edgeWidth"] = edgeWidth
        }
    ) {
        // Tracks whether a touch has landed on the edge area. Becomes false after finger
        // touches a non-edge area after it was raised
        val edgeTouched = remember { mutableStateOf(false) }
        val nestedScrollConnection =
            remember(swipeToDismissBoxState) {
                swipeToDismissBoxState.edgeNestedScrollConnection(edgeTouched)
            }

        val nestedPointerInput: suspend PointerInputScope.() -> Unit = {
            coroutineScope {
                awaitPointerEventScope {
                    var trackFirstTouch = true
                    while (isActive) {
                        awaitPointerEvent(PointerEventPass.Initial).changes.forEach { change ->
                            // If it was a first touch and it hit an edge area, we
                            // set edgeTouched to true.
                            // trackFirstTouch is set to false as we don't handle
                            // any other touches before the finger is lifted up
                            if (trackFirstTouch) {
                                edgeTouched.value = change.position.x < edgeWidth.toPx()
                                trackFirstTouch = false
                            }
                            // When finger is up - reset trackFirstTouch to true
                            if (change.changedToUp()) {
                                trackFirstTouch = true
                            }
                        }
                    }
                }
            }
        }

        pointerInput(edgeWidth, nestedPointerInput)
            .nestedScroll(nestedScrollConnection)
    }

/**
 * A class which is responsible for squeezing animation and all computations related to it
 */
private class SqueezeMotion(
    private val offsetPx: Int,
    private val maxWidth: Float
) {
    private val scaleDelta = 0.2f
    private val dismissScaleDelta = 0.05f
    private val offsetFactor = scaleDelta / 2
    private val contentScrimMaxAlpha = 0.07f
    private val backgroundScrimMinAlpha = 0.65f

    private val progress = calculateProgress(offsetPx.toFloat(), maxWidth)

    /**
     * [scale] can change from 1 to 1-[scaleDelta] - [dismissScaleDelta]
     * As [progress] goes from 0 to 1 and [finalAnimationProgress] from 0 to 1,
     * [scale] decreases accordingly up to a 1 - [scaleDelta] - [dismissScaleDelta]
     */
    fun scale(finalAnimationProgress: Float): Float =
        (1.0 - progress * scaleDelta - finalAnimationProgress * dismissScaleDelta).toFloat()

    /**
     * As [progress] goes from 0 to 1, [contentOffset] changes from 0 to maxWidth
     * mutiplied by [offsetFactor].
     * If [offsetPx] negative ( <= 0) it remains the same.
     * This helps to have a resistance animation if page is swiped to the opposite
     * direction.
     */
    val contentOffset: Int
        get() = if (offsetPx > 0)
            (maxWidth * progress * offsetFactor).toInt()
        else
            offsetPx

    /**
     * As [progress] goes from 0 to 1, [contentScrimAlpha] changes from 0%
     * to [contentScrimMaxAlpha].
     */
    val contentScrimAlpha: Float
        get() = contentScrimMaxAlpha * progress

    /**
     * As [progress] goes from 0 to 1, [backgroundScrimAlpha] is decreasing from 100% to
     * [backgroundScrimMinAlpha] value. [finalAnimationProgress] makes background completely
     * transparent by changing its value from [backgroundScrimMinAlpha] to 0.
     */
    fun backgroundScrimAlpha(finalAnimationProgress: Float): Float =
        1 - (1 - backgroundScrimMinAlpha) * progress -
            backgroundScrimMinAlpha * finalAnimationProgress

    /**
     *  Computes a progress, which is in range from 0 to 1. As offset value changes from 0 to basis,
     * the progress changes by sin function
     */
    private fun calculateProgress(
        offset: Float,
        basis: Float
    ): Float = if (offset > 0)
        sin((offset / basis).coerceIn(-1f, 1f) * PI.toFloat() / 2)
    else 0f
}

/**
 * Class to enable calculating group of modifiers in a single, memoised block.
 */
private data class Modifiers(
    val contentForeground: Modifier,
    val scrimForeground: Modifier,
    val scrimBackground: Modifier,
)

// Map pixel position to states - initially, don't know the width in pixels so omit upper bound.
private fun anchors(maxWidth: Float): Map<Float, SwipeToDismissValue> =
    mapOf(
        0f to SwipeToDismissValue.Default,
        maxWidth to SwipeToDismissValue.Dismissed
    )

private val SwipeThreshold = 0.5f
private val TotalResistance = 1000f