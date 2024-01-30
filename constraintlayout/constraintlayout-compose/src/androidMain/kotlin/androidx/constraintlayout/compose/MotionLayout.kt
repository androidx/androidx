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

import android.os.Build
import android.view.View
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.layout.LayoutScopeMarker
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.core.widgets.Optimizer
import kotlin.math.absoluteValue
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
 * @param debugFlags Flags to enable visual debugging. [DebugFlags.None] by default.
 * @param optimizationLevel Optimization parameter for the underlying ConstraintLayout,
 * [Optimizer.OPTIMIZATION_STANDARD] by default.
 * @param invalidationStrategy Provides strategies to optimize invalidations in [MotionLayout].
 * Excessive invalidations will be the typical cause of bad performance in [MotionLayout]. See
 * [InvalidationStrategy] to learn how to apply common strategies.
 * @param content The content to be laid out by MotionLayout, note that each layout Composable
 * should be bound to an ID defined in the [ConstraintSet]s using
 * [Modifier.layoutId][androidx.compose.ui.layout.layoutId].
 */
@Composable
inline fun MotionLayout(
    start: ConstraintSet,
    end: ConstraintSet,
    progress: Float,
    modifier: Modifier = Modifier,
    transition: Transition? = null,
    debugFlags: DebugFlags = DebugFlags.None,
    optimizationLevel: Int = Optimizer.OPTIMIZATION_STANDARD,
    invalidationStrategy: InvalidationStrategy = InvalidationStrategy.DefaultInvalidationStrategy,
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

        if (invalidationStrategy.onObservedStateChange == null &&
            compositionSource.value == CompositionSource.Unknown) {
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
        invalidationStrategy = invalidationStrategy,
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
 * @param debugFlags Flags to enable visual debugging. [DebugFlags.None] by default.
 * @param optimizationLevel Optimization parameter for the underlying ConstraintLayout,
 * [Optimizer.OPTIMIZATION_STANDARD] by default.
 * @param invalidationStrategy Provides strategies to optimize invalidations in [MotionLayout].
 * Excessive invalidations will be the typical cause of bad performance in [MotionLayout]. See
 * [InvalidationStrategy] to learn how to apply common strategies.
 * @param content The content to be laid out by MotionLayout, note that each layout Composable
 * should be bound to an ID defined in the [ConstraintSet]s using
 * [Modifier.layoutId][androidx.compose.ui.layout.layoutId].
 */
@Composable
inline fun MotionLayout(
    motionScene: MotionScene,
    progress: Float,
    modifier: Modifier = Modifier,
    transitionName: String = "default",
    debugFlags: DebugFlags = DebugFlags.None,
    optimizationLevel: Int = Optimizer.OPTIMIZATION_STANDARD,
    invalidationStrategy: InvalidationStrategy = InvalidationStrategy.DefaultInvalidationStrategy,
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

        if (invalidationStrategy.onObservedStateChange == null &&
            compositionSource.value == CompositionSource.Unknown) {
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
        invalidationStrategy = invalidationStrategy,
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
 * @param debugFlags Flags to enable visual debugging. [DebugFlags.None] by default.
 * @param optimizationLevel Optimization parameter for the underlying ConstraintLayout,
 * [Optimizer.OPTIMIZATION_STANDARD] by default.
 * @param invalidationStrategy Provides strategies to optimize invalidations in [MotionLayout].
 * Excessive invalidations will be the typical cause of bad performance in [MotionLayout]. See
 * [InvalidationStrategy] to learn how to apply common strategies.
 * @param content The content to be laid out by MotionLayout, note that each layout Composable
 * should be bound to an ID defined in the [ConstraintSet]s using
 * [Modifier.layoutId][androidx.compose.ui.layout.layoutId].
 */
@Composable
inline fun MotionLayout(
    motionScene: MotionScene,
    constraintSetName: String?,
    animationSpec: AnimationSpec<Float>,
    modifier: Modifier = Modifier,
    noinline finishedAnimationListener: (() -> Unit)? = null,
    debugFlags: DebugFlags = DebugFlags.None,
    optimizationLevel: Int = Optimizer.OPTIMIZATION_STANDARD,
    invalidationStrategy: InvalidationStrategy = InvalidationStrategy.DefaultInvalidationStrategy,
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

        if (invalidationStrategy.onObservedStateChange == null &&
            compositionSource.value == CompositionSource.Unknown) {
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
        invalidationStrategy = invalidationStrategy,
        content = contentDelegate
    )
}

@PublishedApi
@Composable
internal fun MotionLayoutCore(
    motionScene: MotionScene,
    constraintSetName: String?,
    animationSpec: AnimationSpec<Float>,
    modifier: Modifier = Modifier,
    finishedAnimationListener: (() -> Unit)? = null,
    debugFlags: DebugFlags = DebugFlags.None,
    optimizationLevel: Int = Optimizer.OPTIMIZATION_STANDARD,
    contentTracker: MutableState<Unit>,
    compositionSource: Ref<CompositionSource>,
    invalidationStrategy: InvalidationStrategy,
    @Suppress("HiddenTypeParameter")
    content: @Composable (MotionLayoutScope.() -> Unit)
) {
    val needsUpdate = remember {
        mutableLongStateOf(0L)
    }

    val transition = remember(motionScene, needsUpdate.longValue) {
        motionScene.getTransitionInstance("default")
    }

    val initialStart = remember(motionScene, needsUpdate.longValue) {
        val startId = transition?.getStartConstraintSetId() ?: "start"
        motionScene.getConstraintSetInstance(startId)
    }
    val initialEnd = remember(motionScene, needsUpdate.longValue) {
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
        invalidationStrategy = invalidationStrategy,
        content = content
    )
}

@PublishedApi
@Composable
internal fun MotionLayoutCore(
    @Suppress("HiddenTypeParameter")
    motionScene: MotionScene,
    progress: Float,
    transitionName: String,
    optimizationLevel: Int,
    debugFlags: DebugFlags,
    modifier: Modifier,
    contentTracker: MutableState<Unit>,
    compositionSource: Ref<CompositionSource>,
    invalidationStrategy: InvalidationStrategy,
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
        invalidationStrategy = invalidationStrategy,
        content = content
    )
}

@PublishedApi
@Composable
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
    contentTracker: MutableState<Unit>,
    compositionSource: Ref<CompositionSource>,
    invalidationStrategy: InvalidationStrategy,
    @Suppress("HiddenTypeParameter")
    content: @Composable MotionLayoutScope.() -> Unit
) {
    val motionProgress = createAndUpdateMotionProgress(progress = progress)
    val transitionImpl = (transition as? TransitionImpl) ?: TransitionImpl.EMPTY
    // TODO: Merge this snippet with UpdateWithForcedIfNoUserChange
    val needsUpdate = remember { mutableLongStateOf(0L) }
    needsUpdate.longValue // Read the value to allow recomposition from informationReceiver
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
            progress = motionProgress.floatValue
        )
        true // Remember is required to return a non-Unit value
    }

    if (invalidationStrategy.onObservedStateChange != null) {
        Snapshot.observe(
            readObserver = {
                // Perform a reassignment to the State tracker, this will force readers to recompose at
                // the same pass as the content. The only expected reader is our MeasurePolicy.
                contentTracker.value = Unit

                if (compositionSource.value == CompositionSource.Unknown) {
                    // Set the content as the original composition source if the MotionLayout was not
                    // recomposed by the caller or by itself
                    compositionSource.value = CompositionSource.Content
                }
            },
            block = invalidationStrategy.onObservedStateChange
        )
    }

    val measurePolicy = motionLayoutMeasurePolicy(
        contentTracker = contentTracker,
        compositionSource = compositionSource,
        constraintSetStart = start,
        constraintSetEnd = end,
        transition = transitionImpl,
        motionProgress = motionProgress,
        measurer = measurer,
        optimizationLevel = optimizationLevel,
        invalidationStrategy = invalidationStrategy
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
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
        Api30Impl.isShowingLayoutBounds(LocalView.current)) {
        doShowBounds = true
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
class MotionLayoutScope @Suppress("ShowingMemberInHiddenClass") internal constructor(
    private val measurer: MotionMeasurer,
    private val motionProgress: MutableFloatState
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

    inner class CustomProperties internal constructor(private val id: String) {
        /**
         * Return the current [Color] value of the custom property [name], of the [id] layout.
         *
         * Returns [Color.Unspecified] if the property does not exist.
         */
        fun color(name: String): Color {
            return measurer.getCustomColor(id, name, motionProgress.floatValue)
        }

        /**
         * Return the current [Color] value of the custom property [name], of the [id] layout.
         *
         * Returns [Color.Unspecified] if the property does not exist.
         */
        fun float(name: String): Float {
            return measurer.getCustomFloat(id, name, motionProgress.floatValue)
        }

        /**
         * Return the current [Int] value of the custom property [name], of the [id] layout.
         *
         * Returns `0` if the property does not exist.
         */
        fun int(name: String): Int {
            return measurer.getCustomFloat(id, name, motionProgress.floatValue).toInt()
        }

        /**
         * Return the current [Dp] value of the custom property [name], of the [id] layout.
         *
         * Returns [Dp.Unspecified] if the property does not exist.
         */
        fun distance(name: String): Dp {
            return measurer.getCustomFloat(id, name, motionProgress.floatValue).dp
        }

        /**
         * Return the current [TextUnit] value of the custom property [name], of the [id] layout.
         *
         * Returns [TextUnit.Unspecified] if the property does not exist.
         */
        fun fontSize(name: String): TextUnit {
            return measurer.getCustomFloat(id, name, motionProgress.floatValue).sp
        }
    }

    // TODO: Remove for 1.2.0-alphaXX with all dependent functions
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
            return measurer.getCustomColor(myId, name, motionProgress.floatValue)
        }

        fun float(name: String): Float {
            return measurer.getCustomFloat(myId, name, motionProgress.floatValue)
        }

        fun int(name: String): Int {
            return measurer.getCustomFloat(myId, name, motionProgress.floatValue).toInt()
        }

        fun distance(name: String): Dp {
            return measurer.getCustomFloat(myId, name, motionProgress.floatValue).dp
        }

        fun fontSize(name: String): TextUnit {
            return measurer.getCustomFloat(myId, name, motionProgress.floatValue).sp
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
        return measurer.getCustomColor(id, name, motionProgress.floatValue)
    }

    @Deprecated("Deprecated for naming consistency", ReplaceWith("customFloat(id, name)"))
    fun motionFloat(id: String, name: String): Float {
        return measurer.getCustomFloat(id, name, motionProgress.floatValue)
    }

    @Deprecated("Deprecated for naming consistency", ReplaceWith("customInt(id, name)"))
    fun motionInt(id: String, name: String): Int {
        return measurer.getCustomFloat(id, name, motionProgress.floatValue).toInt()
    }

    @Deprecated("Deprecated for naming consistency", ReplaceWith("customDistance(id, name)"))
    fun motionDistance(id: String, name: String): Dp {
        return measurer.getCustomFloat(id, name, motionProgress.floatValue).dp
    }

    @Deprecated("Deprecated for naming consistency", ReplaceWith("customFontSize(id, name)"))
    fun motionFontSize(id: String, name: String): TextUnit {
        return measurer.getCustomFloat(id, name, motionProgress.floatValue).sp
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
        return measurer.getCustomColor(id, name, motionProgress.floatValue)
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
        return measurer.getCustomFloat(id, name, motionProgress.floatValue)
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
        return measurer.getCustomFloat(id, name, motionProgress.floatValue).toInt()
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
        return measurer.getCustomFloat(id, name, motionProgress.floatValue).dp
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
        return measurer.getCustomFloat(id, name, motionProgress.floatValue).sp
    }
}

internal fun motionLayoutMeasurePolicy(
    contentTracker: State<Unit>,
    compositionSource: Ref<CompositionSource>,
    constraintSetStart: ConstraintSet,
    constraintSetEnd: ConstraintSet,
    @SuppressWarnings("HiddenTypeParameter") transition: TransitionImpl,
    motionProgress: MutableFloatState,
    measurer: MotionMeasurer,
    optimizationLevel: Int,
    invalidationStrategy: InvalidationStrategy
): MeasurePolicy =
    MeasurePolicy { measurables, constraints ->
        // Do a state read, to guarantee that we control measure when the content recomposes without
        // notifying our Composable caller
        contentTracker.value

        val layoutSize = measurer.performInterpolationMeasure(
            constraints = constraints,
            layoutDirection = this.layoutDirection,
            constraintSetStart = constraintSetStart,
            constraintSetEnd = constraintSetEnd,
            transition = transition,
            measurables = measurables,
            optimizationLevel = optimizationLevel,
            progress = motionProgress.floatValue,
            compositionSource = compositionSource.value ?: CompositionSource.Unknown,
            invalidateOnConstraintsCallback = invalidationStrategy.shouldInvalidate
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
 * User changes, (reflected in [MutableFloatState.floatValue]) take priority.
 */
@Composable
internal fun UpdateWithForcedIfNoUserChange(
    motionProgress: MutableFloatState,
    informationReceiver: LayoutInformationReceiver?
) {
    if (informationReceiver == null) {
        return
    }
    val currentUserProgress = motionProgress.floatValue
    val forcedProgress = informationReceiver.getForcedProgress()

    // Save the initial progress
    val lastUserProgress = remember { Ref<Float>().apply { value = currentUserProgress } }

    if (!forcedProgress.isNaN() && lastUserProgress.value == currentUserProgress) {
        // Use the forced progress if the user progress hasn't changed
        motionProgress.floatValue = forcedProgress
    } else {
        informationReceiver.resetForcedProgress()
    }
    lastUserProgress.value = currentUserProgress
}

/**
 * Creates a [MutableFloatState] that may be manipulated internally, but can also be updated by user
 * calls with different [progress] values.
 *
 * @param progress User progress, if changed, updates the underlying [MutableFloatState]
 * @return A [MutableFloatState] instance that may change from internal or external calls
 */
@Composable
internal fun createAndUpdateMotionProgress(progress: Float): MutableFloatState {
    val motionProgress = remember {
        mutableFloatStateOf(progress)
    }
    val last = remember { Ref<Float>().apply { value = progress } }
    if (last.value != progress) {
        // Update on progress change
        last.value = progress
        motionProgress.floatValue = progress
    }
    return motionProgress
}

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
    // TODO: Consider adding an explicit option for Composition initiated internally, in case we
    //  need to differentiate them

    Unknown,

    /**
     * Content recomposed, need to remeasure everything: **start**, **end** and **interpolated**
     * states.
     */
    Content
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

/**
 * Wrapper to pass Class Verification from calling methods unavailable on older API.
 */
@RequiresApi(30)
private object Api30Impl {
    @JvmStatic
    @DoNotInline
    fun isShowingLayoutBounds(view: View): Boolean {
        return view.isShowingLayoutBounds
    }
}

/**
 * Helper scope that provides some strategies to improve performance based on incoming constraints.
 *
 * As a starting approach, we recommend trying the following:
 *
 * ```
 * MotionLayout(
 *     ...,
 *     invalidationStrategy = remember {
 *         InvalidationStrategy(
 *             onIncomingConstraints = { old, new ->
 *                 // We invalidate every third frame, or when the change is higher than 5 pixels
 *                 fixedWidthRate(old, new, skipCount = 3, threshold = 5) ||
 *                     fixedHeightRate(old, new, skipCount = 3, threshold = 5)
 *             },
 *             onObservedStateChange = null // Default behavior
 *         )
 * }
 * ) {
 *    // content
 * }
 * ```
 *
 * See either [fixedWidthRate] or [fixedHeightRate] to learn more about the intent behind
 * rate-limiting invalidation.
 */
class InvalidationStrategyScope internal constructor() {
    private var widthRateCount = 0

    /**
     * Limits the rate at which MotionLayout is invalidated while [Constraints.hasFixedWidth] is
     * true.
     *
     * &nbsp;
     *
     * The rate limit is defined by two variables. Use [skipCount] to indicate how many consecutive
     * measure passes should skip invalidation, you may then provide a [threshold] (in pixels) to
     * indicate when to invalidate regardless of how many passes are left to skip. This is
     * important since you only want to skip invalidation passes when there's **not** a significant
     * change in dimensions.
     *
     * &nbsp;
     *
     * Overall, you don't want [skipCount] to be too high otherwise it'll result in a "jumpy" layout
     * behavior, but you also don't want the [threshold] to be too low, otherwise you'll lose the
     * benefit of rate limiting.
     *
     * A good starting point is setting [skipCount] to 3 and [threshold] to 5. You can then
     * adjust based on your expectations of performance and perceived smoothness.
     */
    fun fixedWidthRate(
        oldConstraints: Constraints,
        newConstraints: Constraints,
        skipCount: Int,
        threshold: Int
    ): Boolean {
        if (oldConstraints.hasFixedWidth && newConstraints.hasFixedWidth) {
            val diff = (newConstraints.maxWidth - oldConstraints.maxWidth).absoluteValue
            if (diff >= threshold) {
                widthRateCount = 0
                return true
            }
            if (diff != 0) {
                widthRateCount++
                if (widthRateCount > skipCount) {
                    widthRateCount = 0
                    return true
                }
            }
        } else {
            widthRateCount = 0
        }
        return false
    }

    private var heightRateCount = 0

    /**
     * Limits the rate at which MotionLayout is invalidated while [Constraints.hasFixedHeight] is
     * true.
     *
     * &nbsp;
     *
     * The rate limit is defined by two variables. Use [skipCount] to indicate how many consecutive
     * measure passes should skip invalidation, you may then provide a [threshold] (in pixels) to
     * indicate when to invalidate regardless of how many passes are left to skip. This is
     * important since you only want to skip invalidation passes when there's **not** a significant
     * change in dimensions.
     *
     * &nbsp;
     *
     * Overall, you don't want [skipCount] to be too high otherwise it'll result in a "jumpy" layout
     * behavior, but you also don't want the [threshold] to be too low, otherwise you'll lose the
     * benefit of rate limiting.
     *
     * A good starting point is setting [skipCount] to 3 and [threshold] to 5. You can then
     * adjust based on your expectations of performance and perceived smoothness.
     */
    fun fixedHeightRate(
        oldConstraints: Constraints,
        newConstraints: Constraints,
        skipCount: Int,
        threshold: Int
    ): Boolean {
        if (oldConstraints.hasFixedHeight && newConstraints.hasFixedHeight) {
            val diff = (newConstraints.maxHeight - oldConstraints.maxHeight).absoluteValue
            if (diff >= threshold) {
                heightRateCount = 0
                return true
            }
            if (diff != 0) {
                heightRateCount++
                if (heightRateCount > skipCount) {
                    heightRateCount = 0
                    return true
                }
            }
        } else {
            heightRateCount = 0
        }
        return false
    }
}

/**
 * Provide different invalidation strategies for [MotionLayout].
 *
 * &nbsp;
 *
 * Whenever [MotionLayout] needs invalidating, it has to recalculate all animations based on the
 * current state at the measure pass, this is the slowest process in the [MotionLayout] cycle.
 *
 * An invalidation can be triggered by two reasons:
 * - Incoming fixed size constraints have changed. This is necessary since layouts are highly
 * dependent on their available space, it'll typically happen if you are externally animating the
 * dimensions of [MotionLayout].
 * - The content of MotionLayout recomposes. This is necessary since Layouts in Compose don't know
 * the reason for a new measure pass, so we need to recalculate animations even if recomposition
 * didn't affect the actual Layout. For example, this **definitely** happens if you are using
 * [MotionLayoutScope.customProperties], even when you are just animating a background color, the
 * custom property will trigger a recomposition in the content and [MotionLayout] will be forced to
 * invalidate since it cannot know that the Layout was not affected.
 *
 * So, you may use [InvalidationStrategy] to help [MotionLayout] decide when to invalidate:
 *
 * - [onObservedStateChange]: Mitigates invalidation from content recomposition by explicitly
 * reading the State variables you want to cause invalidation. You'll likely want to
 * apply this strategy to most of your [MotionLayout] Composables. As, in the most simple cases you
 * can just provide an empty lambda. Here's a full example:
 *
 * ```
 * val progress = remember { Animatable(0f) }
 *
 * MotionLayout(
 *     motionScene = remember {
 *         // A simple MotionScene that animates a background color from Red to Blue
 *         MotionScene {
 *             val (textRef) = createRefsFor("text")
 *
 *             val start = constraintSet {
 *                 constrain(textRef) {
 *                     centerTo(parent)
 *                     customColor("background", Color.Red)
 *                 }
 *             }
 *             val end = constraintSet(extendConstraintSet = start) {
 *                 constrain(textRef) {
 *                     customColor("background", Color.Blue)
 *                 }
 *             }
 *             defaultTransition(from = start, to = end)
 *         }
 *     },
 *     progress = progress.value,
 *     modifier = Modifier.fillMaxSize(),
 *     invalidationStrategy = remember {
 *         InvalidationStrategy(
 *             onObservedStateChange = { /* Empty, no need to invalidate on content recomposition */  }
 *         )
 *     }
 * ) {
 *     // The content doesn't depend on any State variable that may affect the Layout's measure result
 *     Text(
 *         text = "Hello, World",
 *         modifier = Modifier
 *             .layoutId("text")
 *             // However, the custom color is causing recomposition on each animated frame
 *             .background(customColor("text", "background"))
 *     )
 * }
 * LaunchedEffect(Unit) {
 *     delay(1000)
 *     progress.animateTo(targetValue = 1f, tween(durationMillis = 1200))
 * }
 * ```
 *
 * *When should I provide States to read then?*
 *
 * &nbsp;
 *
 * Whenever a State backed variable that affects the Layout's measure result changes. The most
 * common cases are Strings on the Text Composable.
 *
 * Here's an example where the text changes half-way through the animation:
 * ```
 * val progress = remember { Animatable(0f) }
 *
 * var textString by remember { mutableStateOf("Hello, World") }
 * MotionLayout(
 *     motionScene = remember {
 *         // A MotionScene that animates a Text from one corner to the other with an animated
 *         // background color
 *         MotionScene {
 *             val (textRef) = createRefsFor("text")
 *
 *             defaultTransition(
 *                 from = constraintSet {
 *                     constrain(textRef) {
 *                         top.linkTo(parent.top)
 *                         start.linkTo(parent.start)
 *
 *                         customColor("background", Color.LightGray)
 *                     }
 *                 },
 *                 to = constraintSet {
 *                     constrain(textRef) {
 *                         bottom.linkTo(parent.bottom)
 *                         end.linkTo(parent.end)
 *
 *                         customColor("background", Color.Gray)
 *                     }
 *                 }
 *             )
 *         }
 *     },
 *     progress = progress.value,
 *     modifier = Modifier.fillMaxSize(),
 *     invalidationStrategy = remember {
 *         InvalidationStrategy(
 *             onObservedStateChange = @Suppress("UNUSED_EXPRESSION"){
 *                 // We read our State String variable in this block, to guarantee that
 *                 // MotionLayout will invalidate to accommodate the new Text Layout.
 *                 // Note that we do not read the custom color here since it doesn't affect the Layout
 *                 textString
 *             }
 *         )
 *     }
 * ) {
 *     // The text Layout will change based on the provided State String
 *     Text(
 *         text = textString,
 *         modifier = Modifier
 *             .layoutId("text")
 *             // Without an invalidation strategy, the custom color would normally invalidate
 *             // MotionLayout due to recomposition
 *             .background(customColor("text", "background"))
 *     )
 * }
 * LaunchedEffect(Unit) {
 *     delay(1000)
 *     progress.animateTo(targetValue = 1f, tween(durationMillis = 3000)) {
 *         if (value >= 0.5f) {
 *             textString = "This is a\n" + "significantly different text."
 *         }
 *     }
 * }
 * ```
 *
 * *What if my Text changes continuously?*
 *
 * &nbsp;
 *
 * There's a few strategies you can take depending on how you expect the Text to behave.
 *
 * For example, if you don't expect the text to need more than one line, you can set the Text with
 * `softWrap = false` and `overflow = TextOverflow.Visible`:
 *
 * ```
 * MotionLayout(
 *     motionScene = motionScene,
 *     progress = progress,
 *     modifier = Modifier.size(200.dp),
 *     invalidationStrategy = remember { InvalidationStrategy { /* Do not invalidate on content recomposition */  } }
 * ) {
 *     Text(
 *         text = <your-State-String>,
 *         modifier = Modifier.layoutId("text"),
 *         softWrap = false,
 *         overflow = TextOverflow.Visible
 *     )
 * }
 * ```
 *
 * The Text layout won't change significantly and performance will be much improved.
 *
 * - [onIncomingConstraints]: With this lambda you can mitigate invalidation from incoming
 * constraints. You'll only have to worry about providing this lambda if you or the Layout you're
 * using is animating measuring constraints on [MotionLayout]. If the size is only changing in specific,
 * discrete values, then you should allow [MotionLayout] to invalidate normally.
 *
 * Here's an example where we manually animate [MotionLayout]'s size through a Modifier (along with
 * the MotionLayout animation), and shows how to mitigate invalidation by rate-limiting:
 *
 * ```
 * val textId = "text"
 * val progress = remember { Animatable(0f) }
 *
 * val initial = remember { DpSize(100.dp, 100.dp) }
 * val target = remember { DpSize(120.dp, 200.dp) }
 * var size by remember { mutableStateOf(initial) }
 *
 * MotionLayout(
 *     motionScene = remember {
 *         MotionScene {
 *             val (textRef) = createRefsFor( "text")
 *
 *             // Animate text from the bottom of the layout to the top
 *             defaultTransition(
 *                 from = constraintSet {
 *                     constrain(textRef) {
 *                         centerHorizontallyTo(parent)
 *                         bottom.linkTo(parent.bottom)
 *                     }
 *                 },
 *                 to = constraintSet {
 *                     constrain(textRef) {
 *                         centerHorizontallyTo(parent)
 *                         top.linkTo(parent.top)
 *                     }
 *                 }
 *             )
 *         }
 *     },
 *     progress = progress.value,
 *     modifier = Modifier.background(Color.Cyan).size(size),
 *     invalidationStrategy = remember {
 *         InvalidationStrategy(
 *             onIncomingConstraints = { old, new ->
 *                 // We invalidate every third frame, or when the change is higher than 5 pixels
 *                 fixedWidthRate(old, new, skipCount = 3, threshold = 5) ||
 *                     fixedHeightRate(old, new, skipCount = 3, threshold = 5)
 *             },
 *             // No need to worry about content state changes for this example
 *             onObservedStateChange = {}
 *         )
 *     }
 * ) {
 *     Text("Hello, World!", Modifier.layoutId(textId))
 * }
 *
 * // Animate the size along with the MotionLayout. Without an invalidation strategy, this will cause
 * // MotionLayout to invalidate at every measure pass since it's getting fixed size Constraints at
 * // different values
 * LaunchedEffect(Unit) {
 *     val sizeDifference = target - initial
 *     delay(1000)
 *     progress.animateTo(1f, tween(1200)) {
 *         size = initial + (sizeDifference * value)
 *     }
 * }
 * ```
 *
 * Note that [fixedWidthRate][InvalidationStrategyScope.fixedWidthRate] and [fixedHeightRate][InvalidationStrategyScope.fixedHeightRate]
 * are helper methods available in [InvalidationStrategyScope].
 *
 * &nbsp;
 *
 * An alternative to rate-limiting is to "simply" avoid invalidation from changed fixed size constraints.
 * This can be done by leaving [MotionLayout] as wrap content and then have it choose its own start
 * and ending size. Naturally, this is not always feasible, specially if it's a parent Composable the one
 * that's animating the size constraints.
 *
 * But, here's the MotionScene showing how to achieve this behavior based on the example above:
 *
 * ```
 * MotionScene {
 *     // We'll use fakeParentRef to choose our starting and ending size then constrain everything
 *     // else to it. MotionLayout will animate without invalidating.
 *     // There's no need to bind "fakeParent" to any actual Composable.
 *     val (fakeParentRef, textRef) = createRefsFor("fakeParent", "text")
 *
 *     defaultTransition(
 *         from = constraintSet {
 *             constrain(fakeParentRef) {
 *                 width = 100.dp.asDimension()
 *                 height = 100.dp.asDimension()
 *             }
 *
 *             constrain(textRef) {
 *                 bottom.linkTo(fakeParentRef.bottom)
 *             }
 *         },
 *         to = constraintSet {
 *             constrain(fakeParentRef) {
 *                 width = 120.dp.asDimension()
 *                 height = 200.dp.asDimension()
 *             }
 *
 *             constrain(textRef) {
 *                 top.linkTo(fakeParentRef.top)
 *             }
 *         }
 *     )
 * }
 * ```
 *
 * You can then remove the size modifier and the invalidation strategy for `onIncomingConstraints`,
 * as [MotionLayout] will animate through both sizes without invalidating.
 *
 * @see InvalidationStrategy.DefaultInvalidationStrategy
 * @see InvalidationStrategy.OnIncomingConstraints
 * @see InvalidationStrategyScope
 * @see InvalidationStrategyScope.fixedWidthRate
 * @see InvalidationStrategyScope.fixedHeightRate
 *
 * @property onObservedStateChange
 */
class InvalidationStrategy(
    val onIncomingConstraints: OnIncomingConstraints? = null,
    /**
     * Lambda to implement invalidation on observed State changes.
     *
     * [State][androidx.compose.runtime.State] based variables should be read in the block of
     * this lambda to have [MotionLayout] invalidate whenever any of those variables
     * have changed.
     *
     * You may use an assigned value or delegated variable for this purpose:
     * ```
     * val stateVar0 = remember { mutableStateOf("Foo") }
     * var stateVar1 by remember { mutableStateOf("Bar") }
     * val invalidationStrategy = remember {
     *     InvalidationStrategy(
     *         onObservedStateChange = @Suppress("UNUSED_EXPRESSION") {
     *             stateVar0.value
     *             stateVar1
     *         }
     *     )
     * }
     * ```
     *
     * See [InvalidationStrategy] to learn more about common strategies regarding invalidation on
     * onObservedStateChange.
     */
    val onObservedStateChange: (() -> Unit)?
) {
    private val scope = InvalidationStrategyScope()

    /**
     * Hacky thing to transform: `(InvalidationStrategyScope.(old: Constraints, new: Constraints) -> Boolean)?`
     * into `((old: Constraints, new: Constraints) -> Boolean)?`
     */
    internal val shouldInvalidate: ShouldInvalidateCallback? = kotlin.run {
        if (onIncomingConstraints == null) {
            null
        } else {
            ShouldInvalidateCallback { old, new ->
                with(onIncomingConstraints) {
                    scope(old, new)
                }
            }
        }
    }

    companion object {
        /**
         * Default invalidation strategy for [MotionLayout].
         *
         * This will cause it to invalidate whenever its content recomposes or when it receives different
         * fixed size [Constraints] at the measure pass.
         */
        val DefaultInvalidationStrategy = InvalidationStrategy(null, null)
    }

    /**
     * Functional interface to implement invalidation on incoming constraints.
     *
     * See [InvalidationStrategy] or either of [fixedWidthRate][InvalidationStrategyScope.fixedWidthRate]/[fixedHeightRate][InvalidationStrategyScope.fixedHeightRate].
     *
     * To learn some strategies on how to improve invalidation due to incoming constraints.
     */
    fun interface OnIncomingConstraints {
        operator fun InvalidationStrategyScope.invoke(old: Constraints, new: Constraints): Boolean
    }
}
