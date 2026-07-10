/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.glimmer

import androidx.compose.runtime.MonotonicFrameClock
import java.util.concurrent.atomic.AtomicLong

/**
 * A testing frame clock that immediately provides a new frame time upon request.
 *
 * This is necessary when calling suspending animation from a test coroutine (e.g., inside
 * `runBlocking`). Otherwise, methods that need a clock would throw an exception.
 *
 * Typical usage:
 * ```
 * runBlocking(Dispatchers.Main + AutoTestFrameClock()) {
 *     state.animateScrollToItem(10)
 * }
 * ```
 */
internal class AutoTestFrameClock : MonotonicFrameClock {
    private val time = AtomicLong(0)

    override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
        return onFrame(time.getAndAdd(16_000_000))
    }
}
