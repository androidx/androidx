/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.test.android

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import androidx.ui.core.Duration
import androidx.ui.core.SemanticsTreeProvider
import androidx.ui.core.inMilliseconds
import androidx.ui.core.milliseconds
import androidx.ui.lerp
import androidx.ui.test.InputDispatcher
import java.util.concurrent.CountDownLatch
import kotlin.math.min
import kotlin.math.roundToInt

internal class AndroidInputDispatcher(
    private val treeProvider: SemanticsTreeProvider
) : InputDispatcher {
    /**
     * The minimum time between two successive injected MotionEvents. Ideally, the value should
     * reflect a realistic pointer input sample rate, but that depends on too many factors. Instead,
     * the value is chosen comfortably below the targeted frame rate (60 fps, equating to a 16ms
     * period).
     */
    private val eventPeriod = 10.milliseconds.inMilliseconds()

    private val handler = Handler(Looper.getMainLooper())

    override fun sendClick(x: Float, y: Float) {
        val downTime = SystemClock.uptimeMillis()
        treeProvider.sendMotionEvent(downTime, downTime, MotionEvent.ACTION_DOWN, x, y)
        treeProvider.sendMotionEvent(downTime, downTime + eventPeriod, MotionEvent.ACTION_UP, x, y)
    }

    override fun sendSwipe(x0: Float, y0: Float, x1: Float, y1: Float, duration: Duration) {
        var step = 0
        val steps = min(1, (duration.inMilliseconds() / eventPeriod.toFloat()).roundToInt())
        val downTime = SystemClock.uptimeMillis()
        val upTime = downTime + duration.inMilliseconds()

        treeProvider.sendMotionEvent(downTime, downTime, MotionEvent.ACTION_DOWN, x0, y0)
        while (step++ < steps) {
            val progress = step / steps.toFloat()
            val t = lerp(downTime, upTime, progress)
            val x = lerp(x0, x1, progress)
            val y = lerp(y0, y1, progress)
            treeProvider.sendMotionEvent(downTime, t, MotionEvent.ACTION_MOVE, x, y)
        }
        treeProvider.sendMotionEvent(downTime, upTime, MotionEvent.ACTION_UP, x1, y1)
    }

    /**
     * Sends an event with the given parameters. Method blocks depending on [waitUntilEventTime].
     * @param waitUntilEventTime If `true`, blocks until [eventTime]
     */
    private fun SemanticsTreeProvider.sendMotionEvent(
        downTime: Long,
        eventTime: Long,
        action: Int,
        x: Float,
        y: Float,
        waitUntilEventTime: Boolean = true
    ) {
        if (waitUntilEventTime) {
            val currTime = SystemClock.uptimeMillis()
            if (currTime < eventTime) {
                SystemClock.sleep(eventTime - currTime)
            }
        }
        sendAndRecycleEvent(MotionEvent.obtain(downTime, eventTime, action, x, y, 0))
    }

    /**
     * Sends the [event] to the [SemanticsTreeProvider] and [recycles][MotionEvent.recycle] it
     * regardless of the result. This method blocks until the event is sent.
     */
    private fun SemanticsTreeProvider.sendAndRecycleEvent(event: MotionEvent) {
        val latch = CountDownLatch(1)
        handler.post {
            try {
                sendEvent(event)
            } finally {
                event.recycle()
                latch.countDown()
            }
        }
        latch.await()
    }
}