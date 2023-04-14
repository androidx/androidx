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

import androidx.compose.animation.core.AnimationSpec
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Different values which the swipeable modifier can be configured to.
 */
@ExperimentalWearFoundationApi
@JvmInline
public value class RevealValue private constructor(val value: Int) {
    companion object {
        /**
         * The default first value which generally represents the state where the revealable
         * actions has not been revealed yet.
         */
        val Covered = RevealValue(0)

        /**
         * The value which represents the state in which all the actions are revealed and the
         * top content is stable.
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
    internal val anchors: Map<RevealValue, Float>
) {
    /**
     * [SwipeableV2State] internal instance for the state.
     */
    internal val swipeableState = SwipeableV2State(
        initialValue = initialValue,
        animationSpec = animationSpec,
        confirmValueChange = confirmValueChange,
        positionalThreshold = positionalThreshold
    )

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
     * @see Modifier.swipeableV2
     */
    public suspend fun snapTo(targetValue: RevealValue) = swipeableState.snapTo(targetValue)

    /**
     * Animates to the [targetValue] with the animation spec provided.
     *
     * @param targetValue The target [RevealValue] where the [currentValue] will animation
     * to.
     */
    public suspend fun animateTo(targetValue: RevealValue) =
        swipeableState.animateTo(targetValue)

    /**
     * Require the current offset.
     *
     * @throws IllegalStateException If the offset has not been initialized yet
     */
    internal fun requireOffset(): Float = swipeableState.requireOffset()
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
    anchors: Map<RevealValue, Float> = createAnchors()
): RevealState {
    return remember(initialValue, animationSpec) {
        RevealState(
            initialValue = initialValue,
            animationSpec = animationSpec,
            confirmValueChange = confirmValueChange,
            positionalThreshold = positionalThreshold,
            anchors = anchors
        )
    }
}

/**
 * A composable that be used to add extra actions to a composable (up to two) which will be
 * revealed when the original composable is swiped to the left. This composable mandates
 * at least a single extra action, with an optional action along with mandatory one.
 *
 * When the composable reaches to the state where all the actions are revealed and the swiping
 * continues beyond the positional threshold defined in [RevealState], the mandatory action gets
 * triggered automatically.
 *
 * An optional undo action can also be added when the actions are triggered. This undo action will
 * be visible to users once the [RevealValue] becomes [RevealValue.Revealed].
 *
 * It is strongly recommended to have icons represent the actions and maybe a text and icon for
 * the undo action.
 *
 * TODO: Add Sample
 *
 * @param action The mandatory action that needs to be added to the component.
 * @param modifier Optional [Modifier] for this component.
 * @param state The [RevealState] of this component. It can be used to customise the anchors
 * and threshold config of the swipeable modifier which is applied.
 * @param onFullSwipe An optional lambda which will be triggered when a full swipe from either of
 * the anchors is performed.
 * @param additionalAction The optional action that can be added to the component.
 * @param undoAction The optional undo action that will be applied to the component once the
 * mandatory action has been performed.
 * @param content The content that will be initially displayed over the other actions provided.
 */
@ExperimentalWearFoundationApi
@Composable
public fun SwipeToReveal(
    action: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onFullSwipe: () -> Unit = {},
    state: RevealState = rememberRevealState(),
    additionalAction: (@Composable () -> Unit)? = null,
    undoAction: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) = Box(
    modifier = modifier
        .swipeableV2(
            state = state.swipeableState,
            orientation = Orientation.Horizontal,
            enabled = true,
        )
        .swipeAnchors(
            state = state.swipeableState,
            possibleValues = state.swipeAnchors.keys
        ) { value, layoutSize ->
            val swipeableWidth = layoutSize.width.toFloat()
            // Multiply the anchor with -1f to get the actual swipeable anchor
            -state.swipeAnchors[value]!! * swipeableWidth
        }
) {
    val swipeCompleted by remember {
        derivedStateOf { state.currentValue == RevealValue.Revealed }
    }
    val density = LocalDensity.current

    // Total width available for the slot(s) based on the current swipe offset
    val availableWidth = if (state.offset.isNaN()) 0.dp
        else with(density) { abs(state.offset).toDp() }
    // Total padding between the slots based on the available actions
    val totalPadding = if (additionalAction != null) SwipeToRevealDefaults.padding * 2
        else SwipeToRevealDefaults.padding

    Row(
        modifier = Modifier.matchParentSize(),
        horizontalArrangement = Arrangement.Absolute.Right
    ) {
        if (swipeCompleted && undoAction != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                ActionSlot(content = undoAction)
            }
        } else {
            Row(
                modifier = Modifier.width(availableWidth.minus(totalPadding)),
                horizontalArrangement = Arrangement.Absolute.Right
            ) {
                if (additionalAction != null) {
                    Spacer(Modifier.size(SwipeToRevealDefaults.padding))
                    ActionSlot(content = additionalAction)
                }
                Spacer(Modifier.size(SwipeToRevealDefaults.padding))
                ActionSlot(content = action)
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
        if (state.currentValue == RevealValue.Revealed) {
            onFullSwipe()
        }
    }
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

@Composable
private fun RowScope.ActionSlot(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.fillMaxHeight().weight(1f),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}