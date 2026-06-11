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

package androidx.appstate.transform

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.MonotonicFrameClock
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// TODO: remove this once we get the desired clock implementation
internal class GatedFrameClock(scope: CoroutineScope, context: CoroutineContext) :
    MonotonicFrameClock {

    var isRunning: Boolean = true
        set(value) {
            val started = value && !field
            field = value
            if (started) {
                sendFrame()
            }
        }

    private var lastNanos = 0L
    private var lastOffset = 0

    private fun sendFrame() {
        val timeNanos = System.nanoTime()

        // Since we only have millisecond resolution, ensure the nanos form always increases by
        // incrementing a nano offset if we collide with the previous timestamp.
        val offset =
            if (timeNanos == lastNanos) {
                lastOffset + 1
            } else {
                lastNanos = timeNanos
                0
            }
        lastOffset = offset

        clock.sendFrame(timeNanos + offset)
    }

    private val clock = BroadcastFrameClock {
        if (isRunning) {
            scope.launch(context) { sendFrame() }
        }
    }

    override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
        return clock.withFrameNanos(onFrame)
    }
}
