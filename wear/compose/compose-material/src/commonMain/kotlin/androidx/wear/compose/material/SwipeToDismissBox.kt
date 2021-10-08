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
 * Example of a simple SwipeToDismissBox with a different color displayed behind the content:
 * @sample androidx.wear.compose.material.samples.SimpleSwipeToDismissBox
 *
 * For more information, see the
 * [Swipe to dismiss](https://developer.android.com/training/wearables/components/swipe-to-dismiss)
 * guide.
 *
 * Example of a SwipeToDismissBox with stateful composables:
 * @sample androidx.wear.compose.material.samples.StatefulSwipeToDismissBox
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
@ExperimentalWearMaterialApi
fun SwipeToDismissBox(
    state: SwipeToDismissBoxState,
    modifier: Modifier = Modifier,
    backgroundScrimColor: Color = MaterialTheme.colors.background,
    contentScrimColor: Color = contentColorFor(backgroundScrimColor),
    backgroundKey: Any = SwipeToDismissBoxDefaults.BackgroundKey,
    contentKey: Any = SwipeToDismissBoxDefaults.ContentKey,
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
                state = state,
                enabled = hasBackground,
                anchors = anchors(maxWidth),
                thresholds = { _, _ -> FractionalThreshold(SwipeThreshold) },
                orientation = Orientation.Horizontal
            )
    ) {
        val offsetPx = state.offset.value.roundToInt()
        val dismissAnimatable = remember { Animatable(0f) }

        LaunchedEffect(state.isAnimationRunning) {
            if (state.targetValue == SwipeDismissTarget.Dismissal) {
                dismissAnimatable.animateTo(1f, SpringSpec())
            } else {
                // because SwipeToDismiss remains alive, it worth resetting animation to 0
                // when [targetValue] becomes [Original] again
                dismissAnimatable.snapTo(0f)
            }
        }

        val squeezeMotion = SqueezeMotion(offsetPx, maxWidth)

        val contentForegroundModifier =
            Modifier.offset { IntOffset(squeezeMotion.contentOffset, 0) }
                .fillMaxSize()
                .scale(squeezeMotion.scale(dismissAnimatable.value))
                .then(
                    if (isRoundDevice() && squeezeMotion.contentOffset > 0) {
                        Modifier.clip(CircleShape)
                    } else {
                        Modifier
                    }
                )
                .alpha(1 - dismissAnimatable.value)
                .background(backgroundScrimColor)

        val contentBackgroundModifier = Modifier.fillMaxSize()

        val scrimForegroundModifier = Modifier.background(
            contentScrimColor.copy(alpha = squeezeMotion.contentScrimAlpha)
        ).fillMaxSize()

        val scrimBackgroundModifier = Modifier.matchParentSize()
            .background(
                backgroundScrimColor
                    .copy(
                        alpha = squeezeMotion.backgroundScrimAlpha(
                            dismissAnimatable.value
                        )
                    )
            )

        repeat(2) {
            val isBackground = it == 0

            val contentModifier = if (isBackground) {
                contentBackgroundModifier
            } else {
                contentForegroundModifier
            }

            val scrimModifier = if (isBackground) {
                scrimBackgroundModifier
            } else {
                scrimForegroundModifier
            }

            key(if (isBackground) backgroundKey else contentKey) {
                if (!isBackground || (hasBackground && offsetPx > 0)) {
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
@ExperimentalWearMaterialApi
class SwipeToDismissBoxState(
    animationSpec: AnimationSpec<Float> = SwipeToDismissBoxDefaults.AnimationSpec,
    confirmStateChange: (SwipeDismissTarget) -> Boolean = { true },
) : SwipeableState<SwipeDismissTarget>(
    initialValue = SwipeDismissTarget.Original,
    animationSpec = animationSpec,
    confirmStateChange = confirmStateChange,
) {
    companion object {
        /**
         * The default [Saver] implementation for [SwipeToDismissBox].
         */
        fun Saver(
            animationSpec: AnimationSpec<Float>,
            confirmStateChange: (SwipeDismissTarget) -> Boolean
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
}

/**
 * Create a [SwipeToDismissBoxState] and remember it.
 *
 * @param animationSpec The default animation used to animate to a new state.
 * @param confirmStateChange Optional callback to confirm or veto a pending state change.
 */
@Composable
@ExperimentalWearMaterialApi
fun rememberSwipeToDismissBoxState(
    animationSpec: AnimationSpec<Float> = SwipeToDismissBoxDefaults.AnimationSpec,
    confirmStateChange: (SwipeDismissTarget) -> Boolean = { true },
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
@ExperimentalWearMaterialApi
public object SwipeToDismissBoxDefaults {
    /**
     * The default animation that will be used to animate to a new state after the swipe gesture.
     */
    public val AnimationSpec = SwipeableDefaults.AnimationSpec

    /**
     * The default background key to identify the content displayed by the content block
     * when isBackground == true. Specifying a background key instead of using the default
     * allows remembered state to be correctly moved between background and foreground.
     */
    public val BackgroundKey: Any = "background"

    /**
     * The default content key to identify the content displayed by the content block
     * when isBackground == false. Specifying a background key instead of using the default
     * allows remembered state to be correctly moved between background and foreground.
     */
    public val ContentKey: Any = "content"
}

/**
 * States used as targets for the anchor points for swipe-to-dismiss.
 */
@ExperimentalWearMaterialApi
public enum class SwipeDismissTarget {
    /**
     * The state of the SwipeToDismissBox before the swipe started.
     */
    Original,

    /**
     * The state of the SwipeToDismissBox after the swipe passes the swipe-to-dismiss threshold.
     */
    Dismissal
}

// Map pixel position to states - initially, don't know the width in pixels so omit upper bound.
@ExperimentalWearMaterialApi
private fun anchors(maxWidth: Float): Map<Float, SwipeDismissTarget> =
    mapOf(
        0f to SwipeDismissTarget.Original,
        maxWidth to SwipeDismissTarget.Dismissal
    )

private val SwipeStartedBackgroundAlpha = 0.5f
private val SwipeConfirmedBackgroundAlpha = 0.0f
private val SwipeStartedContentAlpha = 0.0f
private val SwipeConfirmedContentAlpha = 0.5f
private val SwipeThreshold = 0.5f
