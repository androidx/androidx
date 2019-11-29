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

internal interface InputDispatcher {
    /**
     * Sends a click event at coordinate ([x], [y]). There will be 10ms in between the down and
     * the up event. This method blocks until all input events have been dispatched.
     */
    fun sendClick(x: Float, y: Float)

    /**
     * Sends a swipe gesture from ([x0], [y0]) to ([x1], [y1]) with the given [duration]. This
     * method blocks until all input events have been dispatched.
     */
    fun sendSwipe(x0: Float, y0: Float, x1: Float, y1: Float, duration: Duration)
}
