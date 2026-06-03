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

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.DeferredTransition
import androidx.compose.animation.core.DeferredTransitionState
import androidx.compose.animation.core.ExperimentalDeferredTransitionApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.VelocityTracker1D
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import kotlin.time.TimeSource

@VisibleForTesting internal var testTimeSource: (() -> Long)? = null

/**
 * An object that allows manual manipulation of the visual transformations (alpha, scale, offset,
 * etc.) of content during the deferred phase (initiated by [DeferredTransitionState.defer]) of a
 * [DeferredTransition] (e.g., for predictive back gestures).
 *
 * Manual transformations defined in this object are applied **on top of** the transition's initial
 * state.
 *
 * This object provides an [invoke] operator that accepts a [TransformScope] lambda. This lambda is
 * evaluated repeatedly to ensure that state reads (e.g., from gesture progress) are deferred to the
 * layout phase, preventing unnecessary composition churn while keeping Draw-phase operations
 * performant.
 *
 * Values set in this object are seamlessly handed off to the automatic transition animation when
 * the deferred phase ends.
 *
 * @param veilMatchParentSize Whether the veil should match the size of the parent.
 * @param offsetVelocityProvider The velocity of the offset change in pixels/sec. The
 *   [offsetVelocityProvider] lambda is evaluated exactly once when the deferred phase ends to
 *   ensure a seamless handoff to the automatic transition.
 * @param block A lambda that applies transformations to the provided [TransformScope]. This block
 *   executes dynamically to reflect state changes.
 */
@ExperimentalDeferredTransitionApi
public class MutableTransform(
    internal var veilMatchParentSize: Boolean = false,
    internal var offsetVelocityProvider: (() -> Offset)? = null,
    internal var block: (TransformScope.(fullSize: IntSize) -> Unit)? = null,
) {

    /**
     * Define the manual transformation to apply during the deferred phase.
     *
     * @param block A lambda that applies transformations to the provided [TransformScope]. This
     *   block executes dynamically to reflect state changes.
     */
    public operator fun invoke(block: TransformScope.(fullSize: IntSize) -> Unit) {
        this.block = block
    }

    internal fun clear() {
        block = null
        veilMatchParentSize = false
        offsetVelocityProvider = null
    }
}

/**
 * Scope for manually manipulating the visual transformation of content during the deferred phase of
 * a [DeferredTransition].
 */
@ExperimentalDeferredTransitionApi
public interface TransformScope {
    /** Manually controls the alpha value during the deferred phase. */
    public var alpha: Float
    /** Manually controls the scale value during the deferred phase. */
    public var scale: Float
    /** Manually controls the pivot point for the scale transformation. */
    public var transformOrigin: TransformOrigin
    /** Manually controls the offset value during the deferred phase. */
    public var offset: IntOffset
    /** Manually controls the veil color during the deferred phase. */
    public var veil: Color
}

@OptIn(ExperimentalDeferredTransitionApi::class)
internal class TransformScopeImpl : TransformScope {
    var isAlphaMutated by mutableStateOf(false)
    private val _alpha = mutableFloatStateOf(1f)
    override var alpha: Float
        get() = _alpha.floatValue
        set(value) {
            _alpha.floatValue = value
            isAlphaMutated = true
        }

    var isScaleMutated by mutableStateOf(false)
    private val _scale = mutableFloatStateOf(1f)
    override var scale: Float
        get() = _scale.floatValue
        set(value) {
            _scale.floatValue = value
            isScaleMutated = true
        }

    var isOffsetMutated = false
    override var offset: IntOffset = IntOffset.Zero
        set(value) {
            field = value
            isOffsetMutated = true
        }

    var isTransformOriginMutated by mutableStateOf(false)
    private val _transformOrigin = mutableStateOf(TransformOrigin.Center)
    override var transformOrigin: TransformOrigin
        get() = _transformOrigin.value
        set(value) {
            _transformOrigin.value = value
            isTransformOriginMutated = true
        }

    var isVeilMutated by mutableStateOf(false)
    private val _veil = mutableStateOf(Color.Transparent)
    override var veil: Color
        get() = _veil.value
        set(value) {
            _veil.value = value
            isVeilMutated = true
        }

    fun reset() {
        isAlphaMutated = false
        isScaleMutated = false
        isOffsetMutated = false
        isTransformOriginMutated = false
        isVeilMutated = false
    }
}

/**
 * [SharedMutableTransformState] object that's shared between EnterExitTransition and shared
 * elements
 */
@OptIn(ExperimentalDeferredTransitionApi::class)
internal class SharedMutableTransformState {
    private val _isMutating = mutableStateOf(false)
    var isMutating: Boolean
        get() = _isMutating.value
        set(value) {
            if (_isMutating.value && !value) {
                isHandoffActive = true
            } else if (value) {
                isHandoffActive = false
            }
            _isMutating.value = value
        }

    var isHandoffActive by mutableStateOf(false)
        private set

    private var lastMutableData: MutableTransform? = null

    var mutableData: MutableTransform? = null
        set(value) {
            if (value != null) {
                lastMutableData = value
            }
            field = value
        }

    internal val transformScope = TransformScopeImpl()

    private val timeSource = TimeSource.Monotonic
    private val startTime = timeSource.markNow()
    private val currentMillis: Long
        get() = testTimeSource?.invoke() ?: startTime.elapsedNow().inWholeMilliseconds

    var lastVeil: Color = Color.Transparent
    var lastAlpha: Float = 1f
    var lastScale: Float = 1f
    var lastTransformOrigin: TransformOrigin = TransformOrigin.Center
    var lastSlide: IntOffset = IntOffset.Zero

    val veilRequiresAnimation: Boolean
        get() =
            (mutableData?.block != null && transformScope.isVeilMutated) ||
                lastVeil != Color.Transparent

    val alphaRequiresAnimation: Boolean
        get() = (mutableData?.block != null && transformScope.isAlphaMutated) || lastAlpha != 1f

    val scaleRequiresAnimation: Boolean
        get() = (mutableData?.block != null && transformScope.isScaleMutated) || lastScale != 1f

    val transformOriginRequiresAnimation: Boolean
        get() =
            (mutableData?.block != null && transformScope.isTransformOriginMutated) ||
                lastTransformOrigin != TransformOrigin.Center

    val slideRequiresAnimation: Boolean
        get() =
            (mutableData?.block != null && transformScope.isOffsetMutated) ||
                lastSlide != IntOffset.Zero

    val veilHandoffValue: Color?
        get() = if (isHandoffActive) lastVeil else null

    val alphaHandoffValue: Float?
        get() = if (isHandoffActive) lastAlpha else null

    val scaleHandoffValue: Float?
        get() = if (isHandoffActive) lastScale else null

    val transformOriginHandoffValue: TransformOrigin?
        get() = if (isHandoffActive) lastTransformOrigin else null

    val slideHandoffValue: IntOffset?
        get() = if (isHandoffActive) lastSlide else null

    private var scaleVelocityTracker: VelocityTracker1D? = null
    private var offsetVelocityTracker: VelocityTracker? = null

    val scaleHandoffVelocity: AnimationVector1D?
        get() =
            if (isHandoffActive) {
                val vel = scaleVelocityTracker?.calculateVelocity()?.takeUnless { it.isNaN() } ?: 0f
                AnimationVector1D(vel)
            } else null

    val slideHandoffVelocity: AnimationVector2D?
        get() =
            if (isHandoffActive) {
                val v = lastMutableData?.offsetVelocityProvider?.invoke()
                if (v != null && v.isSpecified) {
                    AnimationVector2D(v.x, v.y)
                } else {
                    val vel = offsetVelocityTracker?.calculateVelocity() ?: Velocity.Zero
                    AnimationVector2D(
                        vel.x.takeUnless { it.isNaN() } ?: 0f,
                        vel.y.takeUnless { it.isNaN() } ?: 0f,
                    )
                }
            } else null

    private fun trackScaleVelocity(value: Float) {
        if (scaleVelocityTracker == null) {
            scaleVelocityTracker = VelocityTracker1D(isDataDifferential = false)
        }
        scaleVelocityTracker?.addDataPoint(currentMillis, value)
    }

    private fun trackSlideVelocity(value: IntOffset) {
        if (offsetVelocityTracker == null) {
            offsetVelocityTracker = VelocityTracker()
        }
        offsetVelocityTracker?.addPosition(
            currentMillis,
            Offset(value.x.toFloat(), value.y.toFloat()),
        )
    }

    fun evaluateTransformBlock(fullSize: IntSize) {
        if (isMutating) {
            mutableData?.block?.invoke(transformScope, fullSize)
        }
    }

    fun combinedAlpha(transitionValue: Float): Float {
        val isMutated = isMutating && transformScope.isAlphaMutated
        val combined = transitionValue * (if (isMutated) transformScope.alpha else 1f)

        if (isMutating) {
            lastAlpha = combined
        }
        return combined
    }

    fun combinedScale(transitionValue: Float): Float {
        val isMutated = isMutating && transformScope.isScaleMutated
        val combined = transitionValue * (if (isMutated) transformScope.scale else 1f)

        if (isMutating) {
            lastScale = combined
            if (isMutated) trackScaleVelocity(combined)
        }
        return combined
    }

    fun combinedTransformOrigin(transitionValue: TransformOrigin): TransformOrigin {
        val isMutated = isMutating && transformScope.isTransformOriginMutated
        val combined = if (isMutated) transformScope.transformOrigin else transitionValue

        if (isMutating) lastTransformOrigin = combined
        return combined
    }

    fun combinedSlide(transitionValue: IntOffset, fullSize: IntSize): IntOffset {
        evaluateTransformBlock(fullSize)
        val isMutated = isMutating && transformScope.isOffsetMutated
        val combined = transitionValue + (if (isMutated) transformScope.offset else IntOffset.Zero)

        if (isMutating) {
            lastSlide = combined
            if (isMutated) trackSlideVelocity(combined)
        }
        return combined
    }

    fun combinedVeil(transitionValue: Color): Color {
        val isMutated = isMutating && transformScope.isVeilMutated
        val combined = if (isMutated) transformScope.veil else transitionValue

        if (isMutating) lastVeil = combined
        return combined
    }

    fun clear() {
        isHandoffActive = false
        isMutating = false
        transformScope.reset()
        lastVeil = Color.Transparent
        lastAlpha = 1f
        lastScale = 1f
        scaleVelocityTracker?.resetTracking()
        lastTransformOrigin = TransformOrigin.Center
        lastSlide = IntOffset.Zero
        offsetVelocityTracker?.resetTracking()
        lastMutableData = null
        mutableData = null
    }
}
