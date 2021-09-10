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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

/**
 * Wear Material [SwipeToDismissBox] that handles the swipe-to-dismiss gesture. Takes a single
 * slot for the background (only displayed during the swipe gesture) and the foreground content.
 *
 * [SwipeToDismissBox] has not yet been integrated with Android's
 * default handling for swipe to dismiss on Wear applications. Until that is completed,
 * applications using [SwipeToDismissBox] must disable android:windowSwipeToDismiss.
 *
 * Example usage:
 * @sample androidx.wear.compose.material.samples.SimpleSwipeToDismissBox
 *
 * @param state State containing information about ongoing swipe or animation.
 * @param modifier Optional [Modifier] for this component.
 * @param scrimColor Optional [Color] used for the scrim over the background and
 * content composables during the swipe gesture. The alpha on the color is ignored,
 * instead being set individually for each of the background and content layers in order to
 * indicate to the user which state will result if the gesture is released.
 * @param backgroundKey Optional [key] which identifies the content currently composed in
 * the [content] block when isBackground == true. Provide the backgroundKey if your background
 * content will be displayed as a foreground after the swipe animation ends
 * (as is common when [SwipeToDismissBox] is used for the navigation). This allows
 * remembered state to be correctly moved between background and foreground.
 * @Param contentKey Optional [key] which identifies the content currently composed in the
 * [content] block when isBackground == false. See [backgroundKey].
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
    scrimColor: Color = MaterialTheme.colors.surface,
    backgroundKey: Any = SwipeToDismissBoxDefaults.BackgroundKey,
    contentKey: Any = SwipeToDismissBoxDefaults.ContentKey,
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
                anchors = anchors(maxWidth),
                thresholds = { _, _ -> FractionalThreshold(SwipeThreshold) },
                orientation = Orientation.Horizontal
            )
    ) {
        val offsetPx = state.offset.value.roundToInt()
        // This temporary variable added to workaround an error
        // thrown by the compose runtime - see b/199136503, avoids "Invalid Start Index" exception.
        val pxModifier = Modifier.offset { IntOffset(offsetPx, 0) }.fillMaxSize()
        repeat(2) {
            val isBackground = it == 0
            // TODO(b/193606660): Add animations that follow after swipe confirmation.
            val scrimAlpha =
                if (state.targetValue == SwipeDismissTarget.Original) {
                    if (isBackground) SwipeStartedBackgroundAlpha else SwipeStartedContentAlpha
                } else {
                    if (isBackground) SwipeConfirmedBackgroundAlpha else SwipeConfirmedContentAlpha
                }
            val contentModifier = if (isBackground) Modifier.fillMaxSize() else pxModifier
            key(if (isBackground) backgroundKey else contentKey) {
                if (!isBackground || offsetPx > 0) {
                    Box(contentModifier) {
                        // We use the repeat loop above and call content at this location
                        // for both background and foreground so that any persistence
                        // within the content composable has the same call stack which is used
                        // as part of the hash identity for saveable state.
                        content(isBackground)
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(scrimColor.copy(alpha = scrimAlpha))
                        )
                    }
                }
            }
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

private val DefaultBackgroundKey = "background"
private val DefaultContentKey = "content"
private val SwipeStartedBackgroundAlpha = 0.5f
private val SwipeConfirmedBackgroundAlpha = 0.0f
private val SwipeStartedContentAlpha = 0.0f
private val SwipeConfirmedContentAlpha = 0.5f
private val SwipeThreshold = 0.5f
