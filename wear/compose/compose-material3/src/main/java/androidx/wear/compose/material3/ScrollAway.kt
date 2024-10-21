/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.material3

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.wear.compose.foundation.ScrollInfoProvider
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.material3.tokens.MotionTokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Scroll an item vertically in/out of view based on scroll state provided by a scrolling list.
 * Typically used to scroll a [TimeText] item out of view as the user starts to scroll a vertically
 * scrollable list of items upwards and bring additional items into view.
 *
 * Example of using ScrollAway directly (in practice, it is recommended to use [AppScaffold] and
 * [ScreenScaffold] to provide the correct scroll away behavior by default):
 *
 * @sample androidx.wear.compose.material3.samples.ScrollAwaySample
 * @param scrollInfoProvider Used as the basis for the scroll-away implementation, based on the
 *   state of the scrollable container. See [ScrollInfoProvider] methods for creating a
 *   ScrollInfoProvider from common lists such as [ScalingLazyListState].
 * @param screenStage Function that returns the screen stage of the active screen. Scrolled away
 *   items are shown when the screen is new, then scrolled away or hidden when scrolling, and
 *   finally shown again when idle.
 */
fun Modifier.scrollAway(
    scrollInfoProvider: ScrollInfoProvider,
    screenStage: () -> ScreenStage
): Modifier = this then ScrollAwayModifierElement(scrollInfoProvider, screenStage)

/**
 * [ScreenStage] represents the different stages for a screen, which affect visibility of scaffold
 * components such as [TimeText] and [ScrollIndicator] with [scrollAway] and other animations.
 */
@Immutable
@JvmInline
value class ScreenStage internal constructor(internal val value: Int) {
    companion object {
        /**
         * Initial stage for a screen when first displayed. It is expected that the [TimeText] and
         * [ScrollIndicator] are displayed when initially showing a screen.
         */
        val New = ScreenStage(0)

        /**
         * Stage when both the screen is not scrolling and some time has passed after the screen was
         * initially shown. At this stage, the [TimeText] is expected to be displayed and the
         * [ScrollIndicator] will be hidden.
         */
        val Idle = ScreenStage(1)

        /**
         * Stage when the screen is being scrolled. At this stage, it is expected that the
         * [ScrollIndicator] will be shown and [TimeText] will be scrolled away by the scroll
         * operation.
         */
        val Scrolling = ScreenStage(2)
    }

    override fun toString() =
        when (this) {
            New -> "New"
            Idle -> "Idle"
            Scrolling -> "Scrolling"
            else -> "Unknown"
        }
}

private data class ScrollAwayModifierElement(
    val scrollInfoProvider: ScrollInfoProvider,
    val screenStage: () -> ScreenStage
) : ModifierNodeElement<ScrollAwayModifierNode>() {
    override fun create(): ScrollAwayModifierNode =
        ScrollAwayModifierNode(scrollInfoProvider, screenStage)

    override fun update(node: ScrollAwayModifierNode) {
        node.scrollInfoProvider = scrollInfoProvider
        node.screenStage = screenStage
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "scrollAway"
        properties["scrollInfoProvider"] = scrollInfoProvider
    }
}

private class ScrollAwayModifierNode(
    var scrollInfoProvider: ScrollInfoProvider,
    var screenStage: () -> ScreenStage,
) : LayoutModifierNode, Modifier.Node() {
    private val progressAnimatable: Animatable<Float, AnimationVector1D> = Animatable(0f)
    private val alphaAnimatable = Animatable(1f)

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0) {
                val offsetPx = scrollInfoProvider.anchorItemOffset

                // Progress 0 means the item is fully visible, 1 means is scaled and alphaed 50%
                var (targetProgress, targetAlpha) =
                    if (!scrollInfoProvider.isScrollAwayValid) {
                        // When the scroll info provide cannot provide a valid anchor item offset
                        // due to its configuration (e.g. the anchor item index does not exist),
                        // just show the content anyway.
                        0f to 1f
                    } else if (offsetPx.isNaN() || offsetPx > maxScrollOut.toPx()) {
                        // Offset==NaN means the offset is invalid. If offset is invalid
                        // or out of range, we infer that the tracked item is not in the visible
                        // items list, so hide it.
                        1f to 0f
                    } else {
                        // Scale, fade and scroll the content to scroll it away.
                        (offsetPx / maxScrollOut.toPx()).coerceIn(0f, 1f) to 1f
                    }

                val screenStage = screenStage()

                // When idle, or on a new screen, if it's hidden, show it
                val showAfterTimeout =
                    screenStage != ScreenStage.Scrolling &&
                        (targetAlpha == 0f || targetProgress > timeTextVisibilityThreshold)
                if (showAfterTimeout) {
                    targetProgress = 0f
                    targetAlpha = 1f
                }

                if (targetAlpha != alphaAnimatable.targetValue) {
                    if (targetAlpha == 0f) {
                        // Hide is instant.
                        coroutineScope.launch { alphaAnimatable.snapTo(0f) }
                    } else {
                        animateAlpha(targetAlpha, coroutineScope, alphaAnimatable)
                    }
                }

                if (targetProgress != progressAnimatable.targetValue) {
                    val scrollingAtTheTop =
                        targetProgress != 0f &&
                            progressAnimatable.targetValue != 0f &&
                            screenStage() != ScreenStage.New
                    if (scrollingAtTheTop || (showAfterTimeout && screenStage != ScreenStage.New)) {
                        // If we are scrolling at the top, keep in sync.
                        // Also, when we show after a timeout, there is no progress animation.
                        coroutineScope.launch { progressAnimatable.snapTo(targetProgress) }
                    } else {
                        animateProgress(targetProgress, coroutineScope, progressAnimatable)
                    }
                }

                val motionFraction = lerp(minMotionOut, maxMotionOut, progressAnimatable.value)
                val offsetY = -(maxOffset.toPx() * progressAnimatable.value)
                alpha = motionFraction * alphaAnimatable.value
                scaleX = motionFraction
                scaleY = motionFraction
                translationY = offsetY
                transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 0.0f)
            }
        }
    }

    private fun animateProgress(
        targetValue: Float,
        coroutineScope: CoroutineScope,
        animatable: Animatable<Float, AnimationVector1D>
    ) {
        coroutineScope.launch {
            animatable.animateTo(
                targetValue = targetValue,
                animationSpec =
                    tween(
                        durationMillis = MotionTokens.DurationShort4,
                        easing = MotionTokens.EasingStandard
                    )
            )
        }
    }

    private fun animateAlpha(
        targetAlpha: Float,
        coroutineScope: CoroutineScope,
        animatable: Animatable<Float, AnimationVector1D>,
    ) {
        coroutineScope.launch {
            animatable.animateTo(
                targetValue = targetAlpha,
                animationSpec =
                    tween(
                        durationMillis = MotionTokens.DurationMedium1,
                        easing =
                            if (targetAlpha > 0.5f) {
                                // Animation spec for showing the TimeText
                                MotionTokens.EasingStandard
                            } else {
                                // Animation spec for hidding the TimeText
                                MotionTokens.EasingStandardDecelerate
                            },
                    )
            )
        }
    }
}

internal val maxScrollOut = 36.dp
internal val maxOffset = 24.dp
// Fade and scale motion effects are between 100% and 50%.
internal const val minMotionOut = 1f
internal const val maxMotionOut = 0.5f
// Threshold to determine if the time text should be re-displayed after scrolling away.
internal const val timeTextVisibilityThreshold = 0.55f
