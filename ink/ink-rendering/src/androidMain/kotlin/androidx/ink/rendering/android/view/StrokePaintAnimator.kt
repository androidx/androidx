/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.ink.rendering.android.view

import android.view.Choreographer
import androidx.annotation.AnyThread
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.annotation.RestrictTo
import androidx.annotation.UiThread
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicLong
import kotlin.jvm.JvmName
import kotlin.jvm.JvmStatic

/**
 * Controls animated paint textures for rendered strokes. Typically a single [StrokePaintAnimator]
 * object is used for all strokes in a document.
 *
 * This class is mostly not thread-safe, and should in general be accessed only from the UI thread;
 * the one exception is the `getClockStateMillis()` method, which can be safely called from any
 * thread.
 *
 * It is recommended to call `pause()` when finished using an animator, which will disable its
 * internal `Choreographer` callback and allow the animator to be garbage collected more quickly.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
public class StrokePaintAnimator public constructor() {
    private var prevClockStateNanos: Long = 0L
    private val clockStateNanos: AtomicLong = AtomicLong()

    // Current values that should be used for updates during the next `Choreographer` callback.
    private var prevFrameTimeNanos: Long = 0L
    private var prevPaused: Boolean = true
    private var prevSpeedMultiplier: Float = 1.0f

    // The listeners to call during each `Choreographer` callback, after updating `clockStateNanos`.
    private var animationListeners: MutableList<AnimationListener> = mutableListOf()

    private val choreographerCallback: Choreographer.FrameCallback =
        ChoreographerCallback(WeakReference(this))

    /**
     * Returns the number of subjective milliseconds that have elapsed since the animator's zero
     * clock state. Immediately after a `resetClockState()`, this will return zero; after waiting
     * for N milliseconds of real time, this will return (approximately) `speedMultiplier * N` (but
     * note that this value only updates once per frame).
     *
     * The initial clock state for a newly-constructed [StrokePaintAnimator] is unspecified. If you
     * need a known or consistent initial clock state, use `resetClockState()`, optionally followed
     * by `advanceByMillis()`.
     *
     * This method is safe to call from any thread. All other methods and properties of this class
     * should only be accessed from a single thread (typically the UI thread).
     */
    @AnyThread public fun getClockStateMillis(): Long = clockStateNanos.toLong() / NANOS_PER_MILLI

    /**
     * Controls the speed of animation. Set this to 1 for normal speed. Set this to zero to
     * effectively halt animation (but still receive [AnimationListener] callbacks). Set this to a
     * negative value to run animations in reverse.
     */
    @get:UiThread @set:UiThread public var speedMultiplier: Float = 1.0f

    /**
     * Indicates whether the animator is currently paused. While the animator is paused,
     * [AnimationListener] callbacks do not fire.
     */
    @get:JvmName("isPaused")
    @get:UiThread
    @set:UiThread
    public var isPaused: Boolean = true
        private set(paused) {
            if (field != paused) {
                field = paused
                if (paused) {
                    Choreographer.getInstance().removeFrameCallback(choreographerCallback)
                } else {
                    Choreographer.getInstance().postFrameCallback(choreographerCallback)
                }
            }
        }

    /**
     * Pauses the animator (if it's not already paused). While the animator is paused,
     * [AnimationListener] callbacks do not fire.
     */
    @UiThread
    public fun pause() {
        isPaused = true
    }

    /** Unpauses the animator (if it's not already unpaused). */
    @UiThread
    public fun resume() {
        isPaused = false
    }

    /**
     * Jumps the animation clock forward/backward by the given amount of real time. The current
     * speed multiplier will be applied to this duration when adding it to the clock state.
     */
    @UiThread
    public fun advanceByMillis(durationMillis: Long) {
        clockStateNanos.addAndGet(
            ((durationMillis * NANOS_PER_MILLI).toFloat() * speedMultiplier).toLong()
        )
    }

    /**
     * Resets the animation clock back to its zero state, when all strokes are in their base
     * animation phase. This provides a reliable reference point for e.g. document thumbnails or
     * video export of an animated document.
     */
    @UiThread
    public fun resetClockState() {
        clockStateNanos.set(0L)
    }

    /** Adds a listener to be called once per frame while the [StrokePaintAnimator] is unpaused. */
    @UiThread
    public fun addAnimationListener(listener: AnimationListener) {
        animationListeners.add(listener)
    }

    /** Removes a listener previously added with `addAnimationListener()`. */
    @UiThread
    public fun removeAnimationListener(listener: AnimationListener) {
        animationListeners.remove(listener)
    }

    @UiThread
    internal fun onChoreographerFrame(nextFrameTimeNanos: Long) {
        if (!prevPaused) {
            val nanosSinceLastUpdate = nextFrameTimeNanos - prevFrameTimeNanos
            clockStateNanos.addAndGet(
                (nanosSinceLastUpdate.toDouble() * prevSpeedMultiplier).toLong()
            )
        }
        if (!isPaused) {
            Choreographer.getInstance().postFrameCallback(choreographerCallback)
        }
        val newClockStateNanos = clockStateNanos.get()
        if (prevClockStateNanos != newClockStateNanos) {
            prevClockStateNanos = newClockStateNanos
            callAnimationListeners()
        }
        prevFrameTimeNanos = nextFrameTimeNanos
        prevPaused = isPaused
        prevSpeedMultiplier = speedMultiplier
    }

    @UiThread
    private fun callAnimationListeners() {
        for (listener in animationListeners) {
            listener.onAnimationUpdate(this)
        }
    }

    /** A listener for clock state updates from the [StrokePaintAnimator]. */
    public fun interface AnimationListener {
        /**
         * Called once per frame while the [StrokePaintAnimator] is unpaused, after its clock state
         * has been updated for the frame, but before `View`s are drawn.
         */
        @UiThread public fun onAnimationUpdate(animator: StrokePaintAnimator)
    }

    public companion object {
        // The number of nanoseconds in one millisecond.
        private const val NANOS_PER_MILLI: Long = 1_000_000L

        /**
         * Given a whole-stroke animation duration, calculates the 0-1 base phase value for the
         * stroke. This is the animation progress value that the stroke should appear at for the
         * animator's zero clock state, such that the stroke would be at the start of its animation
         * at the animator's current clock state.
         *
         * If `animationLoopDurationMillis` is zero, indicating that the stroke is not animated,
         * then this method returns zero.
         */
        @JvmStatic
        @FloatRange(from = 0.0, to = 1.0, toInclusive = false)
        public fun calculateBasePhaseForNewStroke(
            clockStateMillis: Long,
            @IntRange(from = 0, to = 1 shl 24) animationLoopDurationMillis: Long,
        ): Float =
            if (animationLoopDurationMillis == 0L) {
                0.0f
            } else {
                (-clockStateMillis).mod(animationLoopDurationMillis).toFloat() /
                    animationLoopDurationMillis.toFloat()
            }

        /**
         * Given a stroke's base phase and its whole-stroke animation duration, returns the phase
         * value the stroke should have at the animator's current clock state.
         *
         * If `animationLoopDurationMillis` is zero, indicating that the stroke is not animated,
         * then this method returns zero.
         */
        @JvmStatic
        @FloatRange(from = 0.0, to = 1.0, toInclusive = false)
        public fun calculateCurrentPhaseForStroke(
            clockStateMillis: Long,
            @IntRange(from = 0, to = 1 shl 24) animationLoopDurationMillis: Long,
            @FloatRange(from = 0.0, to = 1.0, toInclusive = false) basePhase: Float,
        ): Float =
            if (animationLoopDurationMillis == 0L) {
                0.0f
            } else {
                (clockStateMillis + (basePhase * animationLoopDurationMillis.toFloat()).toLong())
                    .mod(animationLoopDurationMillis)
                    .toFloat() / animationLoopDurationMillis.toFloat()
            }
    }
}

// Using a weak reference to the [StrokePaintAnimator] ensures that the animator is not kept alive
// solely by the [Choreographer] holding a reference to this callback, and (eventually, once garbage
// collection happens) prevents the callback from re-enqueuing itself forever once the animator is
// no longer being used.
internal class ChoreographerCallback(private val animator: WeakReference<StrokePaintAnimator>) :
    Choreographer.FrameCallback {
    override fun doFrame(nextFrameTimeNanos: Long) {
        animator.get()?.onChoreographerFrame(nextFrameTimeNanos)
    }
}
