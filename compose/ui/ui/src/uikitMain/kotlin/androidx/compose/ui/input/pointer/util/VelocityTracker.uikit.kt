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

package androidx.compose.ui.input.pointer.util

import kotlin.math.abs

internal actual const val AssumePointerMoveStoppedMilliseconds: Int = 100
internal actual const val HistorySize: Int = 40 // Increased to store history on 120 Hz devices

private const val MinimumGestureDurationMilliseconds: Int = 50
private const val MinimumGestureSpeed: Float = 1.0f // Minimum tracking speed, dp/ms

/**
 * Some platforms (e.g. iOS) filter certain gestures during velocity calculation.
 */
internal actual fun VelocityTracker1D.shouldUseDataPoints(
    points: FloatArray,
    times: FloatArray,
    count: Int
): Boolean {
    if (count == 0) {
        return false
    }

    val timeDelta = abs(times[0] - times[count - 1])
    if (timeDelta < MinimumGestureDurationMilliseconds) {
        return false
    }

    val distance = abs(points[0] - points[count - 1])
    if (distance / timeDelta < MinimumGestureSpeed) {
        return false
    }

    return true
}
