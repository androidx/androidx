/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.constraintlayout.compose

import android.annotation.SuppressLint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.movableContentWithReceiverOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LookaheadLayout
import androidx.compose.ui.layout.LookaheadLayoutScope
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round
import androidx.constraintlayout.core.state.Transition.WidgetState
import androidx.constraintlayout.core.widgets.ConstraintWidget
import kotlin.math.abs
import kotlinx.coroutines.launch

/**
 * Composables within [MotionScope] may apply the [MotionScope.motion] modifier to enable animations
 * on bounds change. This means that the Layout with [MotionScope.motion] will animate everytime its
 * position or size change.
 *
 * &nbsp;
 *
 * KeyFrames then may be applied to the animation caused by [MotionScope.motion], see
 * [MotionModifierScope].
 *
 * &nbsp;
 *
 * Use [rememberMotionContent] and [MotionScope.motion] to animate layout changes caused by changing
 * the Composable's Layout parent on state changes.
 *
 * E.g.:
 *
 * ```
 * var vertOrHor by remember { mutableStateOf(false) }
 * // Declare movable content
 * val texts = rememberMotionContent { // this:MotionScope
 *     Text(text = "Hello", modifier = Modifier.motion(tween(250))) // Animate for 250ms
 *     Text(text = "World", modifier = Modifier.motion { // this:MotionModifierScope
 *         keyAttributes { // KeyFrames applied on every layout change
 *             frame(50f) { // Rotate 90° at 50% progress
 *                 rotationZ = 90f
 *             }
 *         }
 *     })
 * }
 * Motion(Modifier.fillMaxSize()) { // this:MotionScope
 *     if (vertOrHor) {
 *         Column { texts() }
 *     } else {
 *         Row { texts() }
 *     }
 * }
 * ```
 */
@ExperimentalMotionApi
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Motion(
    modifier: Modifier = Modifier,
    content: @Composable MotionScope.() -> Unit
) {
    val policy = remember {
        MeasurePolicy { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints) }
            val maxWidth = placeables.maxOf { it.width }
            val maxHeight = placeables.maxOf { it.height }
            // Position the children.
            layout(maxWidth, maxHeight) {
                placeables.forEach {
                    it.place(0, 0)
                }
            }
        }
    }
    LookaheadLayout(
        modifier = modifier,
        content = {
            val scope = remember {
                MotionScope(
                    lookaheadLayoutScope = this
                )
            }
            scope.content()
        },
        measurePolicy = policy
    )
}

@DslMarker
annotation class MotionDslScope

/**
 * Scope for the [Motion] Composable.
 *
 * Use [Modifier.motion] to enable animations on layout changes for Composables defined within this
 * scope.
 */
@ExperimentalMotionApi
@MotionDslScope
@OptIn(ExperimentalComposeUiApi::class)
class MotionScope(
    lookaheadLayoutScope: LookaheadLayoutScope
) : LookaheadLayoutScope by lookaheadLayoutScope {
    private var nextId: Int = 1000
    private var lastId: Int = nextId

    /**
     * Emit the content of every Composable within the list.
     */
    @SuppressLint("ComposableNaming") // it's easier to understand as a regular extension function
    @Composable
    fun List<@Composable MotionScope.(index: Int) -> Unit>.emit() {
        forEachIndexed { index, content ->
            content(index)
        }
    }

    /**
     * Animate layout changes.
     *
     * The duration and easing of the animation is defined by [animationSpec]. Note that this
     * [AnimationSpec] will be used to drive a progress value from 0 to 1.
     *
     * Set [ignoreAxisChanges] to `true` to prevent triggering animations when the Composable is
     * being scrolled either vertically or horizontally.
     */
    fun Modifier.motion(
        animationSpec: AnimationSpec<Float> = spring(),
        ignoreAxisChanges: Boolean = false,
        motionDescription: MotionModifierScope.() -> Unit = {},
    ): Modifier = composed {
        val dpToPxFactor = with(LocalDensity.current) { density }
        val layoutId = remember { nextId++ }
        val transitionScope = remember { MotionModifierScope(layoutId) }
        val snapshotObserver = remember {
            // We use a Snapshot observer to know when state within the DSL has changed and recompose
            // the transition object
            SnapshotStateObserver {
                it()
            }
        }
        remember {
            object : RememberObserver {
                override fun onAbandoned() {
                    // TODO: Investigate if we need to do something here
                }

                override fun onForgotten() {
                    snapshotObserver.stop()
                    snapshotObserver.clear()
                }

                override fun onRemembered() {
                    snapshotObserver.start()
                }
            }
        }
        snapshotObserver.observeReads(currentRecomposeScope, {
            it.invalidate()
        }) {
            transitionScope.reset()
            // Observe state changes within the DSL, to know when to invalidate and update the
            // Transition
            transitionScope.motionDescription()
        }

        val transitionImpl = remember {
            TransitionImpl(transitionScope.getObject())
        }
        val transitionState = remember {
            androidx.constraintlayout.core.state.Transition { dpValue -> dpValue * dpToPxFactor }
                .apply {
                    transitionImpl.applyAllTo(this)
                }
        }
        val startWidget =
            remember { ConstraintWidget().apply { stringId = layoutId.toString() } }
        val endWidget = remember { ConstraintWidget().apply { stringId = layoutId.toString() } }
        val widgetState: WidgetState = remember {
            transitionState.getWidgetState(layoutId.toString(), null, 0).apply {
                update(startWidget, 0)
                update(endWidget, 1)
            }
        }
        // TODO: Optimize all animated items at a time under a single Animatable. E.g.: If after
        //  a state change, 10 different items changed, animate them using one Animatable
        //  object, as opposed to running 10 separate Animatables doing the same thing,
        //  measure/layout calls in the LookAheadLayout MeasurePolicy might provide the clue to
        //  understand the lifecycle of intermediateLayout calls across multiple Measurables.
        val progressAnimation = remember { Animatable(0f) }
        var targetBounds: IntRect by remember { mutableStateOf(IntRect.Zero) }

        fun commitLookAheadChanges(position: IntOffset, size: IntSize) {
            targetBounds = IntRect(position, size)
        }

        var placementOffset: IntOffset by remember { mutableStateOf(IntOffset.Zero) }
        var targetOffset: IntOffset? by remember { mutableStateOf(null) }
        var targetSize: IntSize? by remember { mutableStateOf(null) }
        val lastSize: Ref<IntSize> = remember { Ref<IntSize>().apply { value = null } }
        val parentSize: Ref<IntSize> =
            remember { Ref<IntSize>().apply { value = IntSize.Zero } }
        val lastPosition: Ref<IntOffset> = remember { Ref<IntOffset>().apply { value = null } }

        LaunchedEffect(Unit) {
            launch {
                snapshotFlow {
                    targetBounds
                }.collect { target ->
                    if (target != IntRect.Zero) {
                        if (nextId != lastId) {
                            lastId = nextId
                            transitionImpl.applyAllTo(transitionState)
                        }
                        if (lastSize.value != null) {
                            @Suppress("RedundantSamConstructor")
                            endWidget.applyBounds(target)
                            widgetState.update(startWidget, 0)
                            widgetState.update(endWidget, 1)
                            val newPosition = target.topLeft
                            var skipAnimation = false
                            if (ignoreAxisChanges) {
                                val positionDelta = newPosition - lastPosition.value!!
                                val xAxisChanged = positionDelta.x != 0
                                val yAxisChanged = positionDelta.y != 0
                                skipAnimation = xAxisChanged xor yAxisChanged
                            }
                            if (!skipAnimation) {
                                val newTarget = if (progressAnimation.targetValue == 1f) 0f else 1f
                                launch {
                                    progressAnimation.animateTo(newTarget, animationSpec)
                                }
                            }
                        }
                        lastSize.value = target.size
                        lastPosition.value = target.topLeft
                    }
                    startWidget.applyBounds(target)
                }
            }
        }
        this
            .onPlaced { lookaheadScopeCoordinates, layoutCoordinates ->
                parentSize.value = lookaheadScopeCoordinates.size
                val localPosition = lookaheadScopeCoordinates
                    .localPositionOf(
                        layoutCoordinates,
                        Offset.Zero
                    )
                    .round()
                val lookAheadPosition = lookaheadScopeCoordinates
                    .localLookaheadPositionOf(
                        layoutCoordinates
                    )
                    .round()
                targetOffset = lookAheadPosition
                placementOffset = localPosition
                commitLookAheadChanges(targetOffset!!, targetSize!!)
            }
            .intermediateLayout { measurable, _, lookaheadSize ->
                targetSize = lookaheadSize
                if (targetBounds == IntRect.Zero) {
                    // Unset, this is first measure
                    val newConstraints =
                        Constraints.fixed(lookaheadSize.width, lookaheadSize.height)
                    val placeable = measurable.measure(newConstraints)
                    layout(placeable.width, placeable.height) {
                        placeable.place(targetOffset!! - placementOffset)
                    }
                } else {
                    // Following measures
                    val width: Int
                    val height: Int
                    if (progressAnimation.isRunning) {
                        val fraction =
                            1.0f - abs(progressAnimation.value - progressAnimation.targetValue)
                        widgetState.interpolate(
                            parentSize.value!!.width,
                            parentSize.value!!.height,
                            fraction,
                            transitionState
                        )
                        width = widgetState
                            .getFrame(2)
                            .width()
                        height = widgetState
                            .getFrame(2)
                            .height()
                    } else {
                        width = lastSize.value?.width ?: targetBounds.width
                        height = lastSize.value?.height ?: targetBounds.height
                    }
                    val animatedConstraints = Constraints.fixed(width, height)
                    val placeable = measurable.measure(animatedConstraints)
                    layout(placeable.width, placeable.height) {
                        if (progressAnimation.isRunning) {
                            placeWithFrameTransform(
                                placeable,
                                widgetState.getFrame(2),
                                placementOffset
                            )
                        } else {
                            val (x, y) = (lastPosition.value ?: IntOffset.Zero) - placementOffset
                            placeable.place(x, y)
                        }
                    }
                }
            }
    }

    private fun ConstraintWidget.applyBounds(rect: IntRect) {
        val position = rect.topLeft
        x = position.x
        y = position.y
        width = rect.width
        height = rect.height
    }
}

/**
 * Equivalent to [movableContentOf] with [MotionScope] as context.
 */
@ExperimentalMotionApi
@Composable
fun rememberMotionContent(content: @Composable MotionScope.() -> Unit):
    @Composable MotionScope.() -> Unit {
    return remember {
        movableContentOf(content)
    }
}

/**
 * Alternative to [movableContentOf] to generate a finite List of Composables.
 *
 * Useful when each Composable is meant to be used as an item of a List such as Row or Column.
 *
 * @see [MotionScope.emit]
 */
@ExperimentalMotionApi
@Composable
fun rememberMotionListItems(
    count: Int,
    content: @Composable MotionScope.(index: Int) -> Unit
): List<@Composable MotionScope.(index: Int) -> Unit> {
    val items = remember(count) {
        val list = mutableListOf<@Composable MotionScope.(index: Int) -> Unit>()
        for (i in 0 until count) {
            list.add(movableContentWithReceiverOf(content))
        }
        return@remember list
    }
    return items
}