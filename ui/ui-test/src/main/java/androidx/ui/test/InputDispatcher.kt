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

import androidx.ui.core.Duration
import androidx.ui.core.inMilliseconds
import androidx.ui.lerp

internal interface InputDispatcher {
    /**
     * Sends a click event at coordinate ([x], [y]). There will be 10ms in between the down and
     * the up event. This method blocks until all input events have been dispatched.
     *
     * @param x The x coordinate of the click
     * @param y The y coordinate of the click
     */
    fun sendClick(x: Float, y: Float)

    /**
     * Sends a swipe gesture from ([x0], [y0]) to ([x1], [y1]) with the given [duration]. This
     * method blocks until all input events have been dispatched.
     *
     * @param x0 The x coordinate of the start of the gesture
     * @param y0 The y coordinate of the start of the gesture
     * @param x1 The x coordinate of the end of the gesture
     * @param y1 The y coordinate of the end of the gesture
     * @param duration The duration of the gesture
     */
    fun sendSwipe(x0: Float, y0: Float, x1: Float, y1: Float, duration: Duration) {
        val durationFloat = duration.inMilliseconds().toFloat()
        sendSwipe(
            fx = { lerp(x0, x1, it / durationFloat) },
            fy = { lerp(y0, y1, it / durationFloat) },
            duration = duration
        )
    }

    /**
     * Sends a swipe gesture from `(fx(0), fy(0))` to `(fx(duration), fy(duration))`, following
     * the route defined by [fx] and [fy]. Will force sampling of an event at all times defined
     * in [keyTimes]. The number of events sampled between the key times is implementation
     * dependent. This method blocks until all input events have been dispatched.
     *
     * @param fx The function that defines the x coordinate of the gesture over time
     * @param fy The function that defines the y coordinate of the gesture over time
     * @param duration The duration of the gesture
     * @param keyTimes An optional list of timestamps in milliseconds at which a move event must
     * be sampled
     */
    fun sendSwipe(
        fx: (Long) -> Float,
        fy: (Long) -> Float,
        duration: Duration,
        keyTimes: List<Long> = emptyList()
    )

    // TODO(b/145593518): how to solve reproducible chaining of gestures?
    // TODO(b/145593752): how to solve multi-touch?
}
