/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.annotation.FloatRange
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.internal.ConfirmValueChangeDeprecated
import androidx.compose.material3.internal.draggableAnchorsV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException

/** The directions in which a [SwipeToDismissBox] can be dismissed. */
enum class SwipeToDismissBoxValue {
    /** Can be dismissed by swiping in the reading direction. */
    StartToEnd,

    /** Can be dismissed by swiping in the reverse of the reading direction. */
    EndToStart,

    /** Cannot currently be dismissed. */
    Settled,
}

/** State of the [SwipeToDismissBox] composable. */
class SwipeToDismissBoxState {
    /**
     * State of the [SwipeToDismissBox] composable.
     *
     * @param initialValue The initial value of the state.
     * @param positionalThreshold The positional threshold to be used when calculating the target
     *   state while a swipe is in progress and when settling after the swipe ends. This is the
     *   distance from the start of a transition. It will be, depending on the direction of the
     *   interaction, added or subtracted from/to the origin offset. It should always be a positive
     *   value.
     */
    constructor(
        initialValue: SwipeToDismissBoxValue,
        positionalThreshold: (totalDistance: Float) -> Float,
    ) {
        this.anchoredDraggableState = AnchoredDraggableState(initialValue)
        this.positionalThreshold = positionalThreshold
    }

    /**
     * State of the [SwipeToDismissBox] composable.
     *
     * @param initialValue The initial value of the state.
     * @param density The density that this state can use to convert values to and from dp.
     * @param confirmValueChange Optional callback invoked to confirm or veto a pending state
     *   change.
     * @param positionalThreshold The positional threshold to be used when calculating the target
     *   state while a swipe is in progress and when settling after the swipe ends. This is the
     *   distance from the start of a transition. It will be, depending on the direction of the
     *   interaction, added or subtracted from/to the origin offset. It should always be a positive
     *   value.
     */
    @Deprecated(
        message = ConfirmValueChangeDeprecated,
        level = DeprecationLevel.WARNING,
        replaceWith =
            ReplaceWith("SwipeToDismissBoxState(initialValue, density, positionalThreshold)"),
    )
    @Suppress("Deprecation")
    constructor(
        initialValue: SwipeToDismissBoxValue,
        density: Density,
        confirmValueChange: (SwipeToDismissBoxValue) -> Boolean = { true },
        positionalThreshold: (totalDistance: Float) -> Float,
    ) {
        this.anchoredDraggableState =
            AnchoredDraggableState(
                initialValue = initialValue,
                confirmValueChange = confirmValueChange,
                velocityThreshold = { with(density) { DismissVelocityThreshold.toPx() } },
                positionalThreshold = positionalThreshold,
                snapAnimationSpec = AnchoredDraggableDefaults.SnapAnimationSpec,
                decayAnimationSpec = AnchoredDraggableDefaults.DecayAnimationSpec,
            )
    }

    internal val anchoredDraggableState: AnchoredDraggableState<SwipeToDismissBoxValue>

    internal lateinit var positionalThreshold: (Float) -> Float

    internal val useFlingBehavior: Boolean
        get() = ::positionalThreshold.isInitialized

    internal val offset: Float
        get() = anchoredDraggableState.offset

    /**
     * Require the current offset.
     *
     * @throws IllegalStateException If the offset has not been initialized yet
     */
    fun requireOffset(): Float = anchoredDraggableState.requireOffset()

    /** The current state value of the [SwipeToDismissBoxState]. */
    val currentValue: SwipeToDismissBoxValue
        get() = anchoredDraggableState.currentValue

    /**
     * The target state. This is the closest state to the current offset (taking into account
     * positional thresholds). If no interactions like animations or drags are in progress, this
     * will be the current state.
     */
    val targetValue: SwipeToDismissBoxValue
        get() = anchoredDraggableState.targetValue

    /**
     * The value the [SwipeToDismissBoxState] is currently settled at. When progressing through
     * multiple anchors, e.g. A -> B -> C, settledValue will stay the same until settled at an
     * anchor, while currentValue will update to the closest anchor.
     */
    val settledValue: SwipeToDismissBoxValue
        get() = anchoredDraggableState.settledValue

    /**
     * The fraction of the progress going from currentValue to targetValue, within [0f..1f] bounds.
     */
    @get:FloatRange(from = 0.0, to = 1.0)
    @Suppress("Deprecation")
    val progress: Float
        get() = anchoredDraggableState.progress

    /**
     * The direction (if any) in which the composable has been or is being dismissed.
     *
     * Use this to change the background of the [SwipeToDismissBox] if you want different actions on
     * each side.
     */
    val dismissDirection: SwipeToDismissBoxValue
        get() =
            when {
                offset == 0f || offset.isNaN() -> SwipeToDismissBoxValue.Settled
                offset > 0f -> SwipeToDismissBoxValue.StartToEnd
                else -> SwipeToDismissBoxValue.EndToStart
            }

    /**
     * Set the state without any animation and suspend until it's set
     *
     * @param targetValue The new target value
     */
    suspend fun snapTo(targetValue: SwipeToDismissBoxValue) {
        anchoredDraggableState.snapTo(targetValue)
    }

    /**
     * Reset the component to the default position with animation and suspend until it if fully
     * reset or animation has been cancelled. This method will throw [CancellationException] if the
     * animation is interrupted
     *
     * @return the reason the reset animation ended
     */
    suspend fun reset() =
        anchoredDraggableState.animateTo(targetValue = SwipeToDismissBoxValue.Settled)

    /**
     * Dismiss the component in the given [direction], with an animation and suspend. This method
     * will throw [CancellationException] if the animation is interrupted
     *
     * @param direction The dismiss direction.
     */
    suspend fun dismiss(direction: SwipeToDismissBoxValue) {
        anchoredDraggableState.animateTo(targetValue = direction)
    }

    companion object {

        /** [Saver] implementation for [SwipeToDismissBoxState]. */
        @Suppress("Deprecation")
        @Deprecated(
            message = ConfirmValueChangeDeprecated,
            level = DeprecationLevel.WARNING,
            replaceWith = ReplaceWith("Saver(positionalThreshold, density)"),
        )
        fun Saver(
            confirmValueChange: (SwipeToDismissBoxValue) -> Boolean,
            positionalThreshold: (totalDistance: Float) -> Float,
            density: Density,
        ) =
            Saver<SwipeToDismissBoxState, SwipeToDismissBoxValue>(
                save = { it.currentValue },
                restore = {
                    SwipeToDismissBoxState(it, density, confirmValueChange, positionalThreshold)
                },
            )

        /** The default [Saver] implementation for [SwipeToDismissBoxState]. */
        fun Saver(positionalThreshold: (totalDistance: Float) -> Float) =
            Saver<SwipeToDismissBoxState, SwipeToDismissBoxValue>(
                save = { it.currentValue },
                restore = { SwipeToDismissBoxState(it, positionalThreshold) },
            )
    }
}

/**
 * Create and [remember] a [SwipeToDismissBoxState].
 *
 * @param initialValue The initial value of the state.
 * @param positionalThreshold The positional threshold to be used when calculating the target state
 *   while a swipe is in progress and when settling after the swipe ends. This is the distance from
 *   the start of a transition. It will be, depending on the direction of the interaction, added or
 *   subtracted from/to the origin offset. It should always be a positive value.
 */
@Composable
fun rememberSwipeToDismissBoxState(
    initialValue: SwipeToDismissBoxValue = SwipeToDismissBoxValue.Settled,
    positionalThreshold: (totalDistance: Float) -> Float =
        SwipeToDismissBoxDefaults.positionalThreshold,
): SwipeToDismissBoxState {
    return rememberSaveable(
        saver = SwipeToDismissBoxState.Saver(positionalThreshold = positionalThreshold)
    ) {
        SwipeToDismissBoxState(initialValue, positionalThreshold)
    }
}

/**
 * Create and [remember] a [SwipeToDismissBoxState].
 *
 * @param initialValue The initial value of the state.
 * @param confirmValueChange Optional callback invoked to confirm or veto a pending state change.
 * @param positionalThreshold The positional threshold to be used when calculating the target state
 *   while a swipe is in progress and when settling after the swipe ends. This is the distance from
 *   the start of a transition. It will be, depending on the direction of the interaction, added or
 *   subtracted from/to the origin offset. It should always be a positive value.
 */
@Composable
@Suppress("Deprecation")
@Deprecated(
    message = ConfirmValueChangeDeprecated,
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("rememberSwipeToDismissBoxState(initialValue, positionalThreshold)"),
)
fun rememberSwipeToDismissBoxState(
    initialValue: SwipeToDismissBoxValue = SwipeToDismissBoxValue.Settled,
    confirmValueChange: (SwipeToDismissBoxValue) -> Boolean = { true },
    positionalThreshold: (totalDistance: Float) -> Float =
        SwipeToDismissBoxDefaults.positionalThreshold,
): SwipeToDismissBoxState {
    val density = LocalDensity.current
    return rememberSaveable(
        saver =
            SwipeToDismissBoxState.Saver(
                confirmValueChange = confirmValueChange,
                density = density,
                positionalThreshold = positionalThreshold,
            )
    ) {
        SwipeToDismissBoxState(initialValue, density, confirmValueChange, positionalThreshold)
    }
}

/**
 * A composable that can be dismissed by swiping left or right.
 *
 * @sample androidx.compose.material3.samples.SwipeToDismissListItems
 * @param state The state of this component.
 * @param backgroundContent A composable that is stacked behind the [content] and is exposed when
 *   the content is swiped. You can/should use the [state] to have different backgrounds on each
 *   side.
 * @param modifier Optional [Modifier] for this component.
 * @param enableDismissFromStartToEnd Whether SwipeToDismissBox can be dismissed from start to end.
 * @param enableDismissFromEndToStart Whether SwipeToDismissBox can be dismissed from end to start.
 * @param gesturesEnabled Whether swipe-to-dismiss can be interacted by gestures.
 * @param onDismiss Optional callback to be called when [content] is dismissed. onDismissed provides
 *   the current dismissed direction.
 * @param content The content that can be dismissed.
 */
@Composable
fun SwipeToDismissBox(
    state: SwipeToDismissBoxState,
    backgroundContent: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    enableDismissFromStartToEnd: Boolean = true,
    enableDismissFromEndToStart: Boolean = true,
    gesturesEnabled: Boolean = true,
    onDismiss: (SwipeToDismissBoxValue) -> Unit = {},
    content: @Composable RowScope.() -> Unit,
) {
    Box(
        modifier =
            modifier.anchoredDraggable(
                state = state.anchoredDraggableState,
                orientation = Orientation.Horizontal,
                enabled = gesturesEnabled && state.settledValue == SwipeToDismissBoxValue.Settled,
                flingBehavior =
                    if (state.useFlingBehavior)
                        AnchoredDraggableDefaults.flingBehavior(
                            state = state.anchoredDraggableState,
                            positionalThreshold = state.positionalThreshold,
                        )
                    else null,
            ),
        propagateMinConstraints = true,
    ) {
        Row(content = backgroundContent, modifier = Modifier.matchParentSize())
        Row(
            content = content,
            modifier =
                Modifier.draggableAnchorsV2(state.anchoredDraggableState, Orientation.Horizontal) {
                    size,
                    _ ->
                    val newAnchors = DraggableAnchors {
                        val width = size.width.toFloat()
                        SwipeToDismissBoxValue.Settled at 0f
                        if (enableDismissFromStartToEnd) {
                            SwipeToDismissBoxValue.StartToEnd at width
                        }
                        if (enableDismissFromEndToStart) {
                            SwipeToDismissBoxValue.EndToStart at -width
                        }
                    }
                    val isInitialized = state.anchoredDraggableState.anchors.size > 0
                    val previousValue = state.currentValue
                    val targetValue = state.targetValue
                    val newTarget =
                        if (!isInitialized && newAnchors.hasPositionFor(previousValue)) {
                            previousValue
                        } else if (newAnchors.hasPositionFor(targetValue)) {
                            targetValue
                        } else SwipeToDismissBoxValue.Settled
                    return@draggableAnchorsV2 newAnchors to newTarget
                },
        )
    }
    LaunchedEffect(state.settledValue, onDismiss) {
        // Only call when state is settled in a dismissed direction.
        if (state.settledValue != SwipeToDismissBoxValue.Settled) {
            onDismiss(state.dismissDirection)
        }
    }
}

/** Contains default values for [SwipeToDismissBox] and [SwipeToDismissBoxState]. */
object SwipeToDismissBoxDefaults {
    /** Default positional threshold of 56.dp for [SwipeToDismissBoxState]. */
    val positionalThreshold: (totalDistance: Float) -> Float
        @Composable get() = with(LocalDensity.current) { { 56.dp.toPx() } }
}

@Composable
@Deprecated(
    level = DeprecationLevel.HIDDEN,
    message =
        "Maintained for binary compatibility. Use updated signature with onDismissed " +
            "parameter.",
)
fun SwipeToDismissBox(
    state: SwipeToDismissBoxState,
    backgroundContent: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    enableDismissFromStartToEnd: Boolean = true,
    enableDismissFromEndToStart: Boolean = true,
    gesturesEnabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) =
    SwipeToDismissBox(
        state = state,
        backgroundContent = backgroundContent,
        modifier = modifier,
        enableDismissFromStartToEnd = enableDismissFromStartToEnd,
        enableDismissFromEndToStart = enableDismissFromEndToStart,
        gesturesEnabled = gesturesEnabled,
        onDismiss = {},
        content = content,
    )

private val DismissVelocityThreshold = 125.dp
