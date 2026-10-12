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

package androidx.appsearch.localstorage.lock;

import static com.google.common.truth.Truth.assertWithMessage;

import android.os.SystemClock;

import org.jspecify.annotations.NonNull;

/** Shared synchronization utilities for {@link UpgradeableReadWriteLock} tests. */
class UpgradeableReadWriteLockTestHelper {
    private static final long WAIT_TIMEOUT_MILLIS = 10_000;

    private UpgradeableReadWriteLockTestHelper() {}

    /**
     * Waits until the given thread is queued waiting to acquire one of the internal locks of the
     * given {@link UpgradeableReadWriteLock}.
     *
     * <p>This uses {@link UpgradeableReadWriteLock#hasQueuedThread(Thread)} as the synchronization
     * barrier, which checks the lock's wait queue directly instead of {@link Thread#getState()}.
     * Unlike a thread state check, it does not return early when the thread is waiting on an
     * unrelated synchronizer (e.g. a latch), and it is not affected by how the runtime maps
     * internal thread states.
     */
    static void waitUntilThreadQueued(
            @NonNull UpgradeableReadWriteLock lock, @NonNull Thread thread) {
        long deadline = SystemClock.elapsedRealtime() + WAIT_TIMEOUT_MILLIS;
        while (!lock.hasQueuedThread(thread) && SystemClock.elapsedRealtime() < deadline) {
            Thread.yield();
        }
        assertWithMessage(
                        "Thread %s (state=%s) is not queued on the lock",
                        thread.getName(), thread.getState())
                .that(lock.hasQueuedThread(thread))
                .isTrue();
    }
}
