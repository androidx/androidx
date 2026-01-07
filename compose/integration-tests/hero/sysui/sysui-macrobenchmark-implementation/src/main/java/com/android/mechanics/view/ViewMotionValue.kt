/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.mechanics.view

import android.animation.ValueAnimator
import androidx.compose.ui.util.fastForEach
import com.android.mechanics.MotionValue.Companion.StableThresholdEffect
import com.android.mechanics.debug.DebugInspector
import com.android.mechanics.debug.FrameData
import com.android.mechanics.impl.Computations
import com.android.mechanics.impl.DiscontinuityAnimation
import com.android.mechanics.impl.GuaranteeState
import com.android.mechanics.spec.InputDirection
import com.android.mechanics.spec.MotionSpec
import com.android.mechanics.spec.SegmentData
import com.android.mechanics.spec.SegmentKey
import com.android.mechanics.spec.SemanticKey
import com.android.mechanics.spring.SpringState
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.DisposableHandle

/** Observe MotionValue output changes. */
fun interface ViewMotionValueListener {
    /** Invoked whenever the ViewMotionValue computed a new output. */
    fun onMotionValueUpdated(motionValue: ViewMotionValue)
}

/**
 * [MotionValue] implementation for View-based UIs.
 *
 * See the documentation of [MotionValue].
 */
class ViewMotionValue
@JvmOverloads
constructor(
    initialInput: Float,
    gestureContext: ViewGestureContext,
    initialSpec: MotionSpec = MotionSpec.Identity,
    label: String? = null,
    stableThreshold: Float = StableThresholdEffect,
) : DisposableHandle {

    private val impl =
        ImperativeComputations(
            this,
            initialInput,
            gestureContext,
            initialSpec,
            stableThreshold,
            label,
        )

    var input: Float by impl::currentInput

    var spec: MotionSpec by impl::spec

    /** Animated [output] value. */
    val output: Float by impl::computedOutput

    /**
     * [output] value, but without animations.
     *
     * This value always reports the target value, even before a animation is finished.
     *
     * While [isStable], [outputTarget] and [output] are the same value.
     */
    val outputTarget: Float by impl::computedOutputTarget

    /** Whether an animation is currently running. */
    val isStable: Boolean by impl::computedIsStable

    /**
     * The current value for the [SemanticKey].
     *
     * `null` if not defined in the spec.
     */
    operator fun <T> get(key: SemanticKey<T>): T? {
        return impl.computedSemanticState(key)
    }

    /** The current segment used to compute the output. */
    val segmentKey: SegmentKey
        get() = impl.currentComputedValues.segment.key

    val label: String? by impl::label

    fun addUpdateCallback(listener: ViewMotionValueListener) {
        check(impl.isActive)
        impl.listeners.add(listener)
    }

    fun removeUpdateCallback(listener: ViewMotionValueListener) {
        impl.listeners.remove(listener)
    }

    override fun dispose() {
        impl.dispose()
    }

    companion object {
        internal const val TAG = "ViewMotionValue"
    }

    private var debugInspectorRefCount = AtomicInteger(0)

    private fun onDisposeDebugInspector() {
        if (debugInspectorRefCount.decrementAndGet() == 0) {
            impl.debugInspector = null
        }
    }

    /**
     * Provides access to internal state for debug tooling and tests.
     *
     * The returned [DebugInspector] must be [DebugInspector.dispose]d when no longer needed.
     */
    fun debugInspector(): DebugInspector {
        if (debugInspectorRefCount.getAndIncrement() == 0) {
            impl.debugInspector =
                DebugInspector(
                    FrameData(
                        impl.lastInput,
                        impl.lastSegment.direction,
                        impl.lastGestureDragOffset,
                        impl.lastFrameTimeNanos,
                        impl.lastSpringState,
                        impl.lastSegment,
                        impl.lastAnimation,
                        impl.computedIsOutputFixed,
                    ),
                    impl.isActive,
                    impl.animationFrameDriver.isRunning,
                    ::onDisposeDebugInspector,
                )
        }

        return checkNotNull(impl.debugInspector)
    }
}

private class ImperativeComputations(
    private val motionValue: ViewMotionValue,
    initialInput: Float,
    val gestureContext: ViewGestureContext,
    initialSpec: MotionSpec,
    override val stableThreshold: Float,
    override val label: String?,
) : Computations(), GestureContextUpdateListener {

    init {
        gestureContext.addUpdateCallback(this)
    }

    override fun onGestureContextUpdated() {
        ensureFrameRequested()
    }

    // ----  CurrentFrameInput ---------------------------------------------------------------------

    override var spec: MotionSpec = initialSpec
        set(value) {
            if (field != value) {
                field = value
                ensureFrameRequested()
            }
        }

    override var currentInput: Float = initialInput
        set(value) {
            if (field != value) {
                field = value
                ensureFrameRequested()
            }
        }

    override val currentDirection: InputDirection
        get() = gestureContext.direction

    override val currentGestureDragOffset: Float
        get() = gestureContext.dragOffset

    override var currentAnimationTimeNanos: Long = -1L

    // ----  LastFrameState ---------------------------------------------------------------------

    override var lastSegment: SegmentData = spec.segmentAtInput(currentInput, currentDirection)
    override var lastGuaranteeState: GuaranteeState = GuaranteeState.Inactive
    override var lastAnimation: DiscontinuityAnimation = DiscontinuityAnimation.None
    override var lastSpringState: SpringState = lastAnimation.springStartState
    override var lastFrameTimeNanos: Long = -1L
    override var lastInput: Float = currentInput
    override var lastGestureDragOffset: Float = currentGestureDragOffset
    override var directMappedVelocity: Float = 0f
    override var lastHapticsTimeNanos: Long = -1L
    var lastDirection: InputDirection = currentDirection

    // ---- Lifecycle ------------------------------------------------------------------------------

    // HACK: Use a ValueAnimator to listen to animation frames without using Choreographer directly.
    // This is done solely for testability - because the AnimationHandler is not usable directly[1],
    // this resumes/pauses a - for all practical purposes - infinite animation.
    //
    // [1] the android one is hidden API, the androidx one is package private, and the
    // dynamicanimation one is not controllable from tests).
    val animationFrameDriver =
        ValueAnimator().apply {
            setFloatValues(Float.MIN_VALUE, Float.MAX_VALUE)
            duration = Long.MAX_VALUE
            repeatMode = ValueAnimator.RESTART
            repeatCount = ValueAnimator.INFINITE
            start()
            addUpdateListener {
                val isAnimationFinished = updateOutputValue(currentPlayTime)
                debugInspector?.isAnimating = !isAnimationFinished
                if (isAnimationFinished) {
                    pause()
                }
            }
        }

    fun ensureFrameRequested() {
        if (animationFrameDriver.isPaused) {
            animationFrameDriver.resume()
        }
    }

    fun pauseFrameRequests() {
        if (animationFrameDriver.isRunning) {
            animationFrameDriver.pause()
        }
    }

    /** `true` until disposed with [MotionValue.dispose]. */
    var isActive = true
        set(value) {
            field = value
            debugInspector?.isActive = value
        }

    var debugInspector: DebugInspector? = null

    val listeners = mutableListOf<ViewMotionValueListener>()

    fun dispose() {
        check(isActive) { "ViewMotionValue[$label] is already disposed" }
        pauseFrameRequests()
        animationFrameDriver.end()
        isActive = false
        listeners.clear()
    }

    // indicates whether doAnimationFrame is called continuously (as opposed to being
    // suspended for an undetermined amount of time in between frames).
    var isAnimatingUninterrupted = false

    fun updateOutputValue(frameTimeMillis: Long): Boolean {
        check(isActive) { "ViewMotionValue($label) is already disposed." }

        currentAnimationTimeNanos = frameTimeMillis * 1_000_000L

        // Read currentComputedValues only once and update it, if necessary
        val currentValues = currentComputedValues

        debugInspector?.run {
            frame =
                FrameData(
                    currentInput,
                    currentDirection,
                    currentGestureDragOffset,
                    currentAnimationTimeNanos,
                    currentSpringState,
                    currentValues.segment,
                    currentValues.animation,
                    computedIsOutputFixed,
                )
        }

        if (currentValues.segment.spec == MotionSpec.InitiallyUndefined) return true

        listeners.fastForEach { it.onMotionValueUpdated(motionValue) }

        // Prepare last* state
        if (isAnimatingUninterrupted) {
            directMappedVelocity =
                computeDirectMappedVelocity(currentAnimationTimeNanos - lastFrameTimeNanos)
        } else {
            directMappedVelocity = 0f
        }

        var isAnimationFinished = computedIsStable
        if (lastSegment != currentValues.segment) {
            lastSegment = currentValues.segment
            isAnimationFinished = false
        }

        if (lastGuaranteeState != currentValues.guarantee) {
            lastGuaranteeState = currentValues.guarantee
            isAnimationFinished = false
        }

        if (lastAnimation != currentValues.animation) {
            lastAnimation = currentValues.animation
            isAnimationFinished = false
        }

        if (lastSpringState != currentSpringState) {
            lastSpringState = currentSpringState
            isAnimationFinished = false
        }

        if (lastInput != currentInput) {
            lastInput = currentInput
            isAnimationFinished = false
        }

        if (lastGestureDragOffset != currentGestureDragOffset) {
            lastGestureDragOffset = currentGestureDragOffset
            isAnimationFinished = false
        }

        if (lastDirection != currentDirection) {
            lastDirection = currentDirection
            isAnimationFinished = false
        }

        lastFrameTimeNanos = currentAnimationTimeNanos
        isAnimatingUninterrupted = !isAnimationFinished

        return isAnimationFinished
    }
}
