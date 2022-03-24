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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Wear Material [SwipeToDismissBox] that handles the swipe-to-dismiss gesture. Takes a single
 * slot for the background (only displayed during the swipe gesture) and the foreground content.
 *
 * Example of a SwipeToDismissBox with stateful composables:
 * @sample androidx.wear.compose.material.samples.StatefulSwipeToDismissBox
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
 * For more information, see the
 * [Swipe to dismiss](https://developer.android.com/training/wearables/components/swipe-to-dismiss)
 * guide.
 *
 * @param state State containing information about ongoing swipe or animation.
 * @param onDismissed Executes when the swipe to dismiss has completed.
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
public fun SwipeToDismissBox(
    state: SwipeToDismissBoxState,
    onDismissed: () -> Unit,
    modifier: Modifier = Modifier,
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
 * TODO(b/194492134): extend API to include shortcuts for status and actions like dismissing
 * the screen.
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
    val currentValue: SwipeToDismissValue
        get() = swipeableState.currentValue

    /**
     * The target value of the state.
     *
     * If a swipe is in progress, this is the value that the state would animate to if the
     * swipe finished. If an animation is running, this is the target value of that animation.
     * Finally, if no swipe or animation is in progress, this is the same as the [currentValue].
     */
    val targetValue: SwipeToDismissValue
        get() = swipeableState.targetValue

    /**
     * Whether the state is currently animating.
     */
    val isAnimationRunning: Boolean
        get() = swipeableState.isAnimationRunning

    suspend fun snapTo(targetValue: SwipeToDismissValue) = swipeableState.snapTo(targetValue)

    companion object {
        /**
         * The default [Saver] implementation for [SwipeToDismissBox].
         */
        fun Saver(
            animationSpec: AnimationSpec<Float>,
            confirmStateChange: (SwipeToDismissValue) -> Boolean
        ): Saver<SwipeToDismissBoxState, *> = Saver(
            save = { it.currentValue },
            restore = {
                SwipeToDismissBoxState(
                    animationSpec = animationSpec,
                    confirmStateChange = confirmStateChange
                )
            }
        )
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
    return rememberSaveable(
        saver = SwipeToDismissBoxState.Saver(
            animationSpec = animationSpec,
            confirmStateChange = confirmStateChange,
        )
    ) {
        SwipeToDismissBoxState(
            animationSpec = animationSpec,
            confirmStateChange = confirmStateChange,
        )
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
}

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
