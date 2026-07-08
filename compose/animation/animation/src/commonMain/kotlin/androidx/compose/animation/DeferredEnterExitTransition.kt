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

@file:OptIn(ExperimentalDeferredTransitionApi::class)

package androidx.compose.animation

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.DeferredTransition
import androidx.compose.animation.core.DeferredTransitionState
import androidx.compose.animation.core.ExperimentalDeferredTransitionApi
import androidx.compose.animation.core.VectorConverter
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.modifier.modifierLocalOf
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import kotlin.time.TimeSource
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@VisibleForTesting internal var testTimeSource: (() -> Long)? = null

/**
 * An object that allows manual manipulation of the visual transformations (alpha, scale, offset,
 * etc.) of content during the deferred phase (initiated by [DeferredTransitionState.defer]) of a
 * [DeferredTransition] (e.g., for predictive back gestures).
 *
 * During the deferred phase, the transition's state is held at its initial value. Properties that
 * are not manually set in the [update] block default to the transition's initial value.
 *
 * Properties in [TransformScope] are set directly and reflect the manual value for the current
 * frame. They do not automatically animate between values; instead, they should be updated
 * continuously (e.g., in response to gesture progress) to create a smooth manual animation.
 *
 * The [update] lambda is evaluated repeatedly to ensure that state reads (e.g., from gesture
 * progress) are deferred to the layout phase, preventing unnecessary composition churn while
 * keeping Draw-phase operations performant.
 *
 * Values set in this object are handed off to the automatic transition animation when the deferred
 * phase ends.
 *
 * @param veilMatchParentSize Whether the veil should match the size of the parent.
 * @param offsetVelocityProvider The velocity of the offset change in pixels/sec. The
 *   [offsetVelocityProvider] lambda is evaluated exactly once when the deferred phase ends to
 *   ensure a seamless handoff to the automatic transition. If `null`, the system will automatically
 *   calculate the velocity based on [TransformScope.offset] changes during the deferred phase.
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
    public fun update(block: TransformScope.(fullSize: IntSize) -> Unit) {
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

    /**
     * Manually controls the veil color during the deferred phase.
     *
     * A veil is a color overlay (similar to a scrim) that is drawn on top of the content to
     * partially or fully obscure it. This is typically used to visually signal that the content is
     * in a background or non-interactive state during a transition.
     *
     * @see unveilIn
     * @see veilOut
     */
    public var veil: Color
}

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
    private var _offset = IntOffset.Zero
    override var offset: IntOffset
        get() = _offset
        set(value) {
            _offset = value
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
        _alpha.floatValue = 1f
        isAlphaMutated = false
        _scale.floatValue = 1f
        isScaleMutated = false
        _offset = IntOffset.Zero
        isOffsetMutated = false
        _transformOrigin.value = TransformOrigin.Center
        isTransformOriginMutated = false
        _veil.value = Color.Transparent
        isVeilMutated = false
    }
}

/** Shares the [SharedMutableTransformState] with nested [SharedElement]s. */
internal val ModifierLocalSharedMutableTransformState =
    modifierLocalOf<SharedMutableTransformState?> { null }

/** Represents the distinct phases of a deferred enter/exit transition. */
internal enum class MutationPhase {
    /** Indicates no active gesture mutation or handoff is running. */
    Idle,

    /**
     * Indicates active gesture mutation while waiting for the catch-up animation to start.
     *
     * This state is transient and typically lasts for only a single frame at the beginning of a new
     * deferred phase that interrupts a running transition.
     */
    MutatingPendingCatchUp,

    /**
     * Indicates active gesture mutation running a catch-up animation after interrupting a
     * transition.
     *
     * Catches up the transition values to manual updates using a spring animation. Any new manual
     * updates are applied simultaneously on top of the running catch-up animation.
     */
    MutatingCatchingUp,

    /**
     * Indicates active gesture mutation starting from a settled state, i.e. no catchup involved.
     */
    Mutating,

    /**
     * Indicates active transition handoff after gesture mutation ends.
     *
     * This state persists until the transition completes (settles) or is interrupted.
     */
    Handoff,
}

/**
 * [SharedMutableTransformState] object that's shared between EnterExitTransition and shared
 * elements
 */
internal class SharedMutableTransformState {
    internal var mutationPhase by mutableStateOf(MutationPhase.Idle)

    val isMutating: Boolean
        get() =
            mutationPhase == MutationPhase.MutatingPendingCatchUp ||
                mutationPhase == MutationPhase.MutatingCatchingUp ||
                mutationPhase == MutationPhase.Mutating

    val isHandoffActive: Boolean
        get() = mutationPhase == MutationPhase.Handoff

    fun updateMutationState(isMutating: Boolean, isSettled: Boolean) {
        val currentPhase = mutationPhase
        if (isMutating) {
            if (currentPhase == MutationPhase.Idle || currentPhase == MutationPhase.Handoff) {
                transformScope.reset()
                mutationPhase =
                    if (isSettled) {
                        MutationPhase.Mutating
                    } else {
                        MutationPhase.MutatingPendingCatchUp
                    }
                scaleHandoffVelocity = null
                slideHandoffVelocity = null
            }
        } else {
            if (
                currentPhase == MutationPhase.MutatingPendingCatchUp ||
                    currentPhase == MutationPhase.MutatingCatchingUp ||
                    currentPhase == MutationPhase.Mutating
            ) {
                mutationPhase = MutationPhase.Handoff
                calculateHandoffVelocities()
            }
        }
    }

    var lastMutableData: MutableTransform? = null

    var mutableData: MutableTransform? = null
        set(value) {
            if (value != null) {
                lastMutableData = value
            }
            field = value
        }

    internal val transformScope = TransformScopeImpl()

    internal val activeScale: Float
        get() = if (transformScope.isScaleMutated) transformScope.scale else 1f

    internal val activeOffset: IntOffset
        get() = if (transformScope.isOffsetMutated) transformScope.offset else IntOffset.Zero

    internal val activeTransformOrigin: TransformOrigin
        get() =
            if (transformScope.isTransformOriginMutated) transformScope.transformOrigin
            else TransformOrigin.Center

    private val timeSource = TimeSource.Monotonic
    private val startTime = timeSource.markNow()
    private val currentMillis: Long
        get() = testTimeSource?.invoke() ?: startTime.elapsedNow().inWholeMilliseconds

    var parentLayoutCoordinates: LayoutCoordinates? = null
        internal set

    var lastVeil: Color = Color.Transparent
    var lastAlpha: Float = 1f
    var lastScale: Float = 1f
    var lastTransformOrigin: TransformOrigin = TransformOrigin.Center
    var lastSlide: IntOffset = IntOffset.Zero

    var lastManualScale: Float = 1f
    var lastManualSlide: IntOffset = IntOffset.Zero

    var activeTransitionAlpha = 1f
    var activeTransitionScale = 1f
    var activeTransitionSlide = IntOffset.Zero
    var activeTransitionVeil = Color.Transparent
    var activeTransitionTransformOrigin = TransformOrigin.Center

    var initialManualAlpha = 1f
    var initialManualScale = 1f
    var initialManualSlide = IntOffset.Zero
    var initialManualVeil = Color.Transparent
    var initialManualTransformOrigin = TransformOrigin.Center

    val catchUpAlpha by lazy(LazyThreadSafetyMode.NONE) { Animatable(1f) }
    val catchUpScale by lazy(LazyThreadSafetyMode.NONE) { Animatable(1f) }
    val catchUpSlide by
        lazy(LazyThreadSafetyMode.NONE) { Animatable(IntOffset.Zero, IntOffset.VectorConverter) }
    val catchUpVeil by
        lazy(LazyThreadSafetyMode.NONE) {
            Animatable(Color.Transparent, Color.VectorConverter(ColorSpaces.Srgb))
        }
    val catchUpTransformOrigin by
        lazy(LazyThreadSafetyMode.NONE) {
            Animatable(TransformOrigin.Center, TransformOriginVectorConverter)
        }

    suspend fun startCatchUp() {
        val initialAlpha = activeTransitionAlpha
        val initialScale = activeTransitionScale
        val initialSlide = activeTransitionSlide
        val initialVeil = activeTransitionVeil
        val initialTransformOrigin = activeTransitionTransformOrigin

        initialManualAlpha = if (transformScope.isAlphaMutated) transformScope.alpha else 1f
        initialManualScale = if (transformScope.isScaleMutated) transformScope.scale else 1f
        initialManualSlide =
            if (transformScope.isOffsetMutated) transformScope.offset else IntOffset.Zero
        initialManualVeil =
            if (transformScope.isVeilMutated) transformScope.veil else Color.Transparent
        initialManualTransformOrigin =
            if (transformScope.isTransformOriginMutated) transformScope.transformOrigin
            else TransformOrigin.Center

        catchUpAlpha.snapTo(initialAlpha)
        catchUpScale.snapTo(initialScale)
        catchUpSlide.snapTo(initialSlide)
        catchUpVeil.snapTo(initialVeil)
        catchUpTransformOrigin.snapTo(initialTransformOrigin)
        if (mutationPhase == MutationPhase.MutatingPendingCatchUp) {
            mutationPhase = MutationPhase.MutatingCatchingUp

            coroutineScope {
                if (transformScope.isAlphaMutated)
                    launch { catchUpAlpha.animateTo(initialManualAlpha) }
                if (transformScope.isScaleMutated)
                    launch { catchUpScale.animateTo(initialManualScale) }
                if (transformScope.isOffsetMutated)
                    launch { catchUpSlide.animateTo(initialManualSlide) }
                if (transformScope.isVeilMutated)
                    launch { catchUpVeil.animateTo(initialManualVeil) }
                if (transformScope.isTransformOriginMutated)
                    launch { catchUpTransformOrigin.animateTo(initialManualTransformOrigin) }
            }

            if (mutationPhase == MutationPhase.MutatingCatchingUp) {
                mutationPhase = MutationPhase.Mutating
            }
        }
    }

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

    private var scaleVelocityTracker: VelocityTracker? = null
    private var offsetVelocityTracker: VelocityTracker? = null

    var scaleHandoffVelocity: AnimationVector1D? = null
        private set

    var slideHandoffVelocity: AnimationVector2D? = null
        private set

    private fun calculateHandoffVelocities() {
        val scaleVel = scaleVelocityTracker?.calculateVelocity()?.x?.takeUnless { it.isNaN() } ?: 0f
        scaleHandoffVelocity = AnimationVector1D(scaleVel)

        val v = lastMutableData?.offsetVelocityProvider?.invoke()
        slideHandoffVelocity =
            if (v != null && v.isSpecified) {
                AnimationVector2D(v.x, v.y)
            } else {
                val vel = offsetVelocityTracker?.calculateVelocity() ?: Velocity.Zero
                AnimationVector2D(
                    vel.x.takeUnless { it.isNaN() } ?: 0f,
                    vel.y.takeUnless { it.isNaN() } ?: 0f,
                )
            }
    }

    val slideHandoffOffset: (IntSize) -> IntOffset = { lastSlide }

    private fun trackScaleVelocity(value: Float) {
        if (scaleVelocityTracker == null) {
            // The 2D VelocityTracker is used here because its Lsq2/Framework implementations better
            // smooth out the phase jitter introduced by using TimeSource.Monotonic instead of vsync
            // times. VelocityTracker1D uses an Impulse strategy which is very sensitive to this
            // jitter.
            scaleVelocityTracker = VelocityTracker()
        }
        scaleVelocityTracker?.addPosition(currentMillis, Offset(value, 0f))
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
        activeTransitionAlpha = transitionValue

        val isMutated = isMutating && transformScope.isAlphaMutated
        val combined =
            when {
                isMutated &&
                    mutationPhase == MutationPhase.MutatingCatchingUp &&
                    catchUpAlpha.isRunning ->
                    catchUpAlpha.value + (transformScope.alpha - initialManualAlpha)
                isMutated &&
                    (mutationPhase == MutationPhase.MutatingCatchingUp ||
                        mutationPhase == MutationPhase.Mutating) -> transformScope.alpha
                else -> transitionValue
            }

        if (isMutating) {
            lastAlpha = combined
        }
        return combined
    }

    fun combinedScale(transitionValue: Float): Float {
        activeTransitionScale = transitionValue

        val isMutated = isMutating && transformScope.isScaleMutated
        val combined =
            when {
                isMutated &&
                    mutationPhase == MutationPhase.MutatingCatchingUp &&
                    catchUpScale.isRunning ->
                    catchUpScale.value + (transformScope.scale - initialManualScale)
                isMutated &&
                    (mutationPhase == MutationPhase.MutatingCatchingUp ||
                        mutationPhase == MutationPhase.Mutating) -> transformScope.scale
                else -> transitionValue
            }

        if (isMutating) {
            lastScale = combined
            lastManualScale = if (isMutated) transformScope.scale else 1f
            if (isMutated) trackScaleVelocity(combined)
        }
        return combined
    }

    fun combinedTransformOrigin(transitionValue: TransformOrigin): TransformOrigin {
        activeTransitionTransformOrigin = transitionValue

        val isMutated = isMutating && transformScope.isTransformOriginMutated
        val combined =
            when {
                isMutated &&
                    mutationPhase == MutationPhase.MutatingCatchingUp &&
                    catchUpTransformOrigin.isRunning -> catchUpTransformOrigin.value
                isMutated &&
                    (mutationPhase == MutationPhase.MutatingCatchingUp ||
                        mutationPhase == MutationPhase.Mutating) -> transformScope.transformOrigin
                else -> transitionValue
            }

        if (isMutating) {
            lastTransformOrigin = combined
        }
        return combined
    }

    fun combinedSlide(transitionValue: IntOffset, fullSize: IntSize): IntOffset {
        evaluateTransformBlock(fullSize)
        activeTransitionSlide = transitionValue
        val isMutated = isMutating && transformScope.isOffsetMutated
        val combined =
            when {
                isMutated &&
                    mutationPhase == MutationPhase.MutatingCatchingUp &&
                    catchUpSlide.isRunning ->
                    catchUpSlide.value + (transformScope.offset - initialManualSlide)
                isMutated &&
                    (mutationPhase == MutationPhase.MutatingCatchingUp ||
                        mutationPhase == MutationPhase.Mutating) -> transformScope.offset
                else -> transitionValue
            }

        if (isMutating) {
            lastSlide = combined
            lastManualSlide = if (isMutated) transformScope.offset else IntOffset.Zero
            if (isMutated) trackSlideVelocity(combined)
        }
        return combined
    }

    fun combinedVeil(transitionValue: Color): Color {
        activeTransitionVeil = transitionValue

        val isMutated = isMutating && transformScope.isVeilMutated
        val combined =
            when {
                isMutated &&
                    mutationPhase == MutationPhase.MutatingCatchingUp &&
                    catchUpVeil.isRunning -> catchUpVeil.value
                isMutated &&
                    (mutationPhase == MutationPhase.MutatingCatchingUp ||
                        mutationPhase == MutationPhase.Mutating) -> transformScope.veil
                else -> transitionValue
            }

        if (isMutating) {
            lastVeil = combined
        }
        return combined
    }

    fun clear() {
        mutationPhase = MutationPhase.Idle
        transformScope.reset()
        lastVeil = Color.Transparent
        lastAlpha = 1f
        lastScale = 1f
        scaleVelocityTracker?.resetTracking()
        lastTransformOrigin = TransformOrigin.Center
        lastSlide = IntOffset.Zero
        lastManualScale = 1f
        lastManualSlide = IntOffset.Zero
        offsetVelocityTracker?.resetTracking()
        scaleHandoffVelocity = null
        slideHandoffVelocity = null
        lastMutableData = null
        mutableData = null
    }
}

/**
 * Generates an [ExitTransition] to sustain deferred animations during handoff.
 *
 * Targets the last manual values of all properties animated during the deferred phase.
 */
internal fun SharedMutableTransformState.getHandoffExit(): ExitTransition {
    var handoffExit = ExitTransition.None
    if (this.lastMutableData?.block != null && this.isHandoffActive) {
        if (this.transformScope.isScaleMutated) {
            handoffExit += scaleOut(targetScale = this.lastScale)
        }
        if (this.transformScope.isAlphaMutated) {
            handoffExit += fadeOut(targetAlpha = this.lastAlpha)
        }
        if (this.transformScope.isOffsetMutated) {
            handoffExit += slideOut(targetOffset = this.slideHandoffOffset)
        }
        if (this.transformScope.isVeilMutated) {
            val matchParentSize = this.lastMutableData?.veilMatchParentSize ?: false
            handoffExit += veilOut(targetColor = this.lastVeil, matchParentSize = matchParentSize)
        }
    }

    return handoffExit
}

/**
 * Generates an [EnterTransition] to seamlessly handoff deferred animations.
 *
 * Captures the last manual values of all properties animated during the deferred phase to use as
 * the starting point for the enter transition.
 */
internal fun SharedMutableTransformState.getHandoffEnter(): EnterTransition {
    var handoffEnter = EnterTransition.None
    if (this.lastMutableData?.block != null && this.isHandoffActive) {
        if (this.transformScope.isScaleMutated) {
            handoffEnter += scaleIn(initialScale = this.lastScale)
        }
        if (this.transformScope.isAlphaMutated) {
            handoffEnter += fadeIn(initialAlpha = this.lastAlpha)
        }
        if (this.transformScope.isOffsetMutated) {
            handoffEnter += slideIn(initialOffset = this.slideHandoffOffset)
        }
        if (this.transformScope.isVeilMutated) {
            val matchParentSize = this.lastMutableData?.veilMatchParentSize ?: false
            handoffEnter +=
                unveilIn(initialColor = this.lastVeil, matchParentSize = matchParentSize)
        }
    }

    return handoffEnter
}
