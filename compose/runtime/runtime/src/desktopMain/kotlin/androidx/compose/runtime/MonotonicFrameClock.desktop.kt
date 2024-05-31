/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.runtime

import kotlinx.coroutines.delay

private object DefaultDelayMonotonicFrameClock : MonotonicFrameClock {
    private const val DefaultFrameDelay = 16L // milliseconds

    override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R {
        delay(DefaultFrameDelay)
        return onFrame(System.nanoTime())
    }
}

@Deprecated(
    "MonotonicFrameClocks are not globally applicable across platforms. " +
        "Use an appropriate local clock."
)
actual val DefaultMonotonicFrameClock: MonotonicFrameClock
    get() = DefaultDelayMonotonicFrameClock
