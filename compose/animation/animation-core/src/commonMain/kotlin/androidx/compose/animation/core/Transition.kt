/*
 * Copyright 2020 The Android Open Source Project
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

@file:OptIn(InternalAnimationApi::class)

package androidx.compose.animation.core

import androidx.annotation.FloatRange
import androidx.annotation.RestrictTo
import androidx.collection.MutableObjectLongMap
import androidx.collection.MutableObjectList
import androidx.compose.animation.core.internal.JvmDefaultWithCompatibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFold
import androidx.compose.ui.util.fastForEach
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.jvm.JvmName
import kotlin.math.max
import kotlin.math.roundToLong
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * This sets up a [Transition], and updates it with the target provided by [targetState]. When
 * [targetState] changes, [Transition] will run all of its child animations towards their
 * target values specified for the new [targetState]. Child animations can be dynamically added
 * using [Transition.animateFloat], [animateColor][ androidx.compose.animation.animateColor],
 * [Transition.animateValue], etc.
 *
 * [label] is used to differentiate different transitions in Android Studio.
 *
 * __Note__: There is another [rememberTransition] overload that accepts a [MutableTransitionState].
 * The difference between the two is that the [MutableTransitionState] variant: 1) supports a
 * different initial state than target state (This would allow a transition to start as soon as
 * it enters composition.) 2) can be recreated to intentionally trigger a re-start of the
 * transition.
 *
 * @sample androidx.compose.animation.core.samples.GestureAnimationSample
 *
 * @return a [Transition] object, to which animations can be added.
 * @see Transition
 * @see Transition.animateFloat
 * @see Transition.animateValue
 */
@Composable
fun <T> updateTransition(
    targetState: T,
    label: String? = null
): Transition<T> {
    val transition = remember { Transition(targetState, label = label) }
    transition.animateTo(targetState)
    DisposableEffect(transition) {
        onDispose {
            // Clean up on the way out, to ensure the observers are not stuck in an in-between
            // state.
            transition.onDisposed()
        }
    }
    return transition
}

internal const val AnimationDebugDurationScale = 1

/**
 * Use with [rememberTransition] to create a [Transition] that can be dynamically
 * targeted with [MutableTransitionState] or seekable with [SeekableTransitionState].
 */
sealed class TransitionState<S> {
    /**
     * Current state of the transition. If there is an active transition, [currentState] and
     * [targetState] are different.
     */
    abstract var currentState: S
        internal set

    /**
     * Target state of the transition. If this is the same as [currentState], no transition is
     * active.
     */
    abstract var targetState: S
        internal set

    // Updated from Transition
    internal var isRunning: Boolean by mutableStateOf(false)
    internal abstract fun transitionConfigured(transition: Transition<S>)

    internal abstract fun transitionRemoved()
}

/**
 * This is used to prevent exhaustive `when` from limiting the use of [TransitionState] to only
 * [MutableState] and [SeekableTransitionState]. The developer must always have an `else`.
 * It is unlikely to be a concern, but this will alleviate any worries about expanding the
 * subclasses of [TransitionState].
 */
private class PreventExhaustiveWhenTransitionState : TransitionState<Any?>() {
    override var currentState: Any?
        get() = null
        set(_) {}

    override var targetState: Any?
        get() = null
        set(_) {}

    override fun transitionConfigured(transition: Transition<Any?>) {
    }

    override fun transitionRemoved() {
    }
}

/**
 * MutableTransitionState contains two fields: [currentState] and [targetState]. [currentState] is
 * initialized to the provided initialState, and can only be mutated by a [Transition].
 * [targetState] is also initialized to initialState. It can be mutated to alter the course of a
 * transition animation that is created with the [MutableTransitionState] using [rememberTransition].
 * Both [currentState] and [targetState] are backed by a [State] object.
 *
 * @sample androidx.compose.animation.core.samples.InitialStateSample
 * @see rememberTransition
 */
class MutableTransitionState<S>(initialState: S) : TransitionState<S>() {
    /**
     * Current state of the transition. [currentState] is initialized to the initialState that the
     * [MutableTransitionState] is constructed with.
     *
     * It will be updated by the Transition that is created with this [MutableTransitionState]
     * when the transition arrives at a new state.
     */
    override var currentState: S by mutableStateOf(initialState)
        internal set

    /**
     * Target state of the transition. [targetState] is initialized to the initialState that the
     * [MutableTransitionState] is constructed with.
     *
     * It can be updated to a new state at any time. When that happens, the [Transition] that is
     * created with this [MutableTransitionState] will update its
     * [Transition.targetState] to the same and subsequently starts a transition animation to
     * animate from the current values to the new target.
     */
    override var targetState: S by mutableStateOf(initialState)
        public set

    /**
     * [isIdle] returns whether the transition has finished running. This will return false once
     * the [targetState] has been set to a different value than [currentState].
     *
     * @sample androidx.compose.animation.core.samples.TransitionStateIsIdleSample
     */
    val isIdle: Boolean
        get() = (currentState == targetState) && !isRunning

    override fun transitionConfigured(transition: Transition<S>) {
    }

    override fun transitionRemoved() {
    }
}

/**
 * Lambda to call [SeekableTransitionState.onTotalDurationChanged] when the
 * [Transition.totalDurationNanos] has changed.
 */
private val SeekableTransitionStateTotalDurationChanged: (SeekableTransitionState<*>) -> Unit = {
    it.onTotalDurationChanged()
}

// This observer is also accessed from test. It should be otherwise treated as private.
internal val SeekableStateObserver: SnapshotStateObserver by
    lazy(LazyThreadSafetyMode.NONE) { SnapshotStateObserver { it() }.apply { start() } }

/**
 * A [TransitionState] that can manipulate the progress of the [Transition] by seeking
 * with [seekTo] or animating with [animateTo].
 *
 * A [SeekableTransitionState] can only be used with one [Transition] instance. Once assigned,
 * it cannot be reassigned to a different [Transition] instance.
 * @sample androidx.compose.animation.core.samples.SeekingAnimationSample
 */
class SeekableTransitionState<S>(
    initialState: S
) : TransitionState<S>() {
    override var targetState: S by mutableStateOf(initialState)
        internal set
    override var currentState: S by mutableStateOf(initialState)
        internal set

    /**
     * The value of [targetState] that the composition knows about. Seeking cannot progress until
     * the composition's target state matches [targetState].
     */
    internal var composedTargetState = initialState

    /**
     * The Transition that this is associated with. SeekableTransitionState can only be used
     * with one Transition.
     */
    private var transition: Transition<S>? = null

    // Used for seekToFraction calculations to avoid allocation
    internal var totalDurationNanos = 0L

    private val recalculateTotalDurationNanos: () -> Unit = {
        totalDurationNanos = transition?.totalDurationNanos ?: 0L
    }

    /**
     * The progress of the transition from [currentState] to [targetState] as a fraction
     * of the entire duration.
     *
     * If [targetState] and [currentState] are the same, [fraction] will be 0.
     */
    @get:FloatRange(from = 0.0, to = 1.0)
    var fraction: Float by mutableFloatStateOf(0f)
        private set

    /**
     * The continuation used when waiting for a composition.
     */
    internal var compositionContinuation: CancellableContinuation<S>? = null

    /**
     * Used to lock the [compositionContinuation] while modifying or checking its value.
     * Also used for locking the [composedTargetState].
     */
    internal val compositionContinuationMutex = Mutex()

    /**
     * Used to prevent [snapTo], [seekTo], and [animateTo] from running simultaneously.
     */
    private val mutatorMutex = MutatorMutex()

    /**
     * When the animation is running, this contains the most recent frame time. When the
     * animation has stopped, this is [AnimationConstants.UnspecifiedTime].
     */
    private var lastFrameTimeNanos: Long = AnimationConstants.UnspecifiedTime

    /**
     * List of animation of initial values. The list should be empty when [seekTo],
     * [snapTo], or [animateTo] completes successfully.
     */
    private val initialValueAnimations = MutableObjectList<SeekingAnimationState>()

    /**
     * When [animateTo] is executing, this is non-null, providing the information being
     * used to animate its value. This will be null while seeking or after [snapTo].
     */
    private var currentAnimation: SeekingAnimationState? = null

    /**
     * Lambda instance used for capturing the first frame of an animation.
     */
    private val firstFrameLambda: (Long) -> Unit = { frameTimeNanos ->
        lastFrameTimeNanos = frameTimeNanos
    }

    /**
     * Used in [animateOneFrameLambda], the duration scale must be set immediately
     * prior to invoking [withFrameNanos] with [animateOneFrameLambda].
     */
    private var durationScale: Float = 0f

    /**
     * Lambda instance used for animating a single frame within [withFrameNanos].
     */
    private val animateOneFrameLambda: (Long) -> Unit = { frameTimeNanos ->
        val delta = frameTimeNanos - lastFrameTimeNanos
        lastFrameTimeNanos = frameTimeNanos
        val deltaPlayTimeNanos = (delta / durationScale.toDouble()).roundToLong()
        if (initialValueAnimations.isNotEmpty()) {
            initialValueAnimations.forEach { animation ->
                // updateInitialValues will set to false if the animation isn't
                // complete
                recalculateAnimationValue(animation, deltaPlayTimeNanos)
                animation.isComplete = true
            }
            transition?.updateInitialValues()
            initialValueAnimations.removeIf { it.isComplete }
        }
        val currentAnimation = currentAnimation
        if (currentAnimation != null) {
            currentAnimation.durationNanos = totalDurationNanos
            recalculateAnimationValue(currentAnimation, deltaPlayTimeNanos)
            fraction = currentAnimation.value
            if (currentAnimation.value == 1f) {
                this@SeekableTransitionState.currentAnimation = null // all done!
            }
            seekToFraction()
        }
    }

    /**
     * Stops all animations, including the initial value animations and sets the [fraction]
     * to `1`.
     */
    private fun endAllAnimations() {
        transition?.clearInitialAnimations()
        initialValueAnimations.clear()
        val current = currentAnimation
        if (current != null) {
            currentAnimation = null
            fraction = 1f
            seekToFraction()
        }
    }

    /**
     * Starts the animation. It will advance both the currently-running animation and
     * initial value animations. If the previous animation was stopped ([seekTo] or [snapTo] or
     * no previous animation was running), then it will require one frame to capture the
     * frame time before the animation starts.
     */
    private suspend fun runAnimations() {
        if (initialValueAnimations.isEmpty() && currentAnimation == null) {
            // nothing to animate
            return
        }
        if (coroutineContext.durationScale == 0f) {
            endAllAnimations()
            lastFrameTimeNanos = AnimationConstants.UnspecifiedTime
            return
        }
        if (lastFrameTimeNanos == AnimationConstants.UnspecifiedTime) {
            // have to capture one frame to get the start time
            withFrameNanos(firstFrameLambda)
        }
        while (initialValueAnimations.isNotEmpty() || currentAnimation != null) {
            animateOneFrame()
        }
        lastFrameTimeNanos = AnimationConstants.UnspecifiedTime
    }

    /**
     * Does one frame of work. If there is no previous animation, then it will capture the
     * [lastFrameTimeNanos]. If there was a previous animation, it will advance by one frame.
     */
    private suspend fun doOneFrame() {
        if (lastFrameTimeNanos == AnimationConstants.UnspecifiedTime) {
            // have to capture one frame to get the start time
            withFrameNanos(firstFrameLambda)
        } else {
            animateOneFrame()
        }
    }

    /**
     * Advances all animations by one frame.
     */
    private suspend fun animateOneFrame() {
        val durationScale = coroutineContext.durationScale
        if (durationScale <= 0f) {
            endAllAnimations()
        } else {
            this@SeekableTransitionState.durationScale = durationScale
            withFrameNanos(animateOneFrameLambda)
        }
    }

    /**
     * Calculates the [SeekingAnimationState.value] based on the [deltaPlayTimeNanos]. It uses
     * the animation spec if one is provided, or the progress of the total duration if not. This
     * method does not account for duration scale.
     */
    private fun recalculateAnimationValue(
        animation: SeekingAnimationState,
        deltaPlayTimeNanos: Long
    ) {
        val playTimeNanos = animation.progressNanos + deltaPlayTimeNanos
        animation.progressNanos = playTimeNanos
        val durationNanos = animation.animationSpecDuration
        if (playTimeNanos >= durationNanos) {
            animation.value = 1f
        } else {
            val animationSpec = animation.animationSpec
            if (animationSpec != null) {
                animation.value = animationSpec.getValueFromNanos(
                    playTimeNanos,
                    animation.start,
                    Target1,
                    animation.initialVelocity ?: ZeroVelocity
                )[0].coerceIn(0f, 1f)
            } else {
                animation.value = lerp(
                    animation.start[0],
                    1f,
                    playTimeNanos.toFloat() / durationNanos
                )
            }
        }
    }

    /**
     * Sets [currentState] and [targetState] to `targetState` and snaps all values to those
     * at that state. The transition will not have any animations running after running
     * [snapTo].
     *
     * This can have a similar effect as [seekTo]. However, [seekTo] moves the [currentState]
     * to the former [targetState] and animates the initial values of the animations from the
     * current values to those at [currentState]. [seekTo] also allows the developer to move
     * the state between any fraction between [currentState] and [targetState], while
     * [snapTo] moves all state to [targetState] without any further seeking allowed.
     *
     * @sample androidx.compose.animation.core.samples.SnapToSample
     *
     * @see animateTo
     */
    suspend fun snapTo(targetState: S) {
        val transition = transition ?: return
        if (currentState == targetState &&
            this@SeekableTransitionState.targetState == targetState
        ) {
            return // nothing to change
        }
        mutatorMutex.mutate {
            endAllAnimations()
            lastFrameTimeNanos = AnimationConstants.UnspecifiedTime
            fraction = 0f
            val fraction = when (targetState) {
                currentState -> ResetAnimationSnapCurrent
                this@SeekableTransitionState.targetState -> ResetAnimationSnapTarget
                else -> ResetAnimationSnap
            }
            transition.updateTarget(targetState)
            transition.playTimeNanos = 0L
            this@SeekableTransitionState.targetState = targetState
            this@SeekableTransitionState.fraction = 0f
            currentState = targetState
            transition.resetAnimationFraction(fraction)
            if (fraction == ResetAnimationSnap) {
                // completely changed the value, so we have to wait for a composition to have
                // the correct animation values
                waitForCompositionAfterTargetStateChange()
            }
            transition.onTransitionEnd()
        }
    }

    /**
     * Starts seeking the transition to [targetState] with [fraction] used to indicate the progress
     * towards [targetState]. If the previous `targetState` was already
     * [targetState] then [seekTo] only stops any current animation towards that state and snaps
     * the fraction to the new value. Otherwise, the [currentState] is changed to the former
     * `targetState` and `targetState` is changed to [targetState] and an animation is started,
     * moving the start values towards the former `targetState`. This will return when the
     * initial values have reached `currentState` and the [fraction] has been reached.
     *
     * [snapTo] also allows the developer to change the state, but does not animate any values.
     * Instead, it instantly moves all values to those at the new [targetState].
     *
     * @sample androidx.compose.animation.core.samples.SeekToSample
     *
     * @see animateTo
     */
    suspend fun seekTo(
        @FloatRange(from = 0.0, to = 1.0) fraction: Float,
        targetState: S = this.targetState
    ) {
        requirePrecondition(fraction in 0f..1f) {
            "Expecting fraction between 0 and 1. Got $fraction"
        }
        val transition = transition ?: return
        val oldTargetState = this@SeekableTransitionState.targetState
        mutatorMutex.mutate {
            coroutineScope {
                if (targetState != oldTargetState) {
                    moveAnimationToInitialState()
                } else {
                    currentAnimation = null
                    if (currentState == targetState) {
                        return@coroutineScope // Can't seek when current state is target state
                    }
                }
                if (targetState != oldTargetState) {
                    // Change the target _and_ the fraction
                    transition.updateTarget(targetState)
                    transition.playTimeNanos = 0L
                    this@SeekableTransitionState.targetState = targetState
                    transition.resetAnimationFraction(fraction)
                }
                this@SeekableTransitionState.fraction = fraction
                if (initialValueAnimations.isNotEmpty()) {
                    launch { runAnimations() }
                } else {
                    lastFrameTimeNanos = AnimationConstants.UnspecifiedTime
                }
                waitForCompositionAfterTargetStateChange()
                seekToFraction()
            }
        }
    }

    /**
     * Wait for composition to set up the target values
     */
    private suspend fun waitForCompositionAfterTargetStateChange() {
        val expectedState = targetState
        compositionContinuationMutex.lock()
        if (expectedState == composedTargetState) {
            compositionContinuationMutex.unlock()
        } else {
            val state = suspendCancellableCoroutine { continuation ->
                compositionContinuation = continuation
                compositionContinuationMutex.unlock()
            }
            if (state != expectedState) {
                lastFrameTimeNanos = AnimationConstants.UnspecifiedTime
                throw CancellationException(
                    "snapTo() was canceled because state was changed to " +
                        "$state instead of $expectedState"
                )
            }
        }
    }

    /**
     * Waits for composition, irrespective of whether the target state has changed or not.
     * This is important for when we're waiting for the currentState to change.
     */
    private suspend fun waitForComposition() {
        val expectedState = targetState
        compositionContinuationMutex.lock()
        val state = suspendCancellableCoroutine { continuation ->
            compositionContinuation = continuation
            compositionContinuationMutex.unlock()
        }
        if (state != expectedState) {
            lastFrameTimeNanos = AnimationConstants.UnspecifiedTime
            throw CancellationException("targetState while waiting for composition")
        }
    }

    /**
     * Change the animatedInitialFraction to use the animatedFraction, if it needs to be used.
     */
    private fun moveAnimationToInitialState() {
        val transition = transition ?: return
        val animation = currentAnimation ?: if (totalDurationNanos <= 0 || fraction == 1f ||
            currentState == targetState
        ) {
            null
        } else {
            SeekingAnimationState().also {
                it.value = fraction
                val totalDurationNanos = totalDurationNanos
                it.durationNanos = totalDurationNanos
                it.animationSpecDuration = (totalDurationNanos * (1.0 - fraction)).roundToLong()
                it.start[0] = fraction
            }
        }
        if (animation != null) {
            animation.durationNanos = totalDurationNanos
            initialValueAnimations += animation
            transition.setInitialAnimations(animation)
        }
        currentAnimation = null
    }

    /**
     * Updates the current `targetState` to [targetState] and begins an animation to the new state.
     * If the current `targetState` is the same as [targetState] then the
     * current transition animation is continued. If a previous transition
     * was interrupted, [currentState] is changed to the former `targetState` and the start values
     * are animated toward the former `targetState`.
     *
     * Upon completion of the animation, [currentState] will be changed to [targetState].
     *
     * @param targetState The state to animate towards.
     * @param animationSpec If provided, is used to animate the animation fraction. If `null`,
     * the transition is linearly traversed based on the duration of the transition.
     */
    @Suppress("DocumentExceptions")
    suspend fun animateTo(
        targetState: S = this.targetState,
        animationSpec: FiniteAnimationSpec<Float>? = null
    ) {
        val transition = transition ?: return
        mutatorMutex.mutate {
            coroutineScope {
                val oldTargetState = this@SeekableTransitionState.targetState
                if (targetState != oldTargetState) {
                    moveAnimationToInitialState()
                    fraction = 0f
                    transition.updateTarget(targetState)
                    transition.playTimeNanos = 0L
                    currentState = oldTargetState
                    this@SeekableTransitionState.targetState = targetState
                }
                val composedTargetState =
                    compositionContinuationMutex.withLock { composedTargetState }
                if (targetState != composedTargetState) {
                    doOneFrame() // We have to wait a frame for the composition, so continue
                    // Now we shouldn't skip a frame while waiting for composition
                    waitForCompositionAfterTargetStateChange()
                }
                if (currentState != targetState) {
                    if (fraction < 1f) {
                        val runningAnimation = currentAnimation
                        val newSpec = animationSpec?.vectorize(Float.VectorConverter)
                        if (runningAnimation == null || newSpec != runningAnimation.animationSpec) {
                            // If there is a running animation, it has changed
                            val oldSpec = runningAnimation?.animationSpec
                            val oldVelocity: AnimationVector1D
                            if (oldSpec != null) {
                                oldVelocity = oldSpec.getVelocityFromNanos(
                                    playTimeNanos = runningAnimation.progressNanos,
                                    initialValue = runningAnimation.start,
                                    targetValue = Target1,
                                    initialVelocity =
                                    runningAnimation.initialVelocity ?: ZeroVelocity
                                )
                            } else if (runningAnimation == null ||
                                runningAnimation.progressNanos == 0L
                            ) {
                                oldVelocity = ZeroVelocity
                            } else {
                                val oldDurationNanos = runningAnimation.durationNanos
                                val oldDuration =
                                    if (oldDurationNanos == AnimationConstants.UnspecifiedTime) {
                                        totalDurationNanos
                                    } else {
                                        oldDurationNanos
                                    } / (1000f * MillisToNanos)
                                if (oldDuration <= 0L) {
                                    oldVelocity = ZeroVelocity
                                } else {
                                    oldVelocity = AnimationVector1D(1f / oldDuration)
                                }
                            }
                            val newAnimation = runningAnimation ?: SeekingAnimationState()
                            newAnimation.animationSpec = newSpec
                            newAnimation.isComplete = false
                            newAnimation.value = fraction
                            newAnimation.start[0] = fraction
                            newAnimation.durationNanos = totalDurationNanos
                            newAnimation.progressNanos = 0L
                            newAnimation.initialVelocity = oldVelocity
                            newAnimation.animationSpecDuration = newSpec?.getDurationNanos(
                                initialValue = newAnimation.start,
                                targetValue = Target1,
                                initialVelocity = oldVelocity
                            ) ?: (totalDurationNanos * (1.0 - fraction)).roundToLong()
                            currentAnimation = newAnimation
                        }
                    }
                    runAnimations()
                    currentState = targetState
                    waitForComposition()
                    fraction = 0f
                }
            }
            transition.onTransitionEnd()
        }
    }

    override fun transitionConfigured(transition: Transition<S>) {
        checkPrecondition(this.transition == null || transition == this.transition) {
            "An instance of SeekableTransitionState has been used in different Transitions. " +
                "Previous instance: ${this.transition}, new instance: $transition"
        }
        this.transition = transition
    }

    override fun transitionRemoved() {
        this.transition = null
        SeekableStateObserver.clear(this)
    }

    internal fun observeTotalDuration() {
        SeekableStateObserver.observeReads(
            scope = this,
            onValueChangedForScope = SeekableTransitionStateTotalDurationChanged,
            block = recalculateTotalDurationNanos
        )
    }

    internal fun onTotalDurationChanged() {
        val previousTotalDurationNanos = totalDurationNanos
        observeTotalDuration()
        if (previousTotalDurationNanos != totalDurationNanos) {
            val animation = currentAnimation
            if (animation != null) {
                animation.durationNanos = totalDurationNanos
                if (animation.animationSpec == null) {
                    animation.animationSpecDuration =
                        ((1.0 - animation.start[0]) * totalDurationNanos).roundToLong()
                }
            } else {
                // seekTo() called with a fraction. If an animation is running, we can just wait
                // for the animation to change the value. The fraction may not be the best way
                // to advance a regular animation.
                seekToFraction()
            }
        }
    }

    private fun seekToFraction() {
        val transition = transition ?: return
        val playTimeNanos = (fraction.toDouble() * transition.totalDurationNanos).roundToLong()
        transition.seekAnimations(playTimeNanos)
    }

    /**
     * Contains the state for a running animation. This can be the current animation from
     * [animateTo] or an initial value animation.
     */
    internal class SeekingAnimationState {
        // The current progress with respect to the animationSpec if it exists or
        // durationNanos if animationSpec is null
        var progressNanos: Long = 0L

        // The AnimationSpec used in this animation, or null if it is a linear animation with
        // duration of durationNanos
        var animationSpec: VectorizedAnimationSpec<AnimationVector1D>? = null

        // Used by initial value animations to mark when the animation should continue
        var isComplete = false

        // The current fraction of the animation
        var value: Float = 0f

        // The start value of the animation
        var start: AnimationVector1D = AnimationVector1D(0f)

        // The initial velocity of the animation
        var initialVelocity: AnimationVector1D? = null

        // The total duration of the transition's animations. This is the totalDurationNanos
        // at the time that this was created for initial value animations. Note that this can
        // be different from the animationSpec's duration.
        var durationNanos: Long = 0L

        // The total duration of the animationSpec. This is kept cached because Spring
        // animations can take time to calculate their durations
        var animationSpecDuration: Long = 0L

        override fun toString(): String {
            return "progress nanos: $progressNanos, animationSpec: $animationSpec," +
                " isComplete: $isComplete, value: $value, start: $start," +
                " initialVelocity: $initialVelocity, durationNanos: $durationNanos," +
                " animationSpecDuration: $animationSpecDuration"
        }
    }

    private companion object {
        // AnimationVector1D with 0 value, kept so that we don't have to allocate unnecessarily
        val ZeroVelocity = AnimationVector1D(0f)

        // AnimationVector1D with 1 value, used as the target value of 1f
        val Target1 = AnimationVector1D(1f)
    }
}

/**
 * Creates a [Transition] and puts it in the [currentState][TransitionState.currentState] of
 * the provided [transitionState]. If the [TransitionState.targetState] changes, the [Transition]
 * will change where it will animate to.
 *
 * __Remember__: The provided [transitionState] needs to be [remember]ed.
 *
 * Compared to [updateTransition] that takes a targetState, this function supports a
 * different initial state than the first targetState. Here is an example:
 *
 * @sample androidx.compose.animation.core.samples.InitialStateSample
 *
 * In most cases, it is recommended to reuse the same [transitionState] that is [remember]ed, such
 * that [Transition] preserves continuity when [targetState][MutableTransitionState.targetState] is
 * changed. However, in some rare cases it is more critical to immediately *snap* to a state
 * change (e.g. in response to a user interaction). This can be achieved by creating a new
 * [transitionState]:
 * @sample androidx.compose.animation.core.samples.DoubleTapToLikeSample
 */
@Composable
fun <T> rememberTransition(
    transitionState: TransitionState<T>,
    label: String? = null
): Transition<T> {
    val transition = remember(transitionState) {
        Transition(transitionState = transitionState, label)
    }
    if (transitionState is SeekableTransitionState) {
        LaunchedEffect(transitionState.currentState, transitionState.targetState) {
            transitionState.observeTotalDuration()
            transitionState.compositionContinuationMutex.withLock {
                transitionState.composedTargetState = transitionState.targetState
                transitionState.compositionContinuation?.resume(transitionState.targetState)
                transitionState.compositionContinuation = null
            }
        }
    } else {
        transition.animateTo(transitionState.targetState)
    }
    DisposableEffect(transition) {
        onDispose {
            // Clean up on the way out, to ensure the observers are not stuck in an in-between
            // state.
            transition.onDisposed()
        }
    }
    return transition
}

/**
 * Creates a [Transition] and puts it in the [currentState][MutableTransitionState.currentState] of
 * the provided [transitionState]. Whenever the [targetState][MutableTransitionState.targetState] of
 * the [transitionState] changes, the [Transition] will animate to the new target state.
 *
 * __Remember__: The provided [transitionState] needs to be [remember]ed.
 *
 * Compared to the [rememberTransition] variant that takes a targetState, this function supports a
 * different initial state than the first targetState. Here is an example:
 *
 * @sample androidx.compose.animation.core.samples.InitialStateSample
 *
 * In most cases, it is recommended to reuse the same [transitionState] that is [remember]ed, such
 * that [Transition] preserves continuity when [targetState][MutableTransitionState.targetState] is
 * changed. However, in some rare cases it is more critical to immediately *snap* to a state
 * change (e.g. in response to a user interaction). This can be achieved by creating a new
 * [transitionState]:
 * @sample androidx.compose.animation.core.samples.DoubleTapToLikeSample
 */
@Deprecated(
    "Use rememberTransition() instead",
    replaceWith = ReplaceWith("rememberTransition(transitionState, label)")
)
@Composable
fun <T> updateTransition(
    transitionState: MutableTransitionState<T>,
    label: String? = null
): Transition<T> {
    val state: TransitionState<T> = transitionState
    return rememberTransition(state, label)
}

/**
 * [Transition] manages all the child animations on a state level. Child animations
 * can be created in a declarative way using [Transition.animateFloat], [Transition.animateValue],
 * [animateColor][androidx.compose.animation.animateColor] etc. When the [targetState] changes,
 * [Transition] will automatically start or adjust course for all its child animations to animate
 * to the new target values defined for each animation.
 *
 * After arriving at [targetState], [Transition] will be triggered to run if any child animation
 * changes its target value (due to their dynamic target calculation logic, such as theme-dependent
 * values).
 *
 * @sample androidx.compose.animation.core.samples.GestureAnimationSample
 *
 * @see rememberTransition
 * @see Transition.animateFloat
 * @see Transition.animateValue
 * @see androidx.compose.animation.animateColor
 */
// TODO: Support creating Transition outside of composition and support imperative use of Transition
@Stable
class Transition<S> internal constructor(
    private val transitionState: TransitionState<S>,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    val parentTransition: Transition<*>?,
    val label: String? = null
) {
    @PublishedApi
    internal constructor(
        transitionState: TransitionState<S>,
        label: String? = null
    ) : this(transitionState, null, label)

    internal constructor(
        initialState: S,
        label: String?
    ) : this(MutableTransitionState(initialState), null, label)

    @PublishedApi
    internal constructor(
        transitionState: MutableTransitionState<S>,
        label: String? = null
    ) : this(transitionState as TransitionState<S>, null, label)

    /**
     * Current state of the transition. This will always be the initialState of the transition
     * until the transition is finished. Once the transition is finished, [currentState] will be
     * set to [targetState]. [currentState] is backed by a [MutableState].
     */
    val currentState: S
        get() = transitionState.currentState

    /**
     * Target state of the transition. This will be read by all child animations to determine their
     * most up-to-date target values.
     */
    var targetState: S by mutableStateOf(currentState)
        internal set

    /**
     * [segment] contains the initial state and the target state of the currently on-going
     * transition.
     */
    var segment: Segment<S> by mutableStateOf(SegmentImpl(currentState, currentState))
        private set

    /**
     * Indicates whether there is any animation running in the transition.
     */
    val isRunning: Boolean
        get() = startTimeNanos != AnimationConstants.UnspecifiedTime

    private var _playTimeNanos by mutableLongStateOf(0L)

    /**
     * Play time in nano-seconds. [playTimeNanos] is always non-negative. It starts from 0L at the
     * beginning of the transition and increment until all child animations have finished.
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY)
    var playTimeNanos: Long
        get() {
            return parentTransition?.playTimeNanos ?: _playTimeNanos
        }
        set(value) {
            if (parentTransition == null) {
                _playTimeNanos = value
            }
        }

    // startTimeNanos is in real frame time nanos for the root transition and
    // scaled frame time for child transitions (as offset from the root start)
    internal var startTimeNanos by mutableLongStateOf(AnimationConstants.UnspecifiedTime)

    // This gets calculated every time child is updated/added
    private var updateChildrenNeeded: Boolean by mutableStateOf(false)

    private val _animations = mutableStateListOf<TransitionAnimationState<*, *>>()
    private val _transitions = mutableStateListOf<Transition<*>>()

    /**
     * List of child transitions in a [Transition].
     */
    val transitions: List<Transition<*>>
        get() = _transitions

    /**
     * List of [TransitionAnimationState]s that are in a [Transition].
     */
    val animations: List<TransitionAnimationState<*, *>>
        get() = _animations

    // Seeking related
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY)
    var isSeeking: Boolean by mutableStateOf(false)
        internal set
    internal var lastSeekedTimeNanos = 0L

    /**
     * Used internally to know when a [SeekableTransitionState] is animating initial values
     * after [SeekableTransitionState.animateTo] or [SeekableTransitionState.seekTo] has
     * redirected a transition prior to it completing. This is important for knowing when
     * child transitions must be maintained after a parent target state has changed, but
     * the child target state hasn't changed.
     */
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:Suppress("GetterSetterNames") // Don't care about Java name for this property
    @InternalAnimationApi
    @get:InternalAnimationApi
    val hasInitialValueAnimations: Boolean
        get() = _animations.fastAny { it.initialValueState != null } ||
            _transitions.fastAny { it.hasInitialValueAnimations }

    /**
     * Total duration of the [Transition], accounting for all the animations and child transitions
     * defined on the [Transition].
     *
     * Note: The total duration is subject to change as more animations/child transitions get added
     * to [Transition]. It's strongly recommended to query this *after* all the animations in the
     * [Transition] are set up.
     */
    val totalDurationNanos: Long by derivedStateOf { calculateTotalDurationNanos() }

    private fun calculateTotalDurationNanos(): Long {
        var maxDurationNanos = 0L
        _animations.fastForEach {
            maxDurationNanos = max(maxDurationNanos, it.durationNanos)
        }
        _transitions.fastForEach {
            maxDurationNanos = max(
                maxDurationNanos,
                it.calculateTotalDurationNanos()
            )
        }
        return maxDurationNanos
    }

    @OptIn(InternalAnimationApi::class)
    internal fun onFrame(frameTimeNanos: Long, durationScale: Float) {
        if (startTimeNanos == AnimationConstants.UnspecifiedTime) {
            onTransitionStart(frameTimeNanos)
        }

        val deltaT = frameTimeNanos - startTimeNanos
        val scaledPlayTimeNanos = if (durationScale == 0f) {
            deltaT
        } else {
            (deltaT / durationScale.toDouble()).roundToLong()
        }
        playTimeNanos = scaledPlayTimeNanos
        onFrame(scaledPlayTimeNanos, durationScale == 0f)
    }

    internal fun onFrame(scaledPlayTimeNanos: Long, scaleToEnd: Boolean) {
        if (startTimeNanos == AnimationConstants.UnspecifiedTime) {
            onTransitionStart(scaledPlayTimeNanos)
        } else if (!transitionState.isRunning) {
            transitionState.isRunning = true
        }
        updateChildrenNeeded = false

        var allFinished = true
        // Pulse new playtime
        _animations.fastForEach {
            if (!it.isFinished) {
                it.onPlayTimeChanged(scaledPlayTimeNanos, scaleToEnd)
            }
            // Check isFinished flag again after the animation pulse
            if (!it.isFinished) {
                allFinished = false
            }
        }
        _transitions.fastForEach {
            if (it.targetState != it.currentState) {
                it.onFrame(scaledPlayTimeNanos, scaleToEnd)
            }
            if (it.targetState != it.currentState) {
                allFinished = false
            }
        }
        if (allFinished) {
            onTransitionEnd()
        }
    }

    init {
        transitionState.transitionConfigured(this)
    }

    // onTransitionStart and onTransitionEnd are symmetric. Both are called from onFrame
    internal fun onTransitionStart(frameTimeNanos: Long) {
        startTimeNanos = frameTimeNanos
        transitionState.isRunning = true
    }

    // Called when the Transition is being disposed to clean up any state
    internal fun onDisposed() {
        onTransitionEnd()
        transitionState.transitionRemoved()
    }

    // onTransitionStart and onTransitionEnd are symmetric. Both are called from onFrame
    @OptIn(InternalAnimationApi::class)
    internal fun onTransitionEnd() {
        startTimeNanos = AnimationConstants.UnspecifiedTime
        if (transitionState is MutableTransitionState) {
            transitionState.currentState = targetState
        }
        playTimeNanos = 0
        transitionState.isRunning = false
        _transitions.fastForEach { it.onTransitionEnd() }
    }

    /**
     * This allows tools to set the transition (between initial and target state) to a specific
     * [playTimeNanos].
     *
     * Note: This function is intended for tooling use only.
     *
     * __Caveat:__  Once [initialState] or [targetState] changes, it needs to take a whole
     * composition pass for all the animations and child transitions to recompose with the
     * new [initialState] and [targetState]. Subsequently all the animations will be updated to the
     * given play time.
     *
     * __Caveat:__ This function puts [Transition] in a manual playtime setting mode. From then on
     * the [Transition] will not resume normal animation runs.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @OptIn(InternalAnimationApi::class)
    @JvmName("seek")
    fun setPlaytimeAfterInitialAndTargetStateEstablished(
        initialState: S,
        targetState: S,
        playTimeNanos: Long
    ) {
        // Reset running state
        startTimeNanos = AnimationConstants.UnspecifiedTime
        transitionState.isRunning = false
        if (!isSeeking || this.currentState != initialState || this.targetState != targetState) {
            // Reset all child animations
            if (currentState != initialState && transitionState is MutableTransitionState) {
                transitionState.currentState = initialState
            }
            this.targetState = targetState
            isSeeking = true
            segment = SegmentImpl(initialState, targetState)
        }

        _transitions.fastForEach {
            @Suppress("UNCHECKED_CAST")
            (it as Transition<Any>).let {
                if (it.isSeeking) {
                    it.setPlaytimeAfterInitialAndTargetStateEstablished(
                        it.currentState,
                        it.targetState,
                        playTimeNanos
                    )
                }
            }
        }

        _animations.fastForEach {
            it.seekTo(playTimeNanos)
        }
        lastSeekedTimeNanos = playTimeNanos
    }

    internal fun addTransition(transition: Transition<*>) = _transitions.add(transition)
    internal fun removeTransition(transition: Transition<*>) = _transitions.remove(transition)

    internal fun addAnimation(
        animation: TransitionAnimationState<*, *>
    ) = _animations.add(animation)

    internal fun removeAnimation(
        animation: TransitionAnimationState<*, *>
    ) {
        _animations.remove(animation)
    }

    // This target state should only be used to modify "mutableState"s, as it could potentially
    // roll back. The
    internal fun updateTarget(targetState: S) {
        // This is needed because child animations rely on this target state and the state pair to
        // update their animation specs
        if (this.targetState != targetState) {
            // Starting state should be the "next" state when waypoints are impl'ed
            segment = SegmentImpl(this.targetState, targetState)
            if (currentState != this.targetState) {
                transitionState.currentState = this.targetState
            }
            this.targetState = targetState
            if (!isRunning) {
                updateChildrenNeeded = true
            }

            // If target state is changed, reset all the animations to be re-created in the
            // next frame w/ their new target value. Child animations target values are updated in
            // the side effect that may not have happened when this function in invoked.
            resetAnimations()
        }
    }

    private fun resetAnimations() {
        _animations.fastForEach { it.resetAnimation() }
        _transitions.fastForEach { it.resetAnimations() }
    }

    // This should only be called if PlayTime comes from clock directly, instead of from a parent
    // Transition.
    @OptIn(InternalAnimationApi::class)
    @Suppress("ComposableNaming")
    @Composable
    internal fun animateTo(targetState: S) {
        if (!isSeeking) {
            updateTarget(targetState)
            // target != currentState adds the effect into the tree in the same frame as
            // target change.
            if (targetState != currentState || isRunning || updateChildrenNeeded) {
                // We're using a composition-obtained scope + DisposableEffect here to give us
                // control over coroutine dispatching
                val coroutineScope = rememberCoroutineScope()
                DisposableEffect(coroutineScope, this) {
                    // Launch the coroutine undispatched so the block is executed in the current
                    // frame. This is important as this initializes the state.
                    coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                        val durationScale = coroutineContext.durationScale
                        while (isActive) {
                            withFrameNanos {
                                // This check is very important, as isSeeking may be changed
                                // off-band between the last check in composition and this callback
                                // which happens in the animation callback the next frame.
                                if (!isSeeking) {
                                    onFrame(it / AnimationDebugDurationScale, durationScale)
                                }
                            }
                        }
                    }
                    onDispose { }
                }
            }
        }
    }

    /**
     * Used by [SeekableTransitionState] to seek the current transition animation to
     * [playTimeNanos].
     */
    internal fun seekAnimations(playTimeNanos: Long) {
        if (startTimeNanos == AnimationConstants.UnspecifiedTime) {
            startTimeNanos = playTimeNanos
        }
        this.playTimeNanos = playTimeNanos
        updateChildrenNeeded = false

        // Pulse new playtime
        _animations.fastForEach {
            it.seekTo(playTimeNanos)
        }
        _transitions.fastForEach {
            if (it.targetState != it.currentState) {
                it.seekAnimations(playTimeNanos)
            }
        }
    }

    /**
     * Changes the existing animations to be initial value animations. An existing animation
     * was interrupted, so the current animation is used only for the initial values. The
     * current animation is then changed to an unchanging animation that is only moved by
     * the initial value.
     */
    internal fun setInitialAnimations(
        animationState: SeekableTransitionState.SeekingAnimationState
    ) {
        _animations.fastForEach {
            it.setInitialValueAnimation(animationState)
        }
        _transitions.fastForEach {
            it.setInitialAnimations(animationState)
        }
    }

    /**
     * Clears all animations. The state has been forced directly to a new value and the
     * animations are no longer valid.
     */
    internal fun resetAnimationFraction(fraction: Float) {
        _animations.fastForEach { it.resetAnimationValue(fraction) }
        _transitions.fastForEach { it.resetAnimationFraction(fraction) }
    }

    /**
     * Clears all initial value animations.
     */
    internal fun clearInitialAnimations() {
        _animations.fastForEach {
            it.clearInitialAnimation()
        }
        _transitions.fastForEach {
            it.clearInitialAnimations()
        }
    }

    /**
     * Changes the progress of the initial value.
     *
     * @return true if the animationState is animating anything or false if it isn't
     * animating anything.
     */
    internal fun updateInitialValues() {
        _animations.fastForEach { it.updateInitialValue() }
        _transitions.fastForEach { it.updateInitialValues() }
    }

    override fun toString(): String {
        return animations.fastFold("Transition animation values: ") { acc, anim -> "$acc$anim, " }
    }

    @OptIn(InternalAnimationApi::class)
    private fun onChildAnimationUpdated() {
        updateChildrenNeeded = true
        if (isSeeking) {
            // Update total duration
            var maxDurationNanos = 0L
            _animations.fastForEach {
                maxDurationNanos = max(maxDurationNanos, it.durationNanos)
                it.seekTo(lastSeekedTimeNanos)
            }
            // TODO: Is update duration the only thing that needs to be done during seeking to
            //  accommodate update children?
            updateChildrenNeeded = false
        }
    }

    /**
     * Each animation created using [animateFloat], [animateDp], etc is represented as a
     * [TransitionAnimationState] in [Transition].
     */
    @Stable
    inner class TransitionAnimationState<T, V : AnimationVector> internal constructor(
        initialValue: T,
        initialVelocityVector: V,
        val typeConverter: TwoWayConverter<T, V>,
        val label: String
    ) : State<T> {

        // Changed during composition, may rollback
        private var targetValue: T by mutableStateOf(initialValue)

        private val defaultSpring = spring<T>()

        /**
         * [AnimationSpec] that is used for current animation run. This can change when
         * [targetState] changes.
         */
        var animationSpec: FiniteAnimationSpec<T> by mutableStateOf(defaultSpring)
            private set

        /**
         * All the animation configurations including initial value/velocity & target value for
         * animating from [currentState] to [targetState] are captured in [animation].
         */
        var animation: TargetBasedAnimation<T, V> by mutableStateOf(
            TargetBasedAnimation(
                animationSpec, typeConverter, initialValue, targetValue,
                initialVelocityVector
            )
        )
            private set

        internal var initialValueState: SeekableTransitionState.SeekingAnimationState? = null
        private var initialValueAnimation: TargetBasedAnimation<T, V>? = null

        internal var isFinished: Boolean by mutableStateOf(true)
        internal var resetSnapValue by mutableFloatStateOf(NoReset)

        /**
         * When the target state has changed, but the target value remains the same,
         * the initial value animation completely controls the animated value. When
         * this flag is `true`, the [animation] can be ignored and only the [initialValueState]
         * is needed to determine the value. When this is `false`, if there is
         * an [initialValueState], it is used only for adjusting the initial value
         * of [animation].
         */
        private var useOnlyInitialValue = false

        // Changed during animation, no concerns of rolling back
        override var value by mutableStateOf(initialValue)
            internal set
        private var velocityVector: V = initialVelocityVector
        internal var durationNanos by mutableLongStateOf(animation.durationNanos)

        private var isSeeking = false

        internal fun onPlayTimeChanged(playTimeNanos: Long, scaleToEnd: Boolean) {
            val playTime = if (scaleToEnd) animation.durationNanos else playTimeNanos
            value = animation.getValueFromNanos(playTime)
            velocityVector = animation.getVelocityVectorFromNanos(playTime)
            if (animation.isFinishedFromNanos(playTime)) {
                isFinished = true
            }
        }

        internal fun seekTo(playTimeNanos: Long) {
            if (resetSnapValue != NoReset) {
                return
            }
            isSeeking = true // SeekableTransitionState won't use interrupted animation spec
            if (animation.targetValue == animation.initialValue) {
                // This is likely an interrupted animation and the initial value is changing, but
                // the target value remained the same. The initial value animation has the target
                // value, so only the initial value animation is changing the value.
                value = animation.targetValue
            } else {
                // TODO: unlikely but need to double check that animation returns the correct values
                // when play time is way past their durations.
                value = animation.getValueFromNanos(playTimeNanos)
                velocityVector = animation.getVelocityVectorFromNanos(playTimeNanos)
            }
        }

        /**
         * Updates the initial value animation. When a SeekableTransitionState transition is
         * interrupted, the ongoing animation is moved to changing the initial value. The starting
         * point of the animation is then animated toward the value that would be set at the
         * target state, while the current value is controlled by seeking or animation.
         */
        internal fun updateInitialValue() {
            val animState = initialValueState ?: return
            val animation = initialValueAnimation ?: return

            val initialPlayTimeNanos = (
                // Single-precision floating point is not sufficient here as it only has about 7
                // decimal digits of precision. We are dealing with nanos which has at least 9
                // decimal digits in most cases.
                animState.durationNanos * animState.value.toDouble()
                ).roundToLong()
            val initialValue = animation.getValueFromNanos(initialPlayTimeNanos)
            if (useOnlyInitialValue) {
                this.animation.mutableTargetValue = initialValue
            }
            this.animation.mutableInitialValue = initialValue
            durationNanos = this.animation.durationNanos
            if (resetSnapValue == ResetNoSnap || useOnlyInitialValue) {
                value = initialValue
            } else {
                seekTo(playTimeNanos)
            }
            if (initialPlayTimeNanos >= animState.durationNanos) {
                initialValueState = null
                initialValueAnimation = null
            } else {
                animState.isComplete = false
            }
        }

        private val interruptionSpec: FiniteAnimationSpec<T>

        init {
            val visibilityThreshold: T? = visibilityThresholdMap.get(typeConverter)?.let {
                val vector = typeConverter.convertToVector(initialValue)
                for (id in 0 until vector.size) {
                    vector[id] = it
                }
                typeConverter.convertFromVector(vector)
            }
            interruptionSpec = spring(visibilityThreshold = visibilityThreshold)
        }

        private fun updateAnimation(
            initialValue: T = value,
            isInterrupted: Boolean = false,
        ) {
            if (initialValueAnimation?.targetValue == targetValue) {
                // This animation didn't change the target value, so let the initial value animation
                // take care of it.
                animation = TargetBasedAnimation(
                    interruptionSpec,
                    typeConverter,
                    initialValue,
                    initialValue,
                    velocityVector.newInstance() // 0 velocity
                )
                useOnlyInitialValue = true
                durationNanos = animation.durationNanos
                return
            }
            val specWithoutDelay =
                if (isInterrupted && !isSeeking) {
                    // When interrupted, use the default spring, unless the spec is also a spring.
                    if (animationSpec is SpringSpec<*>) animationSpec else interruptionSpec
                } else {
                    animationSpec
                }
            val spec =
                if (playTimeNanos <= 0L) {
                    specWithoutDelay
                } else {
                    delayed(specWithoutDelay, playTimeNanos)
                }
            animation =
                TargetBasedAnimation(spec, typeConverter, initialValue, targetValue, velocityVector)
            durationNanos = animation.durationNanos
            useOnlyInitialValue = false
            onChildAnimationUpdated()
        }

        internal fun resetAnimation() {
            resetSnapValue = ResetNoSnap
        }

        /**
         * Forces the value to the given fraction or reset value. If [fraction] is
         * [ResetAnimationSnapCurrent] or [ResetAnimationSnapTarget], the animated values
         * are directly moved to the start or end of the animation.
         */
        internal fun resetAnimationValue(fraction: Float) {
            if (fraction == ResetAnimationSnapCurrent || fraction == ResetAnimationSnapTarget) {
                val initAnim = initialValueAnimation
                if (initAnim != null) {
                    animation.mutableInitialValue = initAnim.targetValue
                    initialValueState = null
                    initialValueAnimation = null
                }

                val animationValue = if (fraction == ResetAnimationSnapCurrent) {
                    animation.initialValue
                } else {
                    animation.targetValue
                }
                animation.mutableInitialValue = animationValue
                animation.mutableTargetValue = animationValue
                value = animationValue
                durationNanos = animation.durationNanos
            } else {
                resetSnapValue = fraction
            }
        }

        internal fun setInitialValueAnimation(
            animationState: SeekableTransitionState.SeekingAnimationState
        ) {
            if (animation.targetValue != animation.initialValue) {
                // Continue the animation from the current position to the target
                initialValueAnimation = animation
                initialValueState = animationState
            }
            animation =
                TargetBasedAnimation(
                    interruptionSpec,
                    typeConverter,
                    value,
                    value,
                    velocityVector.newInstance() // 0 velocity
                )
            durationNanos = animation.durationNanos
            useOnlyInitialValue = true
        }

        internal fun clearInitialAnimation() {
            initialValueAnimation = null
            initialValueState = null
            useOnlyInitialValue = false
        }

        override fun toString(): String {
            return "current value: $value, target: $targetValue, spec: $animationSpec"
        }

        // This gets called *during* composition
        @OptIn(InternalAnimationApi::class)
        internal fun updateTargetValue(
            targetValue: T,
            animationSpec: FiniteAnimationSpec<T>
        ) {
            if (useOnlyInitialValue && targetValue == initialValueAnimation?.targetValue) {
                return // we're already animating to the target value through the initial value
            }
            if (this.targetValue == targetValue && resetSnapValue == NoReset) {
                return // nothing to change. Just continue the existing animation.
            }
            this.targetValue = targetValue
            this.animationSpec = animationSpec
            val initialValue = if (resetSnapValue == ResetAnimationSnap) targetValue else value
            updateAnimation(initialValue, isInterrupted = !isFinished)
            isFinished = resetSnapValue == ResetAnimationSnap
            // This is needed because the target change could happen during a transition
            if (resetSnapValue >= 0f) {
                val duration = animation.durationNanos
                value = animation.getValueFromNanos((duration * resetSnapValue).toLong())
            } else if (resetSnapValue == ResetAnimationSnap) {
                value = targetValue
            }
            useOnlyInitialValue = false
            resetSnapValue = NoReset
        }

        // This gets called *during* composition
        internal fun updateInitialAndTargetValue(
            initialValue: T,
            targetValue: T,
            animationSpec: FiniteAnimationSpec<T>
        ) {
            this.targetValue = targetValue
            this.animationSpec = animationSpec
            if (
                animation.initialValue == initialValue &&
                animation.targetValue == targetValue
            ) {
                return
            }
            updateAnimation(initialValue)
        }
    }

    private class SegmentImpl<S>(
        override val initialState: S,
        override val targetState: S
    ) : Segment<S> {
        override fun equals(other: Any?): Boolean {
            return other is Segment<*> && initialState == other.initialState &&
                targetState == other.targetState
        }

        override fun hashCode(): Int {
            return initialState.hashCode() * 31 + targetState.hashCode()
        }
    }

    /**
     * [Segment] holds [initialState] and [targetState], which are the beginning and end of a
     * transition. These states will be used to obtain the animation spec that will be used for this
     * transition from the child animations.
     */
    @JvmDefaultWithCompatibility
    interface Segment<S> {
        /**
         * Initial state of a Transition Segment. This is the state that transition starts from.
         */
        val initialState: S

        /**
         * Target state of a Transition Segment. This is the state that transition will end on.
         */
        val targetState: S

        /**
         * Returns whether the provided state matches the [initialState] && the provided
         * [targetState] matches [Segment.targetState].
         */
        infix fun S.isTransitioningTo(targetState: S): Boolean {
            return this == initialState && targetState == this@Segment.targetState
        }
    }

    /**
     * [DeferredAnimation] can be constructed using [Transition.createDeferredAnimation] during
     * composition and initialized later. It is useful for animations, the target values for
     * which are unknown at composition time (e.g. layout size/position, etc).
     *
     * Once a [DeferredAnimation] is created, it can be configured and updated as needed using
     * [DeferredAnimation.animate] method.
     *
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    inner class DeferredAnimation<T, V : AnimationVector> internal constructor(
        val typeConverter: TwoWayConverter<T, V>,
        val label: String
    ) {
        internal var data: DeferredAnimationData<T, V>? by mutableStateOf(null)

        internal inner class DeferredAnimationData<T, V : AnimationVector>(
            val animation: Transition<S>.TransitionAnimationState<T, V>,
            var transitionSpec: Segment<S>.() -> FiniteAnimationSpec<T>,
            var targetValueByState: (state: S) -> T,
        ) : State<T> {
            fun updateAnimationStates(segment: Segment<S>) {
                val targetValue = targetValueByState(segment.targetState)
                if (isSeeking) {
                    val initialValue = targetValueByState(segment.initialState)
                    // In the case of seeking, we also need to update initial value as needed
                    animation.updateInitialAndTargetValue(
                        initialValue,
                        targetValue,
                        segment.transitionSpec()
                    )
                } else {
                    animation.updateTargetValue(targetValue, segment.transitionSpec())
                }
            }

            override val value: T
                get() {
                    updateAnimationStates(segment)
                    return animation.value
                }
        }

        /**
         * [DeferredAnimation] allows the animation setup to be deferred until a later time after
         * composition. [animate] can be used to set up a [DeferredAnimation]. Like other
         * Transition animations such as [Transition.animateFloat], [DeferredAnimation] also
         * expects [transitionSpec] and [targetValueByState] for the mapping from target state
         * to animation spec and target value, respectively.
         */
        fun animate(
            transitionSpec: Segment<S>.() -> FiniteAnimationSpec<T>,
            targetValueByState: (state: S) -> T
        ): State<T> {
            val animData: DeferredAnimationData<T, V> = data ?: DeferredAnimationData(
                TransitionAnimationState(
                    targetValueByState(currentState),
                    typeConverter.createZeroVectorFrom(targetValueByState(currentState)),
                    typeConverter,
                    label
                ),
                transitionSpec,
                targetValueByState
            ).apply {
                data = this
                addAnimation(this.animation)
            }
            return animData.apply {
                // Update animtion data with the latest mapping
                this.targetValueByState = targetValueByState
                this.transitionSpec = transitionSpec

                updateAnimationStates(segment)
            }
        }

        internal fun setupSeeking() {
            data?.apply {
                animation.updateInitialAndTargetValue(
                    targetValueByState(segment.initialState),
                    targetValueByState(segment.targetState),
                    segment.transitionSpec()
                )
            }
        }
    }

    internal fun removeAnimation(deferredAnimation: DeferredAnimation<*, *>) {
        deferredAnimation.data?.animation?.let {
            removeAnimation(it)
        }
    }
}

// When a TransitionAnimation doesn't need to be reset
private const val NoReset = -1f

// When the animation needs to be changed because of a target update
private const val ResetNoSnap = -2f

// When the animation should be reset to have the same start and end value
private const val ResetAnimationSnap = -3f

// Snap to the current state and set the initial and target values to the same thing
private const val ResetAnimationSnapCurrent = -4f

// Snap to the target state and set the initial and target values to the same thing
private const val ResetAnimationSnapTarget = -5f

/**
 * This creates a [DeferredAnimation], which will not animate until it is set up using
 * [DeferredAnimation.animate]. Once the animation is set up, it will animate from the
 * [currentState][Transition.currentState] to [targetState][Transition.targetState]. If the
 * [Transition] has already arrived at its target state at the time when the animation added, there
 * will be no animation.
 *
 * @param typeConverter A converter to convert any value of type [T] from/to an [AnimationVector]
 * @param label A label for differentiating this animation from others in android studio.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@Composable
fun <S, T, V : AnimationVector> Transition<S>.createDeferredAnimation(
    typeConverter: TwoWayConverter<T, V>,
    label: String = "DeferredAnimation"
): Transition<S>.DeferredAnimation<T, V> {
    val lazyAnim = remember(this) { DeferredAnimation(typeConverter, label) }
    DisposableEffect(lazyAnim) {
        onDispose {
            removeAnimation(lazyAnim)
        }
    }
    if (isSeeking) {
        lazyAnim.setupSeeking()
    }
    return lazyAnim
}

/**
 * [createChildTransition] creates a child Transition based on the mapping between parent state to
 * child state provided in [transformToChildState]. This serves the following purposes:
 * 1) Hoist the child transition state into parent transition. Therefore the parent Transition
 * will be aware of whether there's any on-going animation due to the same target state change.
 * This will further allow sequential animation to be set up when all animations have finished.
 * 2) Separation of concerns. The child transition can respresent a much more simplified state
 * transition when, for example, mapping from an enum parent state to a Boolean visible state for
 * passing further down the compose tree. The child composables hence can be designed around
 * handling a more simple and a more relevant state change.
 *
 * [label] is used to differentiate from other animations in the same transition in Android Studio.
 *
 * @sample androidx.compose.animation.core.samples.CreateChildTransitionSample
 */
@ExperimentalTransitionApi
@Composable
inline fun <S, T> Transition<S>.createChildTransition(
    label: String = "ChildTransition",
    transformToChildState: @Composable (parentState: S) -> T,
): Transition<T> {
    val initialParentState = remember(this) { this.currentState }
    val initialState = transformToChildState(if (isSeeking) currentState else initialParentState)
    val targetState = transformToChildState(this.targetState)
    return createChildTransitionInternal(initialState, targetState, label)
}

@PublishedApi
@Composable
internal fun <S, T> Transition<S>.createChildTransitionInternal(
    initialState: T,
    targetState: T,
    childLabel: String,
): Transition<T> {
    val transition = remember(this) {
        Transition(MutableTransitionState(initialState), this, "${this.label} > $childLabel")
    }

    DisposableEffect(transition) {
        addTransition(transition)
        onDispose {
            removeTransition(transition)
        }
    }

    if (isSeeking) {
        transition.setPlaytimeAfterInitialAndTargetStateEstablished(
            initialState,
            targetState,
            this.lastSeekedTimeNanos
        )
    } else {
        transition.updateTarget(targetState)
        transition.isSeeking = false
    }
    return transition
}

/**
 * Creates an animation of type [T] as a part of the given [Transition]. This means the states
 * of this animation will be managed by the [Transition]. [typeConverter] will be used to convert
 * between type [T] and [AnimationVector] so that the animation system knows how to animate it.
 *
 * [targetValueByState] is used as a mapping from a target state to the target value of this
 * animation. [Transition] will be using this mapping to determine what value to target this
 * animation towards. __Note__ that [targetValueByState] is a composable function. This means the
 * mapping function could access states, CompositionLocals, themes, etc. If the targetValue changes
 * outside of a [Transition] run (i.e. when the [Transition] already reached its targetState), the
 * [Transition] will start running again to ensure this animation reaches its new target smoothly.
 *
 * An optional [transitionSpec] can be provided to specify (potentially different) animation for
 * each pair of initialState and targetState. [FiniteAnimationSpec] includes any non-infinite
 * animation, such as [tween], [spring], [keyframes] and even [repeatable], but not
 * [infiniteRepeatable]. By default, [transitionSpec] uses a [spring] animation for all transition
 * destinations.
 *
 * [label] is used to differentiate from other animations in the same transition in Android Studio.
 *
 * @return A [State] object, the value of which is updated by animation
 * @see updateTransition
 * @see rememberTransition
 * @see Transition.animateFloat
 * @see androidx.compose.animation.animateColor
 */
@Composable
inline fun <S, T, V : AnimationVector> Transition<S>.animateValue(
    typeConverter: TwoWayConverter<T, V>,
    noinline transitionSpec: @Composable Transition.Segment<S>.() -> FiniteAnimationSpec<T> =
        { spring() },
    label: String = "ValueAnimation",
    targetValueByState: @Composable (state: S) -> T
): State<T> {

    val initialValue = targetValueByState(currentState)
    val targetValue = targetValueByState(targetState)
    val animationSpec = transitionSpec(segment)

    return createTransitionAnimation(initialValue, targetValue, animationSpec, typeConverter, label)
}

@PublishedApi
@Composable
internal fun <S, T, V : AnimationVector> Transition<S>.createTransitionAnimation(
    initialValue: T,
    targetValue: T,
    animationSpec: FiniteAnimationSpec<T>,
    typeConverter: TwoWayConverter<T, V>,
    label: String
): State<T> {
    val transitionAnimation = remember(this) {
        // Initialize the animation state to initialState value, so if it's added during a
        // transition run, it'll participate in the animation.
        // This is preferred because it's easy to opt out - Simply adding new animation once
        // currentState == targetState would opt out.
        TransitionAnimationState(
            initialValue,
            typeConverter.createZeroVectorFrom(targetValue),
            typeConverter,
            label
        )
    }
    if (isSeeking) {
        // In the case of seeking, we also need to update initial value as needed
        transitionAnimation.updateInitialAndTargetValue(
            initialValue,
            targetValue,
            animationSpec
        )
    } else {
        transitionAnimation.updateTargetValue(targetValue, animationSpec)
    }

    DisposableEffect(transitionAnimation) {
        addAnimation(transitionAnimation)
        onDispose {
            removeAnimation(transitionAnimation)
        }
    }
    return transitionAnimation
}

// TODO: Remove noinline when b/174814083 is fixed.
/**
 * Creates a Float animation as a part of the given [Transition]. This means the states
 * of this animation will be managed by the [Transition].
 *
 * [targetValueByState] is used as a mapping from a target state to the target value of this
 * animation. [Transition] will be using this mapping to determine what value to target this
 * animation towards. __Note__ that [targetValueByState] is a composable function. This means the
 * mapping function could access states, CompositionLocals, themes, etc. If the targetValue changes
 * outside of a [Transition] run (i.e. when the [Transition] already reached its targetState), the
 * [Transition] will start running again to ensure this animation reaches its new target smoothly.
 *
 * An optional [transitionSpec] can be provided to specify (potentially different) animation for
 * each pair of initialState and targetState. [FiniteAnimationSpec] includes any non-infinite
 * animation, such as [tween], [spring], [keyframes] and even [repeatable], but not
 * [infiniteRepeatable]. By default, [transitionSpec] uses a [spring] animation for all transition
 * destinations.
 *
 * @sample androidx.compose.animation.core.samples.AnimateFloatSample
 *
 * [label] is used to differentiate from other animations in the same transition in Android Studio.
 *
 * @return A [State] object, the value of which is updated by animation
 * @see rememberTransition
 * @see updateTransition
 * @see Transition.animateValue
 * @see androidx.compose.animation.animateColor
 */
@Composable
inline fun <S> Transition<S>.animateFloat(
    noinline transitionSpec:
    @Composable Transition.Segment<S>.() -> FiniteAnimationSpec<Float> = { spring() },
    label: String = "FloatAnimation",
    targetValueByState: @Composable (state: S) -> Float
): State<Float> =
    animateValue(Float.VectorConverter, transitionSpec, label, targetValueByState)

/**
 * Creates a [Dp] animation as a part of the given [Transition]. This means the states
 * of this animation will be managed by the [Transition].
 *
 * [targetValueByState] is used as a mapping from a target state to the target value of this
 * animation. [Transition] will be using this mapping to determine what value to target this
 * animation towards. __Note__ that [targetValueByState] is a composable function. This means the
 * mapping function could access states, CompositionLocals, themes, etc. If the targetValue changes
 * outside of a [Transition] run (i.e. when the [Transition] already reached its targetState), the
 * [Transition] will start running again to ensure this animation reaches its new target smoothly.
 *
 * An optional [transitionSpec] can be provided to specify (potentially different) animation for
 * each pair of initialState and targetState. [FiniteAnimationSpec] includes any non-infinite
 * animation, such as [tween], [spring], [keyframes] and even [repeatable], but not
 * [infiniteRepeatable]. By default, [transitionSpec] uses a [spring] animation for all transition
 * destinations.
 *
 * [label] is used to differentiate from other animations in the same transition in Android Studio.
 *
 * @return A [State] object, the value of which is updated by animation
 */
@Composable
inline fun <S> Transition<S>.animateDp(
    noinline transitionSpec: @Composable Transition.Segment<S>.() -> FiniteAnimationSpec<Dp> = {
        spring(visibilityThreshold = Dp.VisibilityThreshold)
    },
    label: String = "DpAnimation",
    targetValueByState: @Composable (state: S) -> Dp
): State<Dp> =
    animateValue(Dp.VectorConverter, transitionSpec, label, targetValueByState)

/**
 * Creates an [Offset] animation as a part of the given [Transition]. This means the states
 * of this animation will be managed by the [Transition].
 *
 * [targetValueByState] is used as a mapping from a target state to the target value of this
 * animation. [Transition] will be using this mapping to determine what value to target this
 * animation towards. __Note__ that [targetValueByState] is a composable function. This means the
 * mapping function could access states, CompositionLocals, themes, etc. If the targetValue changes
 * outside of a [Transition] run (i.e. when the [Transition] already reached its targetState), the
 * [Transition] will start running again to ensure this animation reaches its new target smoothly.
 *
 * An optional [transitionSpec] can be provided to specify (potentially different) animation for
 * each pair of initialState and targetState. [FiniteAnimationSpec] includes any non-infinite
 * animation, such as [tween], [spring], [keyframes] and even [repeatable], but not
 * [infiniteRepeatable]. By default, [transitionSpec] uses a [spring] animation for all transition
 * destinations.
 *
 * [label] is used to differentiate from other animations in the same transition in Android Studio.
 *
 * @return A [State] object, the value of which is updated by animation
 */
@Composable
inline fun <S> Transition<S>.animateOffset(
    noinline transitionSpec: @Composable Transition.Segment<S>.() -> FiniteAnimationSpec<Offset> = {
        spring(visibilityThreshold = Offset.VisibilityThreshold)
    },
    label: String = "OffsetAnimation",
    targetValueByState: @Composable (state: S) -> Offset
): State<Offset> =
    animateValue(Offset.VectorConverter, transitionSpec, label, targetValueByState)

/**
 * Creates a [Size] animation as a part of the given [Transition]. This means the states
 * of this animation will be managed by the [Transition].
 *
 * [targetValueByState] is used as a mapping from a target state to the target value of this
 * animation. [Transition] will be using this mapping to determine what value to target this
 * animation towards. __Note__ that [targetValueByState] is a composable function. This means the
 * mapping function could access states, CompositionLocals, themes, etc. If the targetValue changes
 * outside of a [Transition] run (i.e. when the [Transition] already reached its targetState), the
 * [Transition] will start running again to ensure this animation reaches its new target smoothly.
 *
 * An optional [transitionSpec] can be provided to specify (potentially different) animation for
 * each pair of initialState and targetState. [FiniteAnimationSpec] includes any non-infinite
 * animation, such as [tween], [spring], [keyframes] and even [repeatable], but not
 * [infiniteRepeatable]. By default, [transitionSpec] uses a [spring] animation for all transition
 * destinations.
 *
 * [label] is used to differentiate from other animations in the same transition in Android Studio.
 *
 * @return A [State] object, the value of which is updated by animation
 */
@Composable
inline fun <S> Transition<S>.animateSize(
    noinline transitionSpec: @Composable Transition.Segment<S>.() -> FiniteAnimationSpec<Size> = {
        spring(visibilityThreshold = Size.VisibilityThreshold)
    },
    label: String = "SizeAnimation",
    targetValueByState: @Composable (state: S) -> Size
): State<Size> =
    animateValue(Size.VectorConverter, transitionSpec, label, targetValueByState)

/**
 * Creates a [IntOffset] animation as a part of the given [Transition]. This means the states
 * of this animation will be managed by the [Transition].
 *
 * [targetValueByState] is used as a mapping from a target state to the target value of this
 * animation. [Transition] will be using this mapping to determine what value to target this
 * animation towards. __Note__ that [targetValueByState] is a composable function. This means the
 * mapping function could access states, CompositionLocals, themes, etc. If the targetValue changes
 * outside of a [Transition] run (i.e. when the [Transition] already reached its targetState), the
 * [Transition] will start running again to ensure this animation reaches its new target smoothly.
 *
 * An optional [transitionSpec] can be provided to specify (potentially different) animation for
 * each pair of initialState and targetState. [FiniteAnimationSpec] includes any non-infinite
 * animation, such as [tween], [spring], [keyframes] and even [repeatable], but not
 * [infiniteRepeatable]. By default, [transitionSpec] uses a [spring] animation for all transition
 * destinations.
 *
 * [label] is used to differentiate from other animations in the same transition in Android Studio.
 *
 * @return A [State] object, the value of which is updated by animation
 */
@Composable
inline fun <S> Transition<S>.animateIntOffset(
    noinline transitionSpec:
    @Composable Transition.Segment<S>.() -> FiniteAnimationSpec<IntOffset> =
        { spring(visibilityThreshold = IntOffset(1, 1)) },
    label: String = "IntOffsetAnimation",
    targetValueByState: @Composable (state: S) -> IntOffset
): State<IntOffset> =
    animateValue(IntOffset.VectorConverter, transitionSpec, label, targetValueByState)

/**
 * Creates a [Int] animation as a part of the given [Transition]. This means the states
 * of this animation will be managed by the [Transition].
 *
 * [targetValueByState] is used as a mapping from a target state to the target value of this
 * animation. [Transition] will be using this mapping to determine what value to target this
 * animation towards. __Note__ that [targetValueByState] is a composable function. This means the
 * mapping function could access states, CompositionLocals, themes, etc. If the targetValue changes
 * outside of a [Transition] run (i.e. when the [Transition] already reached its targetState), the
 * [Transition] will start running again to ensure this animation reaches its new target smoothly.
 *
 * An optional [transitionSpec] can be provided to specify (potentially different) animation for
 * each pair of initialState and targetState. [FiniteAnimationSpec] includes any non-infinite
 * animation, such as [tween], [spring], [keyframes] and even [repeatable], but not
 * [infiniteRepeatable]. By default, [transitionSpec] uses a [spring] animation for all transition
 * destinations.
 *
 * [label] is used to differentiate from other animations in the same transition in Android Studio.
 *
 * @return A [State] object, the value of which is updated by animation
 */
@Composable
inline fun <S> Transition<S>.animateInt(
    noinline transitionSpec: @Composable Transition.Segment<S>.() -> FiniteAnimationSpec<Int> = {
        spring(visibilityThreshold = 1)
    },
    label: String = "IntAnimation",
    targetValueByState: @Composable (state: S) -> Int
): State<Int> =
    animateValue(Int.VectorConverter, transitionSpec, label, targetValueByState)

/**
 * Creates a [IntSize] animation as a part of the given [Transition]. This means the states
 * of this animation will be managed by the [Transition].
 *
 * [targetValueByState] is used as a mapping from a target state to the target value of this
 * animation. [Transition] will be using this mapping to determine what value to target this
 * animation towards. __Note__ that [targetValueByState] is a composable function. This means the
 * mapping function could access states, CompositionLocals, themes, etc. If the targetValue changes
 * outside of a [Transition] run (i.e. when the [Transition] already reached its targetState), the
 * [Transition] will start running again to ensure this animation reaches its new target smoothly.
 *
 * An optional [transitionSpec] can be provided to specify (potentially different) animation for
 * each pair of initialState and targetState. [FiniteAnimationSpec] includes any non-infinite
 * animation, such as [tween], [spring], [keyframes] and even [repeatable], but not
 * [infiniteRepeatable]. By default, [transitionSpec] uses a [spring] animation for all transition
 * destinations.
 *
 * [label] is used to differentiate from other animations in the same transition in Android Studio.
 *
 * @return A [State] object, the value of which is updated by animation
 */
@Composable
inline fun <S> Transition<S>.animateIntSize(
    noinline transitionSpec: @Composable Transition.Segment<S>.() -> FiniteAnimationSpec<IntSize> =
        { spring(visibilityThreshold = IntSize(1, 1)) },
    label: String = "IntSizeAnimation",
    targetValueByState: @Composable (state: S) -> IntSize
): State<IntSize> =
    animateValue(IntSize.VectorConverter, transitionSpec, label, targetValueByState)

/**
 * Creates a [Rect] animation as a part of the given [Transition]. This means the states
 * of this animation will be managed by the [Transition].
 *
 * [targetValueByState] is used as a mapping from a target state to the target value of this
 * animation. [Transition] will be using this mapping to determine what value to target this
 * animation towards. __Note__ that [targetValueByState] is a composable function. This means the
 * mapping function could access states, CompositionLocals, themes, etc. If the targetValue changes
 * outside of a [Transition] run (i.e. when the [Transition] already reached its targetState), the
 * [Transition] will start running again to ensure this animation reaches its new target smoothly.
 *
 * An optional [transitionSpec] can be provided to specify (potentially different) animation for
 * each pair of initialState and targetState. [FiniteAnimationSpec] includes any non-infinite
 * animation, such as [tween], [spring], [keyframes] and even [repeatable], but not
 * [infiniteRepeatable]. By default, [transitionSpec] uses a [spring] animation for all transition
 * destinations.
 *
 * [label] is used to differentiate from other animations in the same transition in Android Studio.
 *
 * @return A [State] object, the value of which is updated by animation
 */
@Composable
inline fun <S> Transition<S>.animateRect(
    noinline transitionSpec: @Composable Transition.Segment<S>.() -> FiniteAnimationSpec<Rect> =
        { spring(visibilityThreshold = Rect.VisibilityThreshold) },
    label: String = "RectAnimation",
    targetValueByState: @Composable (state: S) -> Rect
): State<Rect> =
    animateValue(Rect.VectorConverter, transitionSpec, label, targetValueByState)
