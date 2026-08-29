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

import static com.google.common.truth.Truth.assertThat;

import android.os.SystemClock;

import org.jspecify.annotations.NonNull;

/** Shared synchronization utilities for {@link UpgradeableReadWriteLock} tests. */
class UpgradeableReadWriteLockTestHelper {
    private UpgradeableReadWriteLockTestHelper() {}

    /**
     * Waits until the given thread is blocked or waiting (i.e. WAITING, TIMED_WAITING, or BLOCKED),
     * ensuring deterministic multi-threaded test execution without arbitrary sleep delays.
     */
    static void waitUntilThreadBlocks(@NonNull Thread thread) {
        long deadline = SystemClock.elapsedRealtime() + 2000;
        while (thread.getState() == Thread.State.RUNNABLE
                && SystemClock.elapsedRealtime() < deadline) {
            Thread.yield();
        }
        assertThat(thread.getState())
                .isAnyOf(Thread.State.WAITING, Thread.State.TIMED_WAITING, Thread.State.BLOCKED);
    }
}
