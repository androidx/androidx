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
import android.util.Log
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_POINTER_DOWN
import android.view.MotionEvent.ACTION_POINTER_UP
import android.view.MotionEvent.ACTION_UP
import androidx.compose.ui.geometry.Offset
import androidx.ui.test.InputDispatcher
import androidx.ui.test.PartialGesture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal class AndroidInputDispatcher(
    private val sendEvent: (MotionEvent) -> Unit
) : InputDispatcher() {

    private val handler = Handler(Looper.getMainLooper())

    override val now: Long get() = SystemClock.uptimeMillis()

    override fun PartialGesture.sendDown(pointerId: Int) {
        sendMotionEvent(
            if (lastPositions.size() == 1) ACTION_DOWN else ACTION_POINTER_DOWN,
            lastPositions.indexOfKey(pointerId)
        )
    }

    override fun PartialGesture.sendMove() {
        sendMotionEvent(ACTION_MOVE, 0)
    }

    override fun PartialGesture.sendUp(pointerId: Int) {
        sendMotionEvent(
            if (lastPositions.size() == 1) ACTION_UP else ACTION_POINTER_UP,
            lastPositions.indexOfKey(pointerId)
        )
    }

    override fun PartialGesture.sendCancel() {
        sendMotionEvent(ACTION_CANCEL, 0)
    }

    /**
     * Sends a MotionEvent with the given [action] and [actionIndex], adding all pointers that
     * are currently in the gesture.
     *
     * @see MotionEvent.getAction
     * @see MotionEvent.getActionIndex
     */
    private fun PartialGesture.sendMotionEvent(action: Int, actionIndex: Int) {
        sendMotionEvent(
            downTime,
            lastEventTime,
            action,
            actionIndex,
            List(lastPositions.size()) { lastPositions.valueAt(it) },
            List(lastPositions.size()) { lastPositions.keyAt(it) }
        )
    }

    /**
     * Sends an event with the given parameters.
     */
    private fun sendMotionEvent(
        downTime: Long,
        eventTime: Long,
        action: Int,
        actionIndex: Int,
        coordinates: List<Offset>,
        pointerIds: List<Int>
    ) {
        sleepUntil(eventTime)
        sendAndRecycleEvent(
            MotionEvent.obtain(
                /* downTime = */ downTime,
                /* eventTime = */ eventTime,
                /* action = */ action + (actionIndex shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                /* pointerCount = */ coordinates.size,
                /* pointerProperties = */ Array(coordinates.size) {
                    MotionEvent.PointerProperties().apply { id = pointerIds[it] }
                },
                /* pointerCoords = */ Array(coordinates.size) {
                    MotionEvent.PointerCoords().apply {
                        x = coordinates[it].x
                        y = coordinates[it].y
                    }
                },
                /* metaState = */ 0,
                /* buttonState = */ 0,
                /* xPrecision = */ 0f,
                /* yPrecision = */ 0f,
                /* deviceId = */ 0,
                /* edgeFlags = */ 0,
                /* source = */ 0,
                /* flags = */ 0
            )
        )
    }

    /**
     * Sends the [event] to the MotionEvent dispatcher and [recycles][MotionEvent.recycle] it
     * regardless of the result. This method blocks until the event is sent.
     */
    private fun sendAndRecycleEvent(event: MotionEvent) {
        val latch = CountDownLatch(1)
        handler.post {
            try {
                sendEvent(event)
            } finally {
                event.recycle()
                latch.countDown()
            }
        }
        if (!latch.await(5, TimeUnit.SECONDS)) {
            Log.w("AndroidInputDispatcher", "Dispatching of MotionEvent $event took longer than " +
                    "5 seconds to complete. This should typically only take a few milliseconds.")
            latch.await()
        }
    }
}
