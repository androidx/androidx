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

package androidx.xr.scenecore.testing

import androidx.annotation.RestrictTo
import com.google.common.truth.Truth.assertThat
import java.lang.ref.WeakReference
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Utility class for memory-related testing operations. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object MemoryUtils {

    /**
     * Asserts that the given [WeakReference] is eventually cleared by garbage collection.
     *
     * @param weakRef The [WeakReference] to check.
     * @param maxAttempts The maximum number of GC attempts to make.
     * @param onAttempt Optional task to run during each attempt loop (e.g. run executors).
     */
    @JvmStatic
    @JvmOverloads
    public fun assertGarbageCollected(
        weakRef: WeakReference<*>,
        maxAttempts: Int = 10,
        onAttempt: Runnable? = null,
        afterGcCondition: (() -> Boolean)? = null,
    ) {
        var gcAttempts = 0
        while (weakRef.get() != null && gcAttempts < maxAttempts) {
            System.gc()
            System.runFinalization()
            onAttempt?.run()
            // Use a small delay to allow GC to happen
            CountDownLatch(1).await(100, TimeUnit.MILLISECONDS)
            gcAttempts++
        }
        assertThat(weakRef.get()).isNull()

        if (afterGcCondition != null) {
            var waitAttempts = 0
            while (!afterGcCondition() && waitAttempts < maxAttempts) {
                onAttempt?.run()
                // Use a small delay to allow the cleaner thread to run
                CountDownLatch(1).await(100, TimeUnit.MILLISECONDS)
                waitAttempts++
            }
            assertThat(afterGcCondition()).isTrue()
        }
    }
}
