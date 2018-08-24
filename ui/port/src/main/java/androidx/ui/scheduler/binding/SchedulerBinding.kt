package androidx.ui.scheduler.binding

import androidx.ui.assert
import androidx.ui.async.Timer
import androidx.ui.core.Duration
import androidx.ui.engine.window.AppLifecycleState
import androidx.ui.engine.window.Window
import androidx.ui.foundation.assertions.FlutterError
import androidx.ui.foundation.assertions.FlutterErrorDetails
import androidx.ui.foundation.assertions.InformationCollector
import androidx.ui.foundation.assertions.debugPrintStack
import androidx.ui.foundation.binding.BindingBase
import androidx.ui.foundation.binding.BindingBaseImpl
import androidx.ui.foundation.debugPrint
import androidx.ui.foundation.profile
import androidx.ui.scheduler.debugPrintBeginFrameBanner
import androidx.ui.scheduler.debugPrintEndFrameBanner
import androidx.ui.scheduler.debugPrintScheduleFrameStacks
import androidx.ui.services.ServicesBinding
import androidx.ui.services.ServicesBindingImpl
import androidx.ui.services.SystemChannels
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong

interface SchedulerBinding : ServicesBinding {
    fun ensureVisualUpdate()
    fun scheduleForcedFrame()
    fun scheduleWarmUpFrame()
    fun addPersistentFrameCallback(callback: FrameCallback)
    fun endOfFrame(): Deferred<Unit>
}

open class SchedulerMixinsWrapper(
    base: BindingBase,
    services: ServicesBinding
) : BindingBase by base, ServicesBinding by services

/**
 * Scheduler for running the following:
 *
 * * _Transient callbacks_, triggered by the system's [Window.onBeginFrame]
 *   callback, for synchronizing the application's behavior to the system's
 *   display. For example, [Ticker]s and [AnimationController]s trigger from
 *   these.
 *
 * * _Persistent callbacks_, triggered by the system's [Window.onDrawFrame]
 *   callback, for updating the system's display after transient callbacks have
 *   executed. For example, the rendering layer uses this to drive its
 *   rendering pipeline.
 *
 * * _Post-frame callbacks_, which are run after persistent callbacks, just
 *   before returning from the [Window.onDrawFrame] callback.
 *
 * * Non-rendering tasks, to be run between frames. These are given a
 *   priority and are executed in priority order according to a
 *   [schedulingStrategy].
 */
object SchedulerBindingImpl : SchedulerMixinsWrapper(
        BindingBaseImpl,
        ServicesBindingImpl
), SchedulerBinding {

    init { // was initInstances()
        launch(Unconfined) {
            Window.onBeginFrame.consumeEach { _handleBeginFrame() }
        }
        launch(Unconfined) {
            Window.onDrawFrame.consumeEach { _handleDrawFrame() }
        }
        launch(Unconfined) {
            SystemChannels.lifecycle.consumeEach { handleAppLifecycleStateChanged(it) }
        }
    }

    init { // was  initServiceExtensions()
        // TODO(Migration/Andrey): Debug logic. Ignore for now
//        registerNumericServiceExtension()
//        registerNumericServiceExtension(
//                name: 'timeDilation',
//        getter: () async => timeDilation,
//        setter: (double value) async {
//            timeDilation = value;
//        }
    }

/**
     * Whether the application is visible, and if so, whether it is currently
     * interactive.
     *
     * This is set by [handleAppLifecycleStateChanged] when the
     * [SystemChannels.lifecycle] notification is dispatched.
     *
     * The preferred way to watch for changes to this value is using
     * [WidgetsBindingObserver.didChangeAppLifecycleState].
 */
    var lifecycleState: AppLifecycleState? = null
        private set

    /**
     * Called when the application lifecycle state changes.
     *
     * Notifies all the observers using
     * [WidgetsBindingObserver.didChangeAppLifecycleState].
     *
     * This method exposes notifications from [SystemChannels.lifecycle].
     */
    private fun handleAppLifecycleStateChanged(state: AppLifecycleState) {
        assert(state != null)
        lifecycleState = state
        when (state) {
            AppLifecycleState.RESUMED, AppLifecycleState.INACTIVE -> setFramesEnabledState(true)
            AppLifecycleState.PAUSED, AppLifecycleState.SUSPENDING -> setFramesEnabledState(false)
        }
    }

// /// The default [SchedulingStrategy] for [SchedulerBinding.schedulingStrategy].
// ///
// /// If there are any frame callbacks registered, only runs tasks with
// /// a [Priority] of [Priority.animation] or higher. Otherwise, runs
// /// all tasks.
//    bool defaultSchedulingStrategy({ int priority, SchedulerBinding scheduler }) {
//        if (scheduler.transientCallbackCount > 0)
//            return priority >= Priority.animation.value;
//        return true;
//    }
//
//    /// The strategy to use when deciding whether to run a task or not.
//    ///
//    /// Defaults to [defaultSchedulingStrategy].
//    SchedulingStrategy schedulingStrategy = defaultSchedulingStrategy;
//
//    fun _taskSorter (e1 : _TaskEntry, e2 : _TaskEntry) : Int {
//        return -e1.priority.compareTo(e2.priority);
//    }
//    final PriorityQueue<_TaskEntry<dynamic>> _taskQueue = new HeapPriorityQueue<_TaskEntry<dynamic>>(_taskSorter);
//
//    /// Schedules the given `task` with the given `priority` and returns a
//    /// [Future] that completes to the `task`'s eventual return value.
//    ///
//    /// The `debugLabel` and `flow` are used to report the task to the [Timeline],
//    /// for use when profiling.
//    ///
//    /// ## Processing model
//    ///
//    /// Tasks will be executed between frames, in priority order,
//    /// excluding tasks that are skipped by the current
//    /// [schedulingStrategy]. Tasks should be short (as in, up to a
//    /// millisecond), so as to not cause the regular frame callbacks to
//    /// get delayed.
//    ///
//    /// If an animation is running, including, for instance, a [ProgressIndicator]
//    /// indicating that there are pending tasks, then tasks with a priority below
//    /// [Priority.animation] won't run (at least, not with the
//    /// [defaultSchedulingStrategy]; this can be configured using
//    /// [schedulingStrategy]).
//    fun <T> scheduleTask(
//            task: TaskCallback<T>,
//            priority: Priority,
//            debugLabel: String? = null
// //        , Flow flow,
//    ): Deferred<T> {
//        final bool isFirstTask = _taskQueue.isEmpty;
//        final _TaskEntry<T> entry = new _TaskEntry<T>(
//                task,
//                priority.value,
//                debugLabel,
//                flow,
//                );
//        _taskQueue.add(entry);
//        if (isFirstTask && !locked)
//            _ensureEventLoopCallback();
//        return entry.completer.future;
//    }

//    override fun unlocked() {
//        super.unlocked()
//        if (_taskQueue.isNotEmpty)
//            _ensureEventLoopCallback()
//    }

    // Whether this scheduler already requested to be called from the event loop.
    private var _hasRequestedAnEventLoopCallback: Boolean = false

    // Ensures that the scheduler services a task scheduled by [scheduleTask].
    private fun _ensureEventLoopCallback() {
        TODO()
//        assert(!locked)
//        assert(_taskQueue.isNotEmpty)
//        if (_hasRequestedAnEventLoopCallback)
//            return
//        _hasRequestedAnEventLoopCallback = true
//        Timer.run(_runTasks)
    }

    // Scheduled by _ensureEventLoopCallback.
    private fun _runTasks() {
        _hasRequestedAnEventLoopCallback = false
        if (handleEventLoopCallback())
            _ensureEventLoopCallback() // runs next task when there's time
    }

    /**
     * Execute the highest-priority task, if it is of a high enough priority.
     *
     * Returns true if a task was executed and there are other tasks remaining
     * (even if they are not high-enough priority).
     *
     * Returns false if no task was executed, which can occur if there are no
     * tasks scheduled, if the scheduler is [locked], or if the highest-priority
     * task is of too low a priority given the current [schedulingStrategy].
     *
     * Also returns false if there are no tasks remaining.
     */
//    @visibleForTesting
    fun handleEventLoopCallback(): Boolean {
        TODO()
//        if (_taskQueue.isEmpty || locked)
//            return false;
//        final _TaskEntry<dynamic> entry = _taskQueue.first;
//        if (schedulingStrategy(priority: entry.priority, scheduler: this)) {
//            try {
//                _taskQueue.removeFirst();
//                entry.run();
//            } catch (exception, exceptionStack) {
//            StackTrace callbackStack;
//            assert(() {
//                callbackStack = entry.debugStack;
//                return true;
//            }());
//            FlutterError.reportError(new FlutterErrorDetails(
//                    exception: exception,
//                    stack: exceptionStack,
//                    library: 'scheduler library',
//            context: 'during a task callback',
//            informationCollector: (callbackStack == null) ? null : (StringBuffer information) {
//            information.writeln(
//                    '\nThis exception was thrown in the context of a task callback. '
//                    'When the task callback was _registered_ (as opposed to when the '
//            'exception was thrown), this was the stack:'
//            );
//            FlutterError.defaultStackFilter(callbackStack.toString().trimRight().split('\n')).forEach(information.writeln);
//        }
//            ));
//        }
//            return _taskQueue.isNotEmpty;
//        }
//        return false;
    }

    private var _nextFrameCallbackId: Int = 0 // positive
    private var _transientCallbacks = mutableMapOf<Int, _FrameCallbackEntry>()
    private val _removedIds = mutableSetOf<Int>()

    /**
     * The current number of transient frame callbacks scheduled.
     *
     * This is reset to zero just before all the currently scheduled
     * transient callbacks are called, at the start of a frame.
     *
     * This number is primarily exposed so that tests can verify that
     * there are no unexpected transient callbacks still registered
     * after a test's resources have been gracefully disposed.
     */
    val transientCallbackCount: Int get() = _transientCallbacks.size

    /**
     * Schedules the given transient frame callback.
     *
     * Adds the given callback to the list of frame callbacks and ensures that a
     * frame is scheduled.
     *
     * If this is a one-off registration, ignore the `rescheduling` argument.
     *
     * If this is a callback that will be reregistered each time it fires, then
     * when you reregister the callback, set the `rescheduling` argument to true.
     * This has no effect in release builds, but in debug builds, it ensures that
     * the stack trace that is stored for this callback is the original stack
     * trace for when the callback was _first_ registered, rather than the stack
     * trace for when the callback is reregistered. This makes it easier to track
     * down the original reason that a particular callback was called. If
     * `rescheduling` is true, the call must be in the context of a frame
     * callback.
     *
     * Callbacks registered with this method can be canceled using
     * [cancelFrameCallbackWithId].
     */
    fun scheduleFrameCallback(callback: FrameCallback, rescheduling: Boolean = false): Int {
        scheduleFrame()
        _nextFrameCallbackId += 1
        _transientCallbacks[_nextFrameCallbackId] =
                _FrameCallbackEntry(callback, rescheduling = rescheduling)
        return _nextFrameCallbackId
    }

    /**
     * Cancels the transient frame callback with the given [id].
     *
     * Removes the given callback from the list of frame callbacks. If a frame
     * has been requested, this does not also cancel that request.
     *
     * Transient frame callbacks are those registered using
     * [scheduleFrameCallback].
     */
    fun cancelFrameCallbackWithId(id: Int) {
        assert(id > 0)
        _transientCallbacks.remove(id)
        _removedIds.add(id)
    }

    /**
     * Asserts that there are no registered transient callbacks; if
     * there are, prints their locations and throws an exception.
     *
     * A transient frame callback is one that was registered with
     * [scheduleFrameCallback].
     *
     * This is expected to be called at the end of tests (the
     * flutter_test framework does it automatically in normal cases).
     *
     * Call this method when you expect there to be no transient
     * callbacks registered, in an assert statement with a message that
     * you want printed when a transient callback is registered:
     *
     * ```dart
     * assert(SchedulerBinding.instance.debugAssertNoTransientCallbacks(
     *   'A leak of transient callbacks was detected while doing foo.'
     * ));
     * ```
     *
     * Does nothing if asserts are disabled. Always returns true.
     */
    fun debugAssertNoTransientCallbacks(reason: String): Boolean {
        assert {
            if (transientCallbackCount > 0) {
                // We cache the values so that we can produce them later
                // even if the information collector is called after
                // the problem has been resolved.
                val count = transientCallbackCount
                val callbacks = HashMap<Int, _FrameCallbackEntry>(_transientCallbacks)
                FlutterError.reportError(FlutterErrorDetails(
                        exception = reason,
                        library = "scheduler library",
                        informationCollector = { information ->
                            if (count == 1) {
                                information.append("There was one transient callback left. " +
                                        "The stack trace for when it was" +
                                        " registered is as follows:\n"
                                )
                            } else {
                                information.append("There were $count transient callbacks left. " +
                                        "The stack traces for when they were " +
                                        "registered are as follows:\n"
                                )
                            }
                            for (id in callbacks.keys) {
                                val entry = callbacks[id]
                                information.append("── callback $id ──\n")
                                FlutterError.defaultStackFilter(entry!!.debugStack
                                        .toString()
                                        .trimEnd()
                                        .split("\n")
                                ).forEach {
                                    information.append("$it\n")
                                }
                            }
                        }
                ))
            }
            true
        }
        return true
    }

//    /// Prints the stack for where the current transient callback was registered.
//    ///
//    /// A transient frame callback is one that was registered with
//    /// [scheduleFrameCallback].
//    ///
//    /// When called in debug more and in the context of a transient callback, this
//    /// function prints the stack trace from where the current transient callback
//    /// was registered (i.e. where it first called [scheduleFrameCallback]).
//    ///
//    /// When called in debug mode in other contexts, it prints a message saying
//    /// that this function was not called in the context a transient callback.
//    ///
//    /// In release mode, this function does nothing.
//    ///
//    /// To call this function, use the following code:
//    ///
//    /// ```dart
//    ///   SchedulerBinding.debugPrintTransientCallbackRegistrationStack();
//    /// ```
//    static void debugPrintTransientCallbackRegistrationStack() {
//        assert(() {
//            if (_FrameCallbackEntry.debugCurrentCallbackStack != null) {
//                debugPrint('When the current transient callback was registered, this was the stack:');
//                debugPrint(
//                        FlutterError.defaultStackFilter(
//                                _FrameCallbackEntry.debugCurrentCallbackStack.toString().trimRight().split('\n')
//                        ).join('\n')
//                );
//            } else {
//                debugPrint('No transient callback is currently executing.');
//            }
//            return true;
//        }());
//    }

    private val _persistentCallbacks = mutableListOf<FrameCallback>()

    /**
     * Adds a persistent frame callback.
     *
     * Persistent callbacks are called after transient
     * (non-persistent) frame callbacks.
     *
     * Does *not* request a new frame. Conceptually, persistent frame
     * callbacks are observers of "begin frame" events. Since they are
     * executed after the transient frame callbacks they can drive the
     * rendering pipeline.
     *
     * Persistent frame callbacks cannot be unregistered. Once registered, they
     * are called for every frame for the lifetime of the application.
     */
    override fun addPersistentFrameCallback(callback: FrameCallback) {
        _persistentCallbacks.add(callback)
    }

    private val _postFrameCallbacks = mutableListOf<FrameCallback>()

    /**
     * Schedule a callback for the end of this frame.
     *
     * Does *not* request a new frame.
     *
     * This callback is run during a frame, just after the persistent
     * frame callbacks (which is when the main rendering pipeline has
     * been flushed). If a frame is in progress and post-frame
     * callbacks haven't been executed yet, then the registered
     * callback is still executed during the frame. Otherwise, the
     * registered callback is executed during the next frame.
     *
     * The callbacks are executed in the order in which they have been
     * added.
     *
     * Post-frame callbacks cannot be unregistered. They are called exactly once.
     *
     * See also:
     *
     *  * [scheduleFrameCallback], which registers a callback for the start of
     *    the next frame.
     */
    fun addPostFrameCallback(callback: FrameCallback) {
        _postFrameCallbacks.add(callback)
    }

    /**
     * Returns a Future that completes after the frame completes.
     *
     * If this is called between frames, a frame is immediately scheduled if
     * necessary. If this is called during a frame, the Future completes after
     * the current frame.
     *
     * If the device's screen is currently turned off, this may wait a very long
     * time, since frames are not scheduled while the device's screen is turned
     * off.
     */
    override fun endOfFrame(): Deferred<Unit> {
        val deferred = CompletableDeferred<Unit>()

        if (schedulerPhase == SchedulerPhase.idle) {
            scheduleFrame()
        }
        addPostFrameCallback {
            deferred.complete(Unit)
        }

        return deferred
    }

    /**
     * Whether this scheduler has requested that [handleBeginFrame] be called soon.
     */
    internal var hasScheduledFrame: Boolean = false
    private set

    /**
     * The phase that the scheduler is currently operating under.
     */
    internal var schedulerPhase: SchedulerPhase = SchedulerPhase.idle
    private set

    /**
     * Whether frames are currently being scheduled when [scheduleFrame] is called.
     *
     * This value depends on the value of the [lifecycleState].
     */
    internal var framesEnabled: Boolean = true
        private set

    private fun setFramesEnabledState(enabled: Boolean) {
        if (framesEnabled == enabled)
            return
        framesEnabled = enabled
        if (enabled)
            scheduleFrame()
    }

    /**
     * Schedules a new frame using [scheduleFrame] if this object is not
     * currently producing a frame.
     *
     * Calling this method ensures that [handleDrawFrame] will eventually be
     * called, unless it's already in progress.
     *
     * This has no effect if [schedulerPhase] is
     * [SchedulerPhase.transientCallbacks] or [SchedulerPhase.midFrameMicrotasks]
     * (because a frame is already being prepared in that case), or
     * [SchedulerPhase.persistentCallbacks] (because a frame is actively being
     * rendered in that case). It will schedule a frame if the [schedulerPhase]
     * is [SchedulerPhase.idle] (in between frames) or
     * [SchedulerPhase.postFrameCallbacks] (after a frame).
     */
    override fun ensureVisualUpdate() {
        when (schedulerPhase) {
            SchedulerPhase.idle, SchedulerPhase.postFrameCallbacks -> {
                scheduleFrame()
                return
            }
            SchedulerPhase.transientCallbacks,
            SchedulerPhase.midFrameMicrotasks,
            SchedulerPhase.persistentCallbacks -> return
        }
    }

    /**
     * If necessary, schedules a new frame by calling
     * [Window.scheduleFrame].
     *
     * After this is called, the engine will (eventually) call
     * [handleBeginFrame]. (This call might be delayed, e.g. if the device's
     * screen is turned off it will typically be delayed until the screen is on
     * and the application is visible.) Calling this during a frame forces
     * another frame to be scheduled, even if the current frame has not yet
     * completed.
     *
     * Scheduled frames are serviced when triggered by a "Vsync" signal provided
     * by the operating system. The "Vsync" signal, or vertical synchronization
     * signal, was historically related to the display refresh, at a time when
     * hardware physically moved a beam of electrons vertically between updates
     * of the display. The operation of contemporary hardware is somewhat more
     * subtle and complicated, but the conceptual "Vsync" refresh signal continue
     * to be used to indicate when applications should update their rendering.
     *
     * To have a stack trace printed to the console any time this function
     * schedules a frame, set [debugPrintScheduleFrameStacks] to true.
     *
     * See also:
     *
     *  * [scheduleForcedFrame], which ignores the [lifecycleState] when
     *    scheduling a frame.
     *  * [scheduleWarmUpFrame], which ignores the "Vsync" signal entirely and
     *    triggers a frame immediately.
     */
    fun scheduleFrame() {
        if (hasScheduledFrame || !framesEnabled)
            return
        assert {
            if (debugPrintScheduleFrameStacks)
                debugPrintStack(label = "scheduleFrame() called. Current phase is $schedulerPhase.")
            true
        }
        Window.scheduleFrame()
        hasScheduledFrame = true
    }

    /**
     * Schedules a new frame by calling [Window.scheduleFrame].
     *
     * After this is called, the engine will call [handleBeginFrame], even if
     * frames would normally not be scheduled by [scheduleFrame] (e.g. even if
     * the device's screen is turned off).
     *
     * The framework uses this to force a frame to be rendered at the correct
     * size when the phone is rotated, so that a correctly-sized rendering is
     * available when the screen is turned back on.
     *
     * To have a stack trace printed to the console any time this function
     * schedules a frame, set [debugPrintScheduleFrameStacks] to true.
     *
     * Prefer using [scheduleFrame] unless it is imperative that a frame be
     * scheduled immediately, since using [scheduleForceFrame] will cause
     * significantly higher battery usage when the device should be idle.
     *
     * Consider using [scheduleWarmUpFrame] instead if the goal is to update the
     * rendering as soon as possible (e.g. at application startup).
     */
    override fun scheduleForcedFrame() {
        if (hasScheduledFrame)
            return
        assert {
            if (debugPrintScheduleFrameStacks)
                debugPrintStack(label = "scheduleForcedFrame() called. " +
                        "Current phase is $schedulerPhase.")
            true
        }
        Window.scheduleFrame()
        hasScheduledFrame = true
    }

    private var _warmUpFrame: Boolean = false

    /**
     * Schedule a frame to run as soon as possible, rather than waiting for
     * the engine to request a frame in response to a system "Vsync" signal.
     *
     * This is used during application startup so that the first frame (which is
     * likely to be quite expensive) gets a few extra milliseconds to run.
     *
     * Locks events dispatching until the scheduled frame has completed.
     *
     * If a frame has already been scheduled with [scheduleFrame] or
     * [scheduleForcedFrame], this call may delay that frame.
     *
     * If any scheduled frame has already begun or if another
     * [scheduleWarmUpFrame] was already called, this call will be ignored.
     *
     * Prefer [scheduleFrame] to update the display in normal operation.
     */
    override fun scheduleWarmUpFrame() {
        if (_warmUpFrame || schedulerPhase != SchedulerPhase.idle)
            return

        _warmUpFrame = true
        // TODO(Migration/Andrey): port Timeline
//        Timeline.startSync('Warm-up frame');
        val hadScheduledFrame = hasScheduledFrame
        // We use timers here to ensure that microtasks flush in between.
        Timer.run {
            assert(_warmUpFrame)
            handleBeginFrame(null)
        }
        Timer.run {
            assert(_warmUpFrame)
            handleDrawFrame()
            // We call resetEpoch after this frame so that, in the hot reload case,
            // the very next frame pretends to have occurred immediately after this
            // warm-up frame. The warm-up frame's timestamp will typically be far in
            // the past (the time of the last real frame), so if we didn't reset the
            // epoch we would see a sudden jump from the old time in the warm-up frame
            // to the new time in the "real" frame. The biggest problem with this is
            // that implicit animations end up being triggered at the old time and
            // then skipping every frame and finishing in the new time.
            resetEpoch()
            _warmUpFrame = false
            if (hadScheduledFrame)
                scheduleFrame()
        }

        // Lock events so touch events etc don't insert themselves until the
        // scheduled frame has finished.
        launch(Unconfined) {
            lockEvents {
                endOfFrame()
                // TODO(Migration/Andrey): port Timeline
//                Timeline.finishSync();
            }.await()
        }
    }

    private var _firstRawTimeStampInEpoch: Duration? = null
    private var _epochStart = Duration.zero
    private var _lastRawTimeStamp = Duration.zero

    /**
     * Prepares the scheduler for a non-monotonic change to how time stamps are
     * calculated.
     *
     * Callbacks received from the scheduler assume that their time stamps are
     * monotonically increasing. The raw time stamp passed to [handleBeginFrame]
     * is monotonic, but the scheduler might adjust those time stamps to provide
     * [timeDilation]. Without careful handling, these adjusts could cause time
     * to appear to run backwards.
     *
     * The [resetEpoch] function ensures that the time stamps are monotonic by
     * resetting the base time stamp used for future time stamp adjustments to the
     * current value. For example, if the [timeDilation] decreases, rather than
     * scaling down the [Duration] since the beginning of time, [resetEpoch] will
     * ensure that we only scale down the duration since [resetEpoch] was called.
     *
     * Setting [timeDilation] calls [resetEpoch] automatically. You don't need to
     * call [resetEpoch] yourself.
     */
    fun resetEpoch() {
        _epochStart = _adjustForEpoch(_lastRawTimeStamp)
        _firstRawTimeStampInEpoch = null
    }

    // TODO(Migration/Andrey): Used to slowdown animations by this factor. Hardcode for now
    private var timeDilation = 1.0

    /**
     * Adjusts the given time stamp into the current epoch.
     *
     * This both offsets the time stamp to account for when the epoch started
     * (both in raw time and in the epoch's own time line) and scales the time
     * stamp to reflect the time dilation in the current epoch.
     *
     * These mechanisms together combine to ensure that the durations we give
     * during frame callbacks are monotonically increasing.
     */
    private fun _adjustForEpoch(rawTimeStamp: Duration): Duration {
        val rawDurationSinceEpoch =
                if (_firstRawTimeStampInEpoch == null) Duration.zero
        else rawTimeStamp - _firstRawTimeStampInEpoch!!
        return Duration(
                (rawDurationSinceEpoch.inMicroseconds / timeDilation).roundToLong() +
                _epochStart.inMicroseconds)
    }

    /**
     * The time stamp for the frame currently being processed.
     *
     * This is only valid while [handleBeginFrame] is running, i.e. while a frame
     * is being produced.
     */
    var _currentFrameTimeStamp: Duration? = null

    val currentFrameTimeStamp: Duration get() {
        assert(_currentFrameTimeStamp != null)
        return _currentFrameTimeStamp!!
    }

    private var _profileFrameNumber = 0

    // TODO(Migration/Andrey): Needs StopWatch
//    final Stopwatch _profileFrameStopwatch = new Stopwatch();

    private var _debugBanner: String? = null
    private var _ignoreNextEngineDrawFrame = false

    private fun _handleBeginFrame(rawTimeStamp: Duration? = null) {
        if (_warmUpFrame) {
            assert(!_ignoreNextEngineDrawFrame)
            _ignoreNextEngineDrawFrame = true
            return
        }
        handleBeginFrame(rawTimeStamp)
    }

    private fun _handleDrawFrame() {
        if (_ignoreNextEngineDrawFrame) {
            _ignoreNextEngineDrawFrame = false
            return
        }
        handleDrawFrame()
    }

    /**
     * Called by the engine to prepare the framework to produce a new frame.
     *
     * This function calls all the transient frame callbacks registered by
     * [scheduleFrameCallback]. It then returns, any scheduled microtasks are run
     * (e.g. handlers for any [Future]s resolved by transient frame callbacks),
     * and [handleDrawFrame] is called to continue the frame.
     *
     * If the given time stamp is null, the time stamp from the last frame is
     * reused.
     *
     * To have a banner shown at the start of every frame in debug mode, set
     * [debugPrintBeginFrameBanner] to true. The banner will be printed to the
     * console using [debugPrint] and will contain the frame number (which
     * increments by one for each frame), and the time stamp of the frame. If the
     * given time stamp was null, then the string "warm-up frame" is shown
     * instead of the time stamp. This allows frames eagerly pushed by the
     * framework to be distinguished from those requested by the engine in
     * response to the "Vsync" signal from the operating system.
     *
     * You can also show a banner at the end of every frame by setting
     * [debugPrintEndFrameBanner] to true. This allows you to distinguish log
     * statements printed during a frame from those printed between frames (e.g.
     * in response to events or timers).
     */
    fun handleBeginFrame(rawTimeStamp: Duration?) {
        // TODO(Migration/Andrey): Needs Timeline
//        Timeline.startSync('Frame', arguments: timelineWhitelistArguments);
        _firstRawTimeStampInEpoch = _firstRawTimeStampInEpoch ?: rawTimeStamp
        _currentFrameTimeStamp = _adjustForEpoch(rawTimeStamp ?: _lastRawTimeStamp)
        if (rawTimeStamp != null)
            _lastRawTimeStamp = rawTimeStamp

        profile {
            _profileFrameNumber += 1
            // TODO(Migration/Andrey): Needs StopWatch
//            _profileFrameStopwatch.reset();
//            _profileFrameStopwatch.start();
        }

        assert {
            if (debugPrintBeginFrameBanner || debugPrintEndFrameBanner) {
                val frameTimeStampDescription = StringBuffer()
                if (rawTimeStamp != null) {
                    _debugDescribeTimeStamp(currentFrameTimeStamp, frameTimeStampDescription)
                } else {
                    frameTimeStampDescription.append("(warm-up frame)")
                }
                _debugBanner =
                        "▄▄▄▄▄▄▄▄ Frame ${_profileFrameNumber.toString().padEnd(7)}   " +
                        "${frameTimeStampDescription.toString().padEnd(18)} ▄▄▄▄▄▄▄▄"
                if (debugPrintBeginFrameBanner)
                    debugPrint(_debugBanner!!)
            }
            true
        }

        assert(schedulerPhase == SchedulerPhase.idle)
        hasScheduledFrame = false
        try {
            // TRANSIENT FRAME CALLBACKS
            // TODO(Migration/Andrey): Needs Timeline
//            Timeline.startSync('Animate', arguments: timelineWhitelistArguments);
            schedulerPhase = SchedulerPhase.transientCallbacks
            val callbacks = _transientCallbacks
            _transientCallbacks = mutableMapOf()
            callbacks.entries.forEach {
                if (!_removedIds.contains(it.key))
                    _invokeFrameCallback(it.value.callback,
                        currentFrameTimeStamp,
                        it.value.debugStack
                    )
            }
            _removedIds.clear()
        } finally {
            schedulerPhase = SchedulerPhase.midFrameMicrotasks
        }
    }

    /**
     * Called by the engine to produce a new frame.
     *
     * This method is called immediately after [handleBeginFrame]. It calls all
     * the callbacks registered by [addPersistentFrameCallback], which typically
     * drive the rendering pipeline, and then calls the callbacks registered by
     * [addPostFrameCallback].
     *
     * See [handleBeginFrame] for a discussion about debugging hooks that may be
     * useful when working with frame callbacks.
     */
    fun handleDrawFrame() {
        assert(schedulerPhase == SchedulerPhase.midFrameMicrotasks)
        // TODO(Migration/Andrey): Needs Timeline
//        Timeline.finishSync(); // end the "Animate" phase
        try {
            // PERSISTENT FRAME CALLBACKS
            schedulerPhase = SchedulerPhase.persistentCallbacks
            _persistentCallbacks.forEach { callback ->
                _invokeFrameCallback(callback, currentFrameTimeStamp)
            }
            // POST-FRAME CALLBACKS
            schedulerPhase = SchedulerPhase.postFrameCallbacks
            val localPostFrameCallbacks = _postFrameCallbacks.toList()
            _postFrameCallbacks.clear()
            localPostFrameCallbacks.forEach { callback ->
                _invokeFrameCallback(callback, currentFrameTimeStamp)
            }
        } finally {
            schedulerPhase = SchedulerPhase.idle
            // TODO(Migration/Andrey): Needs Timeline
//            Timeline.finishSync(); // end the Frame
            profile {
                // TODO(Migration/Andrey): Needs StopWatch
//                _profileFrameStopwatch.stop();
                _profileFramePostEvent()
            }
            assert {
                if (debugPrintEndFrameBanner)
                    for (i in 0 until _debugBanner!!.length) {
                        debugPrint("▀")
                    }
                _debugBanner = null
                true
            }
            _currentFrameTimeStamp = null
        }
    }

    private fun _profileFramePostEvent() {
        // TODO(Migration/Andrey): Needs postEvent
//        postEvent('Flutter.Frame', <String, dynamic>{
//            'number': _profileFrameNumber,
//            'startTime': _currentFrameTimeStamp.inMicroseconds,
//            'elapsed': _profileFrameStopwatch.elapsedMicroseconds
//        });
    }

    private fun _debugDescribeTimeStamp(timeStamp: Duration, buffer: StringBuffer) {
        if (timeStamp.inDays > 0)
            buffer.append("${timeStamp.inDays}d ")
        if (timeStamp.inHours > 0)
            buffer.append("${timeStamp.inHours - TimeUnit.DAYS.toHours(timeStamp.inDays)}h ")
        if (timeStamp.inMinutes > 0)
            buffer.append("${timeStamp.inMinutes - TimeUnit.HOURS.toMinutes(timeStamp.inHours)}m ")
        if (timeStamp.inSeconds > 0)
            buffer.append("${timeStamp.inSeconds -
                    TimeUnit.MINUTES.toSeconds(timeStamp.inMinutes)}s ")
        buffer.append("${timeStamp.inMilliseconds -
                TimeUnit.SECONDS.toMillis(timeStamp.inSeconds)}")
        val microseconds = timeStamp.inMicroseconds -
                TimeUnit.MILLISECONDS.toMicros(timeStamp.inMilliseconds)
        if (microseconds > 0)
            buffer.append(".${microseconds.toString().padEnd(3, '0')}")
        buffer.append("ms")
    }

    // Calls the given [callback] with [timestamp] as argument.
    //
    // Wraps the callback in a try/catch and forwards any error to
    // [debugSchedulerExceptionHandler], if set. If not set, then simply prints
    // the error.
    private fun _invokeFrameCallback(
        callback: FrameCallback,
        timeStamp: Duration,
        callbackStack: Any? = null/*StackTrace*/
    ) {
        assert(callback != null)
        assert(_FrameCallbackEntry.debugCurrentCallbackStack == null)
        assert { _FrameCallbackEntry.debugCurrentCallbackStack = callbackStack; true; }
        try {
            callback(timeStamp)
        } catch (exception: Exception) {
            val informationCollector: InformationCollector? =
                    if (callbackStack == null) null
                    else { information ->
                        information.append("\nThis exception was thrown in the context " +
                                "of a scheduler callback. When the scheduler callback was " +
                                "_registered_ (as opposed to when the exception was thrown), " +
                                "this was the stack:\n")
                        FlutterError.defaultStackFilter(
                                callbackStack
                                        .toString()
                                        .trimEnd()
                                        .split("\n")
                        ).forEach {
                            information.append("$it\n")
                        }
                    }
            FlutterError.reportError(FlutterErrorDetails(
                    exception = exception,
                    library = "scheduler library",
                    context = "during a scheduler callback",
                    informationCollector = informationCollector
            ))
        }
        assert { _FrameCallbackEntry.debugCurrentCallbackStack = null; true; }
    }
}