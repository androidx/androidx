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

package androidx.camera.testing.impl.mocks

import androidx.annotation.GuardedBy
import androidx.camera.core.ImageCapture.ScreenFlashListener
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** A mock implementations of [ScreenFlashListener] for testing purpose. */
public class MockScreenFlashListener : ScreenFlashListener {
    private val lock = Object()

    @GuardedBy("lock") private var completeCount: Int = 0
    private val completeLatch = CountDownLatch(1)

    override fun onCompleted() {
        synchronized(lock) { completeCount++ }
        completeLatch.countDown()
    }

    /** Gets the number of times [onCompleted] was invoked. */
    public fun getCompleteCount(): Int = synchronized(lock) { completeCount }

    /**
     * Waits for [onCompleted] to be invoked once.
     *
     * @param timeoutInMillis The timeout of waiting in milliseconds.
     * @return True if [onCompleted] was invoked, false if timed out.
     */
    public fun awaitComplete(timeoutInMillis: Long): Boolean {
        return completeLatch.await(timeoutInMillis, TimeUnit.MILLISECONDS)
    }
}
