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

package androidx.constraintlayout.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.layout.LayoutScopeMarker
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MultiMeasureLayout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.core.widgets.Optimizer
import kotlinx.coroutines.channels.Channel

/**
 * Measure flags for MotionLayout
 */
@Deprecated("Unnecessary, MotionLayout remeasures when its content changes.")
enum class MotionLayoutFlag(@Suppress("UNUSED_PARAMETER") value: Long) {
    Default(0),

    @Suppress("unused")
    FullMeasure(1)
}

enum class MotionLayoutDebugFlags {
    NONE,
    SHOW_ALL,
    UNKNOWN
}

/**
 * Layout that can animate between two different layout states described in [ConstraintSet]s.
 *
 * &nbsp;
 *
 * The animation is driven by the [progress] value, so it will typically be a result of
 * using an [Animatable][androidx.compose.animation.core.Animatable] or
 * [animateFloatAsState][androidx.compose.animation.core.animateFloatAsState]:
 * ```
 *  var animateToEnd by remember { mutableStateOf(false) }
 *  MotionLayout(
 *      start = ConstraintSet {
 *          constrain(createRefFor("button")) {
 *              top.linkTo(parent.top)
 *          }
 *      },
 *      end = ConstraintSet {
 *          constrain(createRefFor("button")) {
 *              bottom.linkTo(parent.bottom)
 *          }
 *      },
 *      progress = animateFloatAsState(if (animateToEnd) 1f else 0f).value,
 *      modifier = Modifier.fillMaxSize()
 *  ) {
 *      Button(onClick = { animateToEnd = !animateToEnd }, Modifier.layoutId("button")) {
 *          Text("Hello, World!")
 *      }
 *  }
 * ```
 *
 * Note that you must use [Modifier.layoutId][androidx.compose.ui.layout.layoutId] to bind the
 * the references used in the [ConstraintSet]s to the Composable.
 *
 * @param start ConstraintSet that defines the layout at 0f progress.
 * @param end ConstraintSet that defines the layout at 1f progress.
 * @param progress Sets the interpolated position of the layout between the ConstraintSets.
 * @param modifier Modifier to apply to this layout node.
 * @param transition Defines the interpolation parameters between the [ConstraintSet]s to achieve
 * fine-tuned animations.
 * @param optimizationLevel Optimization parameter for the underlying ConstraintLayout,
 * [Optimizer.OPTIMIZATION_STANDARD] by default.
 * @param debugFlags Flags to enable visual debugging. [DebugFlags.None] by default.
 * @param content The content to be laid out by MotionLayout, note that each layout Composable
 * should be bound to an ID defined in the [ConstraintSet]s using
 * [Modifier.layoutId][androidx.compose.ui.layout.layoutId].
 */
@ExperimentalMotionApi
@Composable
inline fun MotionLayout(
    start: ConstraintSet,
    end: ConstraintSet,
    progress: Float,
    modifier: Modifier = Modifier,
    transition: Transition? = null,
    debugFlags: DebugFlags = DebugFlags.None,
    optimizationLevel: Int = Optimizer.OPTIMIZATION_STANDARD,
    crossinline content: @Composable MotionLayoutScope.() -> Unit
) {
    /**
     * MutableState used to track content recompositions. It's reassigned at the content's
     * composition scope, so that any function reading it is recomposed with the content.
     * NeverEqualPolicy is used so that we don't have to assign any particular value to trigger a
     * State change.
     */
    val contentTracker = remember { mutableStateOf(Unit, neverEqualPolicy()) }
    val compositionSource =
        remember { Ref<CompositionSource>().apply { value = CompositionSource.Unknown } }

    /**
     * Delegate to handle composition tracking before calling the non-inline Composable
     */
    val contentDelegate: @Composable MotionLayoutScope.() -> Unit = {
        // Perform a reassignment to the State tracker, this will force readers to recompose at
        // the same pass as the content. The only expected reader is our MeasurePolicy.
        contentTracker.value = Unit

        if (compositionSource.value == CompositionSource.Unknown) {
            // Set the content as the original composition source if the MotionLayout was not
            // recomposed by the caller or by itself
            compositionSource.value = CompositionSource.Content
        }
        content()
    }
    MotionLayoutCore(
        start = start,
        end = end,
        transition = transition,
        progress = progress,
        informationReceiver = null,
        optimizationLevel = optimizationLevel,
        showBounds = debugFlags.showBounds,
        showPaths = debugFlags.showPaths,
        showKeyPositions = debugFlags.showKeyPositions,
        modifier = modifier,
        contentTracker = contentTracker,
        compositionSource = compositionSource,
        content = contentDelegate
    )
}

/**
 * Layout that can animate between multiple [ConstraintSet]s as defined by [Transition]s in the
 * given [MotionScene].
 *
 * &nbsp;
 *
 * The animation is driven by the [progress] value, so it will typically be a result of
 * using an [Animatable][androidx.compose.animation.core.Animatable] or
 * [animateFloatAsState][androidx.compose.animation.core.animateFloatAsState]:
 * ```
 *  var animateToEnd by remember { mutableStateOf(false) }
 *  MotionLayout(
 *      motionScene = MotionScene {
 *          val buttonRef = createRefFor("button")
 *          defaultTransition(
 *              from = constraintSet {
 *                  constrain(buttonRef) {
 *                      top.linkTo(parent.top)
 *                  }
 *              },
 *              to = constraintSet {
 *                  constrain(buttonRef) {
 *                      bottom.linkTo(parent.bottom)
 *                  }
 *              }
 *          )
 *      },
 *      progress = animateFloatAsState(if (animateToEnd) 1f else 0f).value,
 *      modifier = Modifier.fillMaxSize()
 *  ) {
 *      Button(onClick = { animateToEnd = !animateToEnd }, Modifier.layoutId("button")) {
 *          Text("Hello, World!")
 *      }
 *  }
 * ```
 *
 * Note that you must use [Modifier.layoutId][androidx.compose.ui.layout.layoutId] to bind the
 * the references used in the [ConstraintSet]s to the Composable.
 *
 * @param motionScene Holds all the layout states defined in [ConstraintSet]s and the
 * interpolation associated between them (known as [Transition]s).
 * @param progress Sets the interpolated position of the layout between the ConstraintSets.
 * @param modifier Modifier to apply to this layout node.
 * @param transitionName The name of the transition to apply on the layout. By default, it will
 * target the transition defined with [MotionSceneScope.defaultTransition].
 * @param optimizationLevel Optimization parameter for the underlying ConstraintLayout,
 * [Optimizer.OPTIMIZATION_STANDARD] by default.
 * @param debugFlags Flags to enable visual debugging. [DebugFlags.None] by default.
 * @param content The content to be laid out by MotionLayout, note that each layout Composable
 * should be bound to an ID defined in the [ConstraintSet]s using
 * [Modifier.layoutId][androidx.compose.ui.layout.layoutId].
 */
@ExperimentalMotionApi
@Composable
inline fun MotionLayout(
    motionScene: MotionScene,
    progress: Float,
    modifier: Modifier = Modifier,
    transitionName: String = "default",
    debugFlags: DebugFlags = DebugFlags.None,
    optimizationLevel: Int = Optimizer.OPTIMIZATION_STANDARD,
    crossinline content: @Composable (MotionLayoutScope.() -> Unit),
) {
    /**
     * MutableState used to track content recompositions. It's reassigned at the content's
     * composition scope, so that any function reading it is recomposed with the content.
     * NeverEqualPolicy is used so that we don't have to assign any particular value to trigger a
     * State change.
     */
    val contentTracker = remember { mutableStateOf(Unit, neverEqualPolicy()) }
    val compositionSource =
        remember { Ref<CompositionSource>().apply { value = CompositionSource.Unknown } }

    /**
     * Delegate to handle composition tracking before calling the non-inline Composable
     */
    val contentDelegate: @Composable MotionLayoutScope.() -> Unit = {
        // Perform a reassignment to the State tracker, this will force readers to recompose at
        // the same pass as the content. The only expected reader is our MeasurePolicy.
        contentTracker.value = Unit

        if (compositionSource.value == CompositionSource.Unknown) {
            // Set the content as the original composition source if the MotionLayout was not
            // recomposed by the caller or by itself
            compositionSource.value = CompositionSource.Content
        }
        content()
    }

    MotionLayoutCore(
        motionScene = motionScene,
        progress = progress,
        transitionName = transitionName,
        optimizationLevel = optimizationLevel,
        debugFlags = debugFlags,
        modifier = modifier,
        contentTracker = contentTracker,
        compositionSource = compositionSource,
        content = contentDelegate
    )
}

/**
 * Layout that can animate between multiple [ConstraintSet]s as defined by [Transition]s in the
 * given [MotionScene].
 *
 * &nbsp;
 *
 * The animation is driven based on the given [constraintSetName]. During recomposition,
 * MotionLayout will interpolate from whichever [ConstraintSet] it currently is, to the one
 * corresponding to [constraintSetName]. So, a null [constraintSetName] will result in no changes.
 *
 * ```
 *  var name by remember { mutableStateOf(0) }
 *  MotionLayout(
 *      motionScene = MotionScene {
 *          val buttonRef = createRefFor("button")
 *          val initialStart = constraintSet("0") {
 *              constrain(buttonRef) {
 *                  centerHorizontallyTo(parent, bias = 0f)
 *                  centerVerticallyTo(parent, bias = 0f)
 *              }
 *          }
 *          val initialEnd = constraintSet("1") {
 *              constrain(buttonRef) {
 *                  centerHorizontallyTo(parent, bias = 0f)
 *                  centerVerticallyTo(parent, bias = 1f)
 *              }
 *          }
 *          constraintSet("2") {
 *              constrain(buttonRef) {
 *                  centerHorizontallyTo(parent, bias = 1f)
 *                  centerVerticallyTo(parent, bias = 0f)
 *              }
 *          }
 *          constraintSet("3") {
 *              constrain(buttonRef) {
 *                  centerHorizontallyTo(parent, bias = 1f)
 *                  centerVerticallyTo(parent, bias = 1f)
 *              }
 *          }
 *          // We need at least the default transition to define the initial state
 *          defaultTransition(initialStart, initialEnd)
 *      },
 *      constraintSetName = name.toString(),
 *      animationSpec = tween(1200),
 *      modifier = Modifier.fillMaxSize()
 *  ) {
 *      // Switch to a random ConstraintSet on click
 *      Button(onClick = { name = IntRange(0, 3).random() }, Modifier.layoutId("button")) {
 *          Text("Hello, World!")
 *      }
 *  }
 * ```
 *
 * Animations are run one after the other, if multiple are queued, only the last one will be
 * executed. You may use [finishedAnimationListener] to know whenever an animation is finished.
 *
 * @param motionScene Holds all the layout states defined in [ConstraintSet]s and the
 * interpolation associated between them (known as [Transition]s).
 * @param constraintSetName The name of the [ConstraintSet] to animate to. Null for no animation.
 * @param animationSpec Specifies how the internal progress value is animated.
 * @param modifier Modifier to apply to this layout node.
 * @param finishedAnimationListener Called when an animation triggered by a change in
 * [constraintSetName] has ended.
 * @param optimizationLevel Optimization parameter for the underlying ConstraintLayout,
 * [Optimizer.OPTIMIZATION_STANDARD] by default.
 * @param debugFlags Flags to enable visual debugging. [DebugFlags.None] by default.
 * @param content The content to be laid out by MotionLayout, note that each layout Composable
 * should be bound to an ID defined in the [ConstraintSet]s using
 * [Modifier.layoutId][androidx.compose.ui.layout.layoutId].
 */
@ExperimentalMotionApi
@Composable
inline fun MotionLayout(
    motionScene: MotionScene,
    constraintSetName: String?,
    animationSpec: AnimationSpec<Float>,
    modifier: Modifier = Modifier,
    noinline finishedAnimationListener: (() -> Unit)? = null,
    debugFlags: DebugFlags = DebugFlags.None,
    optimizationLevel: Int = Optimizer.OPTIMIZATION_STANDARD,
    @Suppress("HiddenTypeParameter")
    crossinline content: @Composable (MotionLayoutScope.() -> Unit)
) {
    /**
     * MutableState used to track content recompositions. It's reassigned at the content's
     * composition scope, so that any function reading it is recomposed with the content.
     * NeverEqualPolicy is used so that we don't have to assign any particular value to trigger a
     * State change.
     */
    val contentTracker = remember { mutableStateOf(Unit, neverEqualPolicy()) }
    val compositionSource =
        remember { Ref<CompositionSource>().apply { value = CompositionSource.Unknown } }

    /**
     * Delegate to handle composition tracking before calling the non-inline Composable
     */
    val contentDelegate: @Composable MotionLayoutScope.() -> Unit = {
        // Perform a reassignment to the State tracker, this will force readers to recompose at
        // the same pass as the content. The only expected reader is our MeasurePolicy.
        contentTracker.value = Unit

        if (compositionSource.value == CompositionSource.Unknown) {
            // Set the content as the original composition source if the MotionLayout was not
            // recomposed by the caller or by itself
            compositionSource.value = CompositionSource.Content
        }
        content()
    }

    MotionLayoutCore(
        motionScene = motionScene,
        constraintSetName = constraintSetName,
        animationSpec = animationSpec,
        modifier = modifier,
        finishedAnimationListener = finishedAnimationListener,
        debugFlags = debugFlags,
        optimizationLevel = optimizationLevel,
        contentTracker = contentTracker,
        compositionSource = compositionSource,
        content = contentDelegate
    )
}

@PublishedApi
@ExperimentalMotionApi
@Composable
internal fun MotionLayoutCore(
    motionScene: MotionScene,
    constraintSetName: String?,
    animationSpec: AnimationSpec<Float>,
    modifier: Modifier = Modifier,
    finishedAnimationListener: (() -> Unit)? = null,
    debugFlags: DebugFlags = DebugFlags.None,
    optimizationLevel: Int = Optimizer.OPTIMIZATION_STANDARD,
    contentTracker: State<Unit>,
    compositionSource: Ref<CompositionSource>,
    @Suppress("HiddenTypeParameter")
    content: @Composable (MotionLayoutScope.() -> Unit)
) {
    val needsUpdate = remember {
        mutableStateOf(0L)
    }

    val transition = remember(motionScene, needsUpdate.value) {
        motionScene.getTransitionInstance("default")
    }

    val initialStart = remember(motionScene, needsUpdate.value) {
        val startId = transition?.getStartConstraintSetId() ?: "start"
        motionScene.getConstraintSetInstance(startId)
    }
    val initialEnd = remember(motionScene, needsUpdate.value) {
        val endId = transition?.getEndConstraintSetId() ?: "end"
        motionScene.getConstraintSetInstance(endId)
    }

    if (initialStart == null || initialEnd == null) {
        return
    }

    var start: ConstraintSet by remember(motionScene) {
        mutableStateOf(initialStart)
    }
    var end: ConstraintSet by remember(motionScene) {
        mutableStateOf(initialEnd)
    }

    val targetConstraintSet = remember(motionScene, constraintSetName) {
        constraintSetName?.let { motionScene.getConstraintSetInstance(constraintSetName) }
    }

    val progress = remember { Animatable(0f) }

    var animateToEnd by remember(motionScene) { mutableStateOf(true) }

    val channel = remember { Channel<ConstraintSet>(Channel.CONFLATED) }

    if (targetConstraintSet != null) {
        SideEffect {
            channel.trySend(targetConstraintSet)
        }

        LaunchedEffect(motionScene, channel) {
            for (constraints in channel) {
                val newConstraintSet = channel.tryReceive().getOrNull() ?: constraints
                val animTargetValue = if (animateToEnd) 1f else 0f
                val currentSet = if (animateToEnd) start else end
                if (newConstraintSet != currentSet) {
                    if (animateToEnd) {
                        end = newConstraintSet
                    } else {
                        start = newConstraintSet
                    }
                    progress.animateTo(animTargetValue, animationSpec)
                    animateToEnd = !animateToEnd
                    finishedAnimationListener?.invoke()
                }
            }
        }
    }
    MotionLayoutCore(
        start = start,
        end = end,
        transition = transition,
        progress = progress.value,
        informationReceiver = motionScene as? LayoutInformationReceiver,
        optimizationLevel = optimizationLevel,
        showBounds = debugFlags.showBounds,
        showPaths = debugFlags.showPaths,
        showKeyPositions = debugFlags.showKeyPositions,
        modifier = modifier,
        contentTracker = contentTracker,
        compositionSource = compositionSource,
        content = content
    )
}

@ExperimentalMotionApi
@PublishedApi
@Composable
@Suppress("UnavailableSymbol")
internal fun MotionLayoutCore(
    @Suppress("HiddenTypeParameter")
    motionScene: MotionScene,
    progress: Float,
    transitionName: String,
    optimizationLevel: Int,
    debugFlags: DebugFlags,
    modifier: Modifier,
    contentTracker: State<Unit>,
    compositionSource: Ref<CompositionSource>,
    @Suppress("HiddenTypeParameter")
    content: @Composable MotionLayoutScope.() -> Unit,
) {
    val transition = remember(motionScene, transitionName) {
        motionScene.getTransitionInstance(transitionName)
    }

    val start = remember(motionScene, transition) {
        val startId = transition?.getStartConstraintSetId() ?: "start"
        motionScene.getConstraintSetInstance(startId)
    }
    val end = remember(motionScene, transition) {
        val endId = transition?.getEndConstraintSetId() ?: "end"
        motionScene.getConstraintSetInstance(endId)
    }
    if (start == null || end == null) {
        return
    }

    MotionLayoutCore(
        start = start,
        end = end,
        transition = transition,
        progress = progress,
        informationReceiver = motionScene as? LayoutInformationReceiver,
        optimizationLevel = optimizationLevel,
        showBounds = debugFlags.showBounds,
        showPaths = debugFlags.showPaths,
        showKeyPositions = debugFlags.showKeyPositions,
        modifier = modifier,
        contentTracker = contentTracker,
        compositionSource = compositionSource,
        content = content
    )
}

@ExperimentalMotionApi
@PublishedApi
@Composable
@Suppress("UnavailableSymbol")
internal fun MotionLayoutCore(
    start: ConstraintSet,
    end: ConstraintSet,
    transition: Transition?,
    progress: Float,
    informationReceiver: LayoutInformationReceiver?,
    optimizationLevel: Int,
    showBounds: Boolean,
    showPaths: Boolean,
    showKeyPositions: Boolean,
    modifier: Modifier,
    contentTracker: State<Unit>,
    compositionSource: Ref<CompositionSource>,
    @Suppress("HiddenTypeParameter")
    content: @Composable MotionLayoutScope.() -> Unit
) {
    val motionProgress = createAndUpdateMotionProgress(progress = progress)
    val transitionImpl = (transition as? TransitionImpl) ?: TransitionImpl.EMPTY
    // TODO: Merge this snippet with UpdateWithForcedIfNoUserChange
    val needsUpdate = remember { mutableStateOf(0L) }
    needsUpdate.value // Read the value to allow recomposition from informationReceiver
    informationReceiver?.setUpdateFlag(needsUpdate)

    UpdateWithForcedIfNoUserChange(
        motionProgress = motionProgress,
        informationReceiver = informationReceiver
    )

    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val measurer = remember { MotionMeasurer(density) }
    val scope = remember { MotionLayoutScope(measurer, motionProgress) }

    remember(start, end, transition) {
        measurer.initWith(
            start = start,
            end = end,
            layoutDirection = layoutDirection,
            transition = transitionImpl,
            progress = motionProgress.currentProgress
        )
        true // Remember is required to return a non-Unit value
    }

    val measurePolicy = motionLayoutMeasurePolicy(
        contentTracker = contentTracker,
        compositionSource = compositionSource,
        constraintSetStart = start,
        constraintSetEnd = end,
        transition = transitionImpl,
        motionProgress = motionProgress,
        measurer = measurer,
        optimizationLevel = optimizationLevel
    )

    measurer.addLayoutInformationReceiver(informationReceiver)

    val forcedDebug = informationReceiver?.getForcedDrawDebug()
    val forcedScaleFactor = measurer.forcedScaleFactor

    var doShowBounds = showBounds
    var doShowPaths = showPaths
    var doShowKeyPositions = showKeyPositions

    if (forcedDebug != null && forcedDebug != MotionLayoutDebugFlags.UNKNOWN) {
        doShowBounds = forcedDebug === MotionLayoutDebugFlags.SHOW_ALL
        doShowPaths = doShowBounds
        doShowKeyPositions = doShowBounds
    }

    @Suppress("DEPRECATION")
    MultiMeasureLayout(
        modifier = modifier
            .motionDebug(
                measurer = measurer,
                scaleFactor = forcedScaleFactor,
                showBounds = doShowBounds,
                showPaths = doShowPaths,
                showKeyPositions = doShowKeyPositions
            )
            .motionPointerInput(
                key = transition ?: TransitionImpl.EMPTY,
                motionProgress = motionProgress,
                measurer = measurer
            )
            .semantics { designInfoProvider = measurer },
        measurePolicy = measurePolicy,
        content = {
            scope.content()
        }
    )
}

@LayoutScopeMarker
@ExperimentalMotionApi
class MotionLayoutScope @Suppress("ShowingMemberInHiddenClass") internal constructor(
    private val measurer: MotionMeasurer,
    private val motionProgress: MotionProgress
) {
    /**
     * Invokes [onBoundsChanged] whenever the Start or End bounds may have changed for the
     * Composable corresponding to the given [layoutId] during positioning. This may happen if the
     * current Transition for [MotionLayout] changes.
     *
     * [onBoundsChanged] will be invoked at least once when the content is placed the first time.
     *
     * Use this [Modifier] instead of [onGloballyPositioned] if you wish to keep track of Composable
     * bounds while ignoring their positioning during animation. Such as when implementing
     * DragAndDrop logic.
     */
    @ExperimentalMotionApi
    fun Modifier.onStartEndBoundsChanged(
        layoutId: Any,
        onBoundsChanged: (startBounds: Rect, endBounds: Rect) -> Unit
    ): Modifier {
        return composed(
            inspectorInfo = debugInspectorInfo {
                name = "onStartEndBoundsChanged"
                properties["layoutId"] = layoutId
                properties["onBoundsChanged"] = onBoundsChanged
            }
        ) {
            // TODO: Consider returning IntRect directly, note that it would imply adding a
            //  dependency to `androidx.compose.ui.unit`
            val id = remember(layoutId) { layoutId.toString() }

            // Mutable Array to keep track of bound changes
            val startPoints = remember { IntArray(4) { 0 } }
            val startBoundsRef = remember { Ref<Rect>().apply { value = Rect.Zero } }

            // Mutable Array to keep track of bound changes
            val endPoints = remember { IntArray(4) { 0 } }
            val endBoundsRef = remember { Ref<Rect>().apply { value = Rect.Zero } }

            // Note that globally positioned is also invoked while animating, so keep the worload as
            // low as possible
            this.onPlaced {
                val startFrame = measurer.transition.getStart(id)
                var changed = false
                if (startFrame.left != startPoints[0] ||
                    startFrame.top != startPoints[1] ||
                    startFrame.right != startPoints[2] ||
                    startFrame.bottom != startPoints[3]
                ) {
                    startPoints[0] = startFrame.left
                    startPoints[1] = startFrame.top
                    startPoints[2] = startFrame.right
                    startPoints[3] = startFrame.bottom

                    // Only instantiate a new Rect when we know the old bounds are invalid
                    startBoundsRef.value = Rect(
                        startPoints[0].toFloat(),
                        startPoints[1].toFloat(),
                        startPoints[2].toFloat(),
                        startPoints[3].toFloat(),
                    )
                    changed = true
                }

                val endFrame = measurer.transition.getEnd(id)
                if (endFrame.left != endPoints[0] ||
                    endFrame.top != endPoints[1] ||
                    endFrame.right != endPoints[2] ||
                    endFrame.bottom != endPoints[3]
                ) {
                    endPoints[0] = endFrame.left
                    endPoints[1] = endFrame.top
                    endPoints[2] = endFrame.right
                    endPoints[3] = endFrame.bottom

                    // Only instantiate a new Rect when we know the old bounds are invalid
                    endBoundsRef.value = Rect(
                        endPoints[0].toFloat(),
                        endPoints[1].toFloat(),
                        endPoints[2].toFloat(),
                        endPoints[3].toFloat(),
                    )
                    changed = true
                }
                if (changed) {
                    onBoundsChanged(
                        startBoundsRef.value ?: Rect.Zero,
                        endBoundsRef.value ?: Rect.Zero
                    )
                }
            }
        }
    }

    @ExperimentalMotionApi
    inner class CustomProperties internal constructor(private val id: String) {
        /**
         * Return the current [Color] value of the custom property [name], of the [id] layout.
         *
         * Returns [Color.Unspecified] if the property does not exist.
         */
        fun color(name: String): Color {
            return measurer.getCustomColor(id, name, motionProgress.currentProgress)
        }

        /**
         * Return the current [Color] value of the custom property [name], of the [id] layout.
         *
         * Returns [Color.Unspecified] if the property does not exist.
         */
        fun float(name: String): Float {
            return measurer.getCustomFloat(id, name, motionProgress.currentProgress)
        }

        /**
         * Return the current [Int] value of the custom property [name], of the [id] layout.
         *
         * Returns `0` if the property does not exist.
         */
        fun int(name: String): Int {
            return measurer.getCustomFloat(id, name, motionProgress.currentProgress).toInt()
        }

        /**
         * Return the current [Dp] value of the custom property [name], of the [id] layout.
         *
         * Returns [Dp.Unspecified] if the property does not exist.
         */
        fun distance(name: String): Dp {
            return measurer.getCustomFloat(id, name, motionProgress.currentProgress).dp
        }

        /**
         * Return the current [TextUnit] value of the custom property [name], of the [id] layout.
         *
         * Returns [TextUnit.Unspecified] if the property does not exist.
         */
        fun fontSize(name: String): TextUnit {
            return measurer.getCustomFloat(id, name, motionProgress.currentProgress).sp
        }
    }

    @ExperimentalMotionApi // TODO: Remove for 1.2.0-alphaXX with all dependent functions
    inner class MotionProperties internal constructor(
        id: String,
        tag: String?
    ) {
        private var myId = id
        private var myTag = tag

        fun id(): String {
            return myId
        }

        fun tag(): String? {
            return myTag
        }

        fun color(name: String): Color {
            return measurer.getCustomColor(myId, name, motionProgress.currentProgress)
        }

        fun float(name: String): Float {
            return measurer.getCustomFloat(myId, name, motionProgress.currentProgress)
        }

        fun int(name: String): Int {
            return measurer.getCustomFloat(myId, name, motionProgress.currentProgress).toInt()
        }

        fun distance(name: String): Dp {
            return measurer.getCustomFloat(myId, name, motionProgress.currentProgress).dp
        }

        fun fontSize(name: String): TextUnit {
            return measurer.getCustomFloat(myId, name, motionProgress.currentProgress).sp
        }
    }

    @Deprecated(
        "Unnecessary composable, name is also inconsistent for custom properties",
        ReplaceWith("customProperties(id)")
    )
    @Composable
    fun motionProperties(id: String): State<MotionProperties> =
    // TODO: There's no point on returning a [State] object, and probably no point on this being
        //  a Composable
        remember(id) {
            mutableStateOf(MotionProperties(id, null))
        }

    @Deprecated("Deprecated for naming consistency", ReplaceWith("customProperties(id)"))
    fun motionProperties(id: String, tag: String): MotionProperties {
        return MotionProperties(id, tag)
    }

    @Deprecated("Deprecated for naming consistency", ReplaceWith("customColor(id, name)"))
    fun motionColor(id: String, name: String): Color {
        return measurer.getCustomColor(id, name, motionProgress.currentProgress)
    }

    @Deprecated("Deprecated for naming consistency", ReplaceWith("customFloat(id, name)"))
    fun motionFloat(id: String, name: String): Float {
        return measurer.getCustomFloat(id, name, motionProgress.currentProgress)
    }

    @Deprecated("Deprecated for naming consistency", ReplaceWith("customInt(id, name)"))
    fun motionInt(id: String, name: String): Int {
        return measurer.getCustomFloat(id, name, motionProgress.currentProgress).toInt()
    }

    @Deprecated("Deprecated for naming consistency", ReplaceWith("customDistance(id, name)"))
    fun motionDistance(id: String, name: String): Dp {
        return measurer.getCustomFloat(id, name, motionProgress.currentProgress).dp
    }

    @Deprecated("Deprecated for naming consistency", ReplaceWith("customFontSize(id, name)"))
    fun motionFontSize(id: String, name: String): TextUnit {
        return measurer.getCustomFloat(id, name, motionProgress.currentProgress).sp
    }

    /**
     * Returns a [CustomProperties] instance to access the values of custom properties defined for
     * [id] in different return types: Color, Float, Int, Dp, TextUnit.
     *
     * &nbsp;
     *
     * Note that there are no type guarantees when setting or getting custom properties, so be
     * mindful of the value type used for it in the MotionScene.
     */
    fun customProperties(id: String): CustomProperties = CustomProperties(id)

    /**
     * Return the current [Color] value of the custom property [name], of the [id] layout.
     *
     * Returns [Color.Unspecified] if the property does not exist.
     *
     * &nbsp;
     *
     * This is a short version of: `customProperties(id).color(name)`.
     */
    fun customColor(id: String, name: String): Color {
        return measurer.getCustomColor(id, name, motionProgress.currentProgress)
    }

    /**
     * Return the current [Color] value of the custom property [name], of the [id] layout.
     *
     * Returns [Color.Unspecified] if the property does not exist.
     *
     * &nbsp;
     *
     * This is a short version of: `customProperties(id).float(name)`.
     */
    fun customFloat(id: String, name: String): Float {
        return measurer.getCustomFloat(id, name, motionProgress.currentProgress)
    }

    /**
     * Return the current [Int] value of the custom property [name], of the [id] layout.
     *
     * Returns `0` if the property does not exist.
     *
     * &nbsp;
     *
     * This is a short version of: `customProperties(id).int(name)`.
     */
    fun customInt(id: String, name: String): Int {
        return measurer.getCustomFloat(id, name, motionProgress.currentProgress).toInt()
    }

    /**
     * Return the current [Dp] value of the custom property [name], of the [id] layout.
     *
     * Returns [Dp.Unspecified] if the property does not exist.
     *
     * &nbsp;
     *
     * This is a short version of: `customProperties(id).distance(name)`.
     */
    fun customDistance(id: String, name: String): Dp {
        return measurer.getCustomFloat(id, name, motionProgress.currentProgress).dp
    }

    /**
     * Return the current [TextUnit] value of the custom property [name], of the [id] layout.
     *
     * Returns [TextUnit.Unspecified] if the property does not exist.
     *
     * &nbsp;
     *
     * This is a short version of: `customProperties(id).fontSize(name)`.
     */
    fun customFontSize(id: String, name: String): TextUnit {
        return measurer.getCustomFloat(id, name, motionProgress.currentProgress).sp
    }
}

@ExperimentalMotionApi
internal fun motionLayoutMeasurePolicy(
    contentTracker: State<Unit>,
    compositionSource: Ref<CompositionSource>,
    constraintSetStart: ConstraintSet,
    constraintSetEnd: ConstraintSet,
    @SuppressWarnings("HiddenTypeParameter") transition: TransitionImpl,
    motionProgress: MotionProgress,
    measurer: MotionMeasurer,
    optimizationLevel: Int,
): MeasurePolicy =
    MeasurePolicy { measurables, constraints ->
        // Do a state read, to guarantee that we control measure when the content recomposes without
        // notifying our Composable caller
        contentTracker.value

        val layoutSize = measurer.performInterpolationMeasure(
            constraints,
            this.layoutDirection,
            constraintSetStart,
            constraintSetEnd,
            transition,
            measurables,
            optimizationLevel,
            motionProgress.currentProgress,
            compositionSource.value ?: CompositionSource.Unknown
        )
        compositionSource.value = CompositionSource.Unknown // Reset after measuring

        layout(layoutSize.width, layoutSize.height) {
            with(measurer) {
                performLayout(measurables)
            }
        }
    }

/**
 * Updates [motionProgress] from changes in [LayoutInformationReceiver.getForcedProgress].
 *
 * User changes, (reflected in [MotionProgress.currentProgress]) take priority.
 */
@Composable
internal fun UpdateWithForcedIfNoUserChange(
    motionProgress: MotionProgress,
    informationReceiver: LayoutInformationReceiver?
) {
    if (informationReceiver == null) {
        return
    }
    val currentUserProgress = motionProgress.currentProgress
    val forcedProgress = informationReceiver.getForcedProgress()

    // Save the initial progress
    val lastUserProgress = remember { Ref<Float>().apply { value = currentUserProgress } }

    if (!forcedProgress.isNaN() && lastUserProgress.value == currentUserProgress) {
        // Use the forced progress if the user progress hasn't changed
        motionProgress.updateProgress(forcedProgress)
    } else {
        informationReceiver.resetForcedProgress()
    }
    lastUserProgress.value = currentUserProgress
}

/**
 * Creates a [MotionProgress] that may be manipulated internally, but can also be updated by user
 * calls with different [progress] values.
 *
 * @param progress User progress, if changed, updates the underlying [MotionProgress]
 * @return A [MotionProgress] instance that may change from internal or external calls
 */
@Composable
internal fun createAndUpdateMotionProgress(progress: Float): MotionProgress {
    val motionProgress = remember {
        MotionProgress.fromMutableState(mutableStateOf(progress))
    }
    val last = remember { Ref<Float>().apply { value = progress } }
    if (last.value != progress) {
        // Update on progress change
        last.value = progress
        motionProgress.updateProgress(progress)
    }
    return motionProgress
}

@ExperimentalMotionApi
internal fun Modifier.motionDebug(
    measurer: MotionMeasurer,
    scaleFactor: Float,
    showBounds: Boolean,
    showPaths: Boolean,
    showKeyPositions: Boolean
): Modifier {
    var debugModifier: Modifier = this
    if (!scaleFactor.isNaN()) {
        debugModifier = debugModifier.scale(scaleFactor)
    }
    if (showBounds || showKeyPositions || showPaths) {
        debugModifier = debugModifier.drawBehind {
            with(measurer) {
                drawDebug(
                    drawBounds = showBounds,
                    drawPaths = showPaths,
                    drawKeyPositions = showKeyPositions
                )
            }
        }
    }
    return debugModifier
}

/**
 * Indicates where the composition was initiated.
 *
 * The source will help us identify possible pathways for optimization.
 *
 * E.g.: If the content was not recomposed, we can assume that previous measurements are still valid,
 * so there's no need to recalculate the entire interpolation, only the current frame.
 */
@PublishedApi
internal enum class CompositionSource {
    // TODO: Add an explicit option for Composition initiated internally

    Unknown,

    /**
     * Content recomposed, need to remeasure everything: **start**, **end** and **interpolated**
     * states.
     */
    Content
}

/**
 * Internal representation to read and set values for the progress.
 */
internal interface MotionProgress {
    // TODO: Since this class has no other uses anymore, consider to substitute it with a simple
    //  MutableState<Float>

    val currentProgress: Float

    fun updateProgress(newProgress: Float)

    companion object {
        fun fromMutableState(mutableProgress: MutableState<Float>): MotionProgress =
            fromState(mutableProgress) { mutableProgress.value = it }

        fun fromState(
            progressState: State<Float>,
            onUpdate: (newProgress: Float) -> Unit
        ): MotionProgress =
            object : MotionProgress {
                override val currentProgress: Float
                    get() = progressState.value

                override fun updateProgress(newProgress: Float) {
                    onUpdate(newProgress)
                }
            }
    }
}

/**
 * Flags to use with MotionLayout to enable visual debugging.
 *
 * @property showBounds
 * @property showPaths
 * @property showKeyPositions
 *
 * @see DebugFlags.None
 * @see DebugFlags.All
 */
@ExperimentalMotionApi
@JvmInline
value class DebugFlags internal constructor(private val flags: Int) {
    /**
     * @param showBounds Whether to show the bounds of widgets at the start and end of the current transition.
     * @param showPaths Whether to show the paths each widget will take through the current transition.
     * @param showKeyPositions Whether to show a diamond icon representing KeyPositions defined for each widget along the path.
     */
    constructor(
        showBounds: Boolean = false,
        showPaths: Boolean = false,
        showKeyPositions: Boolean = false
    ) : this(
        (if (showBounds) BOUNDS_FLAG else 0) or
            (if (showPaths) PATHS_FLAG else 0) or
            (if (showKeyPositions) KEY_POSITIONS_FLAG else 0)
    )

    /**
     * When enabled, shows the bounds of widgets at the start and end of the current transition.
     */
    val showBounds: Boolean
        get() = flags and BOUNDS_FLAG > 0

    /**
     * When enabled, shows the paths each widget will take through the current transition.
     */
    val showPaths: Boolean
        get() = flags and PATHS_FLAG > 0

    /**
     *
     * When enabled, shows a diamond icon representing KeyPositions defined for each widget along
     * the path.
     */
    val showKeyPositions: Boolean
        get() = flags and KEY_POSITIONS_FLAG > 0

    override fun toString(): String =
        "DebugFlags(" +
            "showBounds = $showBounds, " +
            "showPaths = $showPaths, " +
            "showKeyPositions = $showKeyPositions" +
            ")"

    companion object {
        private const val BOUNDS_FLAG = 1
        private const val PATHS_FLAG = 1 shl 1
        private const val KEY_POSITIONS_FLAG = 1 shl 2

        /**
         * [DebugFlags] instance with all flags disabled.
         */
        val None = DebugFlags(0)

        /**
         * [DebugFlags] instance with all flags enabled.
         *
         * Note that this includes any flags added in the future.
         */
        val All = DebugFlags(-1)
    }
}