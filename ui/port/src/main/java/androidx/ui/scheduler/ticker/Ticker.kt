/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.scheduler.ticker

import androidx.annotation.CallSuper
import androidx.ui.assert
import androidx.ui.core.Duration
import androidx.ui.foundation.assertions.FlutterError
import androidx.ui.runtimeType
import androidx.ui.scheduler.binding.SchedulerBinding
import androidx.ui.scheduler.binding.SchedulerPhase

/**
 * Calls its callback once per animation frame.
 *
 * When created, a ticker is initially disabled. Call [start] to
 * enable the ticker.
 *
 * A [Ticker] can be silenced by setting [muted] to true. While silenced, time
 * still elapses, and [start] and [stop] can still be called, but no callbacks
 * are called.
 *
 * By convention, the [start] and [stop] methods are used by the ticker's
 * consumer, and the [muted] property is controlled by the [TickerProvider]
 * that created the ticker.
 *
 * Tickers are driven by the [SchedulerBinding]. See
 * [SchedulerBinding.scheduleFrameCallback].
 *
 * Creates a ticker that will call the provided callback once per frame while
 * running.
 */
open class Ticker(
    private val onTick: TickerCallback,
    /**
     * An optional label can be provided for debugging purposes.
     *
     * This label will appear in the [toString] output in debug builds.
     */
    private val debugLabel: String? = null,
    private val schedulerBinding: SchedulerBinding
) {

    // TODO(Migration:Andrey) needs StackTrace
    // StackTrace _debugCreationStack;
    init {
        assert {
            // TODO(Migration:Andrey) needs StackTrace
//            _debugCreationStack = StackTrace.current;
            true
        }
    }

    private var future: TickerFuture? = null

    /**
     * Whether this ticker has been silenced.
     *
     * While silenced, a ticker's clock can still run, but the callback will not
     * be called.
     * When set to true, silences the ticker, so that it is no longer ticking. If
     * a tick is already scheduled, it will unschedule it. This will not
     * unschedule the next frame, though.
     *
     * When set to false, unsilences the ticker, potentially scheduling a frame
     * to handle the next tick.
     *
     * By convention, the [muted] property is controlled by the object that
     * created the [Ticker] (typically a [TickerProvider]), not the object that
     * listens to the ticker's ticks.
     */
    var muted: Boolean = false
        set(value) {
            if (value == field)
                return
            field = value
            if (value) {
                unscheduleTick()
            } else if (shouldScheduleTick) {
                scheduleTick()
            }
        }

    /**
     * Whether this [Ticker] has scheduled a call to call its callback
     * on the next frame.
     *
     * A ticker that is [muted] can be active (see [isActive]) yet not be
     * ticking. In that case, the ticker will not call its callback, and
     * [isTicking] will be false, but time will still be progressing.
     *
     * This will return false if the [Scheduler.lifecycleState] is one that
     * indicates the application is not currently visible (e.g. if the device's
     * screen is turned off).
     */
    val isTicking: Boolean
        get() {
            if (future == null)
                return false
            if (muted)
                return false
            if (schedulerBinding.framesEnabled)
                return true
            if (schedulerBinding.schedulerPhase != SchedulerPhase.idle)
                return true; // for example, we might be in a warm-up frame or forced frame
            return false
        }

    /**
     * Whether time is elapsing for this [Ticker]. Becomes true when [start] is
     * called and false when [stop] is called.
     *
     * A ticker can be active yet not be actually ticking (i.e. not be calling
     * the callback). To determine if a ticker is actually ticking, use
     * [isTicking].
     */
    val isActive get() = future != null

    private var startTime: Duration? = null

    /**
     * Starts the clock for this [Ticker]. If the ticker is not [muted], then this
     * also starts calling the ticker's callback once per animation frame.
     *
     * The returned future resolves once the ticker [stop]s ticking. If the
     * ticker is disposed, the future does not resolve. A derivative future is
     * available from the returned [TickerFuture] object that resolves with an
     * error in that case, via [TickerFuture.orCancel].
     *
     * Calling this sets [isActive] to true.
     *
     * This method cannot be called while the ticker is active. To restart the
     * ticker, first [stop] it.
     *
     * By convention, this method is used by the object that receives the ticks
     * (as opposed to the [TickerProvider] which created the ticker).
     */
    fun start(): TickerFuture {
        assert {
            if (isActive) {
                throw FlutterError(
                    "A ticker was started twice.\nA ticker that is already active cannot be " +
                            "started again without first stopping it.\nThe affected ticker " +
                            "was: ${toStringParametrized(debugIncludeStack = true)}"
                )
            }
            true
        }
        assert(startTime == null)
        future = TickerFuture()
        if (shouldScheduleTick) {
            scheduleTick()
        }
        if (schedulerBinding.schedulerPhase.ordinal > SchedulerPhase.idle.ordinal &&
            schedulerBinding.schedulerPhase.ordinal < SchedulerPhase.postFrameCallbacks.ordinal
        )
            startTime = schedulerBinding.currentFrameTimeStamp
        return future!!
    }

    /**
     * Stops calling this [Ticker]'s callback.
     *
     * If called with the `canceled` argument set to false (the default), causes
     * the future returned by [start] to resolve. If called with the `canceled`
     * argument set to true, the future does not resolve, and the future obtained
     * from [TickerFuture.orCancel], if any, resolves with a [TickerCanceled]
     * error.
     *
     * Calling this sets [isActive] to false.
     *
     * This method does nothing if called when the ticker is inactive.
     *
     * By convention, this method is used by the object that receives the ticks
     * (as opposed to the [TickerProvider] which created the ticker).
     */
    fun stop(canceled: Boolean = false) {
        if (!isActive)
            return

        // We take the future into a local variable so that isTicking is false
        // when we actually complete the future (isTicking uses future to
        // determine its state).
        val localFuture = future!!
        future = null
        startTime = null
        assert(!isActive)

        unscheduleTick()
        if (canceled) {
            localFuture.cancel(this)
        } else {
            localFuture.complete()
        }
    }

    private var animationId: Int? = null

    /** Whether this [Ticker] has already scheduled a frame callback. */
    protected val scheduled get() = animationId != null

    /**
     * Whether a tick should be scheduled.
     *
     * If this is true, then calling [scheduleTick] should succeed.
     *
     * Reasons why a tick should not be scheduled include:
     *
     * * A tick has already been scheduled for the coming frame.
     * * The ticker is not active ([start] has not been called).
     * * The ticker is not ticking, e.g. because it is [muted] (see [isTicking]).
     */
    protected val shouldScheduleTick get() = !muted && isActive && !scheduled

    private fun tick(timeStamp: Duration) {
        assert(isTicking)
        assert(scheduled)
        animationId = null

        val valStartTime = startTime ?: timeStamp
        startTime = valStartTime

        onTick(timeStamp - valStartTime)

        // The onTick callback may have scheduled another tick already, for
        // example by calling stop then start again.
        if (shouldScheduleTick) {
            scheduleTick(rescheduling = true)
        }
    }

    /**
     * Schedules a tick for the next frame.
     *
     * This should only be called if [shouldScheduleTick] is true.
     */
    protected fun scheduleTick(rescheduling: Boolean = false) {
        assert(!scheduled)
        assert(shouldScheduleTick)
        animationId = schedulerBinding.scheduleFrameCallback(this::tick, rescheduling)
    }

    /**
     * Cancels the frame callback that was requested by [scheduleTick], if any.
     *
     * Calling this method when no tick is [scheduled] is harmless.
     *
     * This method should not be called when [shouldScheduleTick] would return
     * true if no tick was scheduled.
     */
    protected fun unscheduleTick() {
        if (scheduled) {
            schedulerBinding.cancelFrameCallbackWithId(animationId!!)
            animationId = null
        }
        assert(!shouldScheduleTick)
    }

    /**
     * Makes this [Ticker] take the state of another ticker, and disposes the
     * other ticker.
     *
     * This is useful if an object with a [Ticker] is given a new
     * [TickerProvider] but needs to maintain continuity. In particular, this
     * maintains the identity of the [TickerFuture] returned by the [start]
     * function of the original [Ticker] if the original ticker is active.
     *
     * This ticker must not be active when this method is called.
     */
    fun absorbTicker(originalTicker: Ticker) {
        assert(!isActive)
        assert(future == null)
        assert(startTime == null)
        assert(animationId == null)
        assert((originalTicker.future == null) == (originalTicker.startTime == null),
            { "Cannot absorb Ticker after it has been disposed." })
        if (originalTicker.future != null) {
            future = originalTicker.future
            startTime = originalTicker.startTime
            if (shouldScheduleTick)
                scheduleTick()
            originalTicker.future =
                    null; // so that it doesn't get disposed when we dispose of originalTicker
            originalTicker.unscheduleTick()
        }
        originalTicker.dispose()
    }

    /**
     * Release the resources used by this object. The object is no longer usable
     * after this method is called.
     */
    @CallSuper
    open fun dispose() {
        if (future != null) {
            val localFuture = future!!
            future = null
            assert(!isActive)
            unscheduleTick()
            localFuture.cancel(this)
        }
        assert {
            // We intentionally don't null out startTime. This means that if start()
            // was ever called, the object is now in a bogus state. This weakly helps
            // catch cases of use-after-dispose.
            startTime = Duration.zero
            true
        }
    }

    fun toStringParametrized(debugIncludeStack: Boolean): String {
        val buffer = StringBuffer()
        buffer.append("${runtimeType()}(")
        assert {
            buffer.append(debugLabel ?: "")
            true
        }
        buffer.append(")")
        assert {
            if (debugIncludeStack) {
                buffer.appendln()
                buffer.appendln("The stack trace when the ${runtimeType()} " +
                        "was actually created was:")
                TODO("Migration|Android: needs StackTrace")
//                FlutterError.defaultStackFilter(
//                    _debugCreationStack.toString().trimRight()
//                        .split("\n")
//                ).forEach { buffer.appendln(it) }
            }
            true
        }
        return buffer.toString()
    }

    override fun toString() = toStringParametrized(false)
}