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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.util.Predicate
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Short animation in milliseconds.
 */
internal const val SHORT_ANIMATION = 50
/**
 * Flash animation length in milliseconds.
 */
internal const val FLASH_ANIMATION = 100

/**
 * Rapid animation length in milliseconds.
 */
internal const val RAPID_ANIMATION = 200

/**
 * Quick animation length in milliseconds.
 */
internal const val QUICK_ANIMATION = 250

/**
 * Standard easing for Swipe To Reveal.
 */
internal val STANDARD_IN_OUT = CubicBezierEasing(0.20f, 0.0f, 0.0f, 1.00f)

/**
 * Different values which the swipeable modifier can be configured to.
 */
@ExperimentalWearFoundationApi
@JvmInline
public value class RevealValue private constructor(val value: Int) {
    companion object {
        /**
         * The default first value which generally represents the state where the revealable
         * actions have not been revealed yet.
         */
        val Covered = RevealValue(0)

        /**
         * The value which represents the state in which all the actions are revealed and the
         * top content is not being swiped.
         */
        val Revealing = RevealValue(1)

        /**
         * The value which represents the state in which the whole revealable content is fully
         * revealed.
         */
        val Revealed = RevealValue(2)
    }
}

/**
 * Different values which can trigger the state change from one [RevealValue] to another.
 * These are not set by themselves and need to be set appropriately with [RevealState.snapTo]
 * and [RevealState.animateTo].
 */
@ExperimentalWearFoundationApi
@JvmInline
public value class RevealActionType private constructor(val value: Int) {
    companion object {
        /**
         * Represents the primary action composable of [SwipeToReveal]. This corresponds to the
         * mandatory `primaryAction` parameter of [SwipeToReveal].
         */
        val PrimaryAction = RevealActionType(0)

        /**
         * Represents the secondary action composable of [SwipeToReveal]. This corresponds to the
         * optional `secondaryAction` composable of [SwipeToReveal].
         */
        val SecondaryAction = RevealActionType(1)

        /**
         * Represents the undo action composable of [SwipeToReveal]. This corresponds to the
         * `undoAction` composable of [SwipeToReveal] which is shown once an action is performed.
         */
        val UndoAction = RevealActionType(2)

        /**
         * Default value when none of the above are applicable.
         */
        val None = RevealActionType(-1)
    }
}

/**
 * Creates the required anchors to which the top content can be swiped, to reveal the actions.
 * Each value should be in the range [0..1], where 0 represents right most end and 1 represents the
 * full width of the top content starting from right and ending on left.
 *
 * @param coveredAnchor Anchor for the [RevealValue.Covered] value
 * @param revealingAnchor Anchor for the [RevealValue.Revealing] value
 * @param revealedAnchor Anchor for the [RevealValue.Revealed] value
 */
@ExperimentalWearFoundationApi
public fun createAnchors(
    coveredAnchor: Float = 0f,
    revealingAnchor: Float = 0.7f,
    revealedAnchor: Float = 1f
): Map<RevealValue, Float> {
    return mapOf(
        RevealValue.Covered to coveredAnchor,
        RevealValue.Revealing to revealingAnchor,
        RevealValue.Revealed to revealedAnchor
    )
}

/**
 * A class to keep track of the state of the composable. It can be used to customise
 * the behaviour and state of the composable.
 *
 * @constructor Create a [RevealState].
 */
@ExperimentalWearFoundationApi
public class RevealState internal constructor(
    initialValue: RevealValue,
    animationSpec: AnimationSpec<Float>,
    confirmValueChange: (RevealValue) -> Boolean,
    positionalThreshold: Density.(totalDistance: Float) -> Float,
    internal val anchors: Map<RevealValue, Float>,
    internal val coroutineScope: CoroutineScope,
    internal val nestedScrollDispatcher: NestedScrollDispatcher
) {
    /**
     * [SwipeableV2State] internal instance for the state.
     */
    internal val swipeableState = SwipeableV2State(
        initialValue = initialValue,
        animationSpec = animationSpec,
        confirmValueChange = { revealValue ->
            confirmValueChangeAndReset(
                confirmValueChange,
                revealValue
            )
        },
        positionalThreshold = positionalThreshold,
        nestedScrollDispatcher = nestedScrollDispatcher
    )

    public var lastActionType by mutableStateOf(RevealActionType.None)

    /**
     * The current [RevealValue] based on the status of the component.
     *
     * @see Modifier.swipeableV2
     */
    public val currentValue: RevealValue
        get() = swipeableState.currentValue

    /**
     * The target [RevealValue] based on the status of the component. This will be equal to
     * the [currentValue] if there is no animation running or swiping has stopped. Otherwise, this
     * returns the next [RevealValue] based on the animation/swipe direction.
     *
     * @see Modifier.swipeableV2
     */
    public val targetValue: RevealValue
        get() = swipeableState.targetValue

    /**
     * Returns whether the animation is running or not.
     *
     * @see Modifier.swipeableV2
     */
    public val isAnimationRunning: Boolean
        get() = swipeableState.isAnimationRunning

    /**
     * The current amount by which the revealable content has been revealed by.
     *
     * @see Modifier.swipeableV2
     */
    public val offset: Float
        get() = swipeableState.offset ?: 0f

    /**
     * Defines the anchors for revealable content. These anchors are used to determine
     * the width at which the revealable content can be revealed to and stopped without requiring
     * any input from the user.
     *
     * @see Modifier.swipeableV2
     */
    public val swipeAnchors: Map<RevealValue, Float>
        get() = anchors

    /**
     * Snaps to the [targetValue] without any animation.
     *
     * @param targetValue The target [RevealValue] where the [currentValue] will be changed
     * to.
     *
     * @see Modifier.swipeableV2
     */
    public suspend fun snapTo(targetValue: RevealValue) {
        // Cover the previously open component if revealing a different one
        if (targetValue != RevealValue.Covered) {
            resetLastState(this)
        }
        swipeableState.snapTo(targetValue)
    }

    /**
     * Animates to the [targetValue] with the animation spec provided.
     *
     * @param targetValue The target [RevealValue] where the [currentValue] will animate
     * to.
     */
    public suspend fun animateTo(targetValue: RevealValue) {
        // Cover the previously open component if revealing a different one
        if (targetValue != RevealValue.Covered) {
            resetLastState(this)
        }
        swipeableState.animateTo(targetValue)
    }

    /**
     * Require the current offset.
     *
     * @throws IllegalStateException If the offset has not been initialized yet
     */
    internal fun requireOffset(): Float = swipeableState.requireOffset()

    private fun confirmValueChangeAndReset(
        confirmValueChange: Predicate<RevealValue>,
        revealValue: RevealValue,
    ): Boolean {
        val canChangeValue = confirmValueChange.test(revealValue)
        val currentState = this
        // Update the state if the reveal value is changing to a different value than Covered.
        if (canChangeValue &&
            revealValue != RevealValue.Covered) {
            coroutineScope.launch {
                resetLastState(currentState)
            }
        }
        return canChangeValue
    }

    /**
     * Resets last state if a different SwipeToReveal is being moved to new anchor.
     */
    private suspend fun resetLastState(
        currentState: RevealState
    ) {
        val oldState = SingleSwipeCoordinator.lastUpdatedState.getAndSet(currentState)
        if (currentState != oldState) {
            oldState?.animateTo(RevealValue.Covered)
        }
    }

    /**
     * A singleton instance to keep track of the [RevealState] which was modified the last time.
     */
    private object SingleSwipeCoordinator {
        var lastUpdatedState: AtomicReference<RevealState?> = AtomicReference(null)
    }
}

/**
 * Create and [remember] a [RevealState].
 *
 * @param initialValue The initial value of the [RevealValue].
 * @param animationSpec The animation which will be applied on the top content.
 * @param confirmValueChange Optional callback invoked to confirm or veto a pending state change.
 * @param positionalThreshold The positional threshold to be used when calculating the target state
 * while the reveal is in progress and when settling after the revealing ends. This is the distance
 * from the start of a transition. It will be, depending on the direction of the interaction, added
 * or subtracted from/to the origin offset. It should always be a positive value.
 * @param anchors A map of [RevealValue] to the fraction where the content can be revealed to
 * reach that value. Each anchor should be between [0..1] which will be adjusted based on total
 * width.
 */
@ExperimentalWearFoundationApi
@Composable
public fun rememberRevealState(
    initialValue: RevealValue = RevealValue.Covered,
    animationSpec: AnimationSpec<Float> = SwipeToRevealDefaults.animationSpec,
    confirmValueChange: (RevealValue) -> Boolean = { true },
    positionalThreshold: Density.(totalDistance: Float) -> Float =
        SwipeToRevealDefaults.defaultThreshold(),
    anchors: Map<RevealValue, Float> = createAnchors(),
): RevealState {
    val coroutineScope = rememberCoroutineScope()
    val nestedScrollDispatcher = remember { NestedScrollDispatcher() }
    return remember(initialValue, animationSpec) {
        RevealState(
            initialValue = initialValue,
            animationSpec = animationSpec,
            confirmValueChange = confirmValueChange,
            positionalThreshold = positionalThreshold,
            anchors = anchors,
            coroutineScope = coroutineScope,
            nestedScrollDispatcher = nestedScrollDispatcher
        )
    }
}

/**
 * A composable that can be used to add extra actions to a composable (up to two) which will be
 * revealed when the original composable is swiped to the left. This composable requires
 * a primary swipe/click action, a secondary optional click action can also be provided.
 *
 * When the composable reaches the state where all the actions are revealed and the swipe
 * continues beyond the positional threshold defined in [RevealState], the primary action is
 * automatically triggered.
 *
 * An optional undo action can also be added. This undo action will be visible to users once the
 * [RevealValue] becomes [RevealValue.Revealed].
 *
 * It is strongly recommended to have icons represent the actions and maybe a text and icon for
 * the undo action.
 *
 * Example of SwipeToReveal with primary action and undo action
 * @sample androidx.wear.compose.foundation.samples.SwipeToRevealSample
 *
 * Example of SwipeToReveal using [RevealScope]
 * @sample androidx.wear.compose.foundation.samples.SwipeToRevealWithRevealOffset
 *
 * Example of SwipeToReveal used with Expandables
 * @sample androidx.wear.compose.foundation.samples.SwipeToRevealWithExpandables
 *
 * @param primaryAction The primary action that will be triggered in the event of a completed swipe.
 * We also strongly recommend to trigger the action when it is clicked.
 * @param modifier Optional [Modifier] for this component.
 * @param onFullSwipe An optional lambda which will be triggered when a full swipe from either of
 * the anchors is performed.
 * @param state The [RevealState] of this component. It can be used to customise the anchors
 * and threshold config of the swipeable modifier which is applied.
 * @param secondaryAction An optional action that can be added to the component. We strongly
 * recommend triggering the action when it is clicked.
 * @param undoAction The optional undo action that will be applied to the component once the
 * the [RevealState.currentValue] becomes [RevealValue.Revealed].
 * @param content The content that will be initially displayed over the other actions provided.
 */
@ExperimentalWearFoundationApi
@Composable
public fun SwipeToReveal(
    primaryAction: @Composable RevealScope.() -> Unit,
    modifier: Modifier = Modifier,
    onFullSwipe: () -> Unit = {},
    state: RevealState = rememberRevealState(),
    secondaryAction: (@Composable RevealScope.() -> Unit)? = null,
    undoAction: (@Composable RevealScope.() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val revealScope = remember(state) { RevealScopeImpl(state) }
    // A no-op NestedScrollConnection which does not consume scroll/fling events
    val noOpNestedScrollConnection = remember { object : NestedScrollConnection {} }
    Box(
        modifier = modifier
            .swipeableV2(
                state = state.swipeableState,
                orientation = Orientation.Horizontal,
                enabled = state.currentValue != RevealValue.Revealed,
            )
            .swipeAnchors(
                state = state.swipeableState,
                possibleValues = state.swipeAnchors.keys
            ) { value, layoutSize ->
                val swipeableWidth = layoutSize.width.toFloat()
                // Update the total width which will be used to calculate the anchors
                revealScope.width.floatValue = swipeableWidth
                // Multiply the anchor with -1f to get the actual swipeable anchor
                -state.swipeAnchors[value]!! * swipeableWidth
            }
            // NestedScrollDispatcher sends the scroll/fling events from the node to its parent
            // and onwards including the modifier chain. Apply it in the end to let nested scroll
            // connection applied before this modifier consume the scroll/fling events.
            .nestedScroll(noOpNestedScrollConnection, state.nestedScrollDispatcher)
    ) {
        val swipeCompleted by remember {
            derivedStateOf { state.currentValue == RevealValue.Revealed }
        }
        val density = LocalDensity.current

        // Total width available for the slot(s) based on the current swipe offset
        val availableWidth = if (state.offset.isNaN()) 0.dp
        else with(density) { abs(state.offset).toDp() }
        val offsetWidth = with(density) { revealScope.revealOffset.toDp() }

        // Determines whether the secondary action will be visible based on the current
        // reveal offset
        val showSecondaryAction by remember {
            derivedStateOf {
                abs(state.offset) <= revealScope.revealOffset ||
                    state.lastActionType == RevealActionType.SecondaryAction
            }
        }
        // Determines whether both primary and secondary action should be hidden, usually the case
        // when secondary action is clicked
        val hideActions by remember {
            derivedStateOf {
                abs(state.offset) >= revealScope.revealOffset &&
                    state.lastActionType == RevealActionType.SecondaryAction
            }
        }

        // Draw the buttons only when offset is greater than zero.
        if (abs(state.offset) > 0) {
            Box(
                modifier = Modifier.matchParentSize(),
                contentAlignment = AbsoluteAlignment.CenterRight
            ) {
                AnimatedContent(
                    targetState = swipeCompleted && undoAction != null,
                    transitionSpec = {
                        if (targetState) { // Fade in the Undo composable and fade out actions
                            fadeInUndo()
                        } else { // Fade in the actions and fade out the undo composable
                            fadeOutUndo()
                        }
                    },
                    label = "AnimatedContentS2R"
                ) { displayUndo ->
                    if (displayUndo && undoAction != null) {
                        val undoActionAlpha = animateFloatAsState(
                            targetValue = if (swipeCompleted) 1f else 0f,
                            animationSpec = tween(
                                durationMillis = RAPID_ANIMATION,
                                delayMillis = FLASH_ANIMATION,
                                easing = STANDARD_IN_OUT,
                            ),
                            label = "UndoActionAlpha"
                        )
                        Row(
                            modifier = Modifier
                                .graphicsLayer { alpha = undoActionAlpha.value }
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            ActionSlot(revealScope, content = undoAction)
                        }
                    } else {
                        // Animate weight for secondary action slot.
                        val secondaryActionWeight = animateFloatAsState(
                            targetValue = if (showSecondaryAction) 1f else 0f,
                            animationSpec = tween(durationMillis = QUICK_ANIMATION),
                            label = "SecondaryActionAnimationSpec"
                        )
                        val actionContentAlpha = animateFloatAsState(
                            targetValue = if (hideActions) 0f else 1f,
                            animationSpec = tween(durationMillis = 100, easing = LinearEasing),
                            label = "ActionContentOpacity"
                        )
                        val revealedContentAlpha = animateFloatAsState(
                            targetValue = if (swipeCompleted) 0f else 1f,
                            animationSpec = tween(
                                durationMillis = FLASH_ANIMATION,
                                easing = LinearEasing
                            ),
                            label = "RevealedContentAlpha"
                        )
                        Row(
                            modifier = Modifier
                                .graphicsLayer { alpha = revealedContentAlpha.value }
                                .width(if (hideActions) offsetWidth else availableWidth),
                            horizontalArrangement = Arrangement.Absolute.Right
                        ) {
                            // weight cannot be 0 so remove the composable when weight becomes 0
                            if (secondaryAction != null && secondaryActionWeight.value > 0) {
                                Spacer(Modifier.size(SwipeToRevealDefaults.padding))
                                ActionSlot(
                                    revealScope,
                                    weight = secondaryActionWeight.value,
                                    opacity = actionContentAlpha,
                                    content = secondaryAction,
                                )
                            }
                            Spacer(Modifier.size(SwipeToRevealDefaults.padding))
                            ActionSlot(
                                revealScope,
                                content = primaryAction,
                                opacity = actionContentAlpha
                            )
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier.absoluteOffset {
                IntOffset(
                    x = state.requireOffset().roundToInt().coerceAtMost(0),
                    y = 0
                )
            }
        ) {
            content()
        }
        LaunchedEffect(state.currentValue) {
            if (state.currentValue == RevealValue.Revealed &&
                state.lastActionType == RevealActionType.None) {
                onFullSwipe()
            }
        }
    }
}

@ExperimentalWearFoundationApi
public interface RevealScope {

    /**
     * The offset, in pixels, where the revealed actions are fully visible but the existing content
     * would be left in place if the reveal action was stopped. This offset is used to create the
     * anchor for [RevealValue.Revealing].
     * If there is no such anchor defined for [RevealValue.Revealing], it returns 0.0f.
     */
    /* @FloatRange(from = 0.0) */
    public val revealOffset: Float

    /**
     * The last [RevealActionType] that was set in [RevealState]. This may not be set if
     * the state changed via interaction and not through API call.
     */
    public val lastActionType: RevealActionType
}

@OptIn(ExperimentalWearFoundationApi::class)
private class RevealScopeImpl constructor(
    val revealState: RevealState,
) : RevealScope {

    /**
     * The total width of the overlay content in pixels. Initialise to zero,
     * updated when the width changes.
     */
    val width = mutableFloatStateOf(0.0f)

    override val revealOffset: Float
        get() = width.floatValue * (revealState.swipeAnchors[RevealValue.Revealing] ?: 0.0f)

    override val lastActionType: RevealActionType
        get() = revealState.lastActionType
}

/**
 * An internal object containing some defaults used across the Swipe to reveal component.
 */
@OptIn(ExperimentalWearFoundationApi::class)
internal object SwipeToRevealDefaults {

    internal val animationSpec = SwipeableV2Defaults.AnimationSpec

    internal val padding = 2.dp

    internal const val threshold = 0.5f

    internal fun defaultThreshold() = fractionalPositionalThreshold(threshold)
}

@OptIn(ExperimentalWearFoundationApi::class)
@Composable
private fun RowScope.ActionSlot(
    revealScope: RevealScope,
    modifier: Modifier = Modifier,
    weight: Float = 1f,
    opacity: State<Float> = mutableFloatStateOf(1f),
    content: @Composable RevealScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .weight(weight)
            .graphicsLayer { alpha = opacity.value },
        contentAlignment = Alignment.Center
    ) {
        with(revealScope) {
            content()
        }
    }
}

private fun fadeInUndo(): ContentTransform =
    ContentTransform(
        // animation spec for the fading in undo action (fadeIn + scaleIn)
        targetContentEnter = fadeIn(
            animationSpec = tween(
                durationMillis = RAPID_ANIMATION,
                delayMillis = FLASH_ANIMATION,
                easing = LinearEasing,
            )
        ) + scaleIn(
            initialScale = 1.2f,
            animationSpec = tween(
                durationMillis = RAPID_ANIMATION,
                delayMillis = FLASH_ANIMATION,
                easing = STANDARD_IN_OUT
            )
        ),
        // animation spec for the fading out content and actions (fadeOut)
        initialContentExit = fadeOut(
            animationSpec = tween(
                durationMillis = FLASH_ANIMATION,
                easing = LinearEasing
            )
        )
    )

private fun fadeOutUndo(): ContentTransform =
    ContentTransform(
        // No animation, fade-in in 0 milliseconds since enter transition is mandatory
        targetContentEnter = fadeIn(
            animationSpec = tween(
                durationMillis = 0,
                delayMillis = SHORT_ANIMATION
            )
        ),

        // animation spec for the fading out undo action (fadeOut + scaleOut)
        initialContentExit = fadeOut(
            animationSpec = tween(
                durationMillis = SHORT_ANIMATION,
                easing = LinearEasing
            )
        )
    )
