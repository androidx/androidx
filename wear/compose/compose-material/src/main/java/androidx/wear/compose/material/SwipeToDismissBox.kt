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

import androidx.annotation.RestrictTo
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.LocalSwipeToDismissBackgroundScrimColor
import androidx.wear.compose.foundation.LocalSwipeToDismissContentScrimColor
import androidx.wear.compose.foundation.edgeSwipeToDismiss as foundationEdgeSwipeToDismiss

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
public fun SwipeToDismissBox(
    state: SwipeToDismissBoxState,
    modifier: Modifier = Modifier,
    backgroundScrimColor: Color = MaterialTheme.colors.background,
    contentScrimColor: Color = MaterialTheme.colors.background,
    backgroundKey: Any = SwipeToDismissKeys.Background,
    contentKey: Any = SwipeToDismissKeys.Content,
    hasBackground: Boolean = true,
    content: @Composable BoxScope.(isBackground: Boolean) -> Unit
) {
    CompositionLocalProvider(
        LocalSwipeToDismissBackgroundScrimColor provides backgroundScrimColor,
        LocalSwipeToDismissContentScrimColor provides contentScrimColor
    ) {
        androidx.wear.compose.foundation.SwipeToDismissBox(
            state = state.foundationState,
            modifier = modifier,
            backgroundKey = backgroundKey,
            contentKey = contentKey,
            userSwipeEnabled = hasBackground,
            content = content
        )
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
    contentScrimColor: Color = MaterialTheme.colors.background,
    backgroundKey: Any = SwipeToDismissKeys.Background,
    contentKey: Any = SwipeToDismissKeys.Content,
    hasBackground: Boolean = true,
    content: @Composable BoxScope.(isBackground: Boolean) -> Unit
) {
    CompositionLocalProvider(
        LocalSwipeToDismissBackgroundScrimColor provides backgroundScrimColor,
        LocalSwipeToDismissContentScrimColor provides contentScrimColor
    ) {
        androidx.wear.compose.foundation.SwipeToDismissBox(
            state = state.foundationState,
            modifier = modifier,
            onDismissed = onDismissed,
            backgroundKey = backgroundKey,
            contentKey = contentKey,
            userSwipeEnabled = hasBackground,
            content = content
        )
    }
}

/**
 * State for [SwipeToDismissBox].
 *
 * @param animationSpec The default animation that will be used to animate to a new state.
 * @param confirmStateChange Optional callback invoked to confirm or veto a pending state change.
 */
@Stable
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
        get() = convertFromFoundationSwipeToDismissValue(foundationState.currentValue)

    /**
     * The target value of the state.
     *
     * If a swipe is in progress, this is the value that the state would animate to if the
     * swipe finished. If an animation is running, this is the target value of that animation.
     * Finally, if no swipe or animation is in progress, this is the same as the [currentValue].
     */
    public val targetValue: SwipeToDismissValue
        get() = convertFromFoundationSwipeToDismissValue(foundationState.targetValue)

    /**
     * Whether the state is currently animating.
     */
    public val isAnimationRunning: Boolean
        get() = foundationState.isAnimationRunning

    /**
     * Set the state without any animation and suspend until it's set
     *
     * @param targetValue The new target value to set [currentValue] to.
     */
    public suspend fun snapTo(targetValue: SwipeToDismissValue) =
        foundationState.snapTo(convertToFoundationSwipeToDismissValue(targetValue))

    /**
     * Foundation version of the [SwipeToDismissBoxState].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val foundationState = androidx.wear.compose.foundation.SwipeToDismissBoxState(
        animationSpec = animationSpec,
        confirmStateChange = { value: androidx.wear.compose.foundation.SwipeToDismissValue ->
            confirmStateChange(convertFromFoundationSwipeToDismissValue(value))
        }
    )
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        get() = field
}

/**
 * Create a [SwipeToDismissBoxState] and remember it.
 *
 * @param animationSpec The default animation used to animate to a new state.
 * @param confirmStateChange Optional callback to confirm or veto a pending state change.
 */
@Composable
public fun rememberSwipeToDismissBoxState(
    animationSpec: AnimationSpec<Float> = SWIPE_TO_DISMISS_BOX_ANIMATION_SPEC,
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
 * Requires that the element to which this modifier is applied exists within a
 * SwipeToDismissBox which is using the same [SwipeToDismissBoxState] instance.
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
    foundationEdgeSwipeToDismiss(
        swipeToDismissBoxState = swipeToDismissBoxState.foundationState,
        edgeWidth = edgeWidth
    )

private fun convertToFoundationSwipeToDismissValue(
    value: SwipeToDismissValue
) = when (value) {
    SwipeToDismissValue.Default ->
        androidx.wear.compose.foundation.SwipeToDismissValue.Default

    SwipeToDismissValue.Dismissed ->
        androidx.wear.compose.foundation.SwipeToDismissValue.Dismissed
}

private fun convertFromFoundationSwipeToDismissValue(
    value: androidx.wear.compose.foundation.SwipeToDismissValue
) = when (value) {
    androidx.wear.compose.foundation.SwipeToDismissValue.Default ->
        SwipeToDismissValue.Default

    androidx.wear.compose.foundation.SwipeToDismissValue.Dismissed ->
        SwipeToDismissValue.Dismissed
}

private val SWIPE_TO_DISMISS_BOX_ANIMATION_SPEC =
    TweenSpec<Float>(200, 0, LinearOutSlowInEasing)
