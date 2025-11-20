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

package androidx.camera.testing.impl

import kotlinx.coroutines.CompletableDeferred

/** A deferred implementation of a countdown latch. */
public class CountdownDeferred(public val count: Int) {

    private val deferredItems =
        mutableListOf<CompletableDeferred<Unit>>().apply {
            repeat(count) { add(CompletableDeferred()) }
        }
    private var index = 0

    /**
     * Decrements the count of the countdown latch.
     *
     * @throws IllegalStateException if the countdown latch has already reached zero.
     */
    public fun countDown() {
        if (index < count) {
            deferredItems[index++].complete(Unit)
        } else {
            throw IllegalStateException("Countdown already finished")
        }
    }

    /** Suspends until the countdown reaches zero. */
    public suspend fun await() {
        deferredItems.forEach { it.await() }
    }
}
