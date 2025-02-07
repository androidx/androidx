/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.compose.foundation.lazy.layout

import android.os.Build
import android.view.Choreographer
import android.view.Display
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import java.util.concurrent.TimeUnit
import kotlin.math.max

/**
 * Remembers the platform-specific implementation for scheduling lazy layout item prefetch
 * (pre-composing next items in advance during the scrolling).
 */
@Composable
internal fun rememberDefaultPrefetchScheduler(): PrefetchScheduler {
    return if (RobolectricImpl != null) {
        RobolectricImpl
    } else {
        val view = LocalView.current
        remember(view) { AndroidPrefetchScheduler(view) }
    }
}

/**
 * Implementations of this interface accept prefetch requests via [schedulePrefetch] and decide when
 * to execute them in a way that will have minimal impact on user experience, e.g. during frame idle
 * time.
 *
 * Requests should be executed by invoking [PrefetchRequest.execute]. The implementation of
 * [PrefetchRequest.execute] will return `false` when all work for that request is done, or `true`
 * when it still has more to do but doesn't think it can complete it within
 * [PrefetchRequestScope.availableTimeNanos].
 */
internal interface PrefetchScheduler {

    /**
     * Accepts a prefetch request. Implementations should find a time to execute them which will
     * have minimal impact on user experience.
     */
    fun schedulePrefetch(prefetchRequest: PrefetchRequest)
}

/**
 * A request for prefetch which can be submitted to a [PrefetchScheduler] to execute during idle
 * time.
 */
internal sealed interface PrefetchRequest {

    /**
     * Gives this request a chance to execute work. It should only do work if it thinks it can
     * finish it within [PrefetchRequestScope.availableTimeNanos].
     *
     * @return whether this request has more work it wants to do, but ran out of time. `true`
     *   indicates this request wants to have [execute] called again to do more work, while `false`
     *   indicates its work is complete.
     */
    fun PrefetchRequestScope.execute(): Boolean
}

/**
 * Scope for [PrefetchRequest.execute], supplying info about how much time it has to execute
 * requests.
 */
internal interface PrefetchRequestScope {

    /**
     * How much time is available to do prefetch work. Implementations of [PrefetchRequest] should
     * do their best to fit their work into this time without going over.
     */
    fun availableTimeNanos(): Long
}

internal class AndroidPrefetchScheduler(private val view: View) :
    PrefetchScheduler, RememberObserver, Runnable, Choreographer.FrameCallback {

    /**
     * The list of currently not processed prefetch requests. The requests will be processed one by
     * during subsequent [run]s.
     */
    private val prefetchRequests = mutableVectorOf<PrefetchRequest>()
    private var prefetchScheduled = false
    private val choreographer = Choreographer.getInstance()

    /** Is true when LazyList was composed and not yet disposed. */
    private var isActive = false

    private var frameStartTimeNanos = 0L

    init {
        calculateFrameIntervalIfNeeded(view)
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
        // Use both the view drawing time or the frameStartTime given by the choreographer.
        // In most cases the view drawing time should be enough and equal to the frame start
        // time given by the choreographer. These are the cases where they should differ:
        // 1) When this handler is executed in the same frame as it was scheduled. In these cases,
        // using view drawing time will be correct because scheduling is usually followed by a
        // drawing operation as it happens during scroll.
        // 2) When there wasn't enough time to complete a request in the current frame. If there
        // isn't enough time, the handler will be executed in the next frame where there might
        // not have been a drawing operation. Using the choreographer frame start time will be
        // safe in these cases.
        val viewDrawTimeNanos = TimeUnit.MILLISECONDS.toNanos(view.drawingTime)
        val nextFrameNs = maxOf(frameStartTimeNanos, viewDrawTimeNanos) + frameIntervalNs
        val scope = PrefetchRequestScopeImpl(nextFrameNs)
        var scheduleForNextFrame = false
        while (prefetchRequests.isNotEmpty() && !scheduleForNextFrame) {
            if (scope.availableTimeNanos() > 0) {
                val request = prefetchRequests[0]
                val hasMoreWorkToDo = with(request) { scope.execute() }
                if (hasMoreWorkToDo) {
                    scheduleForNextFrame = true
                } else {
                    prefetchRequests.removeAt(0)
                }
            } else {
                scheduleForNextFrame = true
            }
        }

        if (scheduleForNextFrame) {
            // there is not enough time left in this frame. we schedule a next frame callback
            // in which we are going to post the message in the handler again.
            choreographer.postFrameCallback(this)
        } else {
            prefetchScheduled = false
        }
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

    override fun schedulePrefetch(prefetchRequest: PrefetchRequest) {
        prefetchRequests.add(prefetchRequest)
        if (!prefetchScheduled) {
            prefetchScheduled = true
            // schedule the prefetching
            view.post(this)
        }
    }

    override fun onRemembered() {
        isActive = true
    }

    override fun onForgotten() {
        isActive = false
        view.removeCallbacks(this)
        choreographer.removeFrameCallback(this)
    }

    override fun onAbandoned() {}

    class PrefetchRequestScopeImpl(
        private val nextFrameTimeNs: Long,
    ) : PrefetchRequestScope {

        override fun availableTimeNanos() = max(0, nextFrameTimeNs - System.nanoTime())
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

private val RobolectricImpl =
    if (Build.FINGERPRINT.lowercase() == "robolectric") {
        object : PrefetchScheduler {
            override fun schedulePrefetch(prefetchRequest: PrefetchRequest) {
                // Robolectric is reporting incorrect frame start time, so we have to completely
                // disable prefetch on it.
            }
        }
    } else {
        null
    }
