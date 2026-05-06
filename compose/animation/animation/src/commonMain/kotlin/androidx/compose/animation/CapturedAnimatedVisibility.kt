/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.animation

import androidx.compose.animation.EnterExitState.PostExit
import androidx.compose.animation.core.InternalAnimationApi
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.createChildTransition
import androidx.compose.animation.core.rememberTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxOfOrDefault

/**
 * [CapturedAnimatedVisibility] animates the appearance and disappearance of its content as
 * [visible] changes. Unlike [AnimatedVisibility], when [visible] becomes false the child
 * composition is removed **immediately** and the exit transition animates the last captured frame
 * of the content via a graphics layer. This means the caller does not need to retain any data for
 * the content once [visible] is false.
 *
 * [CapturedAnimatedVisibility] is designed for content removal animations where there is a need to
 * immediately release resources or a desire to no longer maintain data or states during exit. Since
 * [CapturedAnimatedVisibility] displays a layer that captured the rendering of the content, it will
 * also not respond to any gestures. In contrast, content in [AnimatedVisibility] remains live in
 * composition during exit, responding to touch input and continuing internal animations (e.g.
 * shared element or infinite animations) until the exit animation finishes and the content is
 * removed from composition.
 *
 * Because the content is no longer live during exit, no [AnimatedVisibilityScope] is provided to
 * the content lambda.
 *
 * @sample androidx.compose.animation.samples.CapturedAnimatedVisibilitySample
 * @param visible controls whether the content should be visible
 * @param modifier [Modifier] for the layout
 * @param enter [EnterTransition] used for the appearing animation
 * @param exit [ExitTransition] applied to the last captured frame when disappearing
 * @param label label to differentiate from other animations in Android Studio Animation Preview
 * @param content content to appear or disappear based on [visible]
 * @see AnimatedVisibility
 */
@Composable
public fun CapturedAnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn() + expandIn(),
    exit: ExitTransition = shrinkOut() + fadeOut(),
    label: String = "CapturedAnimatedVisibility",
    content: @Composable () -> Unit,
) {
    val visibleState = remember { MutableTransitionState(visible) }
    visibleState.targetState = visible
    val transition = rememberTransition(visibleState, label)
    CapturedAnimatedVisibilityImpl(transition, { it }, modifier, enter, exit, content)
}

/**
 * Animates the appearance and disappearance of its content as [visibleState]'s target state
 * changes.
 *
 * Using a [MutableTransitionState] allows observing the state of the animation (e.g.
 * [MutableTransitionState.currentState], * [MutableTransitionState.targetState], and
 * [MutableTransitionState.isIdle]) as well as setting an initial visibility state that is different
 * than the [MutableTransitionState.targetState] to animate content upon entering composition.
 *
 * Unlike [AnimatedVisibility], when [visibleState]'s
 * [targetState][MutableTransitionState.targetState] becomes `false` the child composition is
 * removed **immediately** in [CapturedAnimatedVisibility]. The exit transition animates the last
 * captured frame of the content via a graphics layer.
 *
 * @sample androidx.compose.animation.samples.CapturedAnimatedVisibilityMutableTransitionStateSample
 * @param visibleState [MutableTransitionState] controlling visibility and allowing animation state
 *   observation
 * @param modifier [Modifier] for the layout
 * @param enter [EnterTransition] used for the appearing animation
 * @param exit [ExitTransition] applied to the last captured frame when disappearing
 * @param label label to differentiate from other animations in Android Studio Animation Preview
 * @param content content to appear or disappear based on [visibleState]
 * @see AnimatedVisibility
 */
@Composable
public fun CapturedAnimatedVisibility(
    visibleState: MutableTransitionState<Boolean>,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn() + expandIn(),
    exit: ExitTransition = shrinkOut() + fadeOut(),
    label: String = "CapturedAnimatedVisibility",
    content: @Composable () -> Unit,
) {
    val transition = rememberTransition(visibleState, label)
    CapturedAnimatedVisibilityImpl(transition, { it }, modifier, enter, exit, content)
}

private val emptyContent: @Composable () -> Unit = {}

@OptIn(InternalAnimationApi::class)
@Composable
private fun <T> CapturedAnimatedVisibilityImpl(
    transition: Transition<T>,
    visible: (T) -> Boolean,
    modifier: Modifier,
    enter: EnterTransition,
    exit: ExitTransition,
    content: @Composable () -> Unit,
) {
    if (visible(transition.targetState) || visible(transition.currentState)) {
        val childTransition =
            transition.createChildTransition(label = "EnterExitTransition") {
                transition.targetEnterExit(visible, it)
            }

        val activeEnter = childTransition.trackActiveEnter(enter)
        val activeExit = childTransition.trackActiveExit(exit)

        val isExiting = childTransition.targetState == PostExit

        val layer = rememberGraphicsLayer()
        val measurePolicy = remember { CapturedAnimatedEnterExitMeasurePolicy(layer) }
        measurePolicy.isExiting = isExiting

        Layout(
            content = if (isExiting) emptyContent else content,
            modifier =
                modifier
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val (w, h) =
                            if (isLookingAhead && isExiting) {
                                IntSize.Zero
                            } else {
                                IntSize(placeable.width, placeable.height)
                            }
                        layout(w, h) { placeable.place(0, 0) }
                    }
                    .then(
                        childTransition.createModifier(
                            enter = activeEnter,
                            exit = activeExit,
                            trackActiveEnterExit = false,
                            label = "CapturedBuiltIn",
                        )
                    )
                    .drawWithContent {
                        if (isExiting) {
                            drawLayer(layer)
                        } else {
                            drawContent()
                        }
                    },
            measurePolicy = measurePolicy,
        )
    }
}

private val UnspecifiedSize = IntSize(Int.MIN_VALUE, Int.MIN_VALUE)

private class CapturedAnimatedEnterExitMeasurePolicy(val layer: GraphicsLayer) : MeasurePolicy {
    var isExiting = false
    private var _lookaheadSize: IntSize = UnspecifiedSize
    var lookaheadSize: IntSize
        get() {
            check(_lookaheadSize != UnspecifiedSize) {
                "lookaheadSize accessed before being initialized!"
            }
            return _lookaheadSize
        }
        set(value) {
            _lookaheadSize = value
        }

    var approachSize: IntSize = IntSize.Zero

    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints,
    ): MeasureResult {
        if (!isExiting) {
            val placeables = measurables.fastMap { it.measure(constraints) }
            val w = placeables.fastMaxOfOrDefault(0) { it.width }
            val h = placeables.fastMaxOfOrDefault(0) { it.height }
            val currentSize = IntSize(w, h)

            if (isLookingAhead) {
                lookaheadSize = currentSize
            } else {
                approachSize = currentSize
            }

            return layout(w, h) { placeables.fastForEach { it.placeWithLayer(0, 0, layer) } }
        } else {
            val (w, h) =
                if (isLookingAhead) {
                    lookaheadSize
                } else {
                    approachSize
                }
            return layout(w, h) {
                // No placement needed during exit, since we'll be drawing a layer of captured
                // content.
            }
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ): Int {
        return if (!isExiting) {
            measurables.fastMaxOfOrDefault(0) { it.minIntrinsicWidth(height) }
        } else {
            if (isLookingAhead) lookaheadSize.width else approachSize.width
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
    ): Int {
        return if (!isExiting) {
            measurables.fastMaxOfOrDefault(0) { it.minIntrinsicHeight(width) }
        } else {
            if (isLookingAhead) lookaheadSize.height else approachSize.height
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ): Int {
        return if (!isExiting) {
            measurables.fastMaxOfOrDefault(0) { it.maxIntrinsicWidth(height) }
        } else {
            if (isLookingAhead) lookaheadSize.width else approachSize.width
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
    ): Int {
        return if (!isExiting) {
            measurables.fastMaxOfOrDefault(0) { it.maxIntrinsicHeight(width) }
        } else {
            if (isLookingAhead) lookaheadSize.height else approachSize.height
        }
    }
}
