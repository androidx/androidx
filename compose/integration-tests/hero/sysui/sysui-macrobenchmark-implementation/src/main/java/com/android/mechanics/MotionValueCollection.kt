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

package com.android.mechanics

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
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
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** The type of MotionValue created by the [MotionValueCollection]. */
sealed interface ManagedMotionValue : MotionValueState, DisposableHandle

/**
 * A collection of motion values that all share the same input and gesture context.
 *
 * All [ManagedMotionValue]s are run from the same [keepRunning], and share the same lifecycle.
 *
 * Input, gesture context and spec are updated all at once, at the beginning of the, during
 * [withFrameNanos].
 */
class MotionValueCollection(
    internal val input: () -> Float,
    internal val gestureContext: GestureContext,
    internal val stableThreshold: Float = MotionValue.StableThresholdEffect,
    val label: String? = null,
) {
    private val managedComputations = mutableStateSetOf<ManagedMotionComputation>()

    /**
     * Creates a new [ManagedMotionValue], whose output is controlled by [spec].
     *
     * The returned [ManagedMotionValue] must be disposed when not used anymore, while this
     * [MotionValueCollection] is kept active.
     */
    fun create(spec: () -> MotionSpec, label: String? = null): ManagedMotionValue {
        return ManagedMotionComputation(this, spec, label).also {
            if (isActive) {
                it.onActivate()
            }
            managedComputations.add(it)
        }
    }

    /**
     * Keeps the all created [ManagedMotionValue]'s animated output running.
     *
     * Clients must call [keepRunning], and keep the coroutine running while any of the created
     * [ManagedMotionValue] is in use. Cancel the coroutine if no values are being used anymore.
     *
     * Internally, this method does suspend, unless there are animations ongoing.
     */
    suspend fun keepRunning(): Nothing {
        withContext(CoroutineName("MotionValueCollection($label)")) {
            check(!isActive) { "MotionValueCollection($label) is already running" }
            isActive = true

            currentInput = input.invoke()
            currentGestureDragOffset = gestureContext.dragOffset
            currentDirection = gestureContext.direction

            managedComputations.forEach { it.onActivate() }

            try {
                isAnimating = true

                // indicates whether withFrameNanos is called continuously (as opposed to being
                // suspended for an undetermined amount of time in between withFrameNanos).
                // This is essential after `withFrameNanos` returned: if true at this point,
                // currentAnimationTimeNanos - lastFrameNanos is the duration of the last frame.
                var isAnimatingUninterrupted = false

                while (true) {
                    var scheduleNextFrame = false
                    withFrameNanos { frameTimeNanos ->
                        frameCount++

                        lastFrameTimeNanos = currentAnimationTimeNanos
                        lastInput = currentInput
                        lastDirection = currentDirection
                        lastGestureDragOffset = currentGestureDragOffset

                        currentAnimationTimeNanos = frameTimeNanos
                        currentInput = input.invoke()
                        currentDirection = gestureContext.direction
                        currentGestureDragOffset = gestureContext.dragOffset

                        if (
                            lastInput != currentInput ||
                                lastDirection != currentDirection ||
                                lastGestureDragOffset != currentGestureDragOffset
                        ) {
                            scheduleNextFrame = true
                        }
                        managedComputations.forEach {
                            if (it.onFrameStart(isAnimatingUninterrupted)) {
                                scheduleNextFrame = true
                            }
                        }
                    }

                    isAnimatingUninterrupted = scheduleNextFrame
                    if (scheduleNextFrame) {
                        continue
                    }

                    isAnimating = false
                    managedComputations.forEach { it.debugInspector?.isAnimating = false }
                    val activeComputations = managedComputations.toSet()

                    snapshotFlow {
                            val hasComputations =
                                activeComputations.isNotEmpty() || managedComputations.isNotEmpty()

                            val wakeup =
                                hasComputations &&
                                    (activeComputations != managedComputations ||
                                        activeComputations.any { it.wantWakeup() } ||
                                        input.invoke() != currentInput ||
                                        gestureContext.direction != currentDirection ||
                                        gestureContext.dragOffset != currentGestureDragOffset)
                            wakeup
                        }
                        .first { it }
                    isAnimating = true
                    managedComputations.forEach { it.debugInspector?.isAnimating = true }
                }
            } finally {
                isActive = false
                managedComputations.forEach { it.onDeactivate() }
            }
        }
    }

    // ---- Implementation - State shared with all ManagedMotionComputations  ----------------------
    // Note that all this state is updated exactly once per frame, during [withFrameNanos].
    internal var currentAnimationTimeNanos = -1L
        private set

    @VisibleForTesting
    var currentInput: Float = input.invoke()
        private set

    @VisibleForTesting
    var currentDirection: InputDirection = gestureContext.direction
        private set

    @VisibleForTesting
    var currentGestureDragOffset: Float = gestureContext.dragOffset
        private set

    internal var lastFrameTimeNanos = -1L
    internal var lastInput = currentInput
    internal var lastGestureDragOffset = currentGestureDragOffset
    internal var lastDirection = currentDirection

    // ---- Testing related state ------------------------------------------------------------------

    @VisibleForTesting
    var isActive = false
        private set

    @VisibleForTesting
    var isAnimating = false
        private set

    @VisibleForTesting
    var frameCount = 0
        private set

    @VisibleForTesting
    // Note - this is public so that its accessible by the mechanics:testing library
    val managedMotionValues: Set<ManagedMotionValue>
        get() = managedComputations

    internal fun onDispose(toDispose: ManagedMotionComputation) {
        managedComputations.remove(toDispose)
        toDispose.onDeactivate()
    }
}

internal class ManagedMotionComputation(
    private val owner: MotionValueCollection,
    private val specProvider: () -> MotionSpec,
    override val label: String?,
) : Computations(), ManagedMotionValue {

    override val stableThreshold: Float
        get() = owner.stableThreshold

    // ----  ManagedMotionValue --------------------------------------------------------------------

    override var output: Float by mutableFloatStateOf(Float.NaN)

    /**
     * [output] value, but without animations.
     *
     * This value always reports the target value, even before a animation is finished.
     *
     * While [isStable], [outputTarget] and [output] are the same value.
     */
    override var outputTarget: Float by mutableFloatStateOf(Float.NaN)

    /** Whether an animation is currently running. */
    override var isStable: Boolean by mutableStateOf(false)

    override var spec: MotionSpec = specProvider.invoke()
        private set

    override fun <T> get(key: SemanticKey<T>): T? {
        val segment = capturedComputedValues.segment
        return segment.spec.semanticState(key, segment.key)
    }

    override val segmentKey: SegmentKey
        get() = capturedComputedValues.segment.key

    override val floatValue: Float
        get() = output

    override fun dispose() {
        owner.onDispose(this)
    }

    override fun debugInspector(): DebugInspector {
        debugInspectorRefCount++
        if (debugInspectorRefCount == 1) {
            debugInspector =
                DebugInspector(
                    FrameData(
                        lastInput,
                        lastSegment.direction,
                        lastGestureDragOffset,
                        lastFrameTimeNanos,
                        lastSpringState,
                        lastSegment,
                        lastAnimation,
                        computedIsOutputFixed,
                    ),
                    owner.isActive,
                    owner.isAnimating,
                    ::onDisposeDebugInspector,
                )
        }

        return checkNotNull(debugInspector)
    }

    private var debugInspectorRefCount = 0

    private fun onDisposeDebugInspector() {
        debugInspectorRefCount--
        if (debugInspectorRefCount == 0) {
            debugInspector = null
        }
    }

    // ----  CurrentFrameInput ---------------------------------------------------------------------

    override val currentInput: Float
        get() = owner.currentInput

    override val currentDirection: InputDirection
        get() = owner.currentDirection

    override val currentGestureDragOffset: Float
        get() = owner.currentGestureDragOffset

    override val currentAnimationTimeNanos
        get() = owner.currentAnimationTimeNanos

    private var capturedComputedValues: ComputedValues = currentComputedValues
    private var capturedSpringState: SpringState = currentSpringState

    // ----  LastFrameState ---------------------------------------------------------------------

    private var lastComputedValues: ComputedValues = capturedComputedValues

    override val lastSegment: SegmentData
        get() = lastComputedValues.segment

    override val lastGuaranteeState: GuaranteeState
        get() = lastComputedValues.guarantee

    override val lastAnimation: DiscontinuityAnimation
        get() = lastComputedValues.animation

    override var lastSpringState: SpringState = SpringState.AtRest

    override var directMappedVelocity: Float = 0f

    override val lastFrameTimeNanos
        get() = owner.lastFrameTimeNanos

    override val lastInput
        get() = owner.lastInput

    override val lastGestureDragOffset
        get() = owner.lastGestureDragOffset

    override var lastHapticsTimeNanos: Long by mutableLongStateOf(-1L)

    // ---- Computations ---------------------------------------------------------------------------

    var debugInspector: DebugInspector? = null

    fun onActivate() {
        capturedComputedValues = currentComputedValues
        capturedSpringState = currentSpringState
        lastComputedValues = capturedComputedValues
        lastSpringState = capturedSpringState

        onFrameStart(isAnimatingUninterrupted = false)

        debugInspector?.isAnimating = true
        debugInspector?.isActive = true
    }

    fun onDeactivate() {
        debugInspector?.isAnimating = false
        debugInspector?.isActive = false
    }

    fun onFrameStart(isAnimatingUninterrupted: Boolean): Boolean {
        spec = specProvider.invoke()
        if (isSameSegmentAndAtRest) {
            outputTarget = lastSegment.mapping.map(currentInput)
            output = outputTarget
            isStable = true
        } else {
            lastComputedValues = capturedComputedValues
            lastSpringState = capturedSpringState

            capturedComputedValues = currentComputedValues
            capturedSpringState = currentSpringState

            outputTarget = capturedComputedValues.segment.mapping.map(currentInput)
            output = outputTarget + capturedSpringState.displacement
            isStable = capturedSpringState == SpringState.AtRest
        }

        directMappedVelocity =
            if (isAnimatingUninterrupted) {
                computeDirectMappedVelocity(currentAnimationTimeNanos - lastFrameTimeNanos)
            } else 0f

        debugInspector?.run {
            frame =
                FrameData(
                    currentInput,
                    currentDirection,
                    currentGestureDragOffset,
                    currentAnimationTimeNanos,
                    capturedSpringState,
                    capturedComputedValues.segment,
                    capturedComputedValues.animation,
                    computedIsOutputFixed,
                )
        }

        return if (isSameSegmentAndAtRest) {
            false
        } else {
            lastSpringState != capturedSpringState || lastComputedValues != capturedComputedValues
        }
    }

    fun wantWakeup(): Boolean {
        return specProvider.invoke() != capturedComputedValues.segment.spec
    }
}
