/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.platform

import android.os.Looper
import android.view.Choreographer
import androidx.collection.LongList
import androidx.collection.emptyLongList
import androidx.collection.longListOf
import androidx.compose.runtime.MonotonicFrameClock
import androidx.core.os.HandlerCompat
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * A [CoroutineDispatcher] that will perform dispatch during a [handler] callback or
 * [choreographer]'s animation frame stage, whichever comes first. Use [Main] to obtain a dispatcher
 * for the process's main thread (i.e. the activity thread) or [CurrentThread] to obtain a
 * dispatcher for the current thread.
 */
// Implementation note: the constructor is private to direct users toward the companion object
// accessors for the main/current threads. A choreographer must be obtained from its current
// thread as per the only public API surface for obtaining one as of this writing, and the
// choreographer and handler must match. Constructing an AndroidUiDispatcher with a handler
// not marked as async will adversely affect dispatch behavior but not to the point of
// incorrectness; more operations would be deferred to the choreographer frame as racing handler
// messages would wait behind a frame barrier.
public class AndroidUiDispatcher
private constructor(
    public val choreographer: Choreographer,
    private val handler: android.os.Handler,
) : CoroutineDispatcher() {

    // Guards all properties in this class
    private val lock = Any()

    private val toRunTrampolined = ArrayDeque<Runnable>()
    private var toRunOnFrame = mutableListOf<Choreographer.FrameCallback>()
    private var spareToRunOnFrame = mutableListOf<Choreographer.FrameCallback>()
    private var scheduledTrampolineDispatch = false
    private var scheduledFrameDispatch = false

    private val dispatchCallback =
        object : Choreographer.FrameCallback, Runnable {
            override fun run() {
                val tracer = AndroidUiDispatcher.tracer
                val isEnabled = tracer?.isEnabled() == true
                if (isEnabled) {
                    tracer!!.beginSection("UiDispatcher:HandlerRun", emptyLongList())
                }
                try {
                    performTrampolineDispatch()
                } finally {
                    if (isEnabled) {
                        tracer!!.endSection()
                    }
                }
                synchronized(lock) {
                    if (toRunOnFrame.isEmpty()) {
                        choreographer.removeFrameCallback(this)
                        scheduledFrameDispatch = false
                    }
                }
            }

            override fun doFrame(frameTimeNanos: Long) {
                handler.removeCallbacks(this)

                val tracer = AndroidUiDispatcher.tracer
                val isEnabled = tracer?.isEnabled() == true
                if (isEnabled) {
                    tracer!!.beginSection("UiDispatcher:ChoreographerFrame", emptyLongList())
                }
                try {
                    performTrampolineDispatch()
                } finally {
                    if (isEnabled) {
                        tracer!!.endSection()
                    }
                }

                performFrameDispatch(frameTimeNanos)
            }
        }

    private fun nextTask(): Runnable? {
        val tracer = AndroidUiDispatcher.tracer
        val isEnabled = tracer?.isEnabled() == true
        return synchronized(lock) {
            val task = toRunTrampolined.removeFirstOrNull()
            if (isEnabled) {
                tracer!!.counter("UiDispatcher:QueueSize", toRunTrampolined.size.toLong())
            }
            task
        }
    }

    private fun performTrampolineDispatch() {
        val tracer = AndroidUiDispatcher.tracer
        val isEnabled = tracer?.isEnabled() == true

        do {
            var task = nextTask()
            while (task != null) {
                if (isEnabled) {
                    val flowId = System.identityHashCode(task).toLong()
                    tracer!!.beginSection("UiDispatcher:UiTask", longListOf(flowId))
                }
                try {
                    task.run()
                } finally {
                    if (isEnabled) {
                        tracer!!.endSection()
                    }
                }
                task = nextTask()
            }
        } while (
            // We don't dispatch holding the lock so that other tasks can get in on our
            // trampolining time slice, but once we're done, make sure nothing added a new task
            // before we set scheduledDispatch = false, which would prevent the next dispatch
            // from being correctly scheduled. Loop to run these stragglers now.
            synchronized(lock) {
                if (toRunTrampolined.isEmpty()) {
                    scheduledTrampolineDispatch = false
                    false
                } else true
            }
        )
    }

    private fun performFrameDispatch(frameTimeNanos: Long) {
        val toRun =
            synchronized(lock) {
                if (!scheduledFrameDispatch) return
                scheduledFrameDispatch = false
                val result = toRunOnFrame
                toRunOnFrame = spareToRunOnFrame
                spareToRunOnFrame = result
                result
            }
        for (i in 0 until toRun.size) {
            // This callback will not and must not throw, see AndroidUiFrameClock
            toRun[i].doFrame(frameTimeNanos)
        }
        toRun.clear()
    }

    internal fun postFrameCallback(callback: Choreographer.FrameCallback) {
        synchronized(lock) {
            toRunOnFrame.add(callback)
            if (!scheduledFrameDispatch) {
                scheduledFrameDispatch = true
                choreographer.postFrameCallback(dispatchCallback)
            }
        }
    }

    internal fun removeFrameCallback(callback: Choreographer.FrameCallback) {
        synchronized(lock) { toRunOnFrame.remove(callback) }
    }

    /**
     * A [MonotonicFrameClock] associated with this [AndroidUiDispatcher]'s [choreographer] that may
     * be used to await [Choreographer] frame dispatch.
     */
    public val frameClock: MonotonicFrameClock = AndroidUiFrameClock(choreographer, this)

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        val tracer = AndroidUiDispatcher.tracer
        val isEnabled = tracer?.isEnabled() == true

        if (isEnabled) {
            val flowId = System.identityHashCode(block).toLong()
            tracer!!.instant("UiDispatcher:Queued", longListOf(flowId))
        }

        synchronized(lock) {
            toRunTrampolined.addLast(block)
            if (isEnabled) {
                tracer!!.counter("UiDispatcher:QueueSize", toRunTrampolined.size.toLong())
            }
            if (!scheduledTrampolineDispatch) {
                scheduledTrampolineDispatch = true
                handler.post(dispatchCallback)
                if (!scheduledFrameDispatch) {
                    scheduledFrameDispatch = true
                    choreographer.postFrameCallback(dispatchCallback)
                }
            }
        }
    }

    public companion object {
        private var _tracer: UiDispatcherTracer? = null

        /**
         * Specifies a tracer that instruments task dispatch and frame callbacks. The tracer is
         * global: it applies to every [AndroidUiDispatcher] instance in the process, and replaces
         * any tracer installed earlier. Call [removeTracer] to stop tracing.
         *
         * The dispatcher reads the tracer without synchronization, so a change may not reach other
         * threads immediately and may miss events already in flight.
         */
        public fun setTracer(tracer: UiDispatcherTracer) {
            _tracer = tracer
        }

        /**
         * Removes the tracer installed by [setTracer], if any.
         *
         * As with [setTracer], other threads may not see the change immediately.
         */
        public fun removeTracer() {
            _tracer = null
        }

        internal val tracer: UiDispatcherTracer?
            get() = _tracer

        /**
         * The [CoroutineContext] containing the [AndroidUiDispatcher] and its [frameClock] for the
         * process's main thread.
         */
        public val Main: CoroutineContext by
            lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
                val dispatcher =
                    AndroidUiDispatcher(
                        if (isMainThread()) Choreographer.getInstance()
                        else runBlocking(Dispatchers.Main) { Choreographer.getInstance() },
                        HandlerCompat.createAsync(Looper.getMainLooper()),
                    )

                dispatcher + dispatcher.frameClock
            }

        private val currentThread: ThreadLocal<CoroutineContext> =
            object : ThreadLocal<CoroutineContext>() {
                override fun initialValue(): CoroutineContext =
                    AndroidUiDispatcher(
                            Choreographer.getInstance(),
                            HandlerCompat.createAsync(
                                Looper.myLooper() ?: error("no Looper on this thread")
                            ),
                        )
                        .let { it + it.frameClock }
            }

        /**
         * The canonical [CoroutineContext] containing the [AndroidUiDispatcher] and its
         * [frameClock] for the calling thread. Returns [Main] if accessed from the process's main
         * thread.
         *
         * Throws [IllegalStateException] if the calling thread does not have both a [Choreographer]
         * and an active [Looper].
         */
        public val CurrentThread: CoroutineContext
            get() =
                if (isMainThread()) Main
                else {
                    currentThread.get() ?: error("no AndroidUiDispatcher for this thread")
                }
    }
}

private fun isMainThread() = Looper.myLooper() === Looper.getMainLooper()

/**
 * Receives trace events from [AndroidUiDispatcher], for tools that record how tasks and frame
 * callbacks are queued and run. Install one with [AndroidUiDispatcher.setTracer].
 *
 * The dispatcher may call [instant] and [counter] from any thread, since tasks reach it from
 * arbitrary threads, and calls [beginSection] and [endSection] on its own thread. It calls all of
 * them once per dispatched task, so implementations should not be expensive.
 *
 * Event names, counter names and flow IDs reflect the dispatcher's internal behavior and may change
 * between releases.
 */
public interface UiDispatcherTracer {
    /** Emits an instant event named [name], associated with [flowIds]. */
    public fun instant(name: String, flowIds: LongList)

    /**
     * Begins a trace section named [name], associated with [flowIds]. Match this with a
     * corresponding [endSection] on the same thread.
     */
    public fun beginSection(name: String, flowIds: LongList)

    /** Ends the trace section that [beginSection] most recently started on this thread. */
    public fun endSection()

    /** Records [value] for the counter track named [name]. */
    public fun counter(name: String, value: Long)

    /**
     * Returns `true` if tracing is currently active.
     *
     * The dispatcher calls this before doing any work to build trace arguments, so it should not be
     * expensive.
     */
    public fun isEnabled(): Boolean
}
