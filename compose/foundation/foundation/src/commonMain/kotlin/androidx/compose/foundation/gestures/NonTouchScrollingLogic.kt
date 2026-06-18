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

package androidx.compose.foundation.gestures

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.MutatePriority
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlin.jvm.JvmInline
import kotlin.math.abs
import kotlin.math.sign
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

/** A shared base class for [TrackpadScrollingLogicImpl] and [MouseWheelScrollingLogicImpl]. */
internal abstract class NonTouchScrollingLogic(
    protected val scrollLogic: ScrollLogic,
    protected val onScrollStopped: suspend (velocity: Velocity) -> Unit,
    protected var density: Density,
) {
    fun updateDensity(density: Density) {
        this.density = density
    }

    internal inline val PointerEvent.isConsumed: Boolean
        get() = changes.fastAny { it.isConsumed }

    internal fun PointerEvent.consume() = changes.fastForEach { it.consume() }

    internal var isScrolling = false

    internal suspend fun userScroll(block: suspend NestedScrollScope.() -> Unit) {
        isScrolling = true
        // Run it in supervisorScope to ignore cancellations from scrolls with higher MutatePriority
        supervisorScope { scrollLogic.scroll(MutatePriority.UserInput, block) }
        isScrolling = false
    }

    internal val velocityTracker = DifferentialVelocityTracker()

    /**
     * Returns whether the scrolling logic is interested in the given pointer event.
     *
     * Pointer events for which this returns `false` will not be passed to [onScrollingEvent].
     */
    protected abstract fun isScrollingEvent(pointerEvent: PointerEvent): Boolean

    /**
     * Invoked when a pointer event the logic is interested in is received.
     *
     * Returns whether to consume the event.
     */
    protected abstract fun onScrollingEvent(pointerEvent: PointerEvent, bounds: IntSize): Boolean

    // Called when the node receives a pointer event.
    fun onPointerEvent(pointerEvent: PointerEvent, pass: PointerEventPass, bounds: IntSize) {
        if (pointerEvent.isConsumed) return
        if (!isScrollingEvent(pointerEvent)) return

        // If this scrollable is already scrolling from a previous interaction, consume immediately
        // to give it priority.
        if (pass == PointerEventPass.Initial && isScrolling) {
            onScrollingEvent(pointerEvent, bounds)
            pointerEvent.consume()
        }

        // During the main pass. If this scrollable is not scrolling, decide whether it should be
        // based on whether the event was consumed. If the scrollable is scrolling, we don't need
        // to worry because it was consumed during the initial pass.
        if (pass == PointerEventPass.Main && !isScrolling) {
            val consumed = onScrollingEvent(pointerEvent, bounds)
            if (consumed) {
                pointerEvent.consume()
            }
        }
    }

    /** Begins processing of events sent to [onPointerEvent] using the given [coroutineScope]. */
    abstract fun startReceivingEvents(coroutineScope: CoroutineScope)
}

/**
 * Replacement of regular [Channel.receive] that schedules an invalidation each frame. It avoids
 * entering an idle state while waiting for [ScrollProgressTimeout]. It's important for tests that
 * attempt to trigger another scroll after a mouse wheel event.
 */
internal suspend fun <T> Channel<T>.busyReceive(): T = coroutineScope {
    val job = launch {
        while (coroutineContext.isActive) {
            withFrameNanos {}
        }
    }
    try {
        receive()
    } finally {
        job.cancel()
    }
}

internal fun <E> untilNull(builderAction: () -> E?) =
    sequence<E> {
        do {
            val element = builderAction()?.also { yield(it) }
        } while (element != null)
    }

internal fun ScrollingLogic.canConsumeDelta(delta: Float): Boolean {
    val directionalDelta = delta.reverseIfNeeded()
    return when {
        directionalDelta < 0f -> scrollableState.canScrollBackward
        directionalDelta > 0f -> scrollableState.canScrollForward
        // Nothing to scroll on our axis; let something else handle the other axis.
        else -> false
    }
}

/**
 * To avoid boxing in [ScrollValueAdapter], the scroll values (`Float` and `Offset`) are encoded
 * into this type when passed into and out of the adapter.
 *
 * The reason to use a type, instead of just `Long`, besides good practice, is that with `Long`
 * there's a danger of accidentally using a primitive operator (i.e. [Long.plus]) instead of the one
 * in [ScrollValueAdapter].
 */
@JvmInline internal value class ScrollValue(val bits: Long)

/**
 * Adapter between [Offset] and the value being changed during scrolling.
 *
 * Either [OneDimensionalScrollValueAdapter] or [TwoDimensionalScrollValueAdapter].
 */
internal interface ScrollValueAdapter<T> {
    /**
     * Encodes the scrollable value into a [ScrollValue].
     *
     * This method boxes the scrollable type, and should therefore not be used too frequently. If
     * possible, prefer to use the non-interface methods in the concrete implementation, i.e.,
     * [OneDimensionalScrollValueAdapter.encodeFloat] or
     * [TwoDimensionalScrollValueAdapter.encodeOffset].
     */
    fun T.encode(): ScrollValue

    /**
     * Decodes the scrollable value from a [ScrollValue].
     *
     * This method boxes the scrollable type, and should therefore not be used too frequently. If
     * possible, prefer to use the non-interface methods in the concrete implementation, i.e.,
     * [OneDimensionalScrollValueAdapter.decodeToFloat] or
     * [TwoDimensionalScrollValueAdapter.decodeToOffset].
     */
    fun ScrollValue.decode(): T

    fun ScrollValue.toOffset(): Offset

    fun ScrollValue.toVelocity(): Velocity

    fun Offset.toScrollValue(): ScrollValue

    /**
     * A scrollable value of [size] 1 (as a vector), in the same "direction" (as a vector) as
     * `this`, or a zero scrollable value if `this` is zero.
     *
     * For a 1-dimensional scroll value ([Float]), this is just -1, 1 or 0.
     *
     * For a 2-dimensional scroll value, this is an [Offset] with the same angle, whose
     * [Offset.getDistance] is 1 (or [Offset.Zero] if the original offset itself is [Offset.Zero]).
     */
    fun ScrollValue.normalize(): ScrollValue

    /**
     * The magnitude/length (as a vector) of the scrollable value, in pixels; a non-negative value.
     *
     * For a 1-dimensional scroll value ([Float]), this is just its absolute value.
     *
     * For a 2-dimensional scroll value, this is [Offset.getDistance].
     */
    fun ScrollValue.size(): Float

    operator fun ScrollValue.times(scale: Float): ScrollValue

    operator fun ScrollValue.plus(value: ScrollValue): ScrollValue

    operator fun ScrollValue.minus(value: ScrollValue): ScrollValue

    /**
     * Returns whether the value is too low for visible change in scroll (consumed delta,
     * animation-based change, etc.)
     */
    fun ScrollValue.isLowScrollingDelta(): Boolean

    fun newAnimationState(): AnimationState<T, *>
}

/**
 * [ScrollValueAdapter] for one-dimensional scrolling, where the scrollable value is a [Float].
 *
 * The axis ([isVertical]) is passed in as a lambda to avoid having to update it manually.
 */
internal class OneDimensionalScrollValueAdapter(val isVertical: () -> Boolean) :
    ScrollValueAdapter<Float> {

    /**
     * Encodes a [Float] into a [ScrollValue].
     *
     * Prefer this function over [Float.encode], as it avoids boxing.
     */
    fun encodeFloat(value: Float) = ScrollValue(value.toRawBits().toLong())

    /**
     * Decodes a [ScrollValue] back into a [Float].
     *
     * Prefer this function over [ScrollValue.decode], as it avoids boxing.
     */
    fun decodeToFloat(value: ScrollValue) = Float.fromBits((value.bits and 0xffffffffL).toInt())

    override fun Float.encode(): ScrollValue = encodeFloat(this)

    override fun ScrollValue.decode(): Float = decodeToFloat(this)

    private inline fun ScrollValue.transform(block: Float.() -> Float): ScrollValue {
        return encodeFloat(block(decodeToFloat(this)))
    }

    override fun ScrollValue.toOffset(): Offset {
        val value = decodeToFloat(this)
        return (if (isVertical()) Offset(0f, value) else Offset(value, 0f))
    }

    override fun Offset.toScrollValue(): ScrollValue {
        val isVertical = isVertical()
        val result =
            if (abs(y) >= abs(x)) {
                if (isVertical) this.y else 0f
            } else {
                if (!isVertical) this.x else 0f
            }
        return encodeFloat(result)
    }

    override fun ScrollValue.toVelocity(): Velocity {
        val value = decodeToFloat(this)
        return when {
            value == 0f -> Velocity.Zero
            isVertical() -> Velocity(0f, value)
            else -> Velocity(value, 0f)
        }
    }

    override fun ScrollValue.normalize() = transform { sign(this) }

    override fun ScrollValue.size() = abs(decodeToFloat(this))

    override fun ScrollValue.times(scale: Float) = transform { this * scale }

    override fun ScrollValue.plus(value: ScrollValue) = transform { this + decodeToFloat(value) }

    override fun ScrollValue.minus(value: ScrollValue) = transform { this - decodeToFloat(value) }

    override fun newAnimationState() = AnimationState(0f)

    override fun ScrollValue.isLowScrollingDelta() = abs(decodeToFloat(this)) < 0.5f
}

/**
 * [ScrollValueAdapter] for two-dimensional scrolling, where the scrollable value is an [Offset].
 */
internal object TwoDimensionalScrollValueAdapter : ScrollValueAdapter<Offset> {

    /**
     * Encodes an [Offset] into a [ScrollValue].
     *
     * Prefer this function over [Offset.encode], as it avoids boxing.
     */
    fun encodeOffset(value: Offset) = ScrollValue(value.packedValue)

    /**
     * Decodes a [ScrollValue] back into an [Offset].
     *
     * Prefer this function over [ScrollValue.decode], as it avoids boxing.
     */
    fun decodeToOffset(value: ScrollValue) = Offset(value.bits)

    override fun Offset.encode(): ScrollValue = encodeOffset(this)

    override fun ScrollValue.decode(): Offset = decodeToOffset(this)

    private inline fun ScrollValue.transform(block: Offset.() -> Offset): ScrollValue {
        return encodeOffset(block(decodeToOffset(this)))
    }

    override fun ScrollValue.toOffset() = decodeToOffset(this)

    override fun Offset.toScrollValue() = encodeOffset(this)

    override fun ScrollValue.toVelocity(): Velocity {
        val offset = decodeToOffset(this)
        return when {
            offset == Offset.Zero -> Velocity.Zero
            else -> Velocity(offset.x, offset.y)
        }
    }

    override fun ScrollValue.normalize() = transform {
        if ((this.x == 0f) && (this.y == 0f)) Offset.Zero else this / getDistance()
    }

    override fun ScrollValue.size() = decodeToOffset(this).getDistance()

    override fun ScrollValue.times(scale: Float) = transform { this * scale }

    override fun ScrollValue.plus(value: ScrollValue) = transform { this + decodeToOffset(value) }

    override fun ScrollValue.minus(value: ScrollValue) = transform { this - decodeToOffset(value) }

    override fun newAnimationState() =
        AnimationState(Offset.VectorConverter, Offset.Zero, Offset.Zero)

    override fun ScrollValue.isLowScrollingDelta(): Boolean {
        val value = decodeToOffset(this)
        return (abs(value.x) < 0.5f) && (abs(value.y) < 0.5f)
    }
}
