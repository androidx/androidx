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

package androidx.ui.animation

import androidx.ui.animation.AnimationController.Companion.toString
import androidx.ui.assert
import androidx.ui.clamp
import androidx.ui.core.Duration
import androidx.ui.foundation.assertions.FlutterError
import androidx.ui.lerpDouble
import androidx.ui.physics.Simulation
import androidx.ui.runtimeType
import androidx.ui.scheduler.ticker.Ticker
import androidx.ui.scheduler.ticker.TickerFuture
import androidx.ui.scheduler.ticker.TickerProvider
import androidx.ui.toStringAsFixed
import kotlin.math.absoluteValue

/** The direction in which an animation is running. */
private enum class AnimationDirection {
    /** The animation is running from beginning to end. */
    FORWARD,

    /** The animation is running backwards, from end to beginning. */
    REVERSE
}

/**
 * A controller for an animation.
 *
 * This class lets you perform tasks such as:
 *
 * * Play an animation [forward] or in [reverse], or [stop] an animation.
 * * Set the animation to a specific [value].
 * * Define the [upperBound] and [lowerBound] values of an animation.
 * * Create a [fling] animation effect using a physics simulation.
 *
 * By default, an [AnimationController] linearly produces values that range
 * from 0.0 to 1.0, during a given duration. The animation controller generates
 * a new value whenever the device running your app is ready to display a new
 * frame (typically, this rate is around 60 values per second).
 *
 * An AnimationController needs a [TickerProvider], which is configured using
 * the `vsync` argument on the constructor. If you are creating an
 * AnimationController from a [State], then you can use the
 * [TickerProviderStateMixin] and [SingleTickerProviderStateMixin] classes to
 * obtain a suitable [TickerProvider]. The widget test framework [WidgetTester]
 * object can be used as a ticker provider in the context of tests. In other
 * contexts, you will have to either pass a [TickerProvider] from a higher
 * level (e.g. indirectly from a [State] that mixes in
 * [TickerProviderStateMixin]), or create a custom [TickerProvider] subclass.
 *
 * The methods that start animations return a [TickerFuture] object which
 * completes when the animation completes successfully, and never throws an
 * error; if the animation is canceled, the future never completes. This object
 * also has a [TickerFuture.orCancel] property which returns a future that
 * completes when the animation completes successfully, and completes with an
 * error when the animation is aborted.
 *
 * This can be used to write code such as:
 *
 * ```dart
 * Future<Null> fadeOutAndUpdateState() async {
 *   try {
 *     await fadeAnimationController.forward().orCancel;
 *     await sizeAnimationController.forward().orCancel;
 *     setState(() {
 *       dismissed = true;
 *     });
 *   } on TickerCanceled {
 *     // the animation got canceled, probably because we were disposed
 *   }
 * }
 * ```
 *
 * ...which asynchronously runs one animation, then runs another, then changes
 * the state of the widget, without having to verify [State.mounted] is still
 * true at each step, and without having to chain futures together explicitly.
 * (This assumes that the controllers are created in [State.initState] and
 * disposed in [State.dispose].)
 */
class AnimationController(
    /**
     * * [value] is the initial value of the animation. If defaults to the lower
     *   bound.
     */
    value: Double? = null,
    /** * [duration] is the length of time this animation should last. */
    private val duration: Duration? = null,
    /**
     * * [debugLabel] is a string to help identify this animation during
     *   debugging (used by [toString]).
     */
    private val debugLabel: String? = null,
    /**
     * * [lowerBound] is the smallest value this animation can obtain and the
     *   value at which this animation is deemed to be DISMISSED. It cannot be
     *   null.
     */
    private val lowerBound: Double = 0.0,
    /**
     * * [upperBound] is the largest value this animation can obtain and the
     *   value at which this animation is deemed to be completed. It cannot be
     *   null.
     */
    private val upperBound: Double = 1.0,
    /**
     * * `vsync` is the [TickerProvider] for the current context. It can be
     *   changed by calling [resync]. It is required and must not be null. See
     *   [TickerProvider] for advice on obtaining a ticker provider.
     */
    vsync: TickerProvider
) : AnimationEagerListenerMixin<Double>() {

    private var ticker: Ticker?

    private var direction: AnimationDirection

    private var simulation: Simulation? = null

    /**
     * The current value of the animation.
     *
     * Setting this value notifies all the listeners that the value
     * changed.
     *
     * Setting this value also stops the controller if it is currently
     * running; if this happens, it also notifies all the status
     * listeners.
     */
    private var _value: Double = 0.0
        set(value) {
            field = value.clamp(lowerBound, upperBound)
            _status = if (field == lowerBound) {
                AnimationStatus.DISMISSED
            } else if (field == upperBound) {
                AnimationStatus.COMPLETED
            } else {
                if (direction == AnimationDirection.FORWARD)
                    AnimationStatus.FORWARD else AnimationStatus.REVERSE
            }
        }
    /**
     * Stops the animation controller and sets the current value of the
     * animation.
     *
     * The new value is clamped to the range set by [lowerBound] and [upperBound].
     *
     * Value listeners are notified even if this does not change the value.
     * Status listeners are notified if the animation was previously playing.
     *
     * The most recently returned [TickerFuture], if any, is marked as having been
     * canceled, meaning the future never completes and its [TickerFuture.orCancel]
     * derivative future completes with a [TickerCanceled] error.
     *
     * See also:
     *
     *  * [reset], which is equivalent to setting [value] to [lowerBound].
     *  * [stop], which aborts the animation without changing its value or status
     *    and without dispatching any notifications other than completing or
     *    canceling the [TickerFuture].
     *  * [forward], [reverse], [animateTo], [animateWith], [fling], and [repeat],
     *    which start the animation controller.
     */
    override var value: Double
        get() = _value
        set(newValue) {
            stop()
            _value = newValue
            notifyListeners()
            checkStatusChanged()
        }

    init {
        assert(upperBound >= lowerBound)
        direction = AnimationDirection.FORWARD
        ticker = vsync.createTicker(::tick)
        _value = value ?: lowerBound
    }

    /**
     * Returns an [Animation<double>] for this animation controller, so that a
     * pointer to this object can be passed around without allowing users of that
     * pointer to mutate the [AnimationController] state.
     */
    val view: Animation<Double> = this

    /** Recreates the [Ticker] with the new [TickerProvider]. */
    fun resync(vsync: TickerProvider) {
        val oldTicker = ticker!!
        ticker = vsync.createTicker(this::tick).apply {
            absorbTicker(oldTicker)
        }
    }

    /**
     * Sets the controller's value to [lowerBound], stopping the animation (if
     * in progress), and resetting to its beginning point, or dismissed state.
     *
     * The most recently returned [TickerFuture], if any, is marked as having been
     * canceled, meaning the future never completes and its [TickerFuture.orCancel]
     * derivative future completes with a [TickerCanceled] error.
     *
     * See also:
     *
     *  * [value], which can be explicitly set to a specific value as desired.
     *  * [forward], which starts the animation in the FORWARD direction.
     *  * [stop], which aborts the animation without changing its value or status
     *    and without dispatching any notifications other than completing or
     *    canceling the [TickerFuture].
     */
    fun reset() {
        value = lowerBound
    }

    /**
     * The rate of change of [value] per second.
     *
     * If [isAnimating] is false, then [value] is not changing and the rate of
     * change is zero.
     */
    val velocity: Double
        get() {
            if (!isAnimating)
                return 0.0
            return simulation!!.dx(
                lastElapsedDuration!!.inMicroseconds.toDouble() /
                        Duration.microsecondsPerSecond
            )
        }

    /**
     * The amount of time that has passed between the time the animation started
     * and the most recent tick of the animation.
     *
     * If the controller is not animating, the last elapsed duration is null.
     */
    var lastElapsedDuration: Duration? = null
        private set

    /**
     * Whether this animation is currently animating in either the forward or reverse direction.
     *
     * This is separate from whether it is actively ticking. An animation
     * controller's ticker might get muted, in which case the animation
     * controller's callbacks will no longer fire even though time is continuing
     * to pass. See [Ticker.muted] and [TickerMode].
     */
    val isAnimating get() = ticker?.isActive ?: false

    private lateinit var _status: AnimationStatus
    override val status: AnimationStatus get() = _status

    /**
     * Starts running this animation forwards (towards the end).
     *
     * Returns a [TickerFuture] that completes when the animation is complete.
     *
     * The most recently returned [TickerFuture], if any, is marked as having been
     * canceled, meaning the future never completes and its [TickerFuture.orCancel]
     * derivative future completes with a [TickerCanceled] error.
     *
     * During the animation, [status] is reported as [AnimationStatus.FORWARD],
     * which switches to [AnimationStatus.COMPLETED] when [upperBound] is
     * reached at the end of the animation.
     */
    fun forward(from: Double? = null): TickerFuture {
        assert {
            if (duration == null) {
                throw FlutterError(
                    "AnimationController.forward() called with no default Duration.\n" +
                            "The \"duration\" property should be set, either in the constructor " +
                            "or later, before calling the forward() function."
                )
            }
            true
        }
        direction = AnimationDirection.FORWARD
        if (from != null)
            value = from
        return animateToInternal(upperBound)
    }

    /**
     * Starts running this animation in reverse (towards the beginning).
     *
     * Returns a [TickerFuture] that completes when the animation is DISMISSED.
     *
     * The most recently returned [TickerFuture], if any, is marked as having been
     * canceled, meaning the future never completes and its [TickerFuture.orCancel]
     * derivative future completes with a [TickerCanceled] error.
     *
     * During the animation, [status] is reported as [AnimationStatus.REVERSE],
     * which switches to [AnimationStatus.DISMISSED] when [lowerBound] is
     * reached at the end of the animation.
     */
    fun reverse(from: Double? = null): TickerFuture {
        assert {
            if (duration == null) {
                throw FlutterError(
                    "AnimationController.reverse() called with no default Duration.\n" +
                            "The \"duration\" property should be set, either in the " +
                            "constructor or later, before calling the reverse() function."
                )
            }
            true
        }
        direction = AnimationDirection.REVERSE
        if (from != null)
            value = from
        return animateToInternal(lowerBound)
    }

    /**
     * Drives the animation from its current value to target.
     *
     * Returns a [TickerFuture] that completes when the animation is complete.
     *
     * The most recently returned [TickerFuture], if any, is marked as having been
     * canceled, meaning the future never completes and its [TickerFuture.orCancel]
     * derivative future completes with a [TickerCanceled] error.
     *
     * During the animation, [status] is reported as [AnimationStatus.FORWARD]
     * regardless of whether `target` > [value] or not. At the end of the
     * animation, when `target` is reached, [status] is reported as
     * [AnimationStatus.COMPLETED].
     */
    fun animateTo(
        target: Double,
        duration: Duration? = null,
        curve: Curve = Curves.linear
    ): TickerFuture {
        direction = AnimationDirection.FORWARD
        return animateToInternal(target, duration, curve)
    }

    private fun animateToInternal(
        target: Double,
        duration: Duration? = null,
        curve: Curve = Curves.linear
    ): TickerFuture {
        var simulationDuration = duration
        if (simulationDuration == null) {
            assert {
                if (this.duration == null) {
                    throw FlutterError(
                        "AnimationController.animateTo() called with no explicit Duration and " +
                                "no default Duration.\n Either the \"duration\" argument to the " +
                                "animateTo() method should be provided, or the \"duration\" " +
                                "property should be set, either in the constructor or later, " +
                                "before calling the animateTo() function."
                    )
                }
                true
            }
            val range = upperBound - lowerBound
            val remainingFraction =
                if (range.isFinite()) (target - _value).absoluteValue / range else 1.0
            simulationDuration = this.duration!! * remainingFraction.toInt()
        } else if (target == value) {
            // Already at target, don't animate.
            simulationDuration = Duration.zero
        }
        stop()
        if (simulationDuration == Duration.zero) {
            if (value != target) {
                _value = target.clamp(lowerBound, upperBound)
                notifyListeners()
            }
            _status = if (direction == AnimationDirection.FORWARD)
                AnimationStatus.COMPLETED else
                AnimationStatus.DISMISSED
            checkStatusChanged()
            return TickerFuture.complete()
        }
        assert(simulationDuration > Duration.zero)
        assert(!isAnimating)
        return startSimulation(
            InterpolationSimulation(
                _value,
                target,
                simulationDuration,
                curve
            )
        )
    }

    /**
     * Starts running this animation in the FORWARD direction, and
     * restarts the animation when it completes.
     *
     * Defaults to repeating between the lower and upper bounds.
     *
     * Returns a [TickerFuture] that never completes. The [TickerFuture.orCancel] future
     * completes with an error when the animation is stopped (e.g. with [stop]).
     *
     * The most recently returned [TickerFuture], if any, is marked as having been
     * canceled, meaning the future never completes and its [TickerFuture.orCancel]
     * derivative future completes with a [TickerCanceled] error.
     */
    fun repeat(min: Double? = null, max: Double? = null, period: Duration? = null): TickerFuture {
        val finalMin = min ?: lowerBound
        val finalMax = max ?: upperBound
        val finalPeriod = period ?: duration
        assert {
            if (finalPeriod == null) {
                throw FlutterError(
                    "AnimationController.repeat() called without an explicit period and with no " +
                            "default Duration.\nEither the \"finalPeriod\" argument to the " +
                            "repeat() method should be provided, or the \"duration\" property " +
                            "should be set, either in the constructor or later, before " +
                            "calling the repeat() function."
                )
            }
            true
        }
        return animateWith(RepeatingSimulation(finalMin, finalMax, finalPeriod!!))
    }

    /**
     * Drives the animation with a critically damped spring (within [lowerBound]
     * and [upperBound]) and initial velocity.
     *
     * If velocity is positive, the animation will complete, otherwise it will
     * dismiss.
     *
     * Returns a [TickerFuture] that completes when the animation is complete.
     *
     * The most recently returned [TickerFuture], if any, is marked as having been
     * canceled, meaning the future never completes and its [TickerFuture.orCancel]
     * derivative future completes with a [TickerCanceled] error.
     */
    fun fling(velocity: Double = 1.0): TickerFuture {
        TODO("Migration|Andrey. Needs SpringSimulation")
//        direction = if (velocity < 0.0) AnimationDirection.REVERSE else AnimationDirection.FORWARD
//        val target = if (velocity < 0.0) lowerBound - _kFlingTolerance.distance
//        else upperBound + _kFlingTolerance.distance
//        val simulation = SpringSimulation(_kFlingSpringDescription, value, target, velocity).apply {
//            tolerance = _kFlingTolerance;
//        }
//        return animateWith(simulation);
    }

    /**
     * Drives the animation according to the given simulation.
     *
     * Returns a [TickerFuture] that completes when the animation is complete.
     *
     * The most recently returned [TickerFuture], if any, is marked as having been
     * canceled, meaning the future never completes and its [TickerFuture.orCancel]
     * derivative future completes with a [TickerCanceled] error.
     */
    fun animateWith(simulation: Simulation): TickerFuture {
        stop()
        return startSimulation(simulation)
    }

    private fun startSimulation(simulation: Simulation): TickerFuture {
        assert(!isAnimating)
        this.simulation = simulation
        lastElapsedDuration = Duration.zero
        _value = simulation.x(0.0).clamp(lowerBound, upperBound)
        val result = ticker!!.start()
        _status = if (direction == AnimationDirection.FORWARD)
            AnimationStatus.FORWARD else
            AnimationStatus.REVERSE
        checkStatusChanged()
        return result
    }

    /**
     * Stops running this animation.
     *
     * This does not trigger any notifications. The animation stops in its
     * current state.
     *
     * By default, the most recently returned [TickerFuture] is marked as having
     * been canceled, meaning the future never completes and its
     * [TickerFuture.orCancel] derivative future completes with a [TickerCanceled]
     * error. By passing the `canceled` argument with the value false, this is
     * reversed, and the futures complete successfully.
     *
     * See also:
     *
     *  * [reset], which stops the animation and resets it to the [lowerBound],
     *    and which does send notifications.
     *  * [forward], [reverse], [animateTo], [animateWith], [fling], and [repeat],
     *    which restart the animation controller.
     */
    fun stop(canceled: Boolean = true) {
        simulation = null
        lastElapsedDuration = null
        ticker?.stop(canceled = canceled)
    }

    /**
     * Release the resources used by this object. The object is no longer usable
     * after this method is called.
     *
     * The most recently returned [TickerFuture], if any, is marked as having been
     * canceled, meaning the future never completes and its [TickerFuture.orCancel]
     * derivative future completes with a [TickerCanceled] error.
     */
    override fun dispose() {
        assert {
            if (ticker == null) {
                throw FlutterError(
                    "AnimationController.dispose() called more than once.\n" +
                            "A given ${runtimeType()} cannot be disposed more than once.\n" +
                            "The following ${runtimeType()} object was disposed multiple times:\n" +
                            "  $this"
                )
            }
            true
        }
        ticker?.dispose()
        ticker = null
        super.dispose()
    }

    private var lastReportedStatus: AnimationStatus = AnimationStatus.DISMISSED

    private fun checkStatusChanged() {
        val newStatus = status
        if (lastReportedStatus != newStatus) {
            lastReportedStatus = newStatus
            notifyStatusListeners(newStatus)
        }
    }

    private fun tick(elapsed: Duration) {
        lastElapsedDuration = elapsed
        val elapsedInSeconds = elapsed.inMicroseconds.toDouble() / Duration.microsecondsPerSecond
        assert(elapsedInSeconds >= 0.0)
        val sim = simulation!!
        _value = sim.x(elapsedInSeconds).clamp(lowerBound, upperBound)
        if (sim.isDone(elapsedInSeconds)) {
            _status = if (direction == AnimationDirection.FORWARD)
                AnimationStatus.COMPLETED else
                AnimationStatus.DISMISSED
            stop(canceled = false)
        }
        notifyListeners()
        checkStatusChanged()
    }

    override fun toStringDetails(): String {
        val paused = if (isAnimating) "" else "; paused"
        val ticker = if (ticker == null) "; DISPOSED" else
            (if (ticker?.muted == true) "; silenced" else "")
        val label = if (debugLabel == null) "" else "; for $debugLabel"
        val more = "${super.toStringDetails()} ${value.toStringAsFixed(3)}"
        return "$more$paused$ticker$label"
    }

    companion object {

        /**
         * Creates an animation controller with no upper or lower bound for its value.
         *
         * * [value] is the initial value of the animation.
         *
         * * [duration] is the length of time this animation should last.
         *
         * * [debugLabel] is a string to help identify this animation during
         *   debugging (used by [toString]).
         *
         * * `vsync` is the [TickerProvider] for the current context. It can be
         *   changed by calling [resync]. It is required and must not be null. See
         *   [TickerProvider] for advice on obtaining a ticker provider.
         *
         * This constructor is most useful for animations that will be driven using a
         * physics simulation, especially when the physics simulation has no
         * pre-determined bounds.
         */
        fun unbounded(
            value: Double = 0.0,
            duration: Duration? = null,
            debugLabel: String? = null,
            vsync: TickerProvider
        ): AnimationController {
            return AnimationController(
                value = value,
                duration = duration,
                debugLabel = debugLabel,
                vsync = vsync,
                lowerBound = Double.NEGATIVE_INFINITY,
                upperBound = Double.POSITIVE_INFINITY
            )
        }
    }
}

private class InterpolationSimulation(
    private val begin: Double,
    private val end: Double,
    duration: Duration,
    private val curve: Curve
) : Simulation() {

    init {
        assert(duration.inMicroseconds > 0)
    }

    private val durationInSeconds =
        duration.inMicroseconds.toDouble() / Duration.microsecondsPerSecond

    override fun x(timeInSeconds: Double): Double {
        val t = (timeInSeconds / durationInSeconds).clamp(0.0, 1.0)
        return when (t) {
            0.0 -> begin
            1.0 -> end
            else -> begin + (end - begin) * curve.transform(t)
        }
    }

    override fun dx(timeInSeconds: Double): Double {
        val epsilon = tolerance.time
        return (x(timeInSeconds + epsilon) -
                x(timeInSeconds - epsilon)) / (2 * epsilon)
    }

    override fun isDone(timeInSeconds: Double) = timeInSeconds >= durationInSeconds
}

private class RepeatingSimulation(
    private val min: Double,
    private val max: Double,
    period: Duration
) : Simulation() {

    private val periodInSeconds =
        period.inMicroseconds.toDouble() / Duration.microsecondsPerSecond

    init {
        assert(periodInSeconds > 0.0)
    }

    override fun x(timeInSeconds: Double): Double {
        assert(timeInSeconds >= 0.0)
        val t = (timeInSeconds / periodInSeconds) % 1.0
        return lerpDouble(min, max, t)
    }

    override fun dx(timeInSeconds: Double): Double = (max - min) / periodInSeconds

    override fun isDone(timeInSeconds: Double) = false
}