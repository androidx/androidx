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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.util.fastRoundToInt
import kotlin.math.max
import kotlin.math.roundToLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

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
 * A [TransitionState] that can manipulate the progress of the [Transition] by seeking
 * with [snapTo] or animating with [animateTo].
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
     * The Transition that this is associated with. SeekableTransitionState can only be used
     * with one Transition.
     */
    private var transition: Transition<S>? = null

    /**
     * The animated fraction for the [currentState] to [targetState].
     */
    private var animatedFraction = Animatable(0f).also {
        it.updateBounds(lowerBound = 0f, upperBound = 1f)
    }

    /**
     * The initial value animations from when a seek or animation is interrupted.
     * The key is the [animatedFraction] at the time of interruption and the value
     * is the duration of the animation.
     */
    private val initialFractionAnimatables =
        MutableObjectLongMap<Animatable<Float, AnimationVector1D>>()

    private val observer = SnapshotStateObserver { it() }

    // Used for seekToFraction calculations to avoid allocation
    private var totalDurationNanos = 0L

    private val reseekToFraction: (Unit) -> Unit = {
        seekToFraction()
    }

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
    val fraction: Float
        get() = if (currentState == targetState) 0f else animatedFraction.value

    /**
     * Starts seeking the transition to [targetState] with [fraction] used to indicate the progress
     * towards [targetState]. If the previous `targetState` was already
     * [targetState] then [snapTo] only stops any current animation towards that state and snaps
     * the fraction to the new value. Otherwise, the [currentState] is changed to the former
     * `targetState` and `targetState` is changed to [targetState] and an animation is started,
     * moving the start values towards the former `targetState`.
     *
     * @see animateTo
     */
    suspend fun snapTo(
        targetState: S = this.targetState,
        @FloatRange(from = 0.0, to = 1.0) fraction: Float = 0f
    ) {
        requirePrecondition(fraction in 0f..1f) {
            "Expecting fraction between 0 and 1. Got $fraction"
        }
        val transition = transition ?: return
        coroutineScope {
            if (targetState != this@SeekableTransitionState.targetState) {
                animateInitialState(this@coroutineScope)
                transition.updateTarget(targetState)
                this@SeekableTransitionState.currentState = this@SeekableTransitionState.targetState
                this@SeekableTransitionState.targetState = targetState
            }
            if (currentState != targetState) {
                animatedFraction.snapTo(fraction)
                seekToFraction()
            }
        }
    }

    /**
     * Change the animatedInitialFraction to use the animatedFraction, if it needs to be used.
     */
    private fun animateInitialState(coroutineScope: CoroutineScope) {
        val transition = transition ?: return
        if (animatedFraction.value == 1f || currentState == targetState) {
            return
        }

        val currentAnimatable = animatedFraction
        initialFractionAnimatables[currentAnimatable] = transition.totalDurationNanos
        animatedFraction = Animatable(0f).also {
            it.updateBounds(lowerBound = 0f, upperBound = 1f)
        }

        // Change what it does with the animation
        transition.setInitialAnimations(currentAnimatable)
        initialFractionAnimatables.forEach { animatable, duration ->
            // If the coroutine that launched an animation has been stopped, we must still
            // continue the animation to its conclusion. This can happen, for example, with
            // a LaunchedEffect(key) when the key changes.
            if (!animatable.isRunning) {
                coroutineScope.launch {
                    val currentPlayTime = (duration * animatable.value).roundToLong()
                    val remainderMillis = ((duration - currentPlayTime) / MillisToNanos).toInt()
                    animatable.animateTo(
                        1f,
                        animationSpec = tween(remainderMillis, 0, LinearEasing)
                    ) {
                        val initialNanos = (value * duration).roundToLong()
                        val currentNanos =
                            (animatedFraction.value * transition.totalDurationNanos).roundToLong()
                        transition.updateInitialValues(this, initialNanos, currentNanos)
                    }
                    transition.clearInitialAnimations(animatable)
                    initialFractionAnimatables -= animatable
                }
            }
        }
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
        var deferred: Deferred<Unit>? = null
        coroutineScope {
            if (targetState != this@SeekableTransitionState.targetState) {
                animateInitialState(this@coroutineScope)
                animatedFraction.snapTo(0f)
                transition.updateTarget(targetState)
                this@SeekableTransitionState.currentState = this@SeekableTransitionState.targetState
                this@SeekableTransitionState.targetState = targetState
            }
            if (animationSpec == null) {
                val animated = animatedFraction
                var relaunchAnimateToTargetState: ((Unit) -> Unit)? = null
                // totalDurationNanos will notify when the value _may have_ changed. We don't
                // want to change the animation unless the duration really changes.
                var previousDuration = -1L
                relaunchAnimateToTargetState = {
                    if (animated == animatedFraction && animatedFraction.value < 1f) {
                        observer.observeReads(
                            Unit,
                            relaunchAnimateToTargetState!!,
                            recalculateTotalDurationNanos
                        )
                        if (previousDuration != totalDurationNanos) {
                            previousDuration = totalDurationNanos
                            val remainderMillis = (totalDurationNanos *
                                (1f - animated.value) / MillisToNanos).fastRoundToInt()

                            val spec = if (remainderMillis == 0) {
                                animated.defaultSpringSpec
                            } else {
                                tween(remainderMillis, 0, LinearEasing)
                            }
                            deferred = async {
                                animateToTargetState(spec)
                            }
                        }
                    }
                }
                relaunchAnimateToTargetState(Unit)
            } else {
                animateToTargetState(animationSpec)
            }
        }
        if (deferred?.isCancelled == true) {
            throw CancellationException()
        }
    }

    /**
     * Animates from the current fraction to the [targetState] (fraction = 1).
     *
     * Upon completion of the animation, [currentState] will be changed to [targetState].
     */
    private suspend fun animateToTargetState(animationSpec: FiniteAnimationSpec<Float>) {
        val transition = transition ?: return
        if (currentState == targetState) {
            return
        }

        val animated = animatedFraction
        animated.animateTo(targetValue = 1f, animationSpec = animationSpec) {
            if (this === animatedFraction) {
                seekToFraction()
            } else if (this in initialFractionAnimatables) {
                val durationNanos = initialFractionAnimatables[this]
                val initialValueNanos = (durationNanos * value).roundToLong()
                val playTimeNanos =
                    (animatedFraction.value * transition.totalDurationNanos).roundToLong()
                transition.updateInitialValues(this, initialValueNanos, playTimeNanos)
            }
        }
        if (animated === animatedFraction) {
            currentState = targetState
        } else {
            transition.clearInitialAnimations(animated)
            initialFractionAnimatables -= animated
        }
    }

    override fun transitionConfigured(transition: Transition<S>) {
        checkPrecondition(this.transition == null || transition == this.transition) {
            "An instance of SeekableTransitionState has been used in different Transitions. " +
                "Previous instance: ${this.transition}, new instance: $transition"
        }
        if (this.transition == null) {
            observer.start()
        }
        this.transition = transition
    }

    override fun transitionRemoved() {
        if (this.transition != null) {
            observer.stop()
            this.transition = null
        }
    }

    private fun seekToFraction() {
        val transition = transition ?: return
        observer.observeReads(
            scope = Unit,
            onValueChangedForScope = reseekToFraction,
            block = recalculateTotalDurationNanos
        )
        val fraction = animatedFraction.value
        val playTimeNanos = (fraction * totalDurationNanos).roundToLong()
        transition.seekAnimations(playTimeNanos)
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
    if (transitionState !is SeekableTransitionState) {
        transition.animateTo(transitionState.targetState)
        DisposableEffect(transition) {
            onDispose {
                // Clean up on the way out, to ensure the observers are not stuck in an in-between
                // state.
                transition.onDisposed()
            }
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
class Transition<S> @PublishedApi internal constructor(
    private val transitionState: TransitionState<S>,
    val label: String? = null
) {
    internal constructor(
        initialState: S,
        label: String?
    ) : this(MutableTransitionState(initialState), label)

    @PublishedApi
    internal constructor(
        transitionState: MutableTransitionState<S>,
        label: String? = null
    ) : this(transitionState as TransitionState<S>, label)

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

    /**
     * Play time in nano-seconds. [playTimeNanos] is always non-negative. It starts from 0L at the
     * beginning of the transition and increment until all child animations have finished.
     * @suppress
     */
    @InternalAnimationApi
    var playTimeNanos by mutableLongStateOf(0L)
    private var startTimeNanos by mutableLongStateOf(AnimationConstants.UnspecifiedTime)

    // This gets calculated every time child is updated/added
    internal var updateChildrenNeeded: Boolean by mutableStateOf(true)

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
    /** @suppress */
    @InternalAnimationApi
    var isSeeking: Boolean by mutableStateOf(false)
        internal set
    internal var lastSeekedTimeNanos = 0L

    /**
     * Used internally to know when a [SeekableTransitionState] is animating initial values
     * after [SeekableTransitionState.animateTo] or [SeekableTransitionState.snapTo] has
     * redirected a transition prior to it completing. This is important for knowing when
     * child transitions must be maintained after a parent target state has changed, but
     * the child target state hasn't changed.
     */
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:Suppress("GetterSetterNames") // Don't care about Java name for this property
    @InternalAnimationApi
    @get:InternalAnimationApi
    val hasInitialValueAnimations: Boolean
        get() = _animations.fastAny { it.initialValueAnimatable != null } ||
            _transitions.fastAny { it.hasInitialValueAnimations }

    /**
     * Total duration of the [Transition], accounting for all the animations and child transitions
     * defined on the [Transition].
     *
     * Note: The total duration is subject to change as more animations/child transitions get added
     * to [Transition]. It's strongly recommended to query this *after* all the animations in the
     * [Transition] are set up.
     */
    val totalDurationNanos: Long by derivedStateOf {
        calculateTotalDurationNanos()
    }

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
        updateChildrenNeeded = false

        // Update play time
        playTimeNanos = frameTimeNanos - startTimeNanos
        var allFinished = true
        // Pulse new playtime
        _animations.fastForEach {
            if (!it.isFinished) {
                it.onPlayTimeChanged(playTimeNanos, durationScale)
            }
            // Check isFinished flag again after the animation pulse
            if (!it.isFinished) {
                allFinished = false
            }
        }
        _transitions.fastForEach {
            if (it.targetState != it.currentState) {
                it.onFrame(playTimeNanos, durationScale)
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
            // target != currentState adds LaunchedEffect into the tree in the same frame as
            // target change.
            if (targetState != currentState || isRunning || updateChildrenNeeded) {
                LaunchedEffect(this) {
                    while (true) {
                        val durationScale = coroutineContext.durationScale
                        withFrameNanos {
                            // This check is very important, as isSeeking may be changed off-band
                            // between the last check in composition and this callback which
                            // happens in the animation callback the next frame.
                            if (!isSeeking) {
                                onFrame(it / AnimationDebugDurationScale, durationScale)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Used by [SeekableTransitionState] to seek the current transition animation to
     * [playTimeNanos].
     */
    internal fun seekAnimations(playTimeNanos: Long) {
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
    internal fun setInitialAnimations(animatable: Animatable<Float, AnimationVector1D>) {
        _animations.fastForEach {
            it.setInitialValueAnimation(animatable)
        }
        _transitions.fastForEach {
            it.setInitialAnimations(animatable)
        }
    }

    /**
     * Clears the initial value animations that are governed by [animatable].
     */
    internal fun clearInitialAnimations(animatable: Animatable<Float, AnimationVector1D>) {
        _animations.fastForEach {
            it.clearInitialAnimation(animatable)
        }
        _transitions.fastForEach {
            it.clearInitialAnimations(animatable)
        }
    }

    /**
     * Changes the progress of the initial value governed by [animatable] at the progress
     * [initialAnimationNanos]. The current animation is governed by [playTimeNanos].
     */
    internal fun updateInitialValues(
        animatable: Animatable<Float, AnimationVector1D>,
        initialAnimationNanos: Long,
        playTimeNanos: Long
    ) {
        _animations.fastForEach {
            it.updateInitialValue(animatable, initialAnimationNanos, playTimeNanos)
        }
        _transitions.fastForEach {
            it.updateInitialValues(animatable, initialAnimationNanos, playTimeNanos)
        }
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

        /**
         * [AnimationSpec] that is used for current animation run. This can change when
         * [targetState] changes.
         */
        var animationSpec: FiniteAnimationSpec<T> by mutableStateOf(spring())
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

        internal var initialValueAnimatable: Animatable<Float, AnimationVector1D>?
            by mutableStateOf(null)
            private set
        private var initialValueAnimation: TargetBasedAnimation<T, V>? = null

        internal var isFinished: Boolean by mutableStateOf(true)
        private var offsetTimeNanos by mutableLongStateOf(0L)
        private var needsReset by mutableStateOf(false)

        /**
         * When the target state has changed, but the target value remains the same,
         * the initial value animation completely controls the animated value. When
         * this flag is `true`, the [animation] can be ignored and only the [initialValueAnimation]
         * is needed to determine the value. When this is `false`, if there is
         * an [initialValueAnimation], it is used only for adjusting the initial value
         * of [animation].
         */
        private var useOnlyInitialValue = false

        // Changed during animation, no concerns of rolling back
        override var value by mutableStateOf(initialValue)
            internal set
        private var velocityVector: V = initialVelocityVector
        internal val durationNanos
            get() = animation.durationNanos
        private var isSeeking = false

        internal fun onPlayTimeChanged(playTimeNanos: Long, durationScale: Float) {
            val playTime =
                if (durationScale > 0f) {
                    val scaledTime = (playTimeNanos - offsetTimeNanos) / durationScale
                    checkPrecondition(!scaledTime.isNaN()) {
                        "Duration scale adjusted time is NaN. Duration scale: $durationScale," +
                            "playTimeNanos: $playTimeNanos, offsetTimeNanos: $offsetTimeNanos"
                    }
                    scaledTime.toLong()
                } else {
                    animation.durationNanos
                }
            value = animation.getValueFromNanos(playTime)
            velocityVector = animation.getVelocityVectorFromNanos(playTime)
            if (animation.isFinishedFromNanos(playTime)) {
                isFinished = true
                offsetTimeNanos = 0
            }
        }

        internal fun seekTo(playTimeNanos: Long) {
            if (needsReset) {
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
         *
         * @param initialValueAnimationPlayTimeNanos The play time of the initial value animation
         * @param playTimeNanos The play time of the animation of the target
         */
        internal fun updateInitialValue(
            animatable: Animatable<Float, AnimationVector1D>,
            initialValueAnimationPlayTimeNanos: Long,
            playTimeNanos: Long
        ) {
            val anim = initialValueAnimation
            if (initialValueAnimatable !== animatable || anim == null) {
                return
            }
            val initialValue = anim.getValueFromNanos(initialValueAnimationPlayTimeNanos)
            if (useOnlyInitialValue) {
                animation.mutableTargetValue = initialValue
            }
            animation.mutableInitialValue = initialValue
            if (needsReset) {
                value = initialValue
            } else {
                seekTo(playTimeNanos)
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

        private fun updateAnimation(initialValue: T = value, isInterrupted: Boolean = false) {
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
                return
            }
            val spec = if (isInterrupted && !isSeeking) {
                // When interrupted, use the default spring, unless the spec is also a spring.
                if (animationSpec is SpringSpec<*>) animationSpec else interruptionSpec
            } else {
                animationSpec
            }
            animation = TargetBasedAnimation(
                spec,
                typeConverter,
                initialValue,
                targetValue,
                velocityVector
            )
            useOnlyInitialValue = false
            onChildAnimationUpdated()
        }

        internal fun resetAnimation() {
            needsReset = true
        }

        internal fun setInitialValueAnimation(
            animatable: Animatable<Float, AnimationVector1D>
        ) {
            if (animation.targetValue != animation.initialValue) {
                // Continue the animation from the current position to the target
                initialValueAnimation = animation
                initialValueAnimatable = animatable
            }
            animation = TargetBasedAnimation(
                interruptionSpec,
                typeConverter,
                value,
                value,
                velocityVector.newInstance() // 0 velocity
            )
            useOnlyInitialValue = true
        }

        internal fun clearInitialAnimation(
            animatable: Animatable<Float, AnimationVector1D>
        ) {
            if (animatable === initialValueAnimatable) {
                initialValueAnimatable = null
                initialValueAnimation = null
                useOnlyInitialValue = false
            }
        }

        override fun toString(): String {
            return "current value: $value, target: $targetValue, spec: $animationSpec"
        }

        // This gets called *during* composition
        @OptIn(InternalAnimationApi::class)
        internal fun updateTargetValue(targetValue: T, animationSpec: FiniteAnimationSpec<T>) {
            if (this.targetValue != targetValue || needsReset) {
                this.targetValue = targetValue
                this.animationSpec = animationSpec
                updateAnimation(isInterrupted = !isFinished)
                isFinished = false
                // This is needed because the target change could happen during a transition
                offsetTimeNanos = playTimeNanos
                needsReset = false
            }
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
     * @suppress
     */
    @InternalAnimationApi
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
 * @suppress
 */
@InternalAnimationApi
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
        Transition(MutableTransitionState(initialState), "${this.label} > $childLabel")
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
