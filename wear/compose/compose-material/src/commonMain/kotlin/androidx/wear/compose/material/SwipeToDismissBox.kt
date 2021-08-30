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

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

/**
 * Wear Material [SwipeToDismissBox] that handles the swipe-to-dismiss gesture. Takes two slots,
 * the background (only displayed during the swipe gesture) and a content slot.
 *
 * Example usage:
 * @sample androidx.wear.compose.material.samples.SimpleSwipeToDismissBox
 *
 * @param state State containing information about ongoing swipe or animation.
 * @param modifier Optional [Modifier] for this component.
 * @param background Optional slot for content to be displayed behind the foreground content -
 * the background is normally hidden, is shown behind a scrim during the swipe gesture,
 * and is shown without scrim once the finger passes the swipe-to-dismiss threshold.
 * @param scrimColor Optional [Color] used for the scrim over the background and
 * content composables during the swipe gesture. The alpha on the color is ignored,
 * instead being set individually for each of the background and content layers in order to
 * indicate to the user which state will result if the gesture is released.
 */
@Composable
@ExperimentalWearMaterialApi
fun SwipeToDismissBox(
    state: SwipeToDismissBoxState,
    modifier: Modifier = Modifier,
    background: (@Composable BoxScope.() -> Unit)? = null,
    scrimColor: Color = MaterialTheme.colors.surface,
    content: @Composable BoxScope.() -> Unit
) = BoxWithConstraints(modifier) {
    val maxWidth = constraints.maxWidth.toFloat()
    // Map pixel position to states - initially, don't know the width in pixels so omit upper bound.
    val anchors =
        mapOf(
            0f to SwipeDismissTarget.Original,
            maxWidth to SwipeDismissTarget.Dismissal
        )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .swipeable(
                state = state,
                anchors = anchors,
                thresholds = { _, _ -> FractionalThreshold(SwipeThreshold) },
                orientation = Orientation.Horizontal
            )
    ) {
        val offsetPx = state.offset.value.roundToInt()
        if (background != null && offsetPx > 0) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                background()
                // TODO(b/193606660): Add animations that follow after swipe confirmation.
                val backgroundScrimAlpha =
                    if (state.targetValue == SwipeDismissTarget.Original) {
                        SwipeStartedBackgroundAlpha
                    } else {
                        SwipeConfirmedBackgroundAlpha
                    }
                Box(
                    modifier =
                        Modifier
                            .matchParentSize()
                            .background(
                                scrimColor.copy(alpha = backgroundScrimAlpha)
                            )
                )
            }
        }
        Box(
            Modifier
                .offset { IntOffset(offsetPx, 0) }
                .fillMaxSize()
        ) {
            content()
            val contentScrimAlpha =
                if (state.targetValue == SwipeDismissTarget.Original) {
                    SwipeStartedContentAlpha
                } else {
                    SwipeConfirmedContentAlpha
                }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        scrimColor.copy(alpha = contentScrimAlpha)
                    )
            )
        }
    }
}

@Stable
/**
 * State for [SwipeToDismissBox].
 *
 * TODO(b/194492134): extend API to include shortcuts for status and actions like dismissing
 * the screen.
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
    public val AnimationSpec = SwipeableDefaults.AnimationSpec
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

private val SwipeStartedBackgroundAlpha = 0.5f
private val SwipeConfirmedBackgroundAlpha = 0.0f
private val SwipeStartedContentAlpha = 0.0f
private val SwipeConfirmedContentAlpha = 0.5f
private val SwipeThreshold = 0.5f
