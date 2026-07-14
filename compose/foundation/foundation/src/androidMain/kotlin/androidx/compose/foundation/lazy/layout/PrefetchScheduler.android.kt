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

package androidx.compose.foundation.lazy.layout

import android.os.Build
import android.view.Choreographer
import android.view.Display
import android.view.View
import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.util.trace
import androidx.compose.ui.util.traceValue
import java.util.PriorityQueue
import java.util.concurrent.TimeUnit
import kotlin.math.max

@Suppress("DEPRECATION") // b/420551535
@ExperimentalFoundationApi
@Composable
internal actual fun rememberDefaultPrefetchScheduler(): PrefetchScheduler {
    return if (isRobolectric) {
        // Robolectric is reporting incorrect frame start time, so we have to completely
        // disable prefetch on it.
        remember { noopScheduler() }
    } else {
        val view = LocalView.current
        remember(view) {
            val existing = view.getTag(R.id.compose_prefetch_scheduler) as? PrefetchScheduler
            if (existing == null) {
                val scheduler = AndroidPrefetchScheduler(view)
                view.setTag(R.id.compose_prefetch_scheduler, scheduler)
                scheduler
            } else {
                existing
            }
        }
    }
}

/**
 * Android specific prefetch implementation. The only platform specific things are:
 * 1) Calculating the deadline
 * 2) Posting the delayed runnable This could be refactored in the future in order to keep the most
 *    logic platform agnostic to enable the prefetching on desktop.
 *
 * The differences with the implementation in RecyclerView:
 * 1) Prefetch is per-list-index, and performed on whole item. With RecyclerView, nested scrolling
 *    RecyclerViews would prefetch incrementally, e.g. items like the following in a scrolling
 *    vertical list could be broken up within a frame: `[Row1 [a], [b], [c]]` `[Row2 [d], [e]]`
 *    `[Row3 [f], [g], [h]]` You could have frames that break up this work arbitrarily: Frame 1 -
 *    prefetch `[a]` Frame 2 - prefetch `[b]`, `[c]` Frame 3 - prefetch `[d]` Frame 4 - prefetch
 *    `[e]`, `[f]` Something similar is not possible with LazyColumn yet.
 * 2) Prefetching time estimation only captured during the prefetch. We currently don't track the
 *    time of the regular subcompose call happened during the regular measure pass, only the ones
 *    which are done during the prefetching. The downside is we build our prefetch information only
 *    after scrolling has started and items are showing up. Your very first scroll won't know if
 *    it's safe to prefetch. Why: a) SubcomposeLayout is not exposing an API to understand if
 *    subcompose() call is going to do the real work. The work could be skipped if the same lambda
 *    was passed as for the previous invocation or if there were no recompositions scheduled. We
 *    could workaround it by keeping the extra state in LazyListState about what items we already
 *    composed and to only measure the first composition for the given slot, or consider exposing
 *    extra information in SubcomposeLayoutState API. b) It allows us to nicely decouple the logic,
 *    now the prefetching logic is build on top of the regular LazyColumn measuring functionallity
 *    and the main logic knows nothing about prefetch c) Maybe the better approach would be to wait
 *    till the low-level runtime infra is ready to do subcompositions on the different threads which
 *    illuminates the need to calculate the deadlines completely. Tracking bug: b/187393381.
 * 3) Prefetch is not aware of item type. RecyclerView separates timing metadata about different
 *    item types. For example, in play store style UI, this allows RecyclerView to separately
 *    estimate the cost of a header, separator, and item row. In this implementation, all of these
 *    would be averaged together in the same estimation. There is no view type concept in LazyColumn
 *    at all. What can we possible do: a) Think of different item/items calls in the builder dsl as
 *    different view types automatically. It is close enough but still not entirely correct if the
 *    user have something like a list of elements which are objects of some sealed classes and they
 *    consider different classes as completely different types b) Maybe if we would be able to
 *    precompose on the different thread this issue is also not so critical given that we don't need
 *    to calculate the deadline. Tracking bug: 187393922
 */
@Suppress("DEPRECATION") // b/420551535
@ExperimentalFoundationApi
internal class AndroidPrefetchScheduler(private val view: View) :
    PrefetchScheduler,
    PriorityPrefetchScheduler,
    View.OnAttachStateChangeListener,
    Runnable,
    Choreographer.FrameCallback {

    /**
     * The list of currently not processed prefetch requests. The requests will be processed one by
     * during subsequent [run]s.
     */
    private val prefetchRequests =
        PriorityQueue<PriorityTask>(11, Comparator { a, b -> b.priority.compareTo(a.priority) })
    private var prefetchScheduled = false
    private val choreographer = Choreographer.getInstance()
    private val scope = PrefetchRequestScopeImpl()

    /** Is true when LazyList was composed and not yet disposed. */
    private var isActive = false

    private var frameStartTimeNanos = 0L
    private var lastDrawingTimeNanos = 0L
    private val idleSlack
        get() = 2 * frameIntervalNs

    init {
        calculateFrameIntervalIfNeeded(view)
        view.addOnAttachStateChangeListener(this)
        // if the view is already attached to a window, call onViewAttachedToWindow immediately.
        if (view.isAttachedToWindow) onViewAttachedToWindow(view)
    }

    /**
     * Callback to be executed when the prefetching is needed. [prefetchRequests] will be used as an
     * input.
     */
    override fun run() {
        if (
            prefetchRequests.isEmpty() ||
                !prefetchScheduled ||
                !isActive ||
                view.windowVisibility != View.VISIBLE
        ) {
            // incorrect input. ignore
            prefetchScheduled = false
            return
        }
        if (ComposeFoundationFlags.isPrefetchSchedulerLateFrameDetectionEnabled) {
            runNewBehavior()
        } else {
            runOldBehavior()
        }
    }

    private fun runNewBehavior() {
        // viewDrawTimeNanos is the latter between the last drawing time, and the frame start time.
        var viewDrawTimeNanos =
            maxOf(frameStartTimeNanos, TimeUnit.MILLISECONDS.toNanos(view.drawingTime))
        // We calculate how many nanoseconds have elapsed
        val elapsedSinceDraw = System.nanoTime() - viewDrawTimeNanos
        var isFrameIdle = false
        // Prefents us from posting with `view.post(this)` more than we need to
        var alreadyPostScheduled = false

        val frameIsIdleCandidate = elapsedSinceDraw > idleSlack

        // If this is true, it means that at this point, the frame is either extremely delayed, or
        // we have not drawn something for our idle slack duration because we are actually idle.
        // If this is false, it means that it has not been long enough since our last draw time to
        // qualify for a potentially idle frame, in which case, we will run the prefetch loop using
        // the time remaining before we use up our available time allowance.
        if (frameIsIdleCandidate) {
            // As this callback runs on `view.post(this)`, if we compare the last recorded drawing
            // time with the latest drawing time, and they are essentially the same, it means that
            // we are truly idle as we have both exhausted our idle slack and used no more time
            // doing other main thread things between this invocation and the previous.
            if (lastDrawingTimeNanos == viewDrawTimeNanos) {
                // If we are idle, we set the draw time to the current time such that we can
                // prefetch for a specific duration starting from the current time.
                viewDrawTimeNanos = System.nanoTime()
                isFrameIdle = true
            } else {
                lastDrawingTimeNanos = viewDrawTimeNanos
                view.post(this)
                alreadyPostScheduled = true
            }
        }

        val scheduleForNextFrame = runPrefetchLoop(viewDrawTimeNanos, isFrameIdle)

        if (!alreadyPostScheduled) {
            if (scheduleForNextFrame) {
                // there is not enough time left in this frame. we schedule a next frame callback
                // in which we are going to post the message in the handler again.
                choreographer.postFrameCallback(this)
            } else {
                prefetchScheduled = false
            }
        }
    }

    private fun runPrefetchLoop(viewDrawTimeNanos: Long, isFrameIdle: Boolean): Boolean {
        var frameIsIdle = isFrameIdle
        scope.nextFrameTimeNs = viewDrawTimeNanos + frameIntervalNs
        var scheduleForNextFrame = false

        while (prefetchRequests.isNotEmpty()) {
            val availableTimeNanos = scope.availableTimeNanos()
            if (availableTimeNanos <= 0) {
                scheduleForNextFrame = true
                break
            }

            traceValue("compose:lazy:prefetch:available_time_nanos", availableTimeNanos)
            val hasMoreWork =
                if (frameIsIdle) {
                    frameIsIdle = false
                    trace("compose:lazy:prefetch:idle_frame") { runRequest() }
                } else {
                    runRequest()
                }

            if (hasMoreWork) {
                scheduleForNextFrame = true
                break
            }
        }
        return scheduleForNextFrame
    }

    private fun runOldBehavior() {
        val viewDrawTimeNanos = TimeUnit.MILLISECONDS.toNanos(view.drawingTime)

        // enter idle mode if the last time we draw was 2 frames ago.
        scope.isFrameIdle = (System.nanoTime() > viewDrawTimeNanos + 2 * frameIntervalNs)
        scope.nextFrameTimeNs = maxOf(frameStartTimeNanos, viewDrawTimeNanos) + frameIntervalNs
        var scheduleForNextFrame = false
        while (prefetchRequests.isNotEmpty() && !scheduleForNextFrame) {
            scheduleForNextFrame =
                if (scope.isFrameIdle) {
                    trace("compose:lazy:prefetch:idle_frame") { runRequestOld() }
                } else {
                    runRequestOld()
                }
        }

        if (scheduleForNextFrame) {
            // there is not enough time left in this frame. we schedule a next frame callback
            // in which we are going to post the message in the handler again.
            choreographer.postFrameCallback(this)
        } else {
            prefetchScheduled = false
        }
        traceValue("compose:lazy:prefetch:available_time_nanos", 0L) // reset counter
    }

    private fun runRequest(): Boolean {
        // at this point we know that prefetchRequests is not empty.
        val request = prefetchRequests.peek()!!.request
        val hasMoreWorkToDo = with(request) { scope.execute() }
        if (!hasMoreWorkToDo) {
            prefetchRequests.poll()
        }
        return hasMoreWorkToDo
    }

    private fun runRequestOld(): Boolean {
        var scheduleForNextFrame = false
        val availableTimeNanos = scope.availableTimeNanos()
        traceValue("compose:lazy:prefetch:available_time_nanos", availableTimeNanos)
        if (availableTimeNanos > 0) {
            // at this point we know that prefetchRequests is not empty.
            val request = prefetchRequests.peek()!!.request
            val hasMoreWorkToDo = with(request) { scope.execute() }
            if (hasMoreWorkToDo) {
                scheduleForNextFrame = true
            } else {
                prefetchRequests.poll()
            }
            scope.isFrameIdle = false // reset idle state for subsequent requests.
        } else {
            scheduleForNextFrame = true
        }
        return scheduleForNextFrame
    }

    /**
     * Choreographer frame callback. It will be called when during the previous frame we didn't have
     * enough time left. We will post a new message in the handler in order to try to prefetch again
     * after this frame.
     */
    override fun doFrame(frameTimeNanos: Long) {
        if (isActive) {
            frameStartTimeNanos = frameTimeNanos
            view.post(this)
        }
    }

    private fun startExecution() {
        if (!prefetchScheduled) {
            prefetchScheduled = true
            // schedule the prefetching
            view.post(this)
        }
    }

    override fun scheduleLowPriorityPrefetch(prefetchRequest: PrefetchRequest) {
        prefetchRequests.add(PriorityTask(PriorityTask.Low, prefetchRequest))
        startExecution()
    }

    override fun scheduleHighPriorityPrefetch(prefetchRequest: PrefetchRequest) {
        prefetchRequests.add(PriorityTask(PriorityTask.High, prefetchRequest))
        startExecution()
    }

    override fun onViewAttachedToWindow(v: View) {
        isActive = true
    }

    override fun onViewDetachedFromWindow(v: View) {
        isActive = false
        view.removeCallbacks(this)
        choreographer.removeFrameCallback(this)
    }

    class PrefetchRequestScopeImpl() : PrefetchRequestScope {
        var isFrameIdle: Boolean = false
        var nextFrameTimeNs: Long = 0L

        override fun availableTimeNanos() =
            if (ComposeFoundationFlags.isPrefetchSchedulerLateFrameDetectionEnabled) {
                max(0, nextFrameTimeNs - System.nanoTime())
            } else {
                if (isFrameIdle) {
                    Long.MAX_VALUE
                } else {
                    max(0, nextFrameTimeNs - System.nanoTime())
                }
            }
    }

    companion object {

        /**
         * The static cache in order to not gather the display refresh rate to often (expensive
         * operation).
         */
        private var frameIntervalNs: Long = 0

        private fun calculateFrameIntervalIfNeeded(view: View) {
            // we only do this query once, statically, because it's very expensive (> 1ms)
            if (frameIntervalNs == 0L) {
                val display: Display? = view.display
                var refreshRate = 60f
                if (!view.isInEditMode && display != null) {
                    val displayRefreshRate = display.refreshRate
                    if (displayRefreshRate >= 30f) {
                        // break 60 fps assumption if data from display appears valid
                        refreshRate = displayRefreshRate
                    }
                }
                frameIntervalNs = (1000000000 / refreshRate).toLong()
            }
        }
    }
}

private val isRobolectric
    get() = Build.FINGERPRINT != null && Build.FINGERPRINT == "robolectric"

@Suppress("DEPRECATION") // b/420551535
@ExperimentalFoundationApi
private fun noopScheduler() =
    object : PrefetchScheduler {
        override fun schedulePrefetch(prefetchRequest: PrefetchRequest) {
            // do nothing
        }
    }

@Suppress("DEPRECATION") // b/420551535
@ExperimentalFoundationApi
internal class PriorityTask(val priority: Int, val request: PrefetchRequest) {
    companion object {
        val Low = 0
        val High = 1
    }
}
