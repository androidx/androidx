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

package androidx.ui.test

import androidx.ui.unit.Duration
import androidx.ui.unit.PxPosition
import androidx.ui.unit.inMilliseconds
import androidx.ui.unit.lerp

internal interface InputDispatcher {
    /**
     * Sends a click event at [position]. There will be 10ms in between the down and the up
     * event. This method blocks until all input events have been dispatched.
     *
     * @param position The coordinate of the click
     */
    fun sendClick(position: PxPosition)

    /**
     * Sends a swipe gesture from [start] to [end] with the given [duration]. This method blocks
     * until all input events have been dispatched.
     *
     * @param start The start position of the gesture
     * @param end The end position of the gesture
     * @param duration The duration of the gesture
     */
    fun sendSwipe(start: PxPosition, end: PxPosition, duration: Duration) {
        val durationFloat = duration.inMilliseconds().toFloat()
        sendSwipe(
            curve = { lerp(start, end, it / durationFloat) },
            duration = duration
        )
    }

    /**
     * Sends a swipe gesture from `curve(0)` to `curve([duration])`, following the route defined
     * by [curve]. Will force sampling of an event at all times defined in [keyTimes]. The number
     * of events sampled between the key times is implementation dependent. This method blocks
     * until all input events have been dispatched.
     *
     * @param curve The function that defines the position of the gesture over time
     * @param duration The duration of the gesture
     * @param keyTimes An optional list of timestamps in milliseconds at which a move event must
     * be sampled
     */
    fun sendSwipe(
        curve: (Long) -> PxPosition,
        duration: Duration,
        keyTimes: List<Long> = emptyList()
    )

    // TODO(b/145593518): how to solve reproducible chaining of gestures?
    // TODO(b/145593752): how to solve multi-touch?
}
