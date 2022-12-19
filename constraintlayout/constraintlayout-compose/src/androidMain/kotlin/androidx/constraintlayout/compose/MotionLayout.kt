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
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.LayoutScopeMarker
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MultiMeasureLayout
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.core.widgets.Optimizer
import java.util.EnumSet
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * Measure flags for MotionLayout
 */
enum class MotionLayoutFlag(@Suppress("UNUSED_PARAMETER") value: Long) {
    Default(0),
    FullMeasure(1)
}

/**
 * Layout that interpolate its children layout given two sets of constraint and
 * a progress (from 0 to 1)
 */
@ExperimentalMotionApi
@Suppress("NOTHING_TO_INLINE")
@Composable
inline fun MotionLayout(
    start: ConstraintSet,
    end: ConstraintSet,
    modifier: Modifier = Modifier,
    transition: Transition? = null,
    progress: Float,
    debug: EnumSet<MotionLayoutDebugFlags> = EnumSet.of(MotionLayoutDebugFlags.NONE),
    optimizationLevel: Int = Optimizer.OPTIMIZATION_STANDARD,
    motionLayoutFlags: Set<MotionLayoutFlag> = setOf<MotionLayoutFlag>(),
    crossinline content: @Composable MotionLayoutScope.() -> Unit
) {
    val motionProgress = createAndUpdateMotionProgress(progress = progress)
    MotionLayoutCore(
        start = start,
        end = end,
        transition = transition as? TransitionImpl,
        motionProgress = motionProgress,
        debugFlag = debug.firstOrNull() ?: MotionLayoutDebugFlags.NONE,
        informationReceiver = null,
        modifier = modifier,
        optimizationLevel = optimizationLevel,
        motionLayoutFlags = motionLayoutFlags,
        content = content
    )
}

/**
 * Layout that animates the default transition of a [MotionScene] with a progress value (from 0 to
 * 1).
 */
@ExperimentalMotionApi
@Suppress("NOTHING_TO_INLINE")
@Composable
inline fun MotionLayout(
    motionScene: MotionScene,
    progress: Float,
    modifier: Modifier = Modifier,
    transitionName: String = "default",
    debug: EnumSet<MotionLayoutDebugFlags> = EnumSet.of(MotionLayoutDebugFlags.NONE),
    optimizationLevel: Int = Optimizer.OPTIMIZATION_STANDARD,
    motionLayoutFlags: Set<MotionLayoutFlag> = setOf<MotionLayoutFlag>(),
    crossinline content: @Composable (MotionLayoutScope.() -> Unit),
) {
    MotionLayoutCore(
        motionScene = motionScene,
        progress = progress,
        debug = debug,
        modifier = modifier,
        optimizationLevel = optimizationLevel,
        transitionName = transitionName,
        motionLayoutFlags = motionLayoutFlags,
        content = content
    )
}

/**
 * Layout that takes a MotionScene and animates by providing a [constraintSetName] to animate to.
 *
 * During recomposition, MotionLayout will interpolate from whichever ConstraintSet it is currently
 * in, to [constraintSetName].
 *
 * Typically the first value of [constraintSetName] should match the start ConstraintSet in the
 * default transition, or be null.
 *
 * Animation is run by [animationSpec], and will only start another animation once any other ones
 * are finished. Use [finishedAnimationListener] to know when a transition has stopped.
 */
@ExperimentalMotionApi
@Composable
inline fun MotionLayout(
    motionScene: MotionScene,
    modifier: Modifier = Modifier,
    constraintSetName: String? = null,
    animationSpec: AnimationSpec<Float> = tween(),
    debug: EnumSet<MotionLayoutDebugFlags> = EnumSet.of(MotionLayoutDebugFlags.NONE),
    optimizationLevel: Int = Optimizer.OPTIMIZATION_STANDARD,
    motionLayoutFlags: Set<MotionLayoutFlag> = setOf<MotionLayoutFlag>(),
    noinline finishedAnimationListener: (() -> Unit)? = null,
    crossinline content: @Composable (MotionLayoutScope.() -> Unit)
) {
    MotionLayoutCore(
        motionScene = motionScene,
        constraintSetName = constraintSetName,
        animationSpec = animationSpec,
        debugFlag = debug.firstOrNull() ?: MotionLayoutDebugFlags.NONE,
        modifier = modifier,
        optimizationLevel = optimizationLevel,
        finishedAnimationListener = finishedAnimationListener,
        motionLayoutFlags = motionLayoutFlags,
        content = content
    )
}

@Suppress("NOTHING_TO_INLINE")
@ExperimentalMotionApi
@Composable
inline fun MotionLayout(
    start: ConstraintSet,
    end: ConstraintSet,
    modifier: Modifier = Modifier,
    transition: Transition? = null,
    progress: Float,
    debug: EnumSet<MotionLayoutDebugFlags> = EnumSet.of(MotionLayoutDebugFlags.NONE),
    informationReceiver: LayoutInformationReceiver? = null,
    optimizationLevel: Int = Optimizer.OPTIMIZATION_STANDARD,
    motionLayoutFlags: Set<MotionLayoutFlag> = setOf<MotionLayoutFlag>(),
    crossinline content: @Composable (MotionLayoutScope.() -> Unit)
) {
    val motionProgress = createAndUpdateMotionProgress(progress = progress)
    MotionLayoutCore(
        start = start,
        end = end,
        transition = transition as? TransitionImpl,
        motionProgress = motionProgress,
        debugFlag = debug.firstOrNull() ?: MotionLayoutDebugFlags.NONE,
        informationReceiver = informationReceiver,
        modifier = modifier,
        optimizationLevel = optimizationLevel,
        motionLayoutFlags = motionLayoutFlags,
        content = content
    )
}

@ExperimentalMotionApi
@PublishedApi
@Composable
@Suppress("UnavailableSymbol")
internal inline fun MotionLayoutCore(
    @Suppress("HiddenTypeParameter")
    motionScene: MotionScene,
    modifier: Modifier = Modifier,
    constraintSetName: String? = null,
    animationSpec: AnimationSpec<Float> = tween(),
    debugFlag: MotionLayoutDebugFlags = MotionLayoutDebugFlags.NONE,
    optimizationLevel: Int = Optimizer.OPTIMIZATION_STANDARD,
    motionLayoutFlags: Set<MotionLayoutFlag> = setOf<MotionLayoutFlag>(),
    noinline finishedAnimationListener: (() -> Unit)? = null,
    @Suppress("HiddenTypeParameter")
    crossinline content: @Composable (MotionLayoutScope.() -> Unit)
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

    val scope = rememberCoroutineScope()
    val motionProgress = remember {
        MotionProgress.fromState(progress.asState()) {
            scope.launch { progress.snapTo(it) }
        }
    }
    MotionLayoutCore(
        start = start,
        end = end,
        transition = transition as? TransitionImpl,
        motionProgress = motionProgress,
        debugFlag = debugFlag,
        informationReceiver = motionScene as? LayoutInformationReceiver,
        modifier = modifier,
        optimizationLevel = optimizationLevel,
        motionLayoutFlags = motionLayoutFlags,
        content = content
    )
}

@ExperimentalMotionApi
@PublishedApi
@Composable
@Suppress("UnavailableSymbol")
internal inline fun MotionLayoutCore(
    @Suppress("HiddenTypeParameter")
    motionScene: MotionScene,
    progress: Float,
    modifier: Modifier = Modifier,
    debug: EnumSet<MotionLayoutDebugFlags> = EnumSet.of(MotionLayoutDebugFlags.NONE),
    optimizationLevel: Int = Optimizer.OPTIMIZATION_STANDARD,
    transitionName: String,
    motionLayoutFlags: Set<MotionLayoutFlag> = setOf<MotionLayoutFlag>(),
    @Suppress("HiddenTypeParameter")
    crossinline content: @Composable MotionLayoutScope.() -> Unit,
) {
    val transition = remember(motionScene, transitionName) {
        motionScene.getTransitionInstance(transitionName)
    }

    val start = remember(motionScene) {
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

    MotionLayout(
        start = start,
        end = end,
        transition = transition,
        progress = progress,
        debug = debug,
        informationReceiver = motionScene as? LayoutInformationReceiver,
        modifier = modifier,
        optimizationLevel = optimizationLevel,
        motionLayoutFlags = motionLayoutFlags,
        content = content
    )
}

@ExperimentalMotionApi
@PublishedApi
@Composable
@Suppress("UnavailableSymbol")
internal inline fun MotionLayoutCore(
    start: ConstraintSet,
    end: ConstraintSet,
    modifier: Modifier = Modifier,
    @SuppressWarnings("HiddenTypeParameter") transition: TransitionImpl? = null,
    motionProgress: MotionProgress,
    debugFlag: MotionLayoutDebugFlags = MotionLayoutDebugFlags.NONE,
    informationReceiver: LayoutInformationReceiver? = null,
    optimizationLevel: Int = Optimizer.OPTIMIZATION_STANDARD,
    motionLayoutFlags: Set<MotionLayoutFlag> = setOf<MotionLayoutFlag>(),
    @Suppress("HiddenTypeParameter")
    crossinline content: @Composable MotionLayoutScope.() -> Unit
) {
    // TODO: Merge this snippet with UpdateWithForcedIfNoUserChange
    val needsUpdate = remember { mutableStateOf(0L) }
    needsUpdate.value // Read the value to allow recomposition from informationReceiver
    informationReceiver?.setUpdateFlag(needsUpdate)

    UpdateWithForcedIfNoUserChange(
        motionProgress = motionProgress,
        informationReceiver = informationReceiver
    )

    var usedDebugMode = debugFlag
    val forcedDebug = informationReceiver?.getForcedDrawDebug()
    if (forcedDebug != null && forcedDebug != MotionLayoutDebugFlags.UNKNOWN) {
        usedDebugMode = forcedDebug
    }
    val density = LocalDensity.current
    val measurer = remember { MotionMeasurer(density) }
    val scope = remember { MotionLayoutScope(measurer, motionProgress) }
    val debug = EnumSet.of(usedDebugMode)
    val measurePolicy =
        rememberMotionLayoutMeasurePolicy(
            optimizationLevel,
            debug,
            start,
            end,
            transition,
            motionProgress,
            motionLayoutFlags,
            measurer
        )

    measurer.addLayoutInformationReceiver(informationReceiver)

    val forcedScaleFactor = measurer.forcedScaleFactor

    var debugModifications: Modifier = Modifier
    if (!debug.contains(MotionLayoutDebugFlags.NONE) || !forcedScaleFactor.isNaN()) {
        if (!forcedScaleFactor.isNaN()) {
            debugModifications = debugModifications.scale(forcedScaleFactor)
        }
        debugModifications = debugModifications.drawBehind {
            with(measurer) {
                if (!forcedScaleFactor.isNaN()) {
                    drawDebugBounds(forcedScaleFactor)
                }
                if (!debug.contains(MotionLayoutDebugFlags.NONE)) {
                    drawDebug()
                }
            }
        }
    }
    @Suppress("DEPRECATION")
    (MultiMeasureLayout(
        modifier = modifier
            .then(debugModifications)
            .motionPointerInput(measurePolicy, motionProgress, measurer)
            .semantics { designInfoProvider = measurer },
        measurePolicy = measurePolicy,
        content = { scope.content() }
    ))
}

@ExperimentalMotionApi
@Composable
inline fun MotionLayout(
    modifier: Modifier = Modifier,
    optimizationLevel: Int = Optimizer.OPTIMIZATION_STANDARD,
    motionLayoutState: MotionLayoutState,
    motionScene: MotionScene,
    crossinline content: @Composable MotionLayoutScope.() -> Unit
) {
    MotionLayoutCore(
        modifier = modifier,
        optimizationLevel = optimizationLevel,
        motionLayoutState = motionLayoutState as MotionLayoutStateImpl,
        motionScene = motionScene,
        content = content
    )
}

@PublishedApi
@ExperimentalMotionApi
@Composable
@Suppress("UnavailableSymbol")
internal inline fun MotionLayoutCore(
    modifier: Modifier = Modifier,
    optimizationLevel: Int = Optimizer.OPTIMIZATION_STANDARD,
    motionLayoutState: MotionLayoutStateImpl,
    @Suppress("HiddenTypeParameter")
    motionScene: MotionScene,
    transitionName: String = "default",
    @Suppress("HiddenTypeParameter")
    crossinline content: @Composable MotionLayoutScope.() -> Unit
) {
    val transition = remember(motionScene, transitionName) {
        motionScene.getTransitionInstance(transitionName)
    }

    val startId = transition?.getStartConstraintSetId() ?: "start"
    val endId = transition?.getEndConstraintSetId() ?: "end"

    val start = remember(motionScene) {
        motionScene.getConstraintSetInstance(startId)
    }
    val end = remember(motionScene) {
        motionScene.getConstraintSetInstance(endId)
    }

    if (start == null || end == null) {
        return
    }
    MotionLayoutCore(
        start = start,
        end = end,
        transition = transition as? TransitionImpl,
        motionProgress = motionLayoutState.motionProgress,
        debugFlag = motionLayoutState.debugMode,
        informationReceiver = motionScene as? JSONMotionScene,
        modifier = modifier,
        optimizationLevel = optimizationLevel,
        content = content
    )
}

@LayoutScopeMarker
@ExperimentalMotionApi
class MotionLayoutScope @Suppress("ShowingMemberInHiddenClass")
    @PublishedApi internal constructor(
    private val measurer: MotionMeasurer,
    private val motionProgress: MotionProgress
) {

    @ExperimentalMotionApi
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

    @Composable
    fun motionProperties(id: String): State<MotionProperties> =
    // TODO: There's no point on returning a [State] object, and probably no point on this being
        //  a Composable
        remember(id) {
            mutableStateOf(MotionProperties(id, null))
        }

    fun motionProperties(id: String, tag: String): MotionProperties {
        return MotionProperties(id, tag)
    }

    fun motionColor(id: String, name: String): Color {
        return measurer.getCustomColor(id, name, motionProgress.currentProgress)
    }

    fun motionFloat(id: String, name: String): Float {
        return measurer.getCustomFloat(id, name, motionProgress.currentProgress)
    }

    fun motionInt(id: String, name: String): Int {
        return measurer.getCustomFloat(id, name, motionProgress.currentProgress).toInt()
    }

    fun motionDistance(id: String, name: String): Dp {
        return measurer.getCustomFloat(id, name, motionProgress.currentProgress).dp
    }

    fun motionFontSize(id: String, name: String): TextUnit {
        return measurer.getCustomFloat(id, name, motionProgress.currentProgress).sp
    }
}

enum class MotionLayoutDebugFlags {
    NONE,
    SHOW_ALL,
    UNKNOWN
}

@Composable
@PublishedApi
@ExperimentalMotionApi
internal fun rememberMotionLayoutMeasurePolicy(
    optimizationLevel: Int,
    debug: EnumSet<MotionLayoutDebugFlags>,
    constraintSetStart: ConstraintSet,
    constraintSetEnd: ConstraintSet,
    @SuppressWarnings("HiddenTypeParameter") transition: TransitionImpl?,
    motionProgress: MotionProgress,
    motionLayoutFlags: Set<MotionLayoutFlag> = setOf<MotionLayoutFlag>(),
    measurer: MotionMeasurer
): MeasurePolicy {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    return remember(
        optimizationLevel,
        motionLayoutFlags,
        debug,
        constraintSetStart,
        constraintSetEnd,
        transition
    ) {
        measurer.initWith(
            constraintSetStart,
            constraintSetEnd,
            density,
            layoutDirection,
            transition,
            motionProgress.currentProgress
        )
        MeasurePolicy { measurables, constraints ->
            val layoutSize = measurer.performInterpolationMeasure(
                constraints,
                layoutDirection,
                constraintSetStart,
                constraintSetEnd,
                transition,
                measurables,
                optimizationLevel,
                motionProgress.currentProgress,
                motionLayoutFlags
            )
            layout(layoutSize.width, layoutSize.height) {
                with(measurer) {
                    performLayout(measurables)
                }
            }
        }
    }
}

/**
 * Updates [motionProgress] from changes in [LayoutInformationReceiver.getForcedProgress].
 *
 * User changes, (reflected in [MotionProgress.currentProgress]) take priority.
 */
@PublishedApi
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
@Suppress("NOTHING_TO_INLINE")
@PublishedApi
@Composable
internal inline fun createAndUpdateMotionProgress(progress: Float): MotionProgress {
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